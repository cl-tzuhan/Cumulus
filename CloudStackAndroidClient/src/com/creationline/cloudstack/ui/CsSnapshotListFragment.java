package com.creationline.cloudstack.ui;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.creationline.cloudstack.CloudStackAndroidClient;
import com.creationline.cloudstack.R;
import com.creationline.cloudstack.engine.CsApiConstants;
import com.creationline.cloudstack.engine.CsRestContentProvider;
import com.creationline.cloudstack.engine.CsRestService;
import com.creationline.cloudstack.engine.db.Snapshots;
import com.creationline.cloudstack.engine.db.Transactions;
import com.creationline.cloudstack.util.DateTimeParser;
import com.creationline.cloudstack.util.QuickActionUtils;

public class CsSnapshotListFragment extends CsListFragmentBase implements LoaderManager.LoaderCallbacks<Cursor> {
	public static class INTENT_ACTION {
		//NOTE: changing the value of these constants requires you to change any usage of the same string in Android.manifest!!!
		public static final String CALLBACK_LISTSNAPSHOTS = "com.creationline.cloudstack.ui.CsSnapshotListFragment.CALLBACK_LISTSNAPSHOTS";
		public static final String CALLBACK_DELETESNAPSHOT = "com.creationline.cloudstack.ui.CsSnapshotListFragment.CALLBACK_DELETESNAPSHOT";
	}

	private static final int CSSNAPSHOT_LIST_LOADER = 0x02;
    private CsSnapshotListAdapter adapter = null;  //backer for this list
    private BroadcastReceiver listSnapshotsCallbackReceiver = null;  //used to receive list snapshots complete notifs from CsRestService
    private BroadcastReceiver deleteSnapshotCallbackReceiver = null;  //used to receive request success/failure notifs from CsRestService
    private boolean isProvisioned = false;  //whether we currently have api/secret key or not

    //constants used as keys for saving/restoring app state on pause/resume
    private static final String CSSNAPSHOTLIST_DATESTAMP = "com.creationline.cloudstack.ui.CsSnapshotListFragment.CSSNAPSHOTLIST_DATESTAMP";
    private static final String CSSNAPSHOTLIST_TIMESTAMP = "com.creationline.cloudstack.ui.CsSnapshotListFragment.CSSNAPSHOTLIST_TIMESTAMP";
    
    private class CsSnapshotListAdapter extends ResourceCursorAdapter {
    	//This adaptor used strictly for use with the CsSnapshot class/layout, and expects specific data to fill its contents.
    	
		public CsSnapshotListAdapter(Context context, int layout, Cursor c, int flags) {
			super(context, layout, c, flags);
		}

		@Override
    	public void bindView(View view, Context context, Cursor cursor) {
			setTextViewWithString(view, R.id.id, cursor, Snapshots.ID);
			setTextViewWithString(view, R.id.name, cursor, Snapshots.NAME);
			setTextViewWithString(view, R.id.created, cursor, Snapshots.CREATED);
			setTextViewWithString(view, R.id.volumename, cursor, Snapshots.VOLUMENAME);
			setTextViewWithString(view, R.id.volumetype, cursor, Snapshots.VOLUMETYPE);
			setTextViewWithString(view, R.id.state, cursor, Snapshots.STATE);
			
			setTextViewWithString(view, R.id.inprogress_state, cursor, Snapshots.INPROGRESS_STATE);
			
			configureAttributesBasedOnState(view);
			setStateColor(view);
		
			
			//NOTE: This is not an ideal location as bindView() is called on each item in the list,
			//      and we only need this to be called once whenever the db changes.  Unfortunately,
			//      because of the ViewPager, it is possible that the CsSnapshotListFragment view is
			//      not yet created when the snapshot db is already updated (from, say, a listSnapshots
			//      call), so trying to update #-of-Snapshots when the view does not exist yet causes a crash.
//			updateSnapshotNum(view);
    	}

//		public void updateSnapshotNum(View view) {
//			TextView footersnapshotnum = (TextView)getListView().findViewById(R.id.footersnapshotnum);
//			if(footersnapshotnum!=null) {
//				//update the current #-of-snapshots count
//				final int count = getCursor().getCount();
//				footersnapshotnum.setText(String.valueOf(count));
//			}
//		}

