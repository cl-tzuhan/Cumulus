package com.creationline.cloudstack.ui;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.creationline.cloudstack.R;
import com.creationline.cloudstack.engine.CsApiConstants;
import com.creationline.cloudstack.engine.CsRestService;
import com.creationline.cloudstack.engine.db.Vms;
import com.creationline.cloudstack.util.ClLog;

public class CsVmListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final int CSVM_LIST_LOADER = 0x01;
    
    private CsVmListAdapter adapter = null;
    private static Bundle vmsWithInProgressRequests = new Bundle();
    
    //actionid constants for use with quickaction menus
    private static final int START_VM = 0;
    private static final int STOP_VM = 1;
    private static final int REBOOT_VM = 2;
    
    //animation caches (so we don't need to continually re-created these same animations)
    private static Animation fadein_decelerate = null;
    private static Animation fadeout_decelerate = null;
    

    public class CsVmListAdapter extends ResourceCursorAdapter {
    	//This adaptor use strictly for use with the CsVmList class/layout, and expects specific data to fill its contents.
    	
//    	private int _layout;  //unused as the onClickListner-related code is currently unncessary

    	public CsVmListAdapter(Context context, int layout, Cursor c, int flags) {
			super(context, layout, c, flags);
//			_layout = layout;
		}

		@Override
    	public void bindView(View view, Context context, Cursor cursor) {
			setTextViewWithString(view, R.id.id, cursor, Vms.ID);
			setTextViewWithString(view, R.id.displayname, cursor, Vms.DISPLAYNAME);
			setTextViewWithString(view, R.id.name, cursor, Vms.NAME);
			setTextViewWithString(view, R.id.state, cursor, Vms.STATE);
			setTextViewWithString(view, R.id.serviceofferingname, cursor, Vms.SERVICEOFFERINGNAME);
			setTextViewWithString(view, R.id.templatedisplaytext, cursor, Vms.TEMPLATEDISPLAYTEXT);
			setTextViewWithString(view, R.id.hypervisor, cursor, Vms.HYPERVISOR);
			setTextViewWithString(view, R.id.cpunumber, cursor, Vms.CPUNUMBER);
			setTextViewWithString(view, R.id.cpuspeed, cursor, Vms.CPUSPEED);
			setTextViewWithString(view, R.id.memory, cursor, Vms.MEMORY);

			configureAttributesBasedOnState(view);
    	}

		/**
		 * Looks for a TextView with textViewId in view and sets its text value to the String value from cursor under columnName.
		 * @param view view that contains TextView to update
		 * @param textViewId id of TextView to update
		 * @param cursor cursor with String data to use as updated text
		 * @param columnName name of column in cursor that contains the String data to use as updated text
		 */
		public void setTextViewWithString(View view, int textViewId, Cursor cursor, String columnName) {
			TextView tv = (TextView) view.findViewById(textViewId);
			tv.setText(cursor.getString(cursor.getColumnIndex(columnName)));
		}

		/**
		 * Configures the color/quickaction of the TextView/ImageView specified by stateTextViewId/quickactionIconId
		 * based on the value of the state of the VM.
		 * Currently, these states are hard-coded to "running" and "stopped", with a catch all for unrecognized values.
		 * 
		 * @param view View containing TextView/ImageView specified by stateTextViewId/quickactionIconId
		 */
		public void configureAttributesBasedOnState(View view) {
			//clear out the animations for the icon/progressCircle each time,
			//otherwise, the set animation will run automatically each time the
			//widget visibility is changed
			ImageView quickActionIcon = (ImageView)view.findViewById(R.id.quickactionicon);
			ProgressBar quickActionProgress = (ProgressBar)view.findViewById(R.id.quickactionprogress);
			quickActionIcon.clearAnimation();
			quickActionProgress.clearAnimation();
			
			TextView vmidText = (TextView)view.findViewById(R.id.id);
			TextView stateText = (TextView)view.findViewById(R.id.state);
			final String vmid = vmidText.getText().toString();
			final String state = stateText.getText().toString();
			
			final boolean stateUpdated = determineIfStateHasBeenUpdatedByServer(vmid, state);

			//for the vm state text, we change its color depending on the current state of the vm
			if(Vms.STATE_VALUES.RUNNING.equalsIgnoreCase(state)) {
				stateText.setTextColor(getResources().getColorStateList(R.color.vmrunning_color_selector));
				assignQuickActionTo(view, quickActionIcon, createRunningStateQuickAction(view));
				onVmStateUpdate(view, stateText, stateUpdated);
				showQuickActionIcon(quickActionIcon, quickActionProgress, stateUpdated);
				
			} else if (Vms.STATE_VALUES.STOPPED.equalsIgnoreCase(state)) {
				stateText.setTextColor(getResources().getColorStateList(R.color.vmstopped_color_selector));
				assignQuickActionTo(view, quickActionIcon, createStoppedStateQuickAction(view));
				onVmStateUpdate(view, stateText, stateUpdated);
				showQuickActionIcon(quickActionIcon, quickActionProgress, stateUpdated);

			} else if (Vms.STATE_VALUES.STARTING.equalsIgnoreCase(state)) {
				stateText.setTextColor(getResources().getColorStateList(R.color.vmstarting_color_selector));
				showQuickActionProgress(quickActionIcon, quickActionProgress, stateUpdated);
			
			} else if (Vms.STATE_VALUES.STOPPING.equalsIgnoreCase(state)) {
				stateText.setTextColor(getResources().getColorStateList(R.color.vmstopping_color_selector));
				showQuickActionProgress(quickActionIcon, quickActionProgress, stateUpdated);
			
			}  else if (Vms.STATE_VALUES.REBOOTING.equalsIgnoreCase(state)) {
				stateText.setTextColor(getResources().getColorStateList(R.color.vmrebooting_color_selector));
				showQuickActionProgress(quickActionIcon, quickActionProgress, stateUpdated);
			
			} else {
				//if we run into an unknown state, give...
				stateText.setTextColor(getResources().getColorStateList(R.color.vmunknown_color_selector));  //...state a default color
				//...and no quickaction (perhaps change the icon to something looking unclick-able here?)
			}
		}

		public boolean determineIfStateHasBeenUpdatedByServer(final String vmid, final String currentState) {
			final String inProgressState = vmsWithInProgressRequests.getString(vmid);
			final boolean requestIsPending = inProgressState!=null;
			final boolean isNotInProgressState = currentState.equalsIgnoreCase(Vms.STATE_VALUES.RUNNING) || currentState.equalsIgnoreCase(Vms.STATE_VALUES.STOPPED);
			final boolean stateHasBeenUpdated = !currentState.equalsIgnoreCase(inProgressState);
			if(stateHasBeenUpdated) { vmsWithInProgressRequests.remove(vmid); };
			final boolean stateUpdated =  requestIsPending && isNotInProgressState && stateHasBeenUpdated;
			
			return stateUpdated;
		}

		public void onVmStateUpdate(View view, TextView stateText, final boolean stateUpdated) {
			if(stateUpdated) {
				stateText.startAnimation(fadein_decelerate);
				
				TextView displayNameText = (TextView)view.findViewById(R.id.displayname);
				Toast.makeText(getActivity(), "\""+displayNameText.getText().toString()+"\" is now "+stateText.getText().toString().toLowerCase(), Toast.LENGTH_SHORT).show();
			}
		}

		/**
		 * Sets the supplied quickAction as the onClick handler for the ImageView specified by
		 * quickactionIconId in the view.
		 * 
		 * @param view View containing ImageView specified by quickActionIconId
		 * @param quickActionIcon id of ImageView to use as the icon/"button" trigger for this quickaction menu
		 * @param quickAction quickaction to assign to the onClick handler
		 */
		public void assignQuickActionTo(View view, ImageView quickActionIcon, final QuickAction quickAction) {
			quickActionIcon.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					quickAction.show(v);
				}
			});
		}
		
