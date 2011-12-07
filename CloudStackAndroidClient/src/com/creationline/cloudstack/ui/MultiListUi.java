package com.creationline.cloudstack.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.SimpleOnPageChangeListener;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.creationline.cloudstack.CloudStackAndroidClient;
import com.creationline.cloudstack.R;
import com.creationline.cloudstack.util.QuickActionUtils;
import com.viewpagerindicator.TitlePageIndicator;

public class MultiListUi extends FragmentActivity /*implements ViewSwitcher.ViewFactory*/ {
	
//	private BroadcastReceiver broadcastReceiver = null;
	
	
	//class to cache the currently-shown page of the ViewPager
	private static class CurrentPageListener extends SimpleOnPageChangeListener {
		public static String CURRENT_PAGE = "com.creationline.cloudstack.ui.CurrentPageListener.CURRENT_PAGE";
		private static int currentPage;
		
		public static int getCurrentPage() {
			return currentPage;
		}

		public void onPageSelected(int page) {
			currentPage = page;
		}
	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.multilistui);
        
        //bind the viewpager to the backing adaptor
        ViewPageAdapter vpa = new ViewPageAdapter(getSupportFragmentManager());
        ViewPager vp = (ViewPager)findViewById(R.id.viewpager);
        vp.setAdapter(vpa);
        
        //bind the titlepageindicator to the viewpager
        TitlePageIndicator tpi = (TitlePageIndicator)findViewById(R.id.viewpagerindicator);
        tpi.setViewPager(vp, 0);
        tpi.setOnPageChangeListener(new CurrentPageListener());
        final Animation slide_leftToRight_slow = AnimationUtils.loadAnimation(this, R.anim.slide_lefttoright_slow);
        tpi.setAnimation(slide_leftToRight_slow);
        
        //set animation for apptitle
        TextView apptitle_pt1 = (TextView)findViewById(R.id.apptitle_pt1);
        TextView apptitle_pt2 = (TextView)findViewById(R.id.apptitle_pt2);
        Animation slide_rightToLeft_slow = AnimationUtils.loadAnimation(this, R.anim.slide_righttoleft_slow);
        apptitle_pt1.setAnimation(slide_rightToLeft_slow);
        apptitle_pt2.setAnimation(slide_rightToLeft_slow);
        
//        //set animation for logdrawer
//        final SlidingDrawer logdrawer = (SlidingDrawer)findViewById(R.id.logdrawer);
//        final Animation slide_bottomtotop = AnimationUtils.loadAnimation(this, R.anim.slide_bottomtotop);
//        logdrawer.setAnimation(slide_bottomtotop);
//        //set bottom half of drawer "cloud" to act as the drawer handle like the top half
//        setTextViewAsSecondSlidingDrawerHandle(logdrawer, R.id.name);
//        logdrawer.setOnDrawerCloseListener(new SlidingDrawer.OnDrawerCloseListener() {
//			@Override
//			public void onDrawerClosed() {
//				removeErrorLogIconAndText();
//			}
//		});
        
//        //prevent touch events from falling through the drawer (comes into play when we have no errors and no list is shown)
//        FrameLayout logdrawercontentbg = (FrameLayout)findViewById(R.id.logdrawercontentbg);
//        logdrawercontentbg.setOnTouchListener(new View.OnTouchListener() {
//			@Override
//			public boolean onTouch(View v, MotionEvent event) {
//				return true;  //eat all events
//			}
//		});
//        
//        //set-up error log view to update with animation
//        TextSwitcher ts = (TextSwitcher)findViewById(R.id.errorLogTextView);
//        ts.setFactory(this);
//        Animation fade_in = AnimationUtils.loadAnimation(this,  android.R.anim.fade_in);
//        Animation fade_out = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
//        ts.setInAnimation(fade_in);
//        ts.setOutAnimation(fade_out);
                
//        registerForErrorsDbUpdate();
        
        new QuickActionUtils(this);
        
        
		//select the starting page shown to user depending on whether we are provisioned or not
		SharedPreferences preferences = getSharedPreferences(CloudStackAndroidClient.SHARED_PREFERENCES.PREFERENCES_NAME, Context.MODE_PRIVATE);
		final String savedApiKey = preferences.getString(CloudStackAndroidClient.SHARED_PREFERENCES.APIKEY_SETTING, null);
        boolean isProvisioned = savedApiKey!=null;
		if(isProvisioned) {
			vp.setCurrentItem(ViewPageAdapter.INSTANCES_PAGE);
		} else {
			vp.setCurrentItem(ViewPageAdapter.ACCOUNT_PAGE);
		}
    }

