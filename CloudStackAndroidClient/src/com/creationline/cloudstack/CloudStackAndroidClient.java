package com.creationline.cloudstack;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class CloudStackAndroidClient extends FragmentActivity {
	
	public static class SHARED_PREFERENCES {
		//SharedPreference-related constants
		public static String PREFERENCES_NAME = "CloudStackAndroidClientAccountPreferences";
		public static String LOGIN_INPROGRESS = "LoginInProgress";  //boolean
		public static String CLOUDSTACK_HOST_SETTING = "CloudStackUrl";
		public static String USERNAME_SETTING = "LoginName";
		public static String APIKEY_SETTING = "ApiKey";
		public static String SECRETKEY_SETTING = "SecretKey";
		public static String LOGINERROR_CACHE = "LoginErrorCache";
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