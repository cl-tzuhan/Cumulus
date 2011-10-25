package com.creationline.cloudstack.ui;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.SimpleCursorAdapter;

import com.creationline.cloudstack.engine.db.Transactions;

public class CsSnapshotList extends ListFragment {
    private static final int CSSNAPSHOT_LIST_LOADER = 0x02;
    private SimpleCursorAdapter adapter = null;

    public CsSnapshotList() {
    	//empty constructor is needed by Android for automatically creating fragments from XML declarations
    }
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
//        getLoaderManager().initLoader(CSSNAPSHOT_LIST_LOADER, null, this);
        String[] from = new String[] {"request"};
        int[] to = new int[] {android.R.id.text1};
        String[] cols = new String[] {"_id", "request"};
        Cursor c = getActivity().getContentResolver().query(Transactions.META_DATA.CONTENT_URI, cols, null, null, null);
        adapter = new SimpleCursorAdapter(getActivity().getApplicationContext(), android.R.layout.simple_list_item_1, c, from, to);
//        adapter = new SimpleAdapter(getActivity().getApplicationContext(), R.layout.csvmlistitem, null, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        setListAdapter(adapter);
        
//        setEmptyText("This is the empty Snapshot list");
    }
}