////NOTE: This code below was being used as a work-around for the listview not responding to touch "clicks".
////      As that problem was traced to being a result of using a ImageButton inside the row (ImageView
////      causes no problems), both touches and enter key presses are now working properly.  So the code
////      below is commented out as possible reference for the future.  If you think this is no longer
////      necessary, feel free to delete it.
//		@Override
//		public View getView(int position, View convertView, ViewGroup parent) {
//            if (null == convertView) {
//                    convertView = getLayoutInflater(null).inflate(_layout, null);
//            }
//
//            //sets a custom touch listener for the whole row (does not respond to enter key presses)
//            convertView.setOnClickListener(new OnItemClickListener(position));
//
//            return convertView;
//		}
//		
//		private class OnItemClickListener implements OnClickListener{
//			private int _position;
//	        OnItemClickListener(int position){
//	        	_position = position;
//	        }
//	        @Override
//	        public void onClick(View arg0) {
//	        	ClLog.i("FragmentList", "onItemClick at position "+_position);                      
//	        }               
//	    }
		
		
		
    }



    public CsVmListFragment() {
    	//empty constructor is needed by Android for automatically creating fragments from XML declarations
    }
    
    /** Called when the activity is first created. */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        //make the rest call to cs server for data
        final String action = CsRestService.TEST_CALL;   
        Bundle apiCmd = new Bundle();
        apiCmd.putString(CsRestService.COMMAND, "listVirtualMachines");
        apiCmd.putString(Vms.ACCOUNT, "rickson");
        Intent csRestServiceIntent = CsRestService.createCsRestServiceIntent(getActivity(), action, apiCmd);  //user api
        getActivity().startService(csRestServiceIntent);
      
        //set-up the loader & adapter for populating this list
        getLoaderManager().initLoader(CSVM_LIST_LOADER, null, this);
        adapter = new CsVmListAdapter(getActivity().getApplicationContext(), R.layout.csvmlistitem, null, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        setListAdapter(adapter);
        
        //init global animation cache
        fadein_decelerate = AnimationUtils.loadAnimation(getActivity(), R.anim.fadein_decelerate);
        fadeout_decelerate = AnimationUtils.loadAnimation(getActivity(), R.anim.fadeout_decelerate);
    }

