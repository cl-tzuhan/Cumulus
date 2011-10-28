package com.creationline.cloudstack.engine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class CheckAsyncJobProgress extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		//start up CsRestService to make a queryAsycnJobResult call against the passed in jobid
		
		Bundle bundle = intent.getExtras();
		String jobid = bundle.getString("jobid");
		
		final String action = CsRestService.TEST_CALL;   
        Bundle apiCmd = new Bundle();
        apiCmd.putString(CsRestService.COMMAND, "queryAsyncJobResult");
        apiCmd.putString("jobid", jobid);
        Intent csRestServiceIntent = CsRestService.createCsRestServiceIntent(context, action, apiCmd);
        context.startService(csRestServiceIntent);			
	}
	
}
