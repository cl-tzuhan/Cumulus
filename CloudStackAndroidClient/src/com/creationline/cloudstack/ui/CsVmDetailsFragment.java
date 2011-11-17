package com.creationline.cloudstack.ui;

import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.creationline.cloudstack.R;
import com.creationline.cloudstack.engine.db.Vms;
import com.creationline.cloudstack.util.ClLog;
import com.creationline.cloudstack.util.DateTimeParser;
import com.creationline.cloudstack.util.QuickActionUtils;

public class CsVmDetailsFragment extends Fragment {
	
	private ContentObserver vmsContentObserver = null;

	
	public CsVmDetailsFragment() {
		//empty constructor is needed by Android for automatically creating fragments from XML declarations
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
		
		registerForVmsDbUpdate();
		
		///DEBUG
		System.out.println("we registered for Vms db updates!!!!!!!");
		///endDEBUG
	}

	@Override
	public void onDestroy() {
		
		unregisterVmsDbUpdate();
		
		super.onDestroy();
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.csvmdetailsfragment, container);
		
		setTextViewValues(view);
		
		TextView stateText = (TextView)view.findViewById(R.id.state);
		setStateColor(stateText);
		
		return view;
	}

	public void setTextViewValues(View view) {
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
				Vms.CPUUSED,
				Vms.NETWORKKBSREAD,
				Vms.NETWORKKBSWRITE,
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
		
		setTextViewWithString(view, R.id.displayname, c, Vms.DISPLAYNAME);
		setTextViewWithString(view, R.id.name, c, Vms.NAME);
		setTextViewWithString(view, R.id.state, c, Vms.STATE);
		
		setTextViewWithString(view, R.id.cpunumber, c, Vms.CPUNUMBER);
		setTextViewWithString(view, R.id.cpuspeed, c, Vms.CPUSPEED);
		setTextViewWithString(view, R.id.memory, c, Vms.MEMORY);

		setTextViewWithString(view, R.id.cpuused, c, Vms.CPUUSED);
		setTextViewWithString(view, R.id.networkkbread, c, Vms.NETWORKKBSREAD);
		setTextViewWithString(view, R.id.networkkbwrite, c, Vms.NETWORKKBSWRITE);

		setTextViewWithString(view, R.id.id, c, Vms.ID);
		setTextViewWithString(view, R.id.zonename, c, Vms.ZONENAME);
		setTextViewWithString(view, R.id.hypervisor, c, Vms.HYPERVISOR);
		setTextViewWithString(view, R.id.templatedisplaytext, c, Vms.TEMPLATEDISPLAYTEXT);
		setTextViewWithString(view, R.id.serviceofferingname, c, Vms.SERVICEOFFERINGNAME);
		setTextViewWithString(view, R.id.haenabled, c, Vms.HAENABLE);
		setTextViewWithString(view, R.id.hostname, c, Vms.HOSTNAME);
		setTextViewWithString(view, R.id.group, c, Vms.GROUPA);
		setTextViewWithString(view, R.id.domain, c, Vms.DOMAIN);
		setTextViewWithString(view, R.id.account, c, Vms.ACCOUNT);
		setTextViewWithString(view, R.id.created, c, Vms.CREATED);
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
			//format all dates to a more readable format
			DateTimeParser.setCreatedDateTime(view, tv, text);
		} else if(textViewId==R.id.haenabled) {
			//substitute an easier-to-read string for this instead of true/false
			final String enabledOrDisabled = ("true").equalsIgnoreCase(text)? "enabled" : "disabled";
			tv.setText(enabledOrDisabled);
		} else if(textViewId==R.id.cpuused) {
			if(text!=null) {
				//remove any percentage signs from the value since we already have a '%'label on the ui
				final int indexOfPercentSign = text.indexOf('%');
				if(indexOfPercentSign!=-1) { text = text.substring(0, indexOfPercentSign); }
				tv.setText(text);
			}
		} else if(textViewId==R.id.networkkbread || textViewId==R.id.networkkbwrite) {
			if(text!=null) {
				//show all network transfer values in GB truncated to 2 decimal places
				final int valueInKb = Integer.valueOf(text);
				final double valueInGb = valueInKb / 1048576.0;
				text = String.format("%.2f", valueInGb);
				tv.setText(text);
			}
		} else {
			//for non-special cases, just output text as is
			tv.setText(text);
		}
	}

	public void setStateColor(TextView stateText) {
		final String state = stateText.getText().toString();
		
		//for the vm state text, we change its color depending on the current state of the vm
		if(Vms.STATE_VALUES.RUNNING.equalsIgnoreCase(state)) {
			stateText.setTextColor(getResources().getColorStateList(R.color.vmrunning_color_selector));
			stateText.startAnimation(QuickActionUtils.getFadein_decelerate());
			
		} else if (Vms.STATE_VALUES.STOPPED.equalsIgnoreCase(state)) {
			stateText.setTextColor(getResources().getColorStateList(R.color.vmstopped_color_selector));
			stateText.startAnimation(QuickActionUtils.getFadein_decelerate());
			
		} else if (Vms.STATE_VALUES.STARTING.equalsIgnoreCase(state)) {
			stateText.setTextColor(getResources().getColorStateList(R.color.vmstarting_color_selector));
			
		} else if (Vms.STATE_VALUES.STOPPING.equalsIgnoreCase(state)) {
			stateText.setTextColor(getResources().getColorStateList(R.color.vmstopping_color_selector));
			
		}  else if (Vms.STATE_VALUES.REBOOTING.equalsIgnoreCase(state)) {
			stateText.setTextColor(getResources().getColorStateList(R.color.vmrebooting_color_selector));
			
		} else {
			//if we run into an unknown state, give...
			stateText.setTextColor(getResources().getColorStateList(R.color.vmunknown_color_selector));  //...state a default color
			//...and no quickaction (perhaps change the icon to something looking unclick-able here?)
		}
	}



	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		
	}
	
	private void registerForVmsDbUpdate() {
		final Runnable updatedUiWithResults = new Runnable() {
			//This handles notifs from CsRestContentProvider upon changes in db
			public void run() {
				View csvmdetailview = (View) getActivity().findViewById(R.id.csvmdetailview);
				setTextViewValues(csvmdetailview);
				TextView stateText = (TextView)getActivity().findViewById(R.id.state);
				setStateColor(stateText);
			}
		};

		registerForDbUpdate(Vms.META_DATA.CONTENT_URI, updatedUiWithResults);
	}

    private void registerForDbUpdate(final Uri contentUriToObserve, final Runnable updatedUiWithResults) {
    	final Handler handler = new Handler();
    	vmsContentObserver = new ContentObserver(null) {
    		@Override
    		public void onChange(boolean selfChange) {
    			handler.post(updatedUiWithResults);  //off-loading work to runnable b/c this bg thread can't update ui directly
    		}
    	};
    	getActivity().getContentResolver().registerContentObserver(contentUriToObserve, true, vmsContentObserver);  //activity will now get updated when db is changed
    }
    
	public void unregisterVmsDbUpdate() {
		if(vmsContentObserver!=null) {
			getActivity().getContentResolver().unregisterContentObserver(vmsContentObserver);
		}
	}

	
}
