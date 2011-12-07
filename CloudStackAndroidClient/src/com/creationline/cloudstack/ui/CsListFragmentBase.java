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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.creationline.cloudstack.CloudStackAndroidClient;
import com.creationline.cloudstack.R;

public class CsListFragmentBase extends ListFragment {
	
	public boolean isProvisioned() {
		SharedPreferences preferences = getActivity().getSharedPreferences(CloudStackAndroidClient.SHARED_PREFERENCES.PREFERENCES_NAME, Context.MODE_PRIVATE);
		final String savedApiKey = preferences.getString(CloudStackAndroidClient.SHARED_PREFERENCES.APIKEY_SETTING, null);
        return savedApiKey!=null;
	}
	
	public void setTextView(View view, final int textViewId, final String newText) {
		TextView textView = (TextView)view.findViewById(textViewId);
		if(textView!=null) { textView.setText(newText); }
	}
	
	public void unregisterCallbackReceiver(BroadcastReceiver broadcastReceiver) {
		if(broadcastReceiver!=null) {
			//catch-all here as a safeguard against cases where the activity is exited before BroadcastReceiver.onReceive() has been called-back
			try {
				getActivity().unregisterReceiver(broadcastReceiver);
			} catch (IllegalArgumentException e) {
				//will get this exception if listSnapshotsCallbackReceiver has already been unregistered (or was never registered); will just ignore here
				;
			}
		}
	}
	
	public void setRefreshButtonEnabled(final View view, final boolean enabled) {
		if(view!=null){
			Button refreshbutton = (Button)view.findViewById(R.id.refreshbutton);
			if(refreshbutton!=null) { refreshbutton.setEnabled(enabled); }
		}
	}

	public void setProgressCircleVisible(final View view, final int visibility) {
		if(view!=null) {
			ProgressBar progresscircle = (ProgressBar)view.findViewById(R.id.progresscircle);
			if(progresscircle!=null) { progresscircle.setVisibility(visibility); }
		}
	}
	
	public View addAndInitFooter(Bundle savedInstanceState, final int footerId, final int footerSwitcherId) {
		View footer = getLayoutInflater(savedInstanceState).inflate(footerId, null, false);
		ViewSwitcher footerSwitcher = (ViewSwitcher)footer.findViewById(footerSwitcherId);
		if(footerSwitcher!=null) {
			footerSwitcher.setDisplayedChild(0);
			footerSwitcher.setAnimateFirstView(true);
		}
		getListView().addFooterView(footer, null, false);
		
		return footer;
	}
	
	
	
}
