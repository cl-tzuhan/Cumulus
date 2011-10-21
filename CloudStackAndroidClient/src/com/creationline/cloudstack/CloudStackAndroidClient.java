package com.creationline.cloudstack;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class CloudStackAndroidClient extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent startCsVmList = new Intent(getApplicationContext(), com.creationline.cloudstack.ui.CsVmList.class);
        startActivity(startCsVmList);
        
    }

	@Override
	protected void onDestroy() {
		
		
		super.onDestroy();
	}
    
    
}