package com.creationline.cloudstack.ui;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.creationline.cloudstack.R;
import com.creationline.cloudstack.engine.CsApiConstants;
import com.creationline.cloudstack.engine.CsRestService;
import com.creationline.cloudstack.engine.db.Snapshots;
import com.creationline.cloudstack.engine.db.Transactions;
import com.creationline.cloudstack.util.ClLog;
import com.creationline.cloudstack.util.QuickActionUtils;

public class CsSnapshotListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	public static class INTENT_ACTION {
		public static final String DELETESNAPSHOT_COMMAND = "com.creationline.cloudstack.ui.CsSnapshotListFragment.DELETESNAPSHOT_COMMAND";
	}

	private static final int CSSNAPSHOT_LIST_LOADER = 0x02;
    private CsSnapshotListAdapter adapter = null;  //backer for this list
    private BroadcastReceiver snapshotListCallbackReceiver = null;  //used to receive request success/failure notifs from CsRestService
    private static InProgressCache snapshotsWithInProgressRequests = new InProgressCache();  //used to keep track of which snapshotss have requests in-progress
//    private static Bundle snapshotsWithInProgressRequests = new Bundle();
//    private static final int IDLE = 0;
//    private static final int IN_PROGRESS = 1;
//    private static final int SHOW_ICON = 2;
    

    
    private class CsSnapshotListAdapter extends ResourceCursorAdapter {
    	//This adaptor use strictly for use with the CsSnapshot class/layout, and expects specific data to fill its contents.
    	
    	private SimpleDateFormat datetimeParser=null;
		private SimpleDateFormat timePrinter;
		private SimpleDateFormat datePrinter;

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
			
			configureQuickAction(view);
			setStateColor(view);
    	}

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
			} else {
				colorStateList = getResources().getColorStateList(R.color.supplementaryinfo_color_selector);
			}
			stateText.setTextColor(colorStateList);
		}

		public void configureQuickAction(View view) {
			TextView snapshotIdText = (TextView)view.findViewById(R.id.id);
			ImageView quickActionIcon = (ImageView)view.findViewById(R.id.quickactionicon);
			ProgressBar quickActionProgress = (ProgressBar)view.findViewById(R.id.quickactionprogress);
			final String snapshotId = snapshotIdText.getText().toString();
//			int progress = snapshotsWithInProgressRequests.getInt(snapshotId);
			int progress = snapshotsWithInProgressRequests.getProgressForId(snapshotId);
			if(progress==0) {
				//If a snapshot is created on the server-side w/out csac knowing, then we have no IDLE/IN_PROGRESS info for it.
				//Since you can't issue commands to a creating/backing-up snapshot, we will show the progress-circle for
				//these states as well to handle these server-created snapshots.
				TextView stateText = (TextView)view.findViewById(R.id.state);
				final String state = stateText.getText().toString();
				if(Snapshots.STATE_VALUES.CREATING.equalsIgnoreCase(state) || Snapshots.STATE_VALUES.BACKINGUP.equalsIgnoreCase(state)) {
//					progress = IN_PROGRESS;
					progress = InProgressCache.IN_PROGRESS;
				}
			}
			switch(progress) {
//				case IDLE:
				case InProgressCache.IDLE:
					QuickActionUtils.assignQuickActionTo(view, quickActionIcon, createQuickAction(view));
					QuickActionUtils.showQuickActionIcon(quickActionIcon, quickActionProgress, false);
					break;
//				case IN_PROGRESS:
				case InProgressCache.IN_PROGRESS:
					QuickActionUtils.showQuickActionProgress(quickActionIcon, quickActionProgress, false);
					break;
//				case SHOW_ICON:
				case InProgressCache.SHOW_ICON:
					QuickActionUtils.assignQuickActionTo(view, quickActionIcon, createQuickAction(view));
					QuickActionUtils.showQuickActionIcon(quickActionIcon, quickActionProgress, true);
//					snapshotsWithInProgressRequests.remove(snapshotId);
					snapshotsWithInProgressRequests.removeId(snapshotId);
					break;
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
				setCreatedDateTime(view, tv, text);
			} else {
				//for non-special cases, just output text as is
				tv.setText(text);
			}
		}

		/**
		 * Specifically sets the "created" and "createdTime" textviews in the specified view with
		 * parsed output from passed-in datetimeStr.  Format of datetimeStr is assumed to be fixed.
		 * In the case datetimeStr cannot be successfully parsed/formated, the textviews will not be set.
		 * 
		 * @param view view which contains "created" and "createdTime" textviews to set with date/time text
		 * @param tv the "created" textview (this is passed in just to save processing since we have this already in the existing code flow)
		 * @param datetimeStr string in pre-determined format of the datetime to parse
		 */
		public void setCreatedDateTime(View view, TextView tv, String datetimeStr) {
			try {
				//lazy init & re-use
				if(datetimeParser==null) {datetimeParser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");};
				if(datePrinter==null) {datePrinter = new SimpleDateFormat("yyyy-MM-dd");};
				if(timePrinter==null) {timePrinter = new SimpleDateFormat("HH:mm:ss");};
				
				final Date date = datetimeParser.parse(datetimeStr);  //parse the date string
				
				tv.setText(datePrinter.format(date));  //set just the date info
				
				tv = (TextView) view.findViewById(R.id.createdtime);
				tv.setText(timePrinter.format(date));  //set the time info separately from date info

			} catch (ParseException e) {
				//in the case of an un-parse-able datetime str, we will just display the str as is instead of trying to prettify it
				ClLog.e("setTextViewWithString():", "created timestamp could not be parsed; skipping");
				ClLog.e("setTextViewWithString():", e);
			}
		}

		@Override
		public void notifyDataSetChanged() {
			//update the current #-of-snapshots count
			TextView footersnapshotnum = (TextView)getListView().findViewById(R.id.footersnapshotnum);
			if(footersnapshotnum!=null) {
				final int count = getCursor().getCount();
				footersnapshotnum.setText(String.valueOf(count));
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
        
        snapshotListCallbackReceiver = new BroadcastReceiver(){
        	//This handles callback intents broadcasted by CsRestService
        	@Override
        	public void onReceive(Context contenxt, Intent intent) {
        		Bundle bundle = intent.getExtras();
        		final int successOrFailure = bundle.getInt(CsRestService.CALL_STATUS);
        		final String snapshotId = bundle.getString(Snapshots.ID);
        		if(successOrFailure==CsRestService.CALL_STATUS_VALUES.CALL_FAILURE) {
        			//if deleteSnapshot failed, revert the progress-circle back to icon again
//        			snapshotsWithInProgressRequests.putInt(snapshotId, CsSnapshotListFragment.SHOW_ICON);
        			snapshotsWithInProgressRequests.setShowIconForId(snapshotId);
        			adapter.notifyDataSetChanged();  //faking a data set change so the list will refresh itself
        		} else {
        			//if deleteSnapshot succeeded, CsRestService has already done the deletion for for us, so just stop tracking this id
//        			snapshotsWithInProgressRequests.remove(snapshotId);
        			snapshotsWithInProgressRequests.removeId(snapshotId);
    				Toast.makeText(getActivity(), "Snapshot ("+snapshotId+") deleted", Toast.LENGTH_SHORT).show();
        		}
        	}
        };
        getActivity().registerReceiver(snapshotListCallbackReceiver, new IntentFilter(CsSnapshotListFragment.INTENT_ACTION.DELETESNAPSHOT_COMMAND));  //activity will now get intents broadcast by CsRestService (filtered by DELETESNAPSHOT_COMMAND action)
        

        //make the rest call to cs server for data
        final String action = CsRestService.TEST_CALL;   
        Bundle apiCmd = new Bundle();
        apiCmd.putString(CsRestService.COMMAND, "listSnapshots");
        apiCmd.putString(Snapshots.ACCOUNT, "rickson");
        Intent csRestServiceIntent = CsRestService.createCsRestServiceIntent(getActivity(), action, apiCmd);  //user api
        getActivity().startService(csRestServiceIntent);
    }
	
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		//add the custom footer to the list
        View footerView = getLayoutInflater(savedInstanceState).inflate(R.layout.cssnapshotlistfooter, null, false);
        ViewSwitcher vs = (ViewSwitcher)footerView.findViewById(R.id.footerviewswitcher);
        vs.setDisplayedChild(0);
        vs.setAnimateFirstView(true);
        getListView().addFooterView(footerView, null, false);
        
        //set-up the loader & adapter for populating this list
        getLoaderManager().initLoader(CSSNAPSHOT_LIST_LOADER, null, this);
        adapter = new CsSnapshotListAdapter(getActivity().getApplicationContext(), R.layout.cssnapshotlistitem, null, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        setListAdapter(adapter);
        
        super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onDestroy() {
		
		if(snapshotListCallbackReceiver!=null) {
			//catch-all here as a safeguard against cases where the activity is exited before BroadcastReceiver.onReceive() has been called-back
			try {
				getActivity().unregisterReceiver(snapshotListCallbackReceiver);
			} catch (IllegalArgumentException e) {
				//will get this exception if snapshotListCallbackReceiver has already been unregistered (or was never registered); will just ignore here
				;
			}
		}
		super.onDestroy();
	}

	public QuickAction createQuickAction(final View view) {
//		final ActionItem deleteSnapshotMenuItem = new ActionItem(0, "Delete", getResources().getDrawable(R.drawable.menu_eraser));
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
	
	public void makeDeleteSnapshotCall(View itemView, final String commandName) {
		TextView idText = (TextView)itemView.findViewById(R.id.id);
		final String snapshotId = idText.getText().toString();

		ImageView quickActionIcon = (ImageView)itemView.findViewById(R.id.quickactionicon);
		ProgressBar quickActionProgress = (ProgressBar)itemView.findViewById(R.id.quickactionprogress);
		QuickActionUtils.showQuickActionProgress(quickActionIcon, quickActionProgress, true);
		
//		snapshotsWithInProgressRequests.putInt(snapshotId, CsSnapshotListFragment.IN_PROGRESS);
		snapshotsWithInProgressRequests.setInProgressForId(snapshotId);

        //make the rest call to cs server to start/stop/reboot vm represented by itemView
        final String action = CsRestService.TEST_CALL;   
        Bundle apiCmd = new Bundle();
        apiCmd.putString(CsRestService.COMMAND, commandName);
        apiCmd.putString(Snapshots.ID, snapshotId);
        apiCmd.putString(Transactions.CALLBACK_INTENT_FILTER, CsSnapshotListFragment.INTENT_ACTION.DELETESNAPSHOT_COMMAND);
        Intent csRestServiceIntent = CsRestService.createCsRestServiceIntent(getActivity(), action, apiCmd);
        getActivity().startService(csRestServiceIntent);
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
