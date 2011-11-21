package com.creationline.cloudstack.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.creationline.cloudstack.CloudStackAndroidClient;
import com.creationline.cloudstack.R;
import com.creationline.cloudstack.engine.CsApiConstants;
import com.creationline.cloudstack.engine.CsRestService;
import com.creationline.cloudstack.engine.db.Snapshots;
import com.creationline.cloudstack.engine.db.Transactions;
import com.creationline.cloudstack.util.QuickActionUtils;

public class CsAccountFragment extends Fragment {
	public static class INTENT_ACTION {
		//NOTE: changing the value of this constant requires you to change any usage of the same string in Android.manifest!!!
		public static final String LOGIN = "com.creationline.cloudstack.ui.CsAccountFragment.LOGIN";
	}

    private BroadcastReceiver accountCallbackReceiver = null;  //used to receive request success/failure notifs from CsRestService
 

    public CsAccountFragment() {
    	//empty constructor is needed by Android for automatically creating fragments from XML declarations
    }
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        accountCallbackReceiver = new BroadcastReceiver(){
        	//This handles callback intents broadcasted by CsRestService
        	@Override
        	public void onReceive(Context contenxt, Intent intent) {
        		Bundle bundle = intent.getExtras();
        		final int successOrFailure = bundle.getInt(CsRestService.CALL_STATUS);
        		final String snapshotId = bundle.getString(Snapshots.ID);
        		
        		if(successOrFailure==CsRestService.CALL_STATUS_VALUES.CALL_FAILURE) {
        			
        		} else {
        		
        		}
        	}
        };
        getActivity().registerReceiver(accountCallbackReceiver, new IntentFilter(CsAccountFragment.INTENT_ACTION.LOGIN));  //activity will now get intents broadcast by CsRestService (filtered by LOGIN action)
        

    }
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
        
        super.onActivityCreated(savedInstanceState);
	}
	
	
	@Override
	public void onDestroy() {
		
		if(accountCallbackReceiver!=null) {
			//catch-all here as a safeguard against cases where the activity is exited before BroadcastReceiver.onReceive() has been called-back
			try {
				getActivity().unregisterReceiver(accountCallbackReceiver);
			} catch (IllegalArgumentException e) {
				//will get this exception if snapshotListCallbackReceiver has already been unregistered (or was never registered); will just ignore here
				;
			}
		}
		super.onDestroy();
	}

	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View csaccountfragment = inflater.inflate(R.layout.csaccountfragment, container, false);

		SharedPreferences preferences = getActivity().getSharedPreferences(CloudStackAndroidClient.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
		String savedCsUrl = preferences.getString(CloudStackAndroidClient.CLOUDSTACK_URL_SETTING, null);
		String savedLoginName = preferences.getString(CloudStackAndroidClient.LOGIN_NAME_SETTING, null);
		
        setTextWatcherAndValueForEditText(csaccountfragment, R.id.csinstanceurl, CloudStackAndroidClient.CLOUDSTACK_URL_SETTING, savedCsUrl);
        setTextWatcherAndValueForEditText(csaccountfragment, R.id.loginname, CloudStackAndroidClient.LOGIN_NAME_SETTING, savedLoginName);
		
        setLoginButtonClickHandler(csaccountfragment);
        
		return csaccountfragment;
	}

	public void setTextWatcherAndValueForEditText(final View csaccountfragment, final int idOfEditTextToWatch, final String preferenceKey, final String previousValue) {
		TextWatcher textWatcher = new TextWatcher() {

			@Override
			public void afterTextChanged(Editable arg0) {
				final String inputtedString = arg0.toString();
				SharedPreferences preferences = getActivity().getSharedPreferences(CloudStackAndroidClient.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
				SharedPreferences.Editor preferencesEditor = preferences.edit();
				preferencesEditor.putString(preferenceKey, inputtedString);
				preferencesEditor.commit();
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				;  //do nothing
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				;  //do nothing
			}
        	
        };

        EditText editText = (EditText)csaccountfragment.findViewById(idOfEditTextToWatch);
        editText.addTextChangedListener(textWatcher);
        
        if(previousValue!=null) {
        	editText.setText(previousValue);
        }
	}

	public void setLoginButtonClickHandler(final View csaccountfragment) {
		Button loginButton = (Button)csaccountfragment.findViewById(R.id.loginbutton);
        loginButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				makeLoginCall(csaccountfragment, CsApiConstants.API.stopVirtualMachine);
			}
		});
	}

	public void makeLoginCall(View itemView, final String commandName) {
//		TextView idText = (TextView)itemView.findViewById(R.id.id);
//		final String snapshotId = idText.getText().toString();
//
//		ImageView quickActionIcon = (ImageView)itemView.findViewById(R.id.quickactionicon);
//		ProgressBar quickActionProgress = (ProgressBar)itemView.findViewById(R.id.quickactionprogress);
//		QuickActionUtils.showQuickActionProgress(quickActionIcon, quickActionProgress, true);
//		
//		//set the inprogress_state so the ui will know this snapshot has a pending command
//
//        //make the rest call to cs server to start/stop/reboot vm represented by itemView
//        final String action = CsRestService.TEST_CALL;   
//        Bundle apiCmd = new Bundle();
//        apiCmd.putString(CsRestService.COMMAND, commandName);
//        apiCmd.putString(Snapshots.ID, snapshotId);
//        apiCmd.putString(Transactions.CALLBACK_INTENT_FILTER, CsAccountFragment.INTENT_ACTION.LOGIN);
//        Intent csRestServiceIntent = CsRestService.createCsRestServiceIntent(getActivity(), action, apiCmd);
//        getActivity().startService(csRestServiceIntent);
		
		SharedPreferences preferences = getActivity().getSharedPreferences(CloudStackAndroidClient.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
		String url = preferences.getString(CloudStackAndroidClient.CLOUDSTACK_URL_SETTING, "no value found!!!");
		String login = preferences.getString(CloudStackAndroidClient.LOGIN_NAME_SETTING, "no value found!!!");
		Toast.makeText(getActivity(), "url="+url+"   login="+login, Toast.LENGTH_LONG).show();
	}
	



}
