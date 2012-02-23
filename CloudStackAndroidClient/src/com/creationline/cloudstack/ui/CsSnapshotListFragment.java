/*******************************************************************************
 * Copyright 2011-2012 Creationline,Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.creationline.cloudstack.ui;

import java.util.ArrayList;
import java.util.List;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.ResourceCursorAdapter;
import android.text.TextUtils;
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
import com.creationline.cloudstack.utils.DateTimeParser;
import com.creationline.cloudstack.utils.QuickActionUtils;
import com.creationline.common.engine.RestServiceBase;

public class CsSnapshotListFragment extends CsListFragmentBase implements LoaderManager.LoaderCallbacks<Cursor> {
	public static class INTENT_ACTION {
		//NOTE: changing the value of these constants requires you to change any usage of the same string in Android.manifest!!!
		public static final String CALLBACK_LISTSNAPSHOTS = "com.creationline.cloudstack.ui.CsSnapshotListFragment.CALLBACK_LISTSNAPSHOTS";
		public static final String CALLBACK_DELETESNAPSHOT = "com.creationline.cloudstack.ui.CsSnapshotListFragment.CALLBACK_DELETESNAPSHOT";
	}

	private static final int CSSNAPSHOT_LIST_LOADER = 0x02;
    private ResourceCursorAdapter adapter = null;  //backer for this list
    private BroadcastReceiver listSnapshotsCallbackReceiver = null;  //used to receive list snapshots complete notifs from CsRestService
    private BroadcastReceiver deleteSnapshotCallbackReceiver = null;  //used to receive request success/failure notifs from CsRestService
    private boolean isProvisioned = false;  //whether we currently have api/secret key or not
    private List<String> animateQuickActionButtonSwitchList = new ArrayList<String>();  //ids in this list represent snapshot list items that should animate next time there is an icon->progresscircle/progresscircle->icon switch for its quickaction button

    //constants used as keys for saving/restoring app state on pause/resume
    private static final String CSSNAPSHOTLIST_DATESTAMP = "com.creationline.cloudstack.ui.CsSnapshotListFragment.CSSNAPSHOTLIST_DATESTAMP";
    private static final String CSSNAPSHOTLIST_TIMESTAMP = "com.creationline.cloudstack.ui.CsSnapshotListFragment.CSSNAPSHOTLIST_TIMESTAMP";
    
    private class CsSnapshotListAdapter extends ResourceCursorAdapter {
    	//This adaptor used strictly for use with the CsSnapshotListFragment class/layout, and expects specific data to fill its contents.
    	
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
			overrideStateWithInProgressStateIfNeeded(view, cursor);

			configureAttributesBasedOnState(view, cursor);
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
			} else if (Snapshots.STATE_VALUES.DELETING.equalsIgnoreCase(state)) {
				colorStateList = getResources().getColorStateList(R.color.vmstopping_color_selector);
			} else {
				colorStateList = getResources().getColorStateList(R.color.supplementaryinfo_color_selector);
			}
			stateText.setTextColor(colorStateList);
		}

		public void configureAttributesBasedOnState(View view, Cursor cursor) {
			ImageView quickActionIcon = (ImageView)view.findViewById(R.id.quickactionicon);
			ProgressBar quickActionProgress = (ProgressBar)view.findViewById(R.id.quickactionprogress);
			
			boolean animate = false;
			String state = cursor.getString(cursor.getColumnIndex(Snapshots.STATE));
			final String inProgressState = cursor.getString(cursor.getColumnIndex(Snapshots.INPROGRESS_STATE));
			if(!TextUtils.isEmpty(inProgressState)) {
				state = inProgressState;  //if it exists, inprogress_state values takes precedence over state values for ui-display purposes
			}
			final String snapshotId = cursor.getString(cursor.getColumnIndex(Snapshots.ID));
			if(animateQuickActionButtonSwitchList.contains(snapshotId)) {
				animate = true;  //any items in the animateQuickActionButtonSwitchList are meant to have an animated transition from icon->progresscircle or vice versa
				animateQuickActionButtonSwitchList.remove(snapshotId);  //we only need the animation to play once
			}
			
			quickActionIcon.setEnabled(true);
			//for the vm state text, we change its color depending on the current state of the vm
			if(Snapshots.STATE_VALUES.BACKEDUP.equalsIgnoreCase(state)) {
				QuickActionUtils.assignQuickActionTo(view, quickActionIcon, createQuickAction(view));
				QuickActionUtils.showQuickActionIcon(quickActionIcon, quickActionProgress, animate);
				
			} else if (Snapshots.STATE_VALUES.BACKINGUP.equalsIgnoreCase(state)) {
				QuickActionUtils.showQuickActionProgress(quickActionIcon, quickActionProgress, animate);
				
			} else if (Snapshots.STATE_VALUES.CREATING.equalsIgnoreCase(state)) {
				QuickActionUtils.showQuickActionProgress(quickActionIcon, quickActionProgress, animate);
				
			} else if (Snapshots.STATE_VALUES.DELETING.equalsIgnoreCase(state)) {
				QuickActionUtils.showQuickActionProgress(quickActionIcon, quickActionProgress, animate);
				
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
			} else {
				//for non-special cases, just output text as is
				tv.setText(text);
			}
		}
		
		public void overrideStateWithInProgressStateIfNeeded(View view, Cursor cursor) {
			final String inProgressState = cursor.getString(cursor.getColumnIndex(Snapshots.INPROGRESS_STATE));
			if(!TextUtils.isEmpty(inProgressState)) {
				//we replace the actual state with any existing inprogress state
				TextView stateText = (TextView) view.findViewById(R.id.state);
				stateText.setText(inProgressState);
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
        
        isProvisioned = isProvisioned();  //this needs to be done first as the isProvisioned member var is used at various places

        registerListSnapshotsCallbackReceiver();
        registerDeleteSnapshotCallbackReceiver();
    }
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		
		adapter = setupListAdapter(CSSNAPSHOT_LIST_LOADER);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		//add the summary footer to the list
		addAndInitFooter(savedInstanceState, R.layout.cssnapshotlistsummaryfooter, R.id.cssnapshotlistsummaryfooterviewswitcher);
        
        //add command footer to the list
		View cssnapshotlistcommandfooter = addAndInitFooter(savedInstanceState, R.layout.cssnapshotlistcommandfooter, R.id.cssnapshotlistcommandfooterviewswitcher);
        
        setRefreshButtonClickHandler(cssnapshotlistcommandfooter);
		if(isProvisioned) {
			setRefreshButtonEnabled(cssnapshotlistcommandfooter, true);
        } else {
        	setRefreshButtonEnabled(cssnapshotlistcommandfooter, false);
        }
        
		adapter = setupListAdapter(CSSNAPSHOT_LIST_LOADER);
        
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
        
		super.onResume();
	}
	
	@Override
	public void onPause() {
		
		super.onPause();
	}

	@Override
	public void onDestroy() {
		unregisterCallbackReceiver(listSnapshotsCallbackReceiver);
		unregisterCallbackReceiver(deleteSnapshotCallbackReceiver);

		releaseListAdapter();
		
		super.onDestroy();
	}

	public void releaseListAdapter() {
		//zero-out list adapter-related references so gc can work
		getLoaderManager().destroyLoader(CSSNAPSHOT_LIST_LOADER);
		setListAdapter(null);
		adapter = null;
	}
	
	public ResourceCursorAdapter setupListAdapter(final int listLoaderId) {
		//if we have previous, existing adapter (say, from orientation change), just use it instead of creating new one to prevent mem leak
		ResourceCursorAdapter listAdapter = (ResourceCursorAdapter)getListAdapter();
		if(listAdapter==null) {
			//set-up the loader & adapter for populating this list
			getLoaderManager().initLoader(listLoaderId, null, this);
			listAdapter = new CsSnapshotListAdapter(getActivity().getApplicationContext(), R.layout.cssnapshotlistitem, null, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
		}
		//we want to null-and-set the listAdapter explicitly regardless of whether we are
		//creating from scratch or re-setting an existing adapter b/c if we don't do this,
		//the list seems to lose track of the display of the footers (the list seems to
		//recognize the footers as existing, the footer ids exist in the layout, but are
		//not shown on screen after the list view is reloaded after being swiped too
		//far off-screen (current guess is that being swiped off the multi-list screen
		//results in a state that is not quite paused (destroys ui-level elements),
		//but not quite destroyed either (adapter, non-ui part of footer, layout)))
		setListAdapter(null);
		setListAdapter(listAdapter);
		return listAdapter;
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
        		final String updatedUriStr = bundle.getString(RestServiceBase.PAYLOAD_FIELDS.UPDATED_URI);
        		if(updatedUriStr==null) {
        			return;
        		}

        		final Bundle parsedDateTime =  CsRestContentProvider.getReplyDateTimeFor(activity, updatedUriStr);
        		if(parsedDateTime!=null) {
        			setTextView(cssnapshotlistcommandfooter, R.id.lastrefresheddatestamp, parsedDateTime.getString(CsRestContentProvider.DATESTAMP));
        			setTextView(cssnapshotlistcommandfooter, R.id.lastrefreshedtimestamp, parsedDateTime.getString(CsRestContentProvider.TIMESTAMP));
        		}
        	}
        };
        getActivity().registerReceiver(listSnapshotsCallbackReceiver, new IntentFilter(CsSnapshotListFragment.INTENT_ACTION.CALLBACK_LISTSNAPSHOTS));  //activity will now GET intents broadcast by CsRestService (filtered by CALLBACK_LISTSNAPSHOTS action)
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
				//and mark list item to animate progresscircle->icon
				//(note that we will attempt to update the inprogress_state regardless of whether the delete call
				// succeeded or not.  If it didn't, we are reverting the snapshot to the state previous the delete
				// call.  If it did, there is no entry on db to update and the update call will just do nothing)
				animateQuickActionButtonSwitchList.add(snapshotId);
				updateSnapshotInProgressStateOnDb(snapshotId, null);

				if(successOrFailure==CsRestService.CALL_STATUS_VALUES.CALL_FAILURE) {
					; //do nothing on failure as the updateSnapshotInProgressStateOnDb() will force a list reload
				} else {
					//if deleteSnapshot succeeded, CsRestService has already done the deletion for for us, so just stop tracking this id
					Toast.makeText(getActivity(), "Snapshot ("+snapshotId+") deleted", Toast.LENGTH_SHORT).show();
				}
			}
		};
		getActivity().registerReceiver(deleteSnapshotCallbackReceiver, new IntentFilter(CsSnapshotListFragment.INTENT_ACTION.CALLBACK_DELETESNAPSHOT));  //activity will now GET intents broadcast by CsRestService (filtered by CALLBACK_DELETESNAPSHOT action)
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

		//set the inprogress_state as deleting and mark list item for icon->progresscircle animation
		animateQuickActionButtonSwitchList.add(snapshotId);
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
