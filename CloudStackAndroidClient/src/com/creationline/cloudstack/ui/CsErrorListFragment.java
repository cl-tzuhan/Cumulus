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

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.ResourceCursorAdapter;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SlidingDrawer;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.creationline.cloudstack.CloudStackAndroidClient;
import com.creationline.cloudstack.R;
import com.creationline.cloudstack.engine.db.Errors;
import com.creationline.cloudstack.util.ClLog;
import com.creationline.cloudstack.util.DateTimeParser;

public class CsErrorListFragment extends CsListFragmentBase implements LoaderManager.LoaderCallbacks<Cursor>, ViewSwitcher.ViewFactory {
	public static String ONDISPLAY_ERROR = "com.creationline.cloudstack.ui.CurrentPageListener.ONDISPLAY_ERROR";
	
	private static final int CSERROR_LIST_LOADER = 0x03;
    private CsErrorListAdapter adapter = null;  //backer for this list
	private ContentObserver errorsContentObserver = null;  //used to receive notifs from CsRestContentProvider upon updates to db
    private boolean isProvisioned = false;  //whether we currently have api/secret key or not

    
    private class CsErrorListAdapter extends ResourceCursorAdapter {
    	//This adaptor used strictly for use with the CsErrorListFragment class/layout, and expects specific data to fill its contents.
    	
		public CsErrorListAdapter(Context context, int layout, Cursor c, int flags) {
			super(context, layout, c, flags);
		}

		@Override
    	public void bindView(View view, Context context, Cursor cursor) {
			setTextViewWithString(view, R.id.dbentryid, cursor, Errors._ID);
			setTextViewWithString(view, R.id.errorcode, cursor, Errors.ERRORCODE);
			setTextViewWithString(view, R.id.errortext, cursor, Errors.ERRORTEXT);
			setTextViewWithString(view, R.id.occurred, cursor, Errors.OCCURRED);
			
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
			final String text = cursor.getString(cursor.getColumnIndex(columnName));
			
			if(textViewId==R.id.occurred) {
				TextView occurredtime = (TextView)view.findViewById(R.id.occurredtime);
				if(text!=null) { DateTimeParser.setParsedDateTime3999(tv, occurredtime, text); }
			} else if(textViewId==R.id.dbentryid) {
				tv.setText(text);
				Button deleteerrorbutton = (Button) view.findViewById(R.id.deleteerrorbutton);
				deleteerrorbutton.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						deleteError(text);
					}
				});
			} else {
				//for non-special cases, just output text as is
				tv.setText(text);
			}
		}

		@Override
		public void notifyDataSetChanged() {
			TextView footererrornum = (TextView)getActivity().findViewById(R.id.footererrornum);
			if(footererrornum!=null) {
				//update the current #-of-errors count
				final int count = getCursor().getCount();
				footererrornum.setText(String.valueOf(count));
			}
			
			super.notifyDataSetChanged();
		}
		
    }
    

    public CsErrorListFragment() {
    	//empty constructor is needed by Android for automatically creating fragments from XML declarations
    }
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        isProvisioned = isProvisioned();  //this needs to be done first as the isProvisioned member var is used at various places
        registerForErrorsDbUpdate(errorsContentObserver);
        
    }
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {

		//add the summary footer to the list
		addAndInitFooter(savedInstanceState, R.layout.cserrorlistsummaryfooter, R.id.cserrorlistsummaryfooterviewswitcher);
		
        //set-up the loader & adapter for populating this list
        getLoaderManager().initLoader(CSERROR_LIST_LOADER, null, this);
        adapter = new CsErrorListAdapter(getActivity().getApplicationContext(), R.layout.cserrorlistitem, null, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        setListAdapter(adapter);
        
        //make the list not visually respond (i.e. highlight) to any clicks on individual items
        getListView().setSelector(android.R.color.transparent);
        
        //set animation for logdrawer
        Activity activity = getActivity();
        final SlidingDrawer logdrawer = (SlidingDrawer)activity.findViewById(R.id.logdrawer);
        final Animation slide_bottomtotop = AnimationUtils.loadAnimation(activity, R.anim.slide_bottomtotop);
        logdrawer.setAnimation(slide_bottomtotop);
        //set bottom half of drawer "cloud" to act as the drawer handle like the top half
        setTextViewAsSecondSlidingDrawerHandle(logdrawer, R.id.logdrawercontenttitle);
        logdrawer.setOnDrawerCloseListener(new SlidingDrawer.OnDrawerCloseListener() {
			@Override
			public void onDrawerClosed() {
				removeErrorLogIconAndText();
			}
		});
        
        //prevent touch events from falling through the drawer (comes into play when we have no errors and no list is shown)
        FrameLayout logdrawercontentbg = (FrameLayout)activity.findViewById(R.id.logdrawercontentbg);
        logdrawercontentbg.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return true;  //eat all events
			}
		});
        
        //set-up error log view to update with animation
        TextSwitcher ts = (TextSwitcher)activity.findViewById(R.id.logdrawertextswitcher);
        ts.setFactory(this);
        Animation fade_in = AnimationUtils.loadAnimation(activity,  android.R.anim.fade_in);
        Animation fade_out = AnimationUtils.loadAnimation(activity, android.R.anim.fade_out);
        ts.setInAnimation(fade_in);
        ts.setOutAnimation(fade_out);
        
        super.onActivityCreated(savedInstanceState);
	}

	public void deleteError(final String _id) {
		final String whereClause = Errors._ID+"=?";
		final String[] selectionArgs = new String[] { _id };
		getActivity().getContentResolver().delete(Errors.META_DATA.CONTENT_URI, whereClause, selectionArgs);
	}
	
	
	@Override
	public void onResume() {
		//set any last-error that was displayed when we paused/closed the app before; or reset the display otherwise
		removeErrorLogIconAndText();
		SharedPreferences preferences = getActivity().getSharedPreferences(CloudStackAndroidClient.SHARED_PREFERENCES.PREFERENCES_NAME, Context.MODE_PRIVATE);
		final String onDisplayError = preferences.getString(CloudStackAndroidClient.SHARED_PREFERENCES.ERRORLOG_ONDISPLAYERROR, null);
		if(!TextUtils.isEmpty(onDisplayError)) {
				setErrorLogIconAndText(onDisplayError);
		}
        
		super.onResume();
	}
	
	@Override
	public void onPause() {
		//save any currently on-display last-error so we can show it again when the error log is re-created in the next screen
		SharedPreferences preferences = getActivity().getSharedPreferences(CloudStackAndroidClient.SHARED_PREFERENCES.PREFERENCES_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = preferences.edit();
		final String onDisplayError = getErrorLogText();
		if(onDisplayError!=null) {
			editor.putString(CloudStackAndroidClient.SHARED_PREFERENCES.ERRORLOG_ONDISPLAYERROR, onDisplayError);
			editor.commit();
		} else {
			editor.remove(CloudStackAndroidClient.SHARED_PREFERENCES.ERRORLOG_ONDISPLAYERROR);
		}
		
		super.onPause();
	}
	
	@Override
	public void onDestroy() {
		//we will not keep the on-display last-error between app restarts
		SharedPreferences preferences = getActivity().getSharedPreferences(CloudStackAndroidClient.SHARED_PREFERENCES.PREFERENCES_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = preferences.edit();
		editor.remove(CloudStackAndroidClient.SHARED_PREFERENCES.ERRORLOG_ONDISPLAYERROR);
		editor.commit();
		
		unregisterFromDbUpdate(errorsContentObserver);
		
		super.onDestroy();
	}
	
    private void registerForErrorsDbUpdate(ContentObserver contentObserver) {
    	final Runnable updatedUiWithResults = new Runnable() {
    		//This handles notifs from CsRestContentProvider upon changes in db
    		public void run() {
    			String TAG = "MultiListUi.registerForErrorsDbUpdate()->errors content observer";
    			Activity activity = getActivity();
				if(activity==null) {
    				ClLog.e(TAG, "activity was null");
    				return;
    			}
    			ContentResolver contentResolver = activity.getContentResolver();
    			if(contentResolver==null) {
    				ClLog.e(TAG, "contentResolver was null");
    				return;
    			}

    			final String columns[] = new String[] {
    					Errors._ID,
    					Errors.ERRORTEXT
    			};
    			Cursor errorLog = getActivity().getContentResolver().query(Errors.META_DATA.CONTENT_URI, columns, null, null, Errors._ID+" DESC");
    			if(errorLog==null || errorLog.getCount()<=0) {
    				ClLog.e(TAG, "Returned errorLog was null or 0 results.");
    				return;
    			}
    			
    			errorLog.moveToFirst();
    			//final int latestErrorMsgId = errorLog.getInt(errorLog.getColumnIndex(Errors._ID));
    			final String latestErrorMsg = errorLog.getString(errorLog.getColumnIndex(Errors.ERRORTEXT));
    			
    			setErrorLogIconAndText(latestErrorMsg);
    		}

    	};
    	registerForDbUpdate(contentObserver, Errors.META_DATA.CONTENT_URI, updatedUiWithResults);
    }
    
    private void registerForDbUpdate(ContentObserver contentObserver, final Uri contentUriToObserve, final Runnable updatedUiWithResults) {
    	final Handler handler = new Handler();
    	contentObserver = new ContentObserver(null) {
    		@Override
    		public void onChange(boolean selfChange) {
    			handler.post(updatedUiWithResults);  //off-loading work to runnable b/c this bg thread can't update ui directly
    		}
    	};
    	getActivity().getContentResolver().registerContentObserver(contentUriToObserve, true, contentObserver);  //activity will now get updated when db is changed
    }
    
	public void unregisterFromDbUpdate(ContentObserver contentObserver) {
		if(contentObserver!=null) {
			getActivity().getContentResolver().unregisterContentObserver(contentObserver);
		}
	}
    
	public void setTextViewAsSecondSlidingDrawerHandle(final SlidingDrawer slidingDrawer, final int contentTopId) {
		TextView logdrawercontenttop = (TextView)getActivity().findViewById(contentTopId);
		logdrawercontenttop.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				//make contentdrawertop also act as a handle for the logdrawer
				return slidingDrawer.dispatchTouchEvent(event);
			}
		});
	}
    
	public void setErrorLogIconAndText(final String latestErrorMsg) {
		Activity activity = getActivity();
		ImageView logdrawericon = (ImageView)activity.findViewById(R.id.logdrawericon);
		final Animation shake = AnimationUtils.loadAnimation(activity, R.anim.shake);
		logdrawericon.startAnimation(shake);
		logdrawericon.setVisibility(View.VISIBLE);

		TextSwitcher logdrawertextswitcher = (TextSwitcher)activity.findViewById(R.id.logdrawertextswitcher);
		logdrawertextswitcher.setText(latestErrorMsg);
	}

	public String getErrorLogText() {
		TextSwitcher logdrawertextswitcher = (TextSwitcher)getActivity().findViewById(R.id.logdrawertextswitcher);
		TextView currentView = (TextView)logdrawertextswitcher.getCurrentView();
		final String onDisplayError = currentView.getText().toString();
		return onDisplayError;
	}

	public void removeErrorLogIconAndText() {
		Activity activity = getActivity();
		ImageView logdrawericon = (ImageView)activity.findViewById(R.id.logdrawericon);
		if(logdrawericon.getVisibility()==View.VISIBLE) {
			Animation fade_out = AnimationUtils.loadAnimation(activity, android.R.anim.fade_out);
			logdrawericon.startAnimation(fade_out);
			logdrawericon.setVisibility(View.INVISIBLE);
		}

		TextSwitcher logdrawertextswitcher = (TextSwitcher)activity.findViewById(R.id.logdrawertextswitcher);
		logdrawertextswitcher.setText("");
	}
	
	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] columns = new String[] {
        		Errors._ID,
        		Errors.ERRORCODE,
        		Errors.ERRORTEXT,
        		Errors.ORIGINATINGCALL,
        		Errors.OCCURRED,
        		Errors.UNREAD,
        };
        CursorLoader cl = new CursorLoader(getActivity(), Errors.META_DATA.CONTENT_URI, columns, null, null, Errors._ID+" DESC");
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
	
	@Override
	public View makeView() {
		TextView t = new TextView(getActivity());
		t.setTextSize(15);
		t.setTextColor(getResources().getColor(R.color.error));
		t.setSingleLine();
		t.setHorizontalFadingEdgeEnabled(true);
		return t;
	}
	
}
