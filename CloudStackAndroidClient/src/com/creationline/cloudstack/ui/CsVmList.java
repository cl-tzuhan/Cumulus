package com.creationline.cloudstack.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.creationline.cloudstack.R;
import com.creationline.cloudstack.engine.CsRestService;
import com.creationline.cloudstack.engine.db.Errors;
import com.creationline.cloudstack.engine.db.Transactions;
import com.viewpagerindicator.TitlePageIndicator;

public class CsVmList extends FragmentActivity implements ViewSwitcher.ViewFactory {
	
	private BroadcastReceiver broadcastReceiver = null;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.csvmlist);
        
        // Create the list fragment and add it as our sole content.
//        if (getSupportFragmentManager().findFragmentById(R.id.listfragment) == null) {
//            CsVmListFragment list = new CsVmListFragment();
//            getSupportFragmentManager().beginTransaction().add(R.id.listfragment, list).commit();
//        }
        
        //bind the viewpager to the backing adaptor
        ViewPageAdapter vpa = new ViewPageAdapter(getSupportFragmentManager());
        ViewPager vp = (ViewPager)findViewById(R.id.viewpager);
        vp.setAdapter(vpa);
        
        //bind the titlepageindicator to the viewpager
        TitlePageIndicator tpi = (TitlePageIndicator)findViewById(R.id.viewpagerindicator);
        tpi.setViewPager(vp, 0);
        final Animation slide_leftToRight_slow = AnimationUtils.loadAnimation(this, R.anim.slide_lefttoright_slow);
        tpi.setAnimation(slide_leftToRight_slow);
        
        //set animation for apptitle
        TextView tv = (TextView)findViewById(R.id.apptitle);
        Animation slide_rightToLeft_slow = AnimationUtils.loadAnimation(this, R.anim.slide_righttoleft_slow);
        tv.setAnimation(slide_rightToLeft_slow);
        
        //set-up error log view to update with animation
        TextSwitcher ts = (TextSwitcher) findViewById(R.id.errorLogTextView);
        ts.setFactory(this);
        Animation in = AnimationUtils.loadAnimation(this,  android.R.anim.fade_in);
        Animation out = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
        ts.setInAnimation(in);
        ts.setOutAnimation(out);
                
        final String action = CsRestService.TEST_CALL;     
        broadcastReceiver = new BroadcastReceiver(){
        	//This handles intents broadcasted by CsRestService
        	@Override
        	public void onReceive(Context arg0, Intent arg1) {
        		String responseString = arg1.getStringExtra(CsRestService.RESPONSE);
        		Toast.makeText(getBaseContext(), "CsRestService: request "+responseString+" initiated...", Toast.LENGTH_SHORT).show();
        	}
        };
        registerReceiver(broadcastReceiver, new IntentFilter(action));  //activity will now get intents broadcast by CsRestService (filtered by action str)
        
        
        registerForErrorsDbUpdate();
        //registerForVmsDbUpdate();
        
    }

    
    private void registerForErrorsDbUpdate() {
    	final Runnable updatedUiWithResults = new Runnable() {
    		//This handles notifs from CsRestContentProvider upon changes in db
    		public void run() {
    			final String columns[] = new String[] {
    					Errors._ID,
    					Errors.ERRORTEXT
    			};
    			Cursor errorLog = getContentResolver().query(Errors.META_DATA.CONTENT_URI, columns, null, null, "_ID DESC");
    			errorLog.moveToFirst();
    			final int latestErrorMsgId = errorLog.getInt(errorLog.getColumnIndex(Errors._ID));
    			final String latestErrorMsg = errorLog.getString(errorLog.getColumnIndex(Errors.ERRORTEXT));
    			
    			TextSwitcher errorLogTextView = (TextSwitcher)findViewById(R.id.errorLogTextView);
    			errorLogTextView.setText(latestErrorMsgId+": "+latestErrorMsg);
    		}
    	};
    	
    	registerForDbUpdate(Errors.META_DATA.CONTENT_URI, updatedUiWithResults);
    }
    
    private void registerForVmsDbUpdate() {
    	final Runnable updatedUiWithResults = new Runnable() {
    		//This handles notifs from CsRestContentProvider upon changes in db
    		public void run() {
    			Toast.makeText(getBaseContext(), "Got a notif from vms!!!!!!", Toast.LENGTH_SHORT).show();
    		}
    	};
    	
    	registerForDbUpdate(Transactions.META_DATA.CONTENT_URI, updatedUiWithResults);
    }
    
    private void registerForDbUpdate(final Uri contentUriToObserve, final Runnable updatedUiWithResults) {
    	final Handler handler = new Handler();
    	ContentObserver contentObserver = new ContentObserver(null) {
    		@Override
    		public void onChange(boolean selfChange) {
    			handler.post(updatedUiWithResults);  //off-loading work to runnable b/c this bg thread can't update ui directly
    		}
    	};
    	getContentResolver().registerContentObserver(contentUriToObserve, true, contentObserver);  //activity will now get updated when vms db is changed
    }

    protected void onclick_sideMenu(View view) {
    	//TODO: do something with me or remove!!
    	Toast.makeText(getBaseContext(), "-> got clicked!", Toast.LENGTH_SHORT).show();
    }

    
	@Override
	protected void onPause() {
		Intent csRestServiceIntent = new Intent(this, CsRestService.class);
        stopService(csRestServiceIntent);
        
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		
		if(broadcastReceiver!=null) {
			//catch-all here as a safeguard against cases where the activity is exited before BroadcastReceiver.onReceive() has been called-back
			try {
				unregisterReceiver(broadcastReceiver);
			} catch (IllegalArgumentException e) {
				//will get this exception if broadcastReceiver has already been unregistered (or was never registered); will just ignore here
				;
			}
		}
		super.onDestroy();
	}

	@Override
	public View makeView() {
		TextView t = new TextView(this);
		t.setGravity(Gravity.CENTER | Gravity.CENTER_HORIZONTAL);
		t.setTextSize(15);
		t.setTextColor(Color.YELLOW);
		return t;
	}


    
}