		public void setStateColor(View view) {
			TextView stateText = (TextView)view.findViewById(R.id.state);
			final String state = stateText.getText().toString();
			ColorStateList colorStateList = null;
			if(Snapshots.STATE_VALUES.BACKEDUP.equalsIgnoreCase(state)) {
				colorStateList = getResources().getColorStateList(R.color.snapshotbackedup_color_selector);
			} else if(Snapshots.STATE_VALUES.BACKINGUP.equalsIgnoreCase(state)) {
				colorStateList = getResources().getColorStateList(R.color.snapshotbackingup_color_selector);
			} else if(Snapshots.STATE_VALUES.CREATING.equalsIgnoreCase(state)) {
				colorStateList = getResources().getColorStateList(R.color.snapshotcreating_color_selector);
			} else if (Snapshots.STATE_VALUES.DELETING.equalsIgnoreCase(state)) {
				colorStateList = getResources().getColorStateList(R.color.vmstopping_color_selector);
			} else {
				colorStateList = getResources().getColorStateList(R.color.supplementaryinfo_color_selector);
			}
			stateText.setTextColor(colorStateList);
		}

		public void configureAttributesBasedOnState(View view) {
			ImageView quickActionIcon = (ImageView)view.findViewById(R.id.quickactionicon);
			ProgressBar quickActionProgress = (ProgressBar)view.findViewById(R.id.quickactionprogress);
			
			TextView stateText = (TextView)view.findViewById(R.id.state);
			String state = stateText.getText().toString();
			TextView inProgressStateText = (TextView)view.findViewById(R.id.inprogress_state);

			final String inProgressState = inProgressStateText.getText().toString();
			if(!inProgressState.isEmpty() && !inProgressState.equalsIgnoreCase("show_icon")) {
				state = inProgressState.toString();  //if it exists, inprogress_state values takes precedence over state values for ui-display purposes
			}
			
			quickActionIcon.setEnabled(true);
			//for the vm state text, we change its color depending on the current state of the vm
			if(Snapshots.STATE_VALUES.BACKEDUP.equalsIgnoreCase(state)) {
				QuickActionUtils.assignQuickActionTo(view, quickActionIcon, createQuickAction(view));
				QuickActionUtils.showQuickActionIcon(quickActionIcon, quickActionProgress, false);
				
			} else if (Snapshots.STATE_VALUES.BACKINGUP.equalsIgnoreCase(state)) {
				QuickActionUtils.showQuickActionProgress(quickActionIcon, quickActionProgress, false);
				
			} else if (Snapshots.STATE_VALUES.CREATING.equalsIgnoreCase(state)) {
				QuickActionUtils.showQuickActionProgress(quickActionIcon, quickActionProgress, false);
				
			} else if (Snapshots.STATE_VALUES.DELETING.equalsIgnoreCase(state)) {
				QuickActionUtils.showQuickActionProgress(quickActionIcon, quickActionProgress, false);
				
			} else {
				//if we run into an unknown state, give...
				quickActionIcon.setEnabled(false);  //...disable the quickaction menu since we don't know what running a command might do in this state
			}
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
			String text = cursor.getString(cursor.getColumnIndex(columnName));
			
			if(textViewId==R.id.created) {
				TextView timeText = (TextView)view.findViewById(R.id.createdtime);
				DateTimeParser.setParsedDateTime(tv, timeText, text);
			} else if (textViewId==R.id.inprogress_state) {
				//inprogress_state values takes precedence over state values for ui-display purposes
				if(text!=null) {
					TextView stateText = (TextView) view.findViewById(R.id.state);
					stateText.setText(text);
				}
				tv.setText(text);
			} else {
				//for non-special cases, just output text as is
				tv.setText(text);
			}
		}

		@Override
		public void notifyDataSetChanged() {
			TextView footersnapshotnum = (TextView)getActivity().findViewById(R.id.footersnapshotnum);
			if(footersnapshotnum!=null) {
				//update the current #-of-snapshots count
				final int count = getCursor().getCount();
				footersnapshotnum.setText(String.valueOf(count));
			}
			
			//double-check whether we are still provisioned (use could have reset account in the mean time) and update button state if necessary
			isProvisioned = isProvisioned();
			if(isProvisioned==false) {
        		View cssnapshotlistcommandfooter = getActivity().findViewById(R.id.cssnapshotlistcommandfooter);
				setRefreshButtonEnabled(cssnapshotlistcommandfooter, false);
			}
			
			super.notifyDataSetChanged();
		}
		
    }
    

    public CsSnapshotListFragment() {
    	//empty constructor is needed by Android for automatically creating fragments from XML declarations
    }
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
//        listSnapshotsCallbackReceiver = new BroadcastReceiver(){
//        	//This handles callback intents broadcasted by CsRestService
//        	@Override
//        	public void onReceive(Context contenxt, Intent intent) {
//        		Bundle bundle = intent.getExtras();
//        		final int successOrFailure = bundle.getInt(CsRestService.CALL_STATUS);
//        		final String snapshotId = bundle.getString(Snapshots.ID);
//        		
//        		//as the request is finished, mark inprogress_state as null signifying there is no pending operation left
//        		updateSnapshotInProgressStateOnDb(snapshotId, null);
//        		
//        		if(successOrFailure==CsRestService.CALL_STATUS_VALUES.CALL_FAILURE) {
//        			//if deleteSnapshot failed, revert the progress-circle back to icon again
//        			adapter.notifyDataSetChanged();  //faking a data set change so the list will refresh itself
//        			
//        		} else {
//        			//if deleteSnapshot succeeded, CsRestService has already done the deletion for for us, so just stop tracking this id
//    				Toast.makeText(getActivity(), "Snapshot ("+snapshotId+") deleted", Toast.LENGTH_SHORT).show();
//        		}
//        	}
//        };
//        getActivity().registerReceiver(listSnapshotsCallbackReceiver, new IntentFilter(CsSnapshotListFragment.INTENT_ACTION.CALLBACK_DELETESNAPSHOT));  //activity will now get intents broadcast by CsRestService (filtered by CALLBACK_DELETESNAPSHOT action)

        isProvisioned = isProvisioned();  //this needs to be done first as the isProvisioned member var is used at various places

        registerListSnapshotsCallbackReceiver();
        registerDeleteSnapshotCallbackReceiver();
        
//        SharedPreferences preferences = getActivity().getSharedPreferences(CloudStackAndroidClient.SHARED_PREFERENCES.PREFERENCES_NAME, Context.MODE_PRIVATE);
//		final String username = preferences.getString(CloudStackAndroidClient.SHARED_PREFERENCES.USERNAME_SETTING, null);
//
//		if(username!=null) {
//			//make the rest call to cs server for data
//			final String action = CsRestService.TEST_CALL;   
//			Bundle apiCmd = new Bundle();
//			apiCmd.putString(CsRestService.COMMAND, "listSnapshots");
//			apiCmd.putString(Snapshots.ACCOUNT, username);
//			Intent csRestServiceIntent = CsRestService.createCsRestServiceIntent(getActivity(), action, apiCmd);  //user api
//			getActivity().startService(csRestServiceIntent);
//		}
    }
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		//add the summary footer to the list
		addAndInitFooter(savedInstanceState, R.layout.cssnapshotlistsummaryfooter, R.id.csvmlistsummaryfooterviewswitcher);
//        View cssnapshotlistsummaryfooter = getLayoutInflater(savedInstanceState).inflate(R.layout.cssnapshotlistsummaryfooter, null, false);
//        ViewSwitcher footerviewswitcher = (ViewSwitcher)cssnapshotlistsummaryfooter.findViewById(R.id.footerviewswitcher);
//        if(footerviewswitcher!=null) {
//        	footerviewswitcher.setDisplayedChild(0);
//        	footerviewswitcher.setAnimateFirstView(true);
//        	getListView().addFooterView(cssnapshotlistsummaryfooter, null, false);
//        }
        
        //add command footer to the list
		View cssnapshotlistcommandfooter = addAndInitFooter(savedInstanceState, R.layout.cssnapshotlistcommandfooter, R.id.cssnapshotlistcommandfooterviewswitcher);
