package com.creationline.cloudstack;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import com.creationline.cloudstack.engine.CsRestService;
import com.creationline.cloudstack.engine.db.Vms;

public class CsVmList extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        
        final String action = CsRestService.TEST_CALL;

        
        BroadcastReceiver broadcastreceiver = new BroadcastReceiver(){
        	//This handles intents broadcasted by CsRestService
        	@Override
        	public void onReceive(Context arg0, Intent arg1) {
        		String responseString = arg1.getStringExtra(CsRestService.RESPONSE);
        		Toast.makeText(getBaseContext(), "CsRestService: request "+responseString+" initiated...", Toast.LENGTH_SHORT).show();
        		unregisterReceiver(this); //unregister self once the broadcast has been received
        	}
        };
        registerReceiver(broadcastreceiver, new IntentFilter(action));  //activity will now get intents broadcast by CsRestService (filtered by action str)
        
        
        final Runnable updatedUiWithResults = new Runnable() {
        	//This handles notifs from CsRestContentProvider upon changes in db
        	public void run() {
        		Toast.makeText(getBaseContext(), "Got a notif from VMS!!!!!!", Toast.LENGTH_LONG).show();
        	}
        };
        final Handler handler = new Handler();
        ContentObserver vmsObserver = new ContentObserver(null) {
        	@Override
        	public void onChange(boolean selfChange) {
        		handler.post(updatedUiWithResults);  //off-loading work to runnable b/c this bg thread can't update ui directly
        	}
        };
        getContentResolver().registerContentObserver(Vms.META_DATA.CONTENT_URI, true, vmsObserver);  //activity will now get updated when vms db is changed
        
        
        
//        Intent csRestServiceIntent = CsRestService.createCsRestServiceIntent(this, action, "http://192.168.3.11:8096/?command=listVirtualMachines&account=thsu-account&domainid=2&response=json");  //admin api
//        Intent csRestServiceIntent = CsRestService.createCsRestServiceIntent(this, action, "command=listVirtualMachines&account=thsu-account&domainid=2");  //user api
//        Intent csRestServiceIntent = CsRestService.createCsRestServiceIntent(this, action, "command=listVirtualMachines&account=iizuka1");  //user api
        Bundle apiCmd = new Bundle();
        apiCmd.putString(CsRestService.COMMAND, "listVirtualMachines");
        apiCmd.putString("account", "iizuka1");
        Intent csRestServiceIntent = CsRestService.createCsRestServiceIntent(this, action, apiCmd);  //user api
        startService(csRestServiceIntent);
        
        
    }

	@Override
	protected void onDestroy() {
		
		
		super.onDestroy();
	}
    
    
}