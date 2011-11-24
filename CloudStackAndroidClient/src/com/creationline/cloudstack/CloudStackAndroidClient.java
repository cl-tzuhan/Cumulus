package com.creationline.cloudstack;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class CloudStackAndroidClient extends FragmentActivity {
	
	public static class SHARED_PREFERENCES {
		//SharedPreference-related constants
		public static String NAME = "CloudStackAndroidClientAccountPreferences";
		public static String CLOUDSTACK_HOST_SETTING = "CloudStackUrl";
		public static String LOGIN_NAME_SETTING = "LoginName";
		public static String APIKEY_SETTING = "ApiKey";
		public static String SECRETKEY_SETTING = "SecretKey";
	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent startCsVmList = new Intent(getApplicationContext(), com.creationline.cloudstack.ui.CsVmList.class);
        startCsVmList.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(startCsVmList);
        
    }

	@Override
	protected void onDestroy() {
		
		
		super.onDestroy();
	}
    
    
}