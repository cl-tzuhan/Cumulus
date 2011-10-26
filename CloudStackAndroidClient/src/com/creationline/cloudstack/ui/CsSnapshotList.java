package com.creationline.cloudstack.ui;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

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
import android.widget.TextView;

import com.creationline.cloudstack.R;
import com.creationline.cloudstack.engine.CsRestService;
import com.creationline.cloudstack.engine.db.Snapshots;
import com.creationline.cloudstack.util.ClLog;

public class CsSnapshotList extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final int CSSNAPSHOT_LIST_LOADER = 0x02;
    private CsSnapshotListAdapter adapter = null;
    
    
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
			setTextViewWithString(view, R.id.name, cursor, Snapshots.NAME);
			setTextViewWithString(view, R.id.created, cursor, Snapshots.CREATED);
			setTextViewWithString(view, R.id.volumename, cursor, Snapshots.VOLUMENAME);
			setTextViewWithString(view, R.id.volumetype, cursor, Snapshots.VOLUMETYPE);
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
		
    }
    

    public CsSnapshotList() {
    	//empty constructor is needed by Android for automatically creating fragments from XML declarations
    }
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        ///DEBUG
//        String[] from = new String[] {"request"};
//        int[] to = new int[] {android.R.id.text1};
//        String[] cols = new String[] {"_id", "request"};
//        Cursor c = getActivity().getContentResolver().query(Transactions.META_DATA.CONTENT_URI, cols, null, null, null);
//        adapter = new SimpleCursorAdapter(getActivity().getApplicationContext(), android.R.layout.simple_list_item_1, c, from, to);
//        setListAdapter(adapter);
        ///endDEBUG
        
        //make the rest call to cs server for data
        final String action = CsRestService.TEST_CALL;   
        Bundle apiCmd = new Bundle();
        apiCmd.putString(CsRestService.COMMAND, "listSnapshots");
        apiCmd.putString("account", "rickson");
        Intent csRestServiceIntent = CsRestService.createCsRestServiceIntent(getActivity(), action, apiCmd);  //user api
        getActivity().startService(csRestServiceIntent);
      
        //set-up the loader & adapter for populating this list
        getLoaderManager().initLoader(CSSNAPSHOT_LIST_LOADER, null, this);
        adapter = new CsSnapshotListAdapter(getActivity().getApplicationContext(), R.layout.cssnapshotlistitem, null, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        setListAdapter(adapter);
    }
	
	
	
	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] columns = new String[] {
        		Snapshots._ID,
        		Snapshots.NAME,
        		Snapshots.CREATED,
        		Snapshots.VOLUMENAME,
        		Snapshots.VOLUMETYPE,
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