//        View csvmlistcommandfooter = getLayoutInflater(savedInstanceState).inflate(R.layout.cssnapshotlistcommandfooter, null, false);
//        ViewSwitcher commandfooterviewswitcher = (ViewSwitcher)csvmlistcommandfooter.findViewById(R.id.cssnapshotlistcommandfooterviewswitcher);
//        if(commandfooterviewswitcher!=null) {
//        	commandfooterviewswitcher.setDisplayedChild(0);
//        	commandfooterviewswitcher.setAnimateFirstView(true);
//        }
//        getListView().addFooterView(csvmlistcommandfooter, null, false);
        
        setRefreshButtonClickHandler(cssnapshotlistcommandfooter);
		if(isProvisioned) {
			setRefreshButtonEnabled(cssnapshotlistcommandfooter, true);
        } else {
        	setRefreshButtonEnabled(cssnapshotlistcommandfooter, false);
        }
        
        //set-up the loader & adapter for populating this list
        getLoaderManager().initLoader(CSSNAPSHOT_LIST_LOADER, null, this);
        adapter = new CsSnapshotListAdapter(getActivity().getApplicationContext(), R.layout.cssnapshotlistitem, null, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        setListAdapter(adapter);
        
		final boolean isFreshAppStart = savedInstanceState==null;  //a "fresh app start" means the app was started fresh by the user, not as a result of orientation changes or such
		if(isFreshAppStart) {
			if(isProvisioned) {
				//do an initial refresh for data since it may have been a while since we were running
				makeListSnapshotCall();
			}
		} else {
			//if we have any saved time/date stamps of the last refresh, use them 
			final String savedDatestamp = savedInstanceState.getString(CSSNAPSHOTLIST_DATESTAMP);
			final String savedTimestamp = savedInstanceState.getString(CSSNAPSHOTLIST_TIMESTAMP);

			if(savedDatestamp!=null) {
				setTextView(cssnapshotlistcommandfooter, R.id.lastrefresheddatestamp, savedDatestamp);
			}
			if(savedTimestamp!=null) {
				setTextView(cssnapshotlistcommandfooter, R.id.lastrefreshedtimestamp, savedTimestamp);
			}
		}
        
        super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		View cssnapshotlistcommandfooter = getActivity().findViewById(R.id.cssnapshotlistcommandfooter);
		if(cssnapshotlistcommandfooter==null) {
			return;
		}
		
		TextView lastrefresheddatestamp = (TextView)cssnapshotlistcommandfooter.findViewById(R.id.lastrefresheddatestamp);
		TextView lastrefreshedtimestamp = (TextView)cssnapshotlistcommandfooter.findViewById(R.id.lastrefreshedtimestamp);
		
		outState.putString(CSSNAPSHOTLIST_DATESTAMP, lastrefresheddatestamp.getText().toString());
		outState.putString(CSSNAPSHOTLIST_TIMESTAMP, lastrefreshedtimestamp.getText().toString());
		
		super.onSaveInstanceState(outState);
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		final TextView snapshotIdText = (TextView)v.findViewById(R.id.id);
		
		//start details view activity with id of selected snapshot
		Intent intent = new Intent();
		intent.setClass(getActivity(), CsSnapshotDetailsFragmentActivity.class);
		intent.putExtra(Snapshots.class.toString()+Snapshots.ID, snapshotIdText.getText().toString());
		startActivity(intent);
	}
	
	@Override
	public void onResume() {
//		registerListSnapshotsCallbackReceiver();
//      registerDeleteSnapshotCallbackReceiver();
        
		super.onResume();
	}
	
	@Override
	public void onPause() {
//		unregisterCallbackReceiver(listSnapshotsCallbackReceiver);
//		unregisterCallbackReceiver(deleteSnapshotCallbackReceiver);
		
//		View cssnapshotlistcommandfooter = getActivity().findViewById(R.id.cssnapshotlistcommandfooter);
//		setRefreshButtonEnabled(cssnapshotlistcommandfooter, true);
//		setProgressCircleVisible(cssnapshotlistcommandfooter, ProgressBar.INVISIBLE);
		
		super.onPause();
	}

	@Override
	public void onDestroy() {
		unregisterCallbackReceiver(listSnapshotsCallbackReceiver);
		unregisterCallbackReceiver(deleteSnapshotCallbackReceiver);
//		if(listSnapshotsCallbackReceiver!=null) {
//			//catch-all here as a safeguard against cases where the activity is exited before BroadcastReceiver.onReceive() has been called-back
//			try {
//				getActivity().unregisterReceiver(listSnapshotsCallbackReceiver);
//			} catch (IllegalArgumentException e) {
//				//will get this exception if listSnapshotsCallbackReceiver has already been unregistered (or was never registered); will just ignore here
//				;
//			}
//		}
		super.onDestroy();
	}
	
	public void registerListSnapshotsCallbackReceiver() {
        listSnapshotsCallbackReceiver = new BroadcastReceiver(){
        	//This handles callback intents broadcasted by CsRestService
        	@Override
        	public void onReceive(Context contenxt, Intent intent) {
        		FragmentActivity activity = getActivity();
        		if(activity==null) {
        			return;
        		}
        		
        		View cssnapshotlistcommandfooter = getActivity().findViewById(R.id.cssnapshotlistcommandfooter);
        		if(cssnapshotlistcommandfooter==null) {
        			return;
        		}
        		setRefreshButtonEnabled(cssnapshotlistcommandfooter, true);
        		setProgressCircleVisible(cssnapshotlistcommandfooter, ProgressBar.INVISIBLE);
        		
        		Bundle bundle = intent.getExtras();
        		final String updatedUriStr = bundle.getString(CsRestService.UPDATED_URI);
        		if(updatedUriStr==null) {
        			return;
        		}

        		final Bundle parsedDateTime =  CsRestContentProvider.getReplyDateTimeFor(activity, updatedUriStr);
        		if(parsedDateTime!=null) {
        			setTextView(cssnapshotlistcommandfooter, R.id.lastrefresheddatestamp, parsedDateTime.getString(CsRestContentProvider.DATESTAMP));
        			setTextView(cssnapshotlistcommandfooter, R.id.lastrefreshedtimestamp, parsedDateTime.getString(CsRestContentProvider.TIMESTAMP));
        		}
//    			TextView lastrefresheddatestamp = (TextView)cssnapshotlistcommandfooter.findViewById(R.id.lastrefresheddatestamp);
//    			TextView lastrefreshedtimestamp = (TextView)cssnapshotlistcommandfooter.findViewById(R.id.lastrefreshedtimestamp);
//    			if(lastrefresheddatestamp!=null && lastrefreshedtimestamp!=null && parsedDateTime!=null) {
//    				lastrefresheddatestamp.setText(parsedDateTime.getString(CsRestContentProvider.DATESTAMP));
//    				lastrefreshedtimestamp.setText(parsedDateTime.getString(CsRestContentProvider.TIMESTAMP));
//    			}
        	}
        };
        getActivity().registerReceiver(listSnapshotsCallbackReceiver, new IntentFilter(CsSnapshotListFragment.INTENT_ACTION.CALLBACK_LISTSNAPSHOTS));  //activity will now get intents broadcast by CsRestService (filtered by CALLBACK_LISTSNAPSHOTS action)
	}
	
	public void registerDeleteSnapshotCallbackReceiver() {
		deleteSnapshotCallbackReceiver = new BroadcastReceiver(){
			//This handles callback intents broadcasted by CsRestService
			@Override
			public void onReceive(Context contenxt, Intent intent) {
				Bundle bundle = intent.getExtras();
				final int successOrFailure = bundle.getInt(CsRestService.CALL_STATUS);
				final String snapshotId = bundle.getString(Snapshots.ID);

				//as the request is finished, mark inprogress_state as null signifying there is no pending operation left
				updateSnapshotInProgressStateOnDb(snapshotId, null);

				if(successOrFailure==CsRestService.CALL_STATUS_VALUES.CALL_FAILURE) {
					//if deleteSnapshot failed, revert the progress-circle back to icon again
					adapter.notifyDataSetChanged();  //faking a data set change so the list will refresh itself

				} else {
					//if deleteSnapshot succeeded, CsRestService has already done the deletion for for us, so just stop tracking this id
					Toast.makeText(getActivity(), "Snapshot ("+snapshotId+") deleted", Toast.LENGTH_SHORT).show();
				}
			}
		};
		getActivity().registerReceiver(deleteSnapshotCallbackReceiver, new IntentFilter(CsSnapshotListFragment.INTENT_ACTION.CALLBACK_DELETESNAPSHOT));  //activity will now get intents broadcast by CsRestService (filtered by CALLBACK_DELETESNAPSHOT action)
	}
	
	public void setRefreshButtonClickHandler(final View view) {
		Button refreshbutton = (Button)view.findViewById(R.id.refreshbutton);
		refreshbutton.setOnClickListener(new OnClickListener() {
		    @Override
		    public void onClick(View v) {
		    	makeListSnapshotCall();
		    }
		});
	}
	
	public QuickAction createQuickAction(final View view) {
		final ActionItem deleteSnapshotMenuItem = new ActionItem(0, "Delete", getResources().getDrawable(R.drawable.bin));
		
		//create QuickAction. Use QuickAction.VERTICAL or QuickAction.HORIZONTAL param to define layout orientation
		QuickAction quickAction = new QuickAction(getActivity(), QuickAction.HORIZONTAL);
		
		//add action items into QuickAction
		quickAction.addActionItem(deleteSnapshotMenuItem);
		
		//Set listener for action item clicked
		quickAction.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {          
			@Override
			public void onItemClick(QuickAction source, int pos, int actionId) {
				makeDeleteSnapshotCall(view, CsApiConstants.API.deleteSnapshot);
			}
		});
		quickAction.setAnimStyle(QuickAction.ANIM_GROW_FROM_RIGHT);
		
		return quickAction;
	}
	
	public void makeListSnapshotCall() {
		SharedPreferences preferences = getActivity().getSharedPreferences(CloudStackAndroidClient.SHARED_PREFERENCES.PREFERENCES_NAME, Context.MODE_PRIVATE);
		final String username = preferences.getString(CloudStackAndroidClient.SHARED_PREFERENCES.USERNAME_SETTING, null);

		View cssnapshotlistcommandfooter = getActivity().findViewById(R.id.cssnapshotlistcommandfooter);
		setRefreshButtonEnabled(cssnapshotlistcommandfooter, false);
		setProgressCircleVisible(cssnapshotlistcommandfooter, ProgressBar.VISIBLE);
		
		if(username!=null) {
			//make the rest call to cs server for data
			final String action = CsRestService.TEST_CALL;   
			Bundle apiCmd = new Bundle();
			apiCmd.putString(CsRestService.COMMAND, "listSnapshots");
			apiCmd.putString(Snapshots.ACCOUNT, username);
	        apiCmd.putString(Transactions.CALLBACK_INTENT_FILTER, CsSnapshotListFragment.INTENT_ACTION.CALLBACK_LISTSNAPSHOTS);
			Intent csRestServiceIntent = CsRestService.createCsRestServiceIntent(getActivity(), action, apiCmd);  //user api
			getActivity().startService(csRestServiceIntent);
		}
	}
	
	public void makeDeleteSnapshotCall(View itemView, final String commandName) {
		TextView idText = (TextView)itemView.findViewById(R.id.id);
		final String snapshotId = idText.getText().toString();

		ImageView quickActionIcon = (ImageView)itemView.findViewById(R.id.quickactionicon);
		ProgressBar quickActionProgress = (ProgressBar)itemView.findViewById(R.id.quickactionprogress);
		QuickActionUtils.showQuickActionProgress(quickActionIcon, quickActionProgress, true);
		
		//set the inprogress_state so the ui will know this snapshot has a pending command
		updateSnapshotInProgressStateOnDb(snapshotId, Snapshots.STATE_VALUES.DELETING);

        //make the rest call to cs server to start/stop/reboot vm represented by itemView
        final String action = CsRestService.TEST_CALL;   
        Bundle apiCmd = new Bundle();
        apiCmd.putString(CsRestService.COMMAND, commandName);
        apiCmd.putString(Snapshots.ID, snapshotId);
        apiCmd.putString(Transactions.CALLBACK_INTENT_FILTER, CsSnapshotListFragment.INTENT_ACTION.CALLBACK_DELETESNAPSHOT);
        Intent csRestServiceIntent = CsRestService.createCsRestServiceIntent(getActivity(), action, apiCmd);
        getActivity().startService(csRestServiceIntent);
	}
	
	public void updateSnapshotInProgressStateOnDb(final String snapshotId, String state) {
		ContentValues cv = new ContentValues();
		cv.put(Snapshots.INPROGRESS_STATE, state);
		final String whereClause = Snapshots.ID+"=?";
		final String[] selectionArgs = new String[] { snapshotId };
		getActivity().getContentResolver().update(Snapshots.META_DATA.CONTENT_URI, cv, whereClause, selectionArgs);
	}
	
	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] columns = new String[] {
        		Snapshots._ID,
        		Snapshots.ID,
        		Snapshots.NAME,
        		Snapshots.CREATED,
        		Snapshots.VOLUMENAME,
        		Snapshots.VOLUMETYPE,
        		Snapshots.STATE,
				Snapshots.INPROGRESS_STATE,  //csac-specific field
        };
        CursorLoader cl = new CursorLoader(getActivity(), Snapshots.META_DATA.CONTENT_URI, columns, null, null, null);
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
