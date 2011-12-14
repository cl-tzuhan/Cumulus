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
package com.creationline.cloudstack;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.view.MotionEvent;

import com.creationline.cloudstack.util.ClLog;

public class CloudStackAndroidClient extends FragmentActivity {
	
	public static class SHARED_PREFERENCES {
		//SharedPreference-related constants
		public static String PREFERENCES_NAME = "CloudStackAndroidClientAccountPreferences";
		
		public static String CLOUDSTACK_HOST_SETTING = "CloudStackUrl";
		public static String CLOUDSTACK_DOMAIN_SETTING = "CloudStackDomain";
		public static String USERNAME_SETTING = "LoginName";
		public static String APIKEY_SETTING = "ApiKey";
		public static String SECRETKEY_SETTING = "SecretKey";
		
		public static String LOGIN_INPROGRESS = "LoginInProgress";  //actual stored value is a boolean
		public static String LOGINERROR_CACHE = "LoginErrorCache";
		
		public static String ERRORLOG_ONDISPLAYERROR = "ErrorLog_onDisplayError";
	}
	
	private Thread startMultiListUiThread = null;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if(isStartWithNoAccountData()) {
        	setContentView(R.layout.cloudstackandroidclient);
        	showLogoScreenThenStartMultiListUi();  
        } else {
        	startMultiListUi();
        }
    }

	public boolean isStartWithNoAccountData() {
		SharedPreferences preferences = getSharedPreferences(CloudStackAndroidClient.SHARED_PREFERENCES.PREFERENCES_NAME, Context.MODE_PRIVATE);
        final String savedCsHost = preferences.getString(CloudStackAndroidClient.SHARED_PREFERENCES.CLOUDSTACK_HOST_SETTING, null);
        final String savedCsDomain = preferences.getString(CloudStackAndroidClient.SHARED_PREFERENCES.CLOUDSTACK_DOMAIN_SETTING, null);
        final String savedUsername = preferences.getString(CloudStackAndroidClient.SHARED_PREFERENCES.USERNAME_SETTING, null);
		final String savedApiKey = preferences.getString(CloudStackAndroidClient.SHARED_PREFERENCES.APIKEY_SETTING, null);
		
		final boolean noSavedCsHost = TextUtils.isEmpty(savedCsHost);
		final boolean noSavedCsDomain = TextUtils.isEmpty(savedCsDomain);
		final boolean noSavedUsername = TextUtils.isEmpty(savedUsername);
		final boolean noSavedApiKey = TextUtils.isEmpty(savedApiKey);
		final boolean noSavedAccountInfo = noSavedCsHost && noSavedCsDomain && noSavedUsername && noSavedApiKey;
		
		return noSavedAccountInfo;
	}

	public void showLogoScreenThenStartMultiListUi() {
		startMultiListUiThread = new Thread(){
            @Override
            public void run(){
                try {
                    synchronized(this){
                        wait(3000);  //wait to show user logo screen
                        startMultiListUi();
                    }
                }
                catch(InterruptedException ex){    
                	ClLog.e("startMultiListUiThread.run()", "startMultiListUiThread wait threw exception!");
                	ClLog.e("startMultiListUiThread.run()", ex);
                }
            }
        };
        startMultiListUiThread.start();
	}
	
    public void startMultiListUi() {
    	Intent startMultiListUi = new Intent(getApplicationContext(), com.creationline.cloudstack.ui.MultiListUi.class);
    	startMultiListUi.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    	startActivity(startMultiListUi);
    }

    @Override
    public boolean onTouchEvent(MotionEvent evt)
    {
    	//allow user to skip 3 second wait if she taps the screen
        if(evt.getAction() == MotionEvent.ACTION_DOWN)
        {
            synchronized(startMultiListUiThread){
            	startMultiListUiThread.notifyAll();
            }
        }
        return true;
    }    


	@Override
	protected void onDestroy() {
		
		
		super.onDestroy();
	}
    
}
