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

public class MultiListUi extends FragmentActivity {
	
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

	@Override
	protected void onPause() {
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
		super.onDestroy();
	}
	
}
