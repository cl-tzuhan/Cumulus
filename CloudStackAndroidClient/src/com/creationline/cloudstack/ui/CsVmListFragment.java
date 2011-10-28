package com.creationline.cloudstack.ui;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.creationline.cloudstack.R;
import com.creationline.cloudstack.engine.CsRestService;
import com.creationline.cloudstack.engine.db.Vms;
import com.creationline.cloudstack.util.ClLog;

public class CsVmListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final int CSVM_LIST_LOADER = 0x01;
    
    public CsVmListAdapter adapter = null;
    
//    private QuickAction quickAction = null;  //member var cache to save having to re-create this quickaction each time we show the menu
//    private QuickAction runningStateQuickAction = null;  //member var cache to save having to re-create this quickaction each time we show the menu
//    private QuickAction stoppedStateQuickAction = null;  //member var cache to save having to re-create this quickaction each time we show the menu
    

    public class CsVmListAdapter extends ResourceCursorAdapter {
    	//This adaptor use strictly for use with the CsVmList class/layout, and expects specific data to fill its contents.
    	
//    	private int _layout;  //unused as the onClickListner-related code is currently unncessary

    	public CsVmListAdapter(Context context, int layout, Cursor c, int flags) {
			super(context, layout, c, flags);
//			_layout = layout;
		}

		@Override
    	public void bindView(View view, Context context, Cursor cursor) {
			setTextViewWithString(view, R.id.displayname, cursor, Vms.DISPLAYNAME);
			setTextViewWithString(view, R.id.name, cursor, Vms.NAME);
			setTextViewWithString(view, R.id.state, cursor, Vms.STATE);
			configureAttributesBasedOnState(view, R.id.state, R.id.quickactionicon);
			setTextViewWithString(view, R.id.serviceofferingname, cursor, Vms.SERVICEOFFERINGNAME);
			setTextViewWithString(view, R.id.templatedisplaytext, cursor, Vms.TEMPLATEDISPLAYTEXT);
			setTextViewWithString(view, R.id.hypervisor, cursor, Vms.HYPERVISOR);
			setTextViewWithString(view, R.id.cpunumber, cursor, Vms.CPUNUMBER);
			setTextViewWithString(view, R.id.cpuspeed, cursor, Vms.CPUSPEED);
			setTextViewWithString(view, R.id.memory, cursor, Vms.MEMORY);
			setTextViewWithString(view, R.id.id, cursor, Vms.ID);
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
		 * @param stateTextViewId id of the TextView that contains the current state of the VM
		 * @param quickActionIconId id of the ImageView to use as the icon/"button" for triggering the quickaction menu
		 */
		public void configureAttributesBasedOnState(View view, int stateTextViewId, int quickActionIconId) {
			TextView tv = (TextView) view.findViewById(stateTextViewId);

			//for the vm state text, we change its color depending on the current state of the vm
			String stateStr = tv.getText().toString();
			if("running".equalsIgnoreCase(stateStr)) {
				tv.setTextColor(getResources().getColorStateList(R.color.vmrunning_color_selector));
				assignQuickActionTo(view, quickActionIconId, createRunningStateQuickAction(view));
				
			} else if ("stopped".equalsIgnoreCase(stateStr)) {
				tv.setTextColor(getResources().getColorStateList(R.color.vmstopped_color_selector));
				assignQuickActionTo(view, quickActionIconId, createStoppedStateQuickAction(view));

			} else {
				//if we run into an unknown state, give...
				tv.setTextColor(getResources().getColorStateList(R.color.vmunknown_color_selector));  //...state a default color
				//...and no quickaction (perhaps change the icon to something looking unclick-able here?)
			}
		}

		/**
		 * Sets the supplied quickAction as the onClick handler for the ImageView specified by
		 * quickactionIconId in the view.
		 * 
		 * @param view View containing ImageView specified by quickActionIconId
		 * @param quickActionIconId id of ImageView to use as the icon/"button" trigger for this quickaction menu
		 * @param quickAction quickaction to assign to the onClick handler
		 */
		public void assignQuickActionTo(View view, int quickActionIconId, final QuickAction quickAction) {
			ImageView quickActionIcon = (ImageView) view.findViewById(quickActionIconId);
			quickActionIcon.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					quickAction.show(v);
				}
			});
//			quickAction.setAnchorView(view);  //cache the view which this quickaction is attached to
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
        apiCmd.putString("account", "rickson");
        Intent csRestServiceIntent = CsRestService.createCsRestServiceIntent(getActivity(), action, apiCmd);  //user api
        getActivity().startService(csRestServiceIntent);
      
        //set-up the loader & adapter for populating this list
        getLoaderManager().initLoader(CSVM_LIST_LOADER, null, this);
        adapter = new CsVmListAdapter(getActivity().getApplicationContext(), R.layout.csvmlistitem, null, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        setListAdapter(adapter);
        
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

	public QuickAction createRunningStateQuickAction(final View view) {
        final ActionItem stopVmItem = new ActionItem(1, "Stop VM", getResources().getDrawable(R.drawable.menu_cancel));
        final ActionItem restartVmItem = new ActionItem(2, "Restart VM", getResources().getDrawable(android.R.drawable.ic_lock_power_off));
		
		//create QuickAction. Use QuickAction.VERTICAL or QuickAction.HORIZONTAL param to define layout orientation
		QuickAction runningStateQuickAction = new QuickAction(getActivity(), QuickAction.HORIZONTAL);
		
		//add action items into QuickAction
		runningStateQuickAction.addActionItem(stopVmItem);
		runningStateQuickAction.addActionItem(restartVmItem);
		
		//Set listener for action item clicked
		runningStateQuickAction.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {          
			@Override
			public void onItemClick(QuickAction source, int pos, int actionId) {
				//here we can filter which action item was clicked with pos or actionId parameter
				ActionItem actionItem = source.getActionItem(pos);
				
				TextView tv = (TextView) view.findViewById(R.id.id);
				Toast.makeText(getActivity(), actionItem.getTitle() + " selected [" + tv.getText() +"]", Toast.LENGTH_SHORT).show();   
				
///DEBUG
System.out.println("tv.getText="+tv.getText());
//makeStartVmCall(source.getAnchorView());
///endDEBUG
			}
		});
		runningStateQuickAction.setAnimStyle(QuickAction.ANIM_GROW_FROM_RIGHT);

		return runningStateQuickAction;
	}

	public QuickAction createStoppedStateQuickAction(final View view) {
		final ActionItem startVmItem = new ActionItem(0, "Start VM", getResources().getDrawable(R.drawable.menu_ok));
		
		
		//create QuickAction. Use QuickAction.VERTICAL or QuickAction.HORIZONTAL param to define layout orientation
		QuickAction stoppedStateQuickAction = new QuickAction(getActivity(), QuickAction.HORIZONTAL);
		
		//add action items into QuickAction
		stoppedStateQuickAction.addActionItem(startVmItem);
		
		//Set listener for action item clicked
		stoppedStateQuickAction.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {          
			@Override
			public void onItemClick(QuickAction source, int pos, int actionId) {
				//here we can filter which action item was clicked with pos or actionId parameter
				ActionItem actionItem = source.getActionItem(pos);
				
				TextView tv = (TextView) view.findViewById(R.id.id);
				Toast.makeText(getActivity(), actionItem.getTitle() + " selected [" + tv.getText() +"]", Toast.LENGTH_SHORT).show(); 
				
				makeStartVmCall(view);
			}
		});
		stoppedStateQuickAction.setAnimStyle(QuickAction.ANIM_GROW_FROM_RIGHT);
		
		return stoppedStateQuickAction;
	}
    

	public void onListItemClick(ListView l, View v, int position, long id) {
    	ClLog.d("FragmentList", "Item clicked: " + id);
    }
    
	public void makeStartVmCall(View itemView) {
        //make the rest call to cs server to start VM represented by itemView
		
		TextView idText = (TextView)itemView.findViewById(R.id.id);
		
        final String action = CsRestService.TEST_CALL;   
        Bundle apiCmd = new Bundle();
        apiCmd.putString(CsRestService.COMMAND, "startVirtualMachine");
        apiCmd.putString("id", idText.getText().toString());
        Intent csRestServiceIntent = CsRestService.createCsRestServiceIntent(getActivity(), action, apiCmd);
        getActivity().startService(csRestServiceIntent);
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
