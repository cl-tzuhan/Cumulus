package com.creationline.cloudstack.ui;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.widget.ListView;

import com.creationline.cloudstack.R;
import com.creationline.cloudstack.engine.db.Vms;
import com.creationline.cloudstack.util.ClLog;

public class CsVmListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final int CSVM_LIST_LOADER = 0x01;
    private SimpleCursorAdapter adapter = null;

    public CsVmListFragment() {
    	//empty constructor is needed by Android for automatically creating fragments from XML declarations
    }
    
    /** Called when the activity is first created. */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        
//      ///DEBUG
//      List<Map<String, String>> data = new ArrayList<Map<String,String>>();
//      for( int i = 1; i <= 10; i++ )
//      {
//          Map<String, String> item = new HashMap<String, String>();
//          item.put( "name", String.format( "Item %d", i ) );
//          item.put( "desc", "a short description");
//          data.add( item );
//      }
//      String[] from = { "name", "desc" };
//      int[] to = { R.id.request, R.id.status };
//      ///endDEBUG
//      SimpleAdapter listAdaptor = new SimpleAdapter(getActivity(), data, R.layout.csvmlistitem, from, to);
//      setListAdapter(listAdaptor);
        ///DEBUG

        //init and set up the adaptor that populates this list (actual data will be gotten in onCreateLoader()
        String[] bindFrom = {
        		Vms.DISPLAYNAME,
        		Vms.GROUPA
        };
        int[] bindTo = {
        		R.id.request,
        		R.id.status
        };
        getLoaderManager().initLoader(CSVM_LIST_LOADER, null, this);
        adapter = new SimpleCursorAdapter(getActivity().getApplicationContext(), R.layout.csvmlistitem, null, bindFrom, bindTo, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        setListAdapter(adapter);
        
        //set a display message for cases when there is no data
        setEmptyText(getResources().getString(R.string.status_noVms));
        
    }
    
    public void onListItemClick(ListView l, View v, int position, long id) {
    	ClLog.i("FragmentList", "Item clicked: " + id);
    }
    
    
    
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        inflater.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE); 
//        View view = inflater.inflate(R.layout.csvmlistfragment, null);
//        return view;
//    }
    
    
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] columns = new String[] {
        		Vms._ID,
        		Vms.DISPLAYNAME,
        		Vms.GROUPA
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