//	public void createQuickAction() {
//        final ActionItem startVmItem = new ActionItem(0, "Start VM", getResources().getDrawable(R.drawable.menu_ok));
//        final ActionItem stopVmItem = new ActionItem(1, "Stop VM", getResources().getDrawable(R.drawable.menu_cancel));
//        final ActionItem restartVmItem = new ActionItem(2, "Restart VM", getResources().getDrawable(android.R.drawable.ic_lock_power_off));
//        
//        //use setSticky(true) to disable QuickAction dialog being dismissed after an item is clicked
////        prevItem.setSticky(true);
////        nextItem.setSticky(true);
//
//        //create QuickAction. Use QuickAction.VERTICAL or QuickAction.HORIZONTAL param to define layout 
//        //orientation
//        quickAction = new QuickAction(getActivity(), QuickAction.HORIZONTAL);
//        
//        //add action items into QuickAction
//        quickAction.addActionItem(startVmItem);
//        quickAction.addActionItem(stopVmItem);
//        quickAction.addActionItem(restartVmItem);
//        
//        //Set listener for action item clicked
//        quickAction.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {          
//            @Override
//            public void onItemClick(QuickAction source, int pos, int actionId) {
//                //here we can filter which action item was clicked with pos or actionId parameter
//                ActionItem actionItem = quickAction.getActionItem(pos);
//
//                Toast.makeText(getActivity(), actionItem.getTitle() + " selected", Toast.LENGTH_SHORT).show();                
//            }
//        });
//        
//	}
	
