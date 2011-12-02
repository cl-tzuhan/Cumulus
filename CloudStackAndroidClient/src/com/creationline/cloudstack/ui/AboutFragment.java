package com.creationline.cloudstack.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.creationline.cloudstack.R;
import com.creationline.cloudstack.util.Revision;
import com.creationline.cloudstack.util.Version;

public class AboutFragment extends Fragment {
	
	public AboutFragment() {
		//empty constructor is needed by Android for automatically creating fragments from XML declarations
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.aboutfragment, container);
		
		TextView version = (TextView)view.findViewById(R.id.version);
		version.setText(Version.asString());
		
		TextView revision = (TextView)view.findViewById(R.id.revision);
		revision.setText(String.valueOf(Revision.REVISION_NO));
		
		return view;
	}



	
}
