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
package com.creationline.cloudstack.utils;

import net.londatiga.android.QuickAction;
import android.content.Context;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.creationline.cloudstack.R;

public class QuickActionUtils {

    //animation caches (so we don't need to continually re-created these same animations)
    private static Animation fadein_decelerate = null;
	private static Animation fadeout_decelerate = null;
    
    
    /*
     * Class to handle touch events and respond with haptic feedback.
     * Copied from:
     *   http://androidcookbook.com/Recipe.seam;jsessionid=96AABBB097E371280776B227D746ED26?recipeId=1242&recipeFrom=ViewTOC
     * Haptic feedback set-up code for Android by Adrian Cowham is licensed under AndroidCookbook.com's
     * Creative Commons CC-BY license.
     */
    public static class HapticTouchListener implements OnTouchListener {

        private final int feedbackType;
         
        public HapticTouchListener( int type ) { feedbackType = type; }
         
        public int feedbackType() { return feedbackType; }
         
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            // only perform feedback when the user touches the view, as opposed
            // to lifting a finger off the view
            if( event.getAction() == MotionEvent.ACTION_DOWN ){
                // perform the feedback
                v.performHapticFeedback( feedbackType() );
            }
            return false;  //thsu edit [2011-11-14]: returning false so onClick listener also has a chance to handle the input
        }
    }
    
    
	public QuickActionUtils(Context context) {
        //init global animation cache
        fadein_decelerate = AnimationUtils.loadAnimation(context, R.anim.fadein_decelerate);
        fadeout_decelerate = AnimationUtils.loadAnimation(context, R.anim.fadeout_decelerate);
	}
	
	public static Animation getFadein_decelerate() {
		return fadein_decelerate;
	}

	public static Animation getFadeout_decelerate() {
		return fadeout_decelerate;
	}
	
	/**
	 * Sets the supplied quickAction as the onClick handler for the ImageView specified by
	 * quickactionIconId in the view.
	 * 
	 * @param view View containing ImageView specified by quickActionIconId
	 * @param quickActionIcon id of ImageView to use as the icon/"button" trigger for this quickaction menu
	 * @param quickAction quickaction to assign to the onClick handler
	 */
	public static void assignQuickActionTo(View view, ImageView quickActionIcon, final QuickAction quickAction) {
		//set-up onclick listener to show the quickaction menu
		quickActionIcon.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				quickAction.show(v);
			}
		});
		
		//set-up ontouch listener solely for producing haptic feedback
		quickActionIcon.setOnTouchListener( new HapticTouchListener(HapticFeedbackConstants.KEYBOARD_TAP) );   
	}
	
	public static void showQuickActionIcon(final ImageView quickActionIcon, final ProgressBar quickActionProgress, final boolean animate) {
		if(animate==false && quickActionIcon.getVisibility()==View.VISIBLE && quickActionProgress.getVisibility()==View.INVISIBLE) {
			return;
		}
		
		if(animate) {
			quickActionIcon.startAnimation(fadein_decelerate);
		}
		quickActionIcon.setVisibility(View.VISIBLE);
		quickActionIcon.setClickable(true);
		
		if(animate) {
			quickActionProgress.startAnimation(fadeout_decelerate);
		}
		quickActionProgress.setVisibility(View.INVISIBLE);
	}
	
	public static void showQuickActionProgress(final ImageView quickActionIcon, final ProgressBar quickActionProgress, final boolean animate) {
		if(animate==false && quickActionIcon.getVisibility()==View.INVISIBLE && quickActionProgress.getVisibility()==View.VISIBLE) {
			return;
		}
		
		quickActionIcon.setClickable(false);
		if(animate) {
			quickActionIcon.startAnimation(fadeout_decelerate);
		}
		quickActionIcon.setVisibility(View.INVISIBLE);
		
		if(animate) {
			quickActionProgress.startAnimation(fadein_decelerate);
		}
		quickActionProgress.setVisibility(View.VISIBLE);
	}

	public static void showNeitherQuickAction(final ImageView quickActionIcon, final ProgressBar quickActionProgress, final boolean animate) {
		quickActionIcon.setClickable(false);
		if(animate) {
			quickActionIcon.startAnimation(fadeout_decelerate);
		}
		quickActionIcon.setVisibility(View.INVISIBLE);
		
		if(animate) {
			quickActionProgress.startAnimation(fadeout_decelerate);
		}
		quickActionProgress.setVisibility(View.INVISIBLE);
	}

	
	
	
	
}
