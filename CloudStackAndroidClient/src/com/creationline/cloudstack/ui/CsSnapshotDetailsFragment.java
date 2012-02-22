/*******************************************************************************
 * Copyright 2011 Creationline,Inc.
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

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.creationline.cloudstack.R;
import com.creationline.cloudstack.engine.CsApiConstants;
import com.creationline.cloudstack.engine.CsRestService;
import com.creationline.cloudstack.engine.db.Snapshots;
import com.creationline.cloudstack.engine.db.Transactions;
import com.creationline.cloudstack.utils.DateTimeParser;
import com.creationline.cloudstack.utils.QuickActionUtils;
import com.creationline.common.utils.ClLog;

public class CsSnapshotDetailsFragment extends Fragment {
	
	private ContentObserver snapshotsContentObserver = null;  //used to receive notifs from CsRestContentProvider upon updates to db
    private BroadcastReceiver snapshotDetailsCallbackReceiver = null;  //used to receive request success/failure notifs from CsRestService


	
	public CsSnapshotDetailsFragment() {
		//empty constructor is needed by Android for automatically creating fragments from XML declarations
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		snapshotDetailsCallbackReceiver = new BroadcastReceiver(){
        	//This handles callback intents broadcasted by CsRestService
        	@Override
        	public void onReceive(Context contenxt, Intent intent) {
        		Bundle bundle = intent.getExtras();
        		final int successOrFailure = bundle.getInt(CsRestService.CALL_STATUS);
        		final String snapshotId = bundle.getString(Snapshots.ID);
        		
        		//as the request is finished, mark inprogress_state as null signifying there is no pending operation left
        		updateSnapshotInProgressStateOnDb(snapshotId, "show_icon");
        		
        		View cssnapshotdetailsfragment = (View)getActivity().findViewById(R.id.cssnapshotdetailsfragment);
				setDisplayWidgetsAndConfigure(cssnapshotdetailsfragment);
        		if(successOrFailure==CsRestService.CALL_STATUS_VALUES.CALL_FAILURE) {
        			//if deleteSnapshot failed, revert the progress-circle back to icon again
//        			snapshotDetailsCallbackReceiver.setShowIconForId(snapshotId);
        		} else {
        			//if deleteSnapshot succeeded, CsRestService has already done the deletion for for us, so just stop tracking this id
//        			snapshotDetailsCallbackReceiver.removeId(snapshotId);
    				Toast.makeText(getActivity(), "Snapshot ("+snapshotId+") deleted", Toast.LENGTH_SHORT).show();
        		}
        	}
        };
        getActivity().registerReceiver(snapshotDetailsCallbackReceiver, new IntentFilter(CsSnapshotListFragment.INTENT_ACTION.CALLBACK_DELETESNAPSHOT));  //activity will now GET intents broadcast by CsRestService (filtered by CALLBACK_DELETESNAPSHOT action)
        
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		registerForSnapshotsDbUpdate();

	}

	@Override
	public void onDestroy() {
		
		unregisterSnapshotsDbUpdate();
		
		super.onDestroy();
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.cssnapshotdetailsfragment, container);
		
		setDisplayWidgetsAndConfigure(view);
		
		deleteSnapshotButtonClickHandler(view);
		
		return view;
	}

	public void setDisplayWidgetsAndConfigure(View view) {
		setTextViewValues(view);
		configureAttributesBasedOnState(view);
	}

	public void setTextViewValues(View view) {
		final String TAG = "CsSnapshotDetailsFragment.setTextViewValues()";
		
		final String selectedSnapshotId = getActivity().getIntent().getStringExtra(Snapshots.class.toString()+Snapshots.ID);
		if(selectedSnapshotId==null) {
			//Null check to guard against cases when CsVmDetailsFragment is called with outdated (i.e. null) snapshotId.
			//Since we can't do anything with this view w/out vm data, just refuse to start in this case.
			ClLog.e(TAG, "aborted start of snapshop details view because selectedSnapshotId=null");
			getActivity().finish();
			return;
		}
		
		final String columns[] = new String[] {
				Snapshots.ID,
				Snapshots.ACCOUNT,
				Snapshots.CREATED,
				Snapshots.DOMAIN,
				Snapshots.DOMAINID,
				Snapshots.INTERVALTYPE,
				Snapshots.JOBID,
				Snapshots.JOBSTATUS,
				Snapshots.NAME,
				Snapshots.SNAPSHOTTYPE,
				Snapshots.STATE,
				Snapshots.VOLUMEID,
				Snapshots.VOLUMENAME,
				Snapshots.VOLUMETYPE,
				Snapshots.INPROGRESS_STATE,  //csac-specific field
		};
		final String whereClause = Snapshots.ID+"=?";
		ClLog.d(TAG, "starting with selectedSnapshotId= "+selectedSnapshotId);
		final String[] selectionArgs = new String[] { selectedSnapshotId };
		Cursor c = getActivity().getContentResolver().query(Snapshots.META_DATA.CONTENT_URI, columns, whereClause, selectionArgs, null);
		
		if(c==null || c.getCount()<=0) {
			//Null check to guard against cases when CsSnapshotDetailsFragment is called with outdated snapshotId.
			//Since we can't do anything with this view w/out snapshot data, just refuse to start in this case.
			if(c!=null) { c.close(); };
			ClLog.e(TAG, "aborted start of snapshop details view, because data does not exist for snapshotId="+selectedSnapshotId);
			getActivity().finish();
			return;
		}
		
		setTextViewWithString(view, R.id.name, c, Snapshots.NAME);
		setTextViewWithString(view, R.id.volumename, c, Snapshots.VOLUMENAME);
		setTextViewWithString(view, R.id.volumetype, c, Snapshots.VOLUMETYPE);
		setTextViewWithString(view, R.id.state, c, Snapshots.STATE);
		
		setTextViewWithString(view, R.id.id, c, Snapshots.ID);
		setTextViewWithString(view, R.id.intervaltype, c, Snapshots.INTERVALTYPE);
		setTextViewWithString(view, R.id.domain, c, Snapshots.DOMAIN);
		setTextViewWithString(view, R.id.snapshottype, c, Snapshots.SNAPSHOTTYPE);
		setTextViewWithString(view, R.id.account, c, Snapshots.ACCOUNT);
		setTextViewWithString(view, R.id.created, c, Snapshots.CREATED);

		setTextViewWithString(view, R.id.inprogress_state, c, Snapshots.INPROGRESS_STATE);
		
		c.close();
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
		final int columnIndex = cursor.getColumnIndex(columnName);
		String text = null;
		if(columnIndex!=-1) {
			text = cursor.getString(columnIndex);
		}
		
		if (text==null) {
			//if we have a missing value, set a null value and let the textview show its hint
			tv.setText(null);
		} else if (textViewId==R.id.created) {
			//format all dates to a more readable format
			TextView timeText = (TextView)view.findViewById(R.id.createdtime);
			DateTimeParser.setParsedDateTime(tv, timeText, text);
		} else if (textViewId==R.id.inprogress_state) {
			//inprogress_state values takes precedence over state values for ui-display purposes
			if(text!=null && !text.equalsIgnoreCase("show_icon")) {
				TextView stateText = (TextView) view.findViewById(R.id.state);
				stateText.setText(text);
			}
			tv.setText(text);
		} else {
			//for non-special cases, just output text as is
			tv.setText(text);
		}
	}

	public void configureAttributesBasedOnState(View view) {
		TextView stateText = (TextView)view.findViewById(R.id.state);
		String state = stateText.getText().toString();
		TextView inProgressStateText = (TextView)view.findViewById(R.id.inprogress_state);

		final String inProgressState = inProgressStateText.getText().toString();
		if(!TextUtils.isEmpty(inProgressState) && !inProgressState.equalsIgnoreCase("show_icon")) {
			state = inProgressState.toString();  //if it exists, inprogress_state values takes precedence over state values for ui-display purposes
		}
		if(inProgressState.equalsIgnoreCase("show_icon")) {
			TextView snapshotIdText = (TextView)view.findViewById(R.id.id);
			final String snapshotId = snapshotIdText.getText().toString();
			updateSnapshotInProgressStateOnDb(snapshotId, null);
		}
		
		//for the vm state text, we change its color depending on the current state of the vm
		if(Snapshots.STATE_VALUES.BACKEDUP.equalsIgnoreCase(state)) {
			stateText.setTextColor(getResources().getColorStateList(R.color.snapshotbackedup_color_selector));
			stateText.startAnimation(QuickActionUtils.getFadein_decelerate());
			setButtonEnabled(view, true);
			makeProgressInvisible(view);
			
		} else if (Snapshots.STATE_VALUES.BACKINGUP.equalsIgnoreCase(state)) {
			stateText.setTextColor(getResources().getColorStateList(R.color.snapshotbackingup_color_selector));
			stateText.startAnimation(QuickActionUtils.getFadein_decelerate());
			setButtonEnabled(view, false);
			setProgressVisible(view);
			
		} else if (Snapshots.STATE_VALUES.CREATING.equalsIgnoreCase(state)) {
			stateText.setTextColor(getResources().getColorStateList(R.color.snapshotcreating_color_selector));
			setButtonEnabled(view, false);
			setProgressVisible(view);
			
		} else if (Snapshots.STATE_VALUES.DELETING.equalsIgnoreCase(state)) {
			stateText.setTextColor(getResources().getColorStateList(R.color.vmstopping_color_selector));
			setButtonEnabled(view, false);
			setProgressVisible(view);
			
		} else {
			//if we run into an unknown state, give...
			stateText.setTextColor(getResources().getColorStateList(R.color.vmunknown_color_selector));  //...state a default color
			setButtonEnabled(view, false);  //...and no buttons to be safe since we don't know which commands may/not work
			makeProgressInvisible(view);
		}
	}

	public void setProgressVisible(View view) {
		ProgressBar progresscircle = (ProgressBar)view.findViewById(R.id.progresscircle);
		if(progresscircle.getVisibility()==View.INVISIBLE) {
			progresscircle.startAnimation(QuickActionUtils.getFadein_decelerate());
			progresscircle.setVisibility(View.VISIBLE);
		}
	}

	public void makeProgressInvisible(View view) {
		ProgressBar progresscircle = (ProgressBar)view.findViewById(R.id.progresscircle);
		if(progresscircle.getVisibility()==View.VISIBLE) {
			progresscircle.startAnimation(QuickActionUtils.getFadeout_decelerate());
			progresscircle.setVisibility(View.INVISIBLE);
		}
	}

	public void setButtonEnabled(View view, boolean deleteSnapshotEnabled) {
		Button deleteSnapshotButton = (Button)view.findViewById(R.id.deletesnapshotbutton);
		deleteSnapshotButton.setEnabled(deleteSnapshotEnabled);
	}


	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {

	}
	
	private void registerForSnapshotsDbUpdate() {
		final Runnable updatedUiWithResults = new Runnable() {
			//This handles notifs from CsRestContentProvider upon changes in db
			public void run() {
				View cssnapshotdetailsfragment = (View)getActivity().findViewById(R.id.cssnapshotdetailsfragment);
				setDisplayWidgetsAndConfigure(cssnapshotdetailsfragment);
			}
		};

		registerForDbUpdate(Snapshots.META_DATA.CONTENT_URI, updatedUiWithResults);
	}

    private void registerForDbUpdate(final Uri contentUriToObserve, final Runnable updatedUiWithResults) {
    	final Handler handler = new Handler();
    	snapshotsContentObserver = new ContentObserver(null) {
    		@Override
    		public void onChange(boolean selfChange) {
    			handler.post(updatedUiWithResults);  //off-loading work to runnable b/c this bg thread can't update ui directly
    		}
    	};
    	getActivity().getContentResolver().registerContentObserver(contentUriToObserve, true, snapshotsContentObserver);  //activity will now GET updated when db is changed
    }
    
	public void unregisterSnapshotsDbUpdate() {
		if(snapshotsContentObserver!=null) {
			getActivity().getContentResolver().unregisterContentObserver(snapshotsContentObserver);
		}
		
		if(snapshotDetailsCallbackReceiver!=null) {
			//catch-all here as a safeguard against cases where the activity is exited before BroadcastReceiver.onReceive() has been called-back
			try {
				getActivity().unregisterReceiver(snapshotDetailsCallbackReceiver);
			} catch (IllegalArgumentException e) {
				//will get this exception if snapshotDetailsCallbackReceiver has already been unregistered (or was never registered); will just ignore here
				;
			}
		}
	}

	public void makeDeleteSnapshotCall(View itemView, final String commandName) {
		TextView idText = (TextView)itemView.findViewById(R.id.id);
		final String snapshotId = idText.getText().toString();

//		TextView stateText = (TextView)itemView.findViewById(R.id.state);
//		stateText.setText(Snapshots.STATE_VALUES.DELETING);
//		configureAttributesBasedOnState(itemView);
		
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
        
//        getActivity().finish();
	}
	
	public void updateSnapshotInProgressStateOnDb(final String snapshotId, String state) {
		ContentValues cv = new ContentValues();
		cv.put(Snapshots.INPROGRESS_STATE, state);
		final String whereClause = Snapshots.ID+"=?";
		final String[] selectionArgs = new String[] { snapshotId };
		getActivity().getContentResolver().update(Snapshots.META_DATA.CONTENT_URI, cv, whereClause, selectionArgs);
	}
	
	public void deleteSnapshotButtonClickHandler(final View view) {
		Button deleteSnapshotButton = (Button)view.findViewById(R.id.deletesnapshotbutton);
		deleteSnapshotButton.setOnClickListener(new OnClickListener() {
		    @Override
		    public void onClick(View v) {
		      	makeDeleteSnapshotCall(view, CsApiConstants.API.deleteSnapshot);
		    }
		  });
	}
	

	
}