//	public void createRunningAndStoppedStateQuickActions() {
//		final ActionItem startVmItem = new ActionItem(0, "Start VM", getResources().getDrawable(R.drawable.menu_ok));
//		final ActionItem stopVmItem = new ActionItem(1, "Stop VM", getResources().getDrawable(R.drawable.menu_cancel));
//		final ActionItem restartVmItem = new ActionItem(2, "Restart VM", getResources().getDrawable(android.R.drawable.ic_lock_power_off));
//		
//		//create QuickAction. Use QuickAction.VERTICAL or QuickAction.HORIZONTAL param to define layout orientation
//		runningStateQuickAction = new QuickAction(getActivity(), QuickAction.HORIZONTAL);
//		
//		//add action items into QuickAction
//		runningStateQuickAction.addActionItem(stopVmItem);
//		runningStateQuickAction.addActionItem(restartVmItem);
/////DEBUG
//		runningStateQuickAction.addActionItem(startVmItem);
/////endDEBUG
//		
//		//Set listener for action item clicked
//		runningStateQuickAction.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {          
//			@Override
//			public void onItemClick(QuickAction source, int pos, int actionId) {
//				//here we can filter which action item was clicked with pos or actionId parameter
//				ActionItem actionItem = source.getActionItem(pos);
//				
//				TextView tv = (TextView)source.getAnchorView().findViewById(R.id.id);
//				Toast.makeText(getActivity(), actionItem.getTitle() + " selected [" + tv.getText() +"]", Toast.LENGTH_SHORT).show();   
//				
/////DEBUG
//				System.out.println("tv.getText="+tv.getText());
////makeStartVmCall(source.getAnchorView());
/////endDEBUG
//			}
//		});
//		runningStateQuickAction.setAnimStyle(QuickAction.ANIM_GROW_FROM_RIGHT);
//		
//		
//		//create QuickAction. Use QuickAction.VERTICAL or QuickAction.HORIZONTAL param to define layout orientation
//		stoppedStateQuickAction = new QuickAction(getActivity(), QuickAction.HORIZONTAL);
//		
//		//add action items into QuickAction
//		stoppedStateQuickAction.addActionItem(startVmItem);
//		
//		//Set listener for action item clicked
//		stoppedStateQuickAction.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {          
//			@Override
//			public void onItemClick(QuickAction source, int pos, int actionId) {
//				//here we can filter which action item was clicked with pos or actionId parameter
//				ActionItem actionItem = source.getActionItem(pos);
//				
//				TextView tv = (TextView)source.getAnchorView().findViewById(R.id.id);
//				Toast.makeText(getActivity(), actionItem.getTitle() + " selected [" + tv.getText() +"]", Toast.LENGTH_SHORT).show(); 
//				
//				makeStartVmCall(source.getAnchorView());
//			}
//		});
//		stoppedStateQuickAction.setAnimStyle(QuickAction.ANIM_GROW_FROM_RIGHT);
//		
//	}
    
	public void showQuickActionIcon(final ImageView quickActionIcon, final ProgressBar quickActionProgress, final boolean animate) {
		
		if(animate) {
			quickActionIcon.startAnimation(fadein_decelerate);
		}
		quickActionIcon.setVisibility(View.VISIBLE);
		quickActionIcon.setClickable(true);

		if(animate) {
			quickActionProgress.startAnimation(fadeout_decelerate);
		}
		quickActionProgress.setVisibility(View.INVISIBLE);

	}

	public void showQuickActionProgress(final ImageView quickActionIcon, final ProgressBar quickActionProgress, final boolean animate) {

		quickActionIcon.setClickable(false);
		if(animate) {
			quickActionIcon.startAnimation(fadeout_decelerate);
		}
		quickActionIcon.setVisibility(View.INVISIBLE);

		if(animate) {
			quickActionProgress.startAnimation(fadein_decelerate);
		}
		quickActionProgress.setVisibility(View.VISIBLE);
		
	}

	public QuickAction createRunningStateQuickAction(final View view) {
        final ActionItem stopVmItem = new ActionItem(STOP_VM, "Stop VM", getResources().getDrawable(R.drawable.menu_cancel));
        final ActionItem rebootVmItem = new ActionItem(REBOOT_VM, "Reboot VM", getResources().getDrawable(android.R.drawable.ic_lock_power_off));
		
		//create QuickAction. Use QuickAction.VERTICAL or QuickAction.HORIZONTAL param to define layout orientation
		QuickAction runningStateQuickAction = new QuickAction(getActivity(), QuickAction.HORIZONTAL);
		
		//add action items into QuickAction
		runningStateQuickAction.addActionItem(stopVmItem);
		runningStateQuickAction.addActionItem(rebootVmItem);
		
		//Set listener for action item clicked
		runningStateQuickAction.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {          
			@Override
			public void onItemClick(QuickAction source, int pos, int actionId) {
				switch(actionId) {
					case STOP_VM:
						makeStartOrStopOrRebootVmCall(view, CsApiConstants.API.stopVirtualMachine);
						break;
					case REBOOT_VM:
						makeStartOrStopOrRebootVmCall(view, CsApiConstants.API.rebootVirtualMachine);
						break;
					default:
						ClLog.e("runningStateQuickAction", "Unrecognized actionId="+actionId);
				}
			}
		});
		runningStateQuickAction.setAnimStyle(QuickAction.ANIM_GROW_FROM_RIGHT);

		return runningStateQuickAction;
	}

	public QuickAction createStoppedStateQuickAction(final View view) {
		final ActionItem startVmItem = new ActionItem(START_VM, "Start VM", getResources().getDrawable(R.drawable.menu_ok));
		
		//create QuickAction. Use QuickAction.VERTICAL or QuickAction.HORIZONTAL param to define layout orientation
		QuickAction stoppedStateQuickAction = new QuickAction(getActivity(), QuickAction.HORIZONTAL);
		
		//add action items into QuickAction
		stoppedStateQuickAction.addActionItem(startVmItem);
		
		//Set listener for action item clicked
		stoppedStateQuickAction.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {          
			@Override
			public void onItemClick(QuickAction source, int pos, int actionId) {
				makeStartOrStopOrRebootVmCall(view, CsApiConstants.API.startVirtualMachine);
			}
		});
		stoppedStateQuickAction.setAnimStyle(QuickAction.ANIM_GROW_FROM_RIGHT);
		
		return stoppedStateQuickAction;
	}
    
	///Note: for debug purposes
	//public void onListItemClick(ListView l, View v, int position, long id) {
    //	ClLog.d("FragmentList", "Item clicked: " + id);
    //}
    
	public void makeStartOrStopOrRebootVmCall(View itemView, final String commandName) {
		TextView idText = (TextView)itemView.findViewById(R.id.id);
		final String vmid = idText.getText().toString();

		ImageView quickActionIcon = (ImageView)itemView.findViewById(R.id.quickactionicon);
		ProgressBar quickActionProgress = (ProgressBar)itemView.findViewById(R.id.quickactionprogress);
		showQuickActionProgress(quickActionIcon, quickActionProgress, true);

		String inProgressState = null;
		if(CsApiConstants.API.startVirtualMachine.equalsIgnoreCase(commandName)) {
			inProgressState = Vms.STATE_VALUES.STARTING;
		} else if(CsApiConstants.API.stopVirtualMachine.equalsIgnoreCase(commandName)) {
			inProgressState = Vms.STATE_VALUES.STOPPING;
		} else if(CsApiConstants.API.rebootVirtualMachine.equalsIgnoreCase(commandName)) {
			inProgressState = Vms.STATE_VALUES.REBOOTING;
		}
		
		updateVmStateOnDb(vmid, inProgressState);  //update vm data with in-progress state
		vmsWithInProgressRequests.putString(vmid, inProgressState);  //cache in-progress state, so we compare and know when it has been updated by the server reply
		
        //make the rest call to cs server to start/stop/reboot vm represented by itemView
        final String action = CsRestService.TEST_CALL;   
        Bundle apiCmd = new Bundle();
        apiCmd.putString(CsRestService.COMMAND, commandName);
        apiCmd.putString(Vms.ID, vmid);
        Intent csRestServiceIntent = CsRestService.createCsRestServiceIntent(getActivity(), action, apiCmd);
        getActivity().startService(csRestServiceIntent);
	}

	public void updateVmStateOnDb(final String vmid, String state) {
		ContentValues cv = new ContentValues();
		cv.put(Vms.STATE, state);
		final String whereClause = Vms.ID+"=?";
		final String[] selectionArgs = new String[] { vmid };
		getActivity().getContentResolver().update(Vms.META_DATA.CONTENT_URI, cv, whereClause, selectionArgs);
	}
	
    
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] columns = new String[] {
        		Vms._ID,
        		Vms.ID,
        		Vms.DISPLAYNAME,
        		Vms.NAME,
        		Vms.STATE,
        		Vms.SERVICEOFFERINGNAME,
        		Vms.TEMPLATEDISPLAYTEXT,
        		Vms.HYPERVISOR,
        		Vms.CPUNUMBER,
        		Vms.CPUSPEED,
        		Vms.MEMORY
        };
        CursorLoader cl = new CursorLoader(getActivity(), Vms.META_DATA.CONTENT_URI, columns, null, null, null);
		return cl;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		adapter.swapCursor(cursor);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		adapter.swapCursor(null);
	}
    
}
