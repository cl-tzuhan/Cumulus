/*******************************************************************************
 * Copyright 2011 Creationline,Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
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
