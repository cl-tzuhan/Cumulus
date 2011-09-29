package com.creationline.cloudstack;

import com.creationline.cloudstack.engine.CsRestService;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.Toast;

public class CsVmList extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        System.out.println("This is a test");
        
        final String action = CsRestService.TEST_CALL;

        
        BroadcastReceiver broadcastreceiver = new BroadcastReceiver(){
	            @Override
	            public void onReceive(Context arg0, Intent arg1) {
	            	String responseString = arg1.getStringExtra(CsRestService.RESPONSE);
	            	Toast.makeText(getBaseContext(), "CsRestService: will have gotten reply = "+responseString, Toast.LENGTH_LONG).show();
	            	unregisterReceiver(this); //unregister self once the broadcast has been received
	            }
            };
        registerReceiver(broadcastreceiver, new IntentFilter(action));
        
        Intent csRestServiceIntent = CsRestService.createCsRestServiceIntent(this, action, "http://192.168.3.11:8096/?command=listVirtualMachines&account=thsu-account&domainid=2&response=json");
        startService(csRestServiceIntent);
        
        
    }

	@Override
	protected void onDestroy() {
		
		
		super.onDestroy();
	}
    
    
}