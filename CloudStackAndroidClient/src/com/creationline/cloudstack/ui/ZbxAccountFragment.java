/*******************************************************************************
 * Copyright 2011-2012 Creationline,Inc.
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.creationline.cloudstack.CloudStackAndroidClient;
import com.creationline.cloudstack.R;
import com.creationline.common.utils.ClLog;
import com.creationline.zabbix.engine.ZbxRestContentProvider;
import com.creationline.zabbix.engine.ZbxRestService;
import com.creationline.zabbix.engine.api.UserLoginRequest;
import com.creationline.zabbix.engine.api.ZbxApiConstants;

public class ZbxAccountFragment extends Fragment implements ViewSwitcher.ViewFactory {
	//zbxloginaccountswitcher-related constants
	public static final int LOGIN_SCREEN = 0;  //the viewswitcher xml declaration must define the login screen first in order for this to work
	public static final int ACCOUNT_SCREEN = 1;  //the viewswitcher xml declaration must define the account info screen second in order for this to work

	//zbxloginprogressswitcher-related constants
	private static final int BUTTON_CHILD = 0;  //the viewswitcher xml declaration must define the button view in order for this to work
	private static final int PROGRESSCIRCLE_CHILD = 1;  //the viewswitcher xml declaration must define the progressbar view second in order for this to work

	DefaultHttpClient httpclient = null;

	final Pattern hostPattern = Pattern.compile("^(((ht|f)tp(s?))\\://)?[\\w\\-\\.:/~\\+#]*");  //allows any mix of letters+digits along with period, colon, tilda, pound, slash, and optional preceding "http:", "https:", or "ftp:")
	final Pattern usernamePattern = Pattern.compile("[\\w\\d-]*");  //allows letters(+underscore), digits, hyphen

    private BroadcastReceiver loginCompleteCallbackReceiver = null;  //used to receive login-complete notifs from ZbxRestService

	private static String ZBX_LOGIN_IN_PROGRESS = "com.creationline.zabbix.engine.api.ZbxAccountFragment.ZBX_LOGIN_IN_PROGRESS";

	
    public ZbxAccountFragment() {
    	//empty constructor is needed by Android for automatically creating fragments from XML declarations
    }
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		final FragmentActivity activity = getActivity();
		final Animation shake = AnimationUtils.loadAnimation(activity,  R.anim.shake);
		final Animation out = AnimationUtils.loadAnimation(activity, android.R.anim.slide_out_right);
        
        //setup error frame TextSwitchers
		initTextSwitcher(R.id.zbxhosterrorframe, shake, out);
		initTextSwitcher(R.id.zbxusernamepassworderrorframe, shake, out);
		initTextSwitcher(R.id.zbxloginerrorframe, shake, out);
        
		SharedPreferences preferences = activity.getSharedPreferences(CloudStackAndroidClient.SHARED_PREFERENCES.PREFERENCES_NAME, Context.MODE_PRIVATE);
		final String savedAuthToken = preferences.getString(CloudStackAndroidClient.SHARED_PREFERENCES.ZABBIX_AUTH_TOKEN, null);
        final boolean haveZbxAccountCredentials = savedAuthToken!=null;
		if(haveZbxAccountCredentials) {
			//if we are already provisioned, just show the accounts screen
        	displayAccountScreen();
			fillDynamicAccountInfo(preferences);
        } else {
        	//if we aren't provisioned yet, we will get the login screen by default
        	; //nothing special to do so far
        }
		setResetButtonOnClickHandler(getActivity());
		
		//do processing for a resume-type situation if we have state from before the pause
		if(savedInstanceState!=null) {
			final boolean zbxLoginInProgress = savedInstanceState.getBoolean(ZbxAccountFragment.ZBX_LOGIN_IN_PROGRESS);
			if(zbxLoginInProgress) {
				//if we are coming back from a normal pause,
				//the previous broadcast receiver is still alive,
				//so we just need to put up the busy sign
				showLoginProgressCircle();
				if(loginCompleteCallbackReceiver==null) {
					//we only get here if we had an orientation change:
					//orientation changes, unlike plain pauses, destroy the app;
					//so we need to re-register a new broadcast receiver here
					registerLoginCompleteCallbackReceiver();
				}
			}
		}
		
        super.onActivityCreated(savedInstanceState);
	}

	public void fillDynamicAccountInfo(SharedPreferences preferences) {
		final String savedZbxHost = preferences.getString(CloudStackAndroidClient.SHARED_PREFERENCES.ZABBIX_HOST_SETTING, null);
		final String savedZbxUsername = preferences.getString(CloudStackAndroidClient.SHARED_PREFERENCES.ZABBIX_USERNAME_SETTING, null);
		final String savedZbxAuthtoken = preferences.getString(CloudStackAndroidClient.SHARED_PREFERENCES.ZABBIX_AUTH_TOKEN, null);
		
		setTextViewValue(R.id.zbxhost, savedZbxHost);
		setTextViewValue(R.id.zbxusername, savedZbxUsername);
		setTextViewValue(R.id.zbxauthtoken, savedZbxAuthtoken);
	}

	public void displayLoginScreen() {
		switchLoginAccountScreens(ZbxAccountFragment.LOGIN_SCREEN);
	}
	public void displayAccountScreen() {
		switchLoginAccountScreens(ZbxAccountFragment.ACCOUNT_SCREEN);
	}
	public void switchLoginAccountScreens(final int screenToShow) {
		ViewSwitcher loginaccountswitcher = (ViewSwitcher)getView().findViewById(R.id.zbxloginaccountswitcher);  //if fragment is embedded in dialog, getView() needed to access the view of the dialog and not the app
		loginaccountswitcher.setDisplayedChild(screenToShow);
	}

	public void setResetButtonOnClickHandler(final FragmentActivity activity) {
		Button resetbutton = (Button)getView().findViewById(R.id.zbxresetbutton);  //if fragment is embedded in dialog, getView() needed to access the view of the dialog and not the app
        resetbutton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				View confirmdialog_resetaccount = activity.getLayoutInflater().inflate(R.layout.zbxconfirmdialog_resetzbxaccount, null);
				
				AlertDialog.Builder builder = new AlertDialog.Builder(activity);
			    builder.setView(confirmdialog_resetaccount)
			    	   .setPositiveButton("Reset", new DialogInterface.OnClickListener() {
			               public void onClick(DialogInterface dialog, int id) {
			                    resetAccount();
			               }
			           })
			           .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			               public void onClick(DialogInterface dialog, int id) {
			                    dialog.cancel();
			               }
			           });
			    AlertDialog confirmDialog = builder.create();
			    confirmDialog.show();
			}
		});
	}
	
	public void initTextSwitcher(final int textSwitcherId, final Animation inAnimation, final Animation outAnimation) {
		TextSwitcher textSwitcher = (TextSwitcher)getView().findViewById(textSwitcherId);
        textSwitcher.setFactory(this);
		textSwitcher.setInAnimation(inAnimation);
		textSwitcher.setOutAnimation(outAnimation);
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		if(loginCompleteCallbackReceiver!=null) {
			//mark the login as in-progress whether we are pausing or having an orientation change
			outState.putBoolean(ZbxAccountFragment.ZBX_LOGIN_IN_PROGRESS, true);
		}
		
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onDestroy() {
		if(loginCompleteCallbackReceiver!=null) {
			//if we are exiting, we can't guarantee catching the login result broadcast from ZbxRestService,
			//so we will just act as if the login has been canceled
			unregisterLoginCompleteCallbackReceiver();
		}
		super.onDestroy();
	}

	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View zbxaccountfragment = inflater.inflate(R.layout.zbxaccountfragment, container, false);

		SharedPreferences preferences = getActivity().getSharedPreferences(CloudStackAndroidClient.SHARED_PREFERENCES.PREFERENCES_NAME, Context.MODE_PRIVATE);
		final String savedZbxHost = preferences.getString(CloudStackAndroidClient.SHARED_PREFERENCES.ZABBIX_HOST_SETTING, null);
		final String savedZbxUsername = preferences.getString(CloudStackAndroidClient.SHARED_PREFERENCES.ZABBIX_USERNAME_SETTING, null);
		final String savedZbxPassword = preferences.getString(CloudStackAndroidClient.SHARED_PREFERENCES.ZABBIX_PASSWORD_SETTING, null);
		
        setTextWatcherAndValueForEditText(zbxaccountfragment, R.id.zbxhostfield, CloudStackAndroidClient.SHARED_PREFERENCES.ZABBIX_HOST_SETTING, savedZbxHost);
        setTextWatcherAndValueForEditText(zbxaccountfragment, R.id.zbxusernamefield, CloudStackAndroidClient.SHARED_PREFERENCES.ZABBIX_USERNAME_SETTING, savedZbxUsername);
        setTextWatcherAndValueForEditText(zbxaccountfragment, R.id.zbxpasswordfield, CloudStackAndroidClient.SHARED_PREFERENCES.ZABBIX_PASSWORD_SETTING, savedZbxPassword);

        setLoginButtonClickHandler(zbxaccountfragment);
        
		return zbxaccountfragment;
	}

	public void setTextViewValue(final int idOfTextViewToSet, final String newValue) {
		TextView textView = (TextView)getView().findViewById(idOfTextViewToSet);  //if fragment is embedded in dialog, getView() needed to access the view of the dialog and not the app
        if(textView!=null) { textView.setText(newValue); }
	}

	public void setTextWatcherAndValueForEditText(final View zbxAccountfragment, final int idOfEditTextToWatch, final String preferenceKey, final String previousValue) {
		TextWatcher textWatcher = new TextWatcher() {

			@Override
			public void afterTextChanged(Editable arg0) {
				//persist input to preferences
				final String inputtedString = arg0.toString();
				SharedPreferences preferences = getActivity().getSharedPreferences(CloudStackAndroidClient.SHARED_PREFERENCES.PREFERENCES_NAME, Context.MODE_PRIVATE);
				SharedPreferences.Editor preferencesEditor = preferences.edit();
				preferencesEditor.putString(preferenceKey, inputtedString);
				preferencesEditor.commit();
				
				validateTextInput(idOfEditTextToWatch, inputtedString);
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				;  //do nothing
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				;  //do nothing
			}
        	
        };

        EditText editText = (EditText)zbxAccountfragment.findViewById(idOfEditTextToWatch);
        if(editText!=null) {
        	if(previousValue!=null) {
        		editText.setText(previousValue);
        	}
	        editText.addTextChangedListener(textWatcher);
        }
	}

	public void setLoginButtonClickHandler(final View accountfragment) {
		final Button loginButton = (Button)accountfragment.findViewById(R.id.zbxloginbutton);
        loginButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				loginButton.setClickable(false);
				
				//automatically close the soft keyboard
				final EditText passwordfield = (EditText)accountfragment.findViewById(R.id.zbxpasswordfield);
				InputMethodManager mgr = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
				mgr.hideSoftInputFromWindow(passwordfield.getWindowToken(), 0);
				
				makeLoginCall(accountfragment);
			}
		});
	}

	public void makeLoginCall(View itemView) {
		Activity activity = getActivity();
		
		showLoginProgressCircle();
		
		SharedPreferences preferences = activity.getSharedPreferences(CloudStackAndroidClient.SHARED_PREFERENCES.PREFERENCES_NAME, Context.MODE_PRIVATE);
		final String zbxHost = preferences.getString(CloudStackAndroidClient.SHARED_PREFERENCES.ZABBIX_HOST_SETTING, null);
		final String zbxUsername = preferences.getString(CloudStackAndroidClient.SHARED_PREFERENCES.ZABBIX_USERNAME_SETTING, null);
		final String zbxPassword = preferences.getString(CloudStackAndroidClient.SHARED_PREFERENCES.ZABBIX_PASSWORD_SETTING, null);

		clearErrorFrames();
		if(emptyInputValidationFailed(zbxHost, zbxUsername, zbxPassword)) {
			showLoginButton();
			return;  //if input validation failed, short-circuit the rest of the method
		}
		final boolean hostFieldHasInvalidInput = !validateTextInput(R.id.zbxhostfield, zbxHost);
		final boolean usernameFieldHasInvalidInput = !validateTextInput(R.id.zbxusernamefield, zbxUsername);
		if(hostFieldHasInvalidInput || usernameFieldHasInvalidInput) {
			showLoginButton();
			return;  //if input validation failed, short-circuit the rest of the method
		}
		
		//we got this far, so set up a receiver to wait for login result, then make the initial login call
		//(we really don't need to do this call per say since ZbxRestService is capable of automatically
		// logging in when necessary during a process, but we'll pro-actively to do this initial login
		// anyways as a nice way of pre-authenticating the user can giving them a nice message in case their credentials are wrong)
    	registerLoginCompleteCallbackReceiver();
    	ClLog.i("ZbxAccountFragment.makeLoginCall()", "doing initial login with zbxHost="+zbxHost+" zbxUsername="+zbxUsername);
    	//make the rest call to zabbix server
    	Bundle userLoginPayload = UserLoginRequest.createUserLoginActionPayload(activity, null);
    	Intent userLoginIntent = ZbxRestService.createZbxRestServiceIntent(activity, userLoginPayload);
    	activity.startService(userLoginIntent);
    	//we will wait in registerLoginCompleteCallbackReceiver() for the result of the login
		
	}
	
	public void clearErrorFrames() {
		showLoginErrorMessage(R.id.zbxhosterrorframe, "");
		showLoginErrorMessage(R.id.zbxusernamepassworderrorframe, "");
		showLoginErrorMessage(R.id.zbxloginerrorframe, "");
		
		removePreferenceSetting(CloudStackAndroidClient.SHARED_PREFERENCES.LOGINERROR_CACHE);
	}

	public void removePreferenceSetting(final String keyToRemove) {
		SharedPreferences preferences = getActivity().getSharedPreferences(CloudStackAndroidClient.SHARED_PREFERENCES.PREFERENCES_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = preferences.edit();
		editor.remove(keyToRemove);
		editor.commit();
	}

	public boolean emptyInputValidationFailed(final String host, final String username, final String password) {
		boolean inputIsEmpty = false;
		
		if(TextUtils.isEmpty(host)) {
			showLoginErrorMessage(R.id.zbxhosterrorframe, "please enter a valid Zabbix server URL");
			inputIsEmpty = true;
		}
		
		if(TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
			showLoginErrorMessage(R.id.zbxusernamepassworderrorframe, "enter your username and password");
			inputIsEmpty = true;
		}
		
		return inputIsEmpty;
	}
	
	public boolean validateTextInput(final int idOfEditTextBeingValidated, final String stringToValidate) {
		if(idOfEditTextBeingValidated==R.id.zbxhostfield){
			Matcher matcher = hostPattern.matcher(stringToValidate);
			if(!matcher.matches()) { //host cannot contain 1-byte nor 2-byte spaces
				showLoginErrorMessage(R.id.zbxhosterrorframe, "host value must be a valid URL");
				return false;
			} else {
				showLoginErrorMessage(R.id.zbxhosterrorframe, "");
				return true;
			}
		} else if(idOfEditTextBeingValidated==R.id.zbxusernamefield) {
			Matcher matcher = usernamePattern.matcher(stringToValidate);
			if(!matcher.matches()) { //username cannot contain 1-byte nor 2-byte spaces
				showLoginErrorMessage(R.id.zbxusernamepassworderrorframe, "login name cannot contain special characters");
				return false;
			} else {
				showLoginErrorMessage(R.id.zbxusernamepassworderrorframe, "");
				return true;
			}
		}
		return false;  //fail validation as default
	}
	
	private void showLoginErrorMessage(final int errorFrameId, final String message) {
		TextSwitcher ts = (TextSwitcher)getView().findViewById(errorFrameId);
		if(ts!=null) { ts.setText(message); }
	}
	
	public void resetAccount() {
		final FragmentActivity activity = getActivity();

		//reset the view switcher to the original state
		ViewSwitcher zbxloginaccountswitcher = (ViewSwitcher)getView().findViewById(R.id.zbxloginaccountswitcher);  //if fragment is embedded in dialog, getView() needed to access the view of the dialog and not the app
		zbxloginaccountswitcher.reset();
		
		//delete all saved zabbix-related prefs
		SharedPreferences preferences = activity.getSharedPreferences(CloudStackAndroidClient.SHARED_PREFERENCES.PREFERENCES_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor preferencesEditor = preferences.edit();
		preferencesEditor.remove(CloudStackAndroidClient.SHARED_PREFERENCES.ZABBIX_AUTH_TOKEN);
		preferencesEditor.remove(CloudStackAndroidClient.SHARED_PREFERENCES.ZABBIX_HOST_SETTING);
		preferencesEditor.remove(CloudStackAndroidClient.SHARED_PREFERENCES.ZABBIX_USERNAME_SETTING);
		preferencesEditor.remove(CloudStackAndroidClient.SHARED_PREFERENCES.ZABBIX_PASSWORD_SETTING);
		preferencesEditor.commit();
		
		//delete all existing zabbix data only (cs data we leave untouched since the user may still want to use cs)
		ZbxRestContentProvider.deleteAllData(activity);
		
		//just to be safe in case there has been scrolling, move scrollview back to beginning so user sees top of view after we flip
		ScrollView detailscrollview = (ScrollView)getView().findViewById(R.id.zbxdetailscrollview);  //if fragment is embedded in dialog, getView() needed to access the view of the dialog and not the app
		if(detailscrollview!=null) { detailscrollview.fullScroll(ScrollView.FOCUS_UP); }
		
		//switch the ui to "login screen"
		flipBetweenLoginAndAccountScreens(ZbxAccountFragment.LOGIN_SCREEN);
		
		//clear the edittexts of previous values
		setTextViewValue(R.id.zbxhostfield, "");
		setTextViewValue(R.id.zbxusernamefield, "");
		setTextViewValue(R.id.zbxpasswordfield, "");
	}

	public void initAndFlipToAccountScreen() {
		Activity activity = getActivity();
		
		//disable the slide-in animation for ui elements while ViewSwitcher animates flipping the screen
		RelativeLayout zbxloginaccountswitcher_view1 = (RelativeLayout)activity.findViewById(R.id.zbxloginaccountswitcher_view1);
		if(zbxloginaccountswitcher_view1!=null) { zbxloginaccountswitcher_view1.setLayoutAnimation(null); }
		
		//just to be safe in case there has been scrolling, move scrollview back to beginning so user sees top of view after we flip
		ScrollView zbxdetailscrollview = (ScrollView)getView().findViewById(R.id.zbxdetailscrollview);  //if fragment is embedded in dialog, getView() needed to access the view of the dialog and not the app
		if(zbxdetailscrollview!=null) { zbxdetailscrollview.smoothScrollTo(0, 0); }
		
		flipBetweenLoginAndAccountScreens(ZbxAccountFragment.ACCOUNT_SCREEN);
		SharedPreferences preferences = activity.getSharedPreferences(CloudStackAndroidClient.SHARED_PREFERENCES.PREFERENCES_NAME, Context.MODE_PRIVATE);
		fillDynamicAccountInfo(preferences);
	}

	/**
	 * Switches between login and account screens like switchLoginAccountScreens(),
	 * but changes the animation of the switch to a flip-like animation.
	 * These two methods are candidates for merging into a single method.
	 * 
	 * @param screenToFlipTo id of login or account screen, whichever you wish to display
	 */
	public void flipBetweenLoginAndAccountScreens(final int screenToFlipTo) {
		final FragmentActivity activity = getActivity();
		
		ViewSwitcher zbxloginaccountswitcher = (ViewSwitcher)getView().findViewById(R.id.zbxloginaccountswitcher);  //if fragment is embedded in dialog, getView() needed to access the view of the dialog and not the app
		if(zbxloginaccountswitcher.getDisplayedChild()!=screenToFlipTo) {
			final Animation flipout_withstartdelay = AnimationUtils.loadAnimation(activity, R.anim.flipout_withstartdelay);
			final Animation flipin_withstartdelay = AnimationUtils.loadAnimation(activity, R.anim.flipin_withstartdelay);

			zbxloginaccountswitcher.setOutAnimation(flipout_withstartdelay);
			zbxloginaccountswitcher.setInAnimation(flipin_withstartdelay);
			zbxloginaccountswitcher.showNext();

			//in case the focus was on something before the flip, clear focus here so we don't attract attention to any buttons from the get-go
			Button zbxresetbutton = (Button)getView().findViewById(R.id.zbxresetbutton);
			if(zbxresetbutton!=null) { zbxresetbutton.clearFocus(); };
		}
		
	}
	
	public void showLoginButton() {
		switchLoginProgress(ZbxAccountFragment.BUTTON_CHILD);
	}
	public void showLoginProgressCircle() {
		switchLoginProgress(ZbxAccountFragment.PROGRESSCIRCLE_CHILD);
	}
	public void switchLoginProgress(final int childToShow) {
		ViewSwitcher loginprogressswitcher = (ViewSwitcher)getView().findViewById(R.id.zbxloginprogressswitcher);  //if fragment is embedded in dialog, getView() needed to access the view of the dialog and not the app
		if(loginprogressswitcher!=null) {
			loginprogressswitcher.setDisplayedChild(childToShow);
			loginprogressswitcher.getCurrentView().setClickable(true);  //always set whatever child is in front to clickable; we want to make the login button re-clickable after it was made unclickable when pressed + we don't care if the progress circle is clickable
		}
	}
	

	public void registerLoginCompleteCallbackReceiver() {
		final String TAG = "ZbxAccountFragment.registerLoginCompleteCallbackReceiver()";
		
		loginCompleteCallbackReceiver = new BroadcastReceiver(){
        	//This handles callback intents broadcasted by ZbxRestService
        	@Override
        	public void onReceive(Context contenxt, Intent intent) {
        		unregisterLoginCompleteCallbackReceiver();
        		showLoginButton();
        		
    			Bundle payload = intent.getExtras();
    			final int callStatus = payload.getInt(ZbxRestService.CALL_STATUS);
    			if(callStatus==ZbxRestService.CALL_STATUS_VALUES.CALL_SUCCESS) {
    				ClLog.d(TAG, "intial zabbix login succeeded");
    				initAndFlipToAccountScreen();

    			} else if(callStatus==ZbxRestService.CALL_STATUS_VALUES.CALL_FAILURE) {
    				final String errorMessage = payload.getString(ZbxApiConstants.FIELDS.DATA);
    				ClLog.d(TAG, "intial zabbix login failed!  error="+errorMessage);
    				showLoginErrorMessage(R.id.zbxloginerrorframe, errorMessage);
    			} else {
    				ClLog.w(TAG, "got unhandled callStatus="+callStatus);
    			}
        	}
        };
        
        getActivity().registerReceiver(loginCompleteCallbackReceiver, new IntentFilter(ZbxRestService.INTENT_ACTION.ZBXRESTSERVICE_BROADCAST));  //activity will now get intents broadcast by ZbxRestService (filtered by ZBXRESTSERVICE_BROADCAST action)
	}
	
	public void unregisterLoginCompleteCallbackReceiver() {
		Activity activity = getActivity();
		if(activity!=null) {
			activity.unregisterReceiver(loginCompleteCallbackReceiver);
			loginCompleteCallbackReceiver = null;
		}
	}
	
	@Override
	public View makeView() {
		TextView t = new TextView(getActivity());
		t.setTextSize(14);
		t.setGravity(Gravity.CENTER_HORIZONTAL);
		t.setTextColor(getResources().getColor(R.color.error));
		return t;
	}


}