//	public void setTextViewAsSecondSlidingDrawerHandle(final SlidingDrawer slidingDrawer, final int contentTopId) {
//		TextView logdrawercontenttop = (TextView)findViewById(contentTopId);
//		logdrawercontenttop.setOnTouchListener(new View.OnTouchListener() {
//			@Override
//			public boolean onTouch(View v, MotionEvent event) {
//				//make contentdrawertop also act as a handle for the logdrawer
//				return slidingDrawer.dispatchTouchEvent(event);
//			}
//		});
//	}

//    private void registerForErrorsDbUpdate() {
//    	final Runnable updatedUiWithResults = new Runnable() {
//    		//This handles notifs from CsRestContentProvider upon changes in db
//    		public void run() {
//    			final String columns[] = new String[] {
//    					Errors._ID,
//    					Errors.ERRORTEXT
//    			};
//    			Cursor errorLog = getContentResolver().query(Errors.META_DATA.CONTENT_URI, columns, null, null, Errors._ID+" DESC");
//    			if(errorLog==null || errorLog.getCount()<=0) {
//    				ClLog.e("MultiListUi.registerForErrorsDbUpdate()->errors content observer", "Returned errorLog was null or 0 results.");
//    				return;
//    			}
//    			
//    			errorLog.moveToFirst();
//    			//final int latestErrorMsgId = errorLog.getInt(errorLog.getColumnIndex(Errors._ID));
//    			final String latestErrorMsg = errorLog.getString(errorLog.getColumnIndex(Errors.ERRORTEXT));
//    			
//    			setErrorLogIconAndText(latestErrorMsg);
//    		}
//
//    	};
//    	registerForDbUpdate(Errors.META_DATA.CONTENT_URI, updatedUiWithResults);
//    }
//    
//    private void registerForDbUpdate(final Uri contentUriToObserve, final Runnable updatedUiWithResults) {
//    	final Handler handler = new Handler();
//    	ContentObserver contentObserver = new ContentObserver(null) {
//    		@Override
//    		public void onChange(boolean selfChange) {
//    			handler.post(updatedUiWithResults);  //off-loading work to runnable b/c this bg thread can't update ui directly
//    		}
//    	};
//    	getContentResolver().registerContentObserver(contentUriToObserve, true, contentObserver);  //activity will now get updated when db is changed
//    }

	@Override
	protected void onPause() {
//		Intent csRestServiceIntent = new Intent(this, CsRestService.class);
//        stopService(csRestServiceIntent);
        
		super.onPause();
	}

	@Override
	protected void onSaveInstanceState(Bundle savedInstanceState) {
		//save the currently shown page index so we can show it again when app resumes
		savedInstanceState.putInt(CurrentPageListener.CURRENT_PAGE, CurrentPageListener.getCurrentPage());

		super.onSaveInstanceState(savedInstanceState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		//if resuming, show the last page shown when app went into pause
		final int currentPage = savedInstanceState.getInt(CurrentPageListener.CURRENT_PAGE);
		ViewPager vp = (ViewPager)findViewById(R.id.viewpager);
		vp.setCurrentItem(currentPage);
	}


	@Override
	protected void onDestroy() {
		
//		if(broadcastReceiver!=null) {
//			//catch-all here as a safeguard against cases where the activity is exited before BroadcastReceiver.onReceive() has been called-back
//			try {
//				unregisterReceiver(broadcastReceiver);
//			} catch (IllegalArgumentException e) {
//				//will get this exception if broadcastReceiver has already been unregistered (or was never registered); will just ignore here
//				;
//			}
//		}
		super.onDestroy();
	}
	

//	@Override
//	public View makeView() {
//		TextView t = new TextView(this);
//		t.setTextSize(15);
//		t.setTextColor(getResources().getColor(R.color.error));
//		t.setSingleLine();
//		t.setHorizontalFadingEdgeEnabled(true);
//		return t;
//	}


	public void onContentDrawerTopClick(View view) {
		Toast.makeText(getApplicationContext(), "clickeD!", Toast.LENGTH_SHORT).show();

	}
	
//	public void setErrorLogIconAndText(final String latestErrorMsg) {
//		ImageView logdrawericon = (ImageView)findViewById(R.id.logdrawericon);
//		final Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
//		logdrawericon.startAnimation(shake);
//		logdrawericon.setVisibility(View.VISIBLE);
//
//		TextSwitcher errorLogTextView = (TextSwitcher)findViewById(R.id.errorLogTextView);
//		errorLogTextView.setText(latestErrorMsg);
//	}
//
//	public void removeErrorLogIconAndText() {
//		ImageView logdrawericon = (ImageView)findViewById(R.id.logdrawericon);
//		if(logdrawericon.getVisibility()==View.VISIBLE) {
//			Animation fade_out = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
//			logdrawericon.startAnimation(fade_out);
//			logdrawericon.setVisibility(View.INVISIBLE);
//		}
//
//		TextSwitcher errorLogTextView = (TextSwitcher)findViewById(R.id.errorLogTextView);
//		errorLogTextView.setText("");
//	}
    
}
