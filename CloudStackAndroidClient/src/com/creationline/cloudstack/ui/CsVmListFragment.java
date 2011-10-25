package com.creationline.cloudstack.ui;

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
import android.widget.ListView;
import android.widget.TextView;

import com.creationline.cloudstack.R;
import com.creationline.cloudstack.engine.CsRestService;
import com.creationline.cloudstack.engine.db.Vms;
import com.creationline.cloudstack.util.ClLog;

public class CsVmListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final int CSVM_LIST_LOADER = 0x01;
    private CsVmListAdapter adapter = null;
    

    private class CsVmListAdapter extends ResourceCursorAdapter {
    	//This adaptor use strictly for use with the CsVmList class/layout, and expects specific data to fill its contents.
    	
    	private int _layout;  //unused as the onClickListner-related code is currently unncessary

    	public CsVmListAdapter(Context context, int layout, Cursor c, int flags) {
			super(context, layout, c, flags);
			_layout = layout;
		}

		@Override
    	public void bindView(View view, Context context, Cursor cursor) {
			setTextViewWithString(view, R.id.displayname, cursor, Vms.DISPLAYNAME);
			setTextViewWithString(view, R.id.name, cursor, Vms.NAME);
			setTextViewWithString(view, R.id.state, cursor, Vms.STATE);
			setTextViewWithString(view, R.id.serviceofferingname, cursor, Vms.SERVICEOFFERINGNAME);
			setTextViewWithString(view, R.id.templatedisplaytext, cursor, Vms.TEMPLATEDISPLAYTEXT);
			setTextViewWithString(view, R.id.hypervisor, cursor, Vms.HYPERVISOR);
			setTextViewWithString(view, R.id.cpunumber, cursor, Vms.CPUNUMBER);
			setTextViewWithString(view, R.id.cpuspeed, cursor, Vms.CPUSPEED);
			setTextViewWithString(view, R.id.memory, cursor, Vms.MEMORY);
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

			if(textViewId==R.id.state) {
				//for the vm state text, we change its color depending on the current state of the vm
				String stateStr = tv.getText().toString();
				if("running".equalsIgnoreCase(stateStr)) {
					tv.setTextColor(getResources().getColorStateList(R.color.vmrunning_color_selector));
				} else if ("stopped".equalsIgnoreCase(stateStr)) {
					tv.setTextColor(getResources().getColorStateList(R.color.vmstopped_color_selector));
				} else {
					tv.setTextColor(getResources().getColorStateList(R.color.vmunknown_color_selector));
				}
			}
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
    

	public void onListItemClick(ListView l, View v, int position, long id) {
    	ClLog.i("FragmentList", "Item clicked: " + id);
    }
    
    
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] columns = new String[] {
        		Vms._ID,
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
