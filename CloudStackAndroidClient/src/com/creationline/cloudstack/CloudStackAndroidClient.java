package com.creationline.cloudstack;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class CloudStackAndroidClient extends FragmentActivity {
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