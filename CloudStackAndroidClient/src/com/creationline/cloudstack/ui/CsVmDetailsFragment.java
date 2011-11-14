package com.creationline.cloudstack.ui;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.creationline.cloudstack.R;
import com.creationline.cloudstack.engine.db.Vms;
import com.creationline.cloudstack.util.ClLog;

public class CsVmDetailsFragment extends Fragment {

	
	public CsVmDetailsFragment() {
		
		///DEBUG
		System.out.println("we are in CsVmDetailsFragment constructor!!!!!!!");
		///endDEBUG
	}
	
	
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		///DEBUG
		System.out.println("we are in onCraete()!!!!!!!");
		///endDEBUG
	}



	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		///DEBUG
		System.out.println("we are in onActivityCreated()!!!!!!!");
		///endDEBUG
	}



	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.csvmdetailsfragment, container);
		
		
		final String columns[] = new String[] {
				Vms.ID,
				Vms.NAME,
				Vms.DISPLAYNAME,
				Vms.ACCOUNT,
//				Vms.DOMAINID,
				Vms.DOMAIN,
				Vms.CREATED,
				Vms.STATE,
//				Vms.GROUPID,
				Vms.GROUPA,
				Vms.HAENABLE,
//				Vms.HOSTID,
				Vms.HOSTNAME,
//				Vms.ZONEID,
				Vms.ZONENAME,
//				Vms.TEMPLATEID,
//				Vms.TEMPLATENAME,
				Vms.TEMPLATEDISPLAYTEXT,
//				Vms.PASSWORDENABLED,
//				Vms.SERVICEOFFERINGID,
				Vms.SERVICEOFFERINGNAME,
				Vms.STATE,
				Vms.CPUNUMBER,
				Vms.CPUSPEED,
				Vms.MEMORY,
//				Vms.CPUUSED,
//				Vms.NETWORKKBSREAD,
//				Vms.NETWORKKBSWRITE,
//				Vms.GUESTOSID,
//				Vms.ROOTDEVICEID,
//				Vms.ROOTDEVICETYPE,
//				Vms.NIC,
				Vms.HYPERVISOR};
		final String whereClause = Vms.ID+"=?";
		final String selectedVmId = getActivity().getIntent().getStringExtra(Vms.class.toString()+Vms.ID);
		ClLog.d("CsVmDetailsFragment.onActivityCreated()", "starting with selectedVmId= "+selectedVmId);
		final String[] selectionArgs = new String[] { selectedVmId };
		Cursor c = getActivity().getContentResolver().query(Vms.META_DATA.CONTENT_URI, columns, whereClause, selectionArgs, null);
		
		TextView textView = null;
		textView = (TextView)view.findViewById(R.id.displayname);
		textView.setText(c.getString(c.getColumnIndex(Vms.DISPLAYNAME)));
		textView = (TextView)view.findViewById(R.id.name);
		textView.setText(c.getString(c.getColumnIndex(Vms.NAME)));
		textView = (TextView)view.findViewById(R.id.state);
		textView.setText(c.getString(c.getColumnIndex(Vms.STATE)));
		
		textView = (TextView)view.findViewById(R.id.cpunumber);
		textView.setText(c.getString(c.getColumnIndex(Vms.CPUNUMBER)));
		textView = (TextView)view.findViewById(R.id.cpuspeed);
		textView.setText(c.getString(c.getColumnIndex(Vms.CPUSPEED)));
		textView = (TextView)view.findViewById(R.id.memory);
		textView.setText(c.getString(c.getColumnIndex(Vms.MEMORY)));
		
		textView = (TextView)view.findViewById(R.id.id);
		textView.setText(c.getString(c.getColumnIndex(Vms.ID)));
		textView = (TextView)view.findViewById(R.id.zonename);
		textView.setText(c.getString(c.getColumnIndex(Vms.ZONENAME)));
		textView = (TextView)view.findViewById(R.id.hypervisor);
		textView.setText(c.getString(c.getColumnIndex(Vms.HYPERVISOR)));
		textView = (TextView)view.findViewById(R.id.templatedisplaytext);
		textView.setText(c.getString(c.getColumnIndex(Vms.TEMPLATEDISPLAYTEXT)));
		textView = (TextView)view.findViewById(R.id.serviceofferingname);
		textView.setText(c.getString(c.getColumnIndex(Vms.SERVICEOFFERINGNAME)));
		textView = (TextView)view.findViewById(R.id.haenabled);
		textView.setText(c.getString(c.getColumnIndex(Vms.HAENABLE)));
		textView = (TextView)view.findViewById(R.id.hostname);
		textView.setText(c.getString(c.getColumnIndex(Vms.HOSTNAME)));
		textView = (TextView)view.findViewById(R.id.group);
		textView.setText(c.getString(c.getColumnIndex(Vms.GROUPA)));
		textView = (TextView)view.findViewById(R.id.domain);
		textView.setText(c.getString(c.getColumnIndex(Vms.DOMAIN)));
		textView = (TextView)view.findViewById(R.id.account);
		textView.setText(c.getString(c.getColumnIndex(Vms.ACCOUNT)));
		textView = (TextView)view.findViewById(R.id.created);
		textView.setText(c.getString(c.getColumnIndex(Vms.CREATED)));
		
		///DEBUG
		System.out.println("we are in onCreateView()!!!!!!!");
		///endDEBUG
		
		return view;
	}



	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		
	}


	
}
