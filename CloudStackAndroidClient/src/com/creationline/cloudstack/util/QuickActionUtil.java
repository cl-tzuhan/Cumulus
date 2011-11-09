package com.creationline.cloudstack.util;

import net.londatiga.android.QuickAction;
import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.creationline.cloudstack.R;

public class QuickActionUtil {

    //animation caches (so we don't need to continually re-created these same animations)
    private static Animation fadein_decelerate = null;
    private static Animation fadeout_decelerate = null;
    
	public QuickActionUtil(Context context) {
        //init global animation cache
        fadein_decelerate = AnimationUtils.loadAnimation(context, R.anim.fadein_decelerate);
        fadeout_decelerate = AnimationUtils.loadAnimation(context, R.anim.fadeout_decelerate);
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
		quickActionIcon.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				quickAction.show(v);
			}
		});
	}
	
	public static void showQuickActionIcon(final ImageView quickActionIcon, final ProgressBar quickActionProgress, final boolean animate) {
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
	
	
	
	
}
