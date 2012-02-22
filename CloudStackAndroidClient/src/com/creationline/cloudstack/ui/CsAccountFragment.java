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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
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
import com.creationline.cloudstack.engine.CsApiConstants;
import com.creationline.cloudstack.engine.CsRestContentProvider;
import com.creationline.cloudstack.engine.CsRestService;
import com.creationline.cloudstack.engine.db.Transactions;
import com.creationline.cloudstack.engine.db.Vms;
import com.creationline.common.utils.ClLog;
import com.creationline.zabbix.engine.ZbxRestContentProvider;

public class CsAccountFragment extends Fragment implements ViewSwitcher.ViewFactory {
	//loginscreenkeyscreenswitcher-related constants
	private static final int LOGIN_SCREEN = 0;  //the viewswitcher xml declaration must define the LOGIN screen first in order for this to work
	private static final int KEYS_SCREEN = 1;  //the viewswitcher xml declaration must define the keys info screen second in order for this to work

	public static class INTENT_ACTION {
		//NOTE: changing the value of these constants requires you to change any usage of the same string in Android.manifest!!!
		public static final String LOGIN = "com.creationline.cloudstack.ui.CsAccountFragment.LOGIN";
	}
	
	//AsyncTask-related constants
	private static String ASYNC_RESULT = "asyncResult";
	private static String ASYNC_ERROR = "asyncError";
	
	DefaultHttpClient httpclient = null;

	final Pattern csHostPattern = Pattern.compile("^(((ht|f)tp(s?))\\://)?[\\w\\-\\.:/~\\+#]*");  //allows any mix of letters+digits along with period, colon, tilda, pound, slash, and optional preceding "http:", "https:", or "ftp:")
	final Pattern usernamePattern = Pattern.compile("[\\w\\d-]*");  //allows letters(+underscore), digits, hyphen

	///zbx-related members
	private AlertDialog zbxAccountDialogGlobalHandle = null;  //reference to whatever dialog is being shown so the app can dismiss it if necessary (eg. during orientation changes)
	private static String ZBX_ACCOUNT_DIALOG_VISIBLE = "com.creationline.cloudstack.ui.CsAccountFragment.ZBX_ACCOUNT_DIALOG_VISIBLE";
	
	private CsSessionBasedRequestTask provisionTaskHandle = null;  //task member var so we can have a handle to cancel in-progress tasks on exit
	private class CsSessionBasedRequestTask extends AsyncTask<Bundle, Void, Bundle> {

		@Override
		protected Bundle doInBackground(Bundle... params) {
			// sub-classes must implement this
			return null;
		}
		
	    /*
	     * Copied from:
	     *   http://www.kospol.gr/204/create-md5-hashes-in-android/ 
	     */
		public final String md5(final String s) {
		    try {
		        // Create MD5 Hash
		        MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
		        digest.update(s.getBytes());
		        byte messageDigest[] = digest.digest();
		 
		        // Create Hex String
		        StringBuffer hexString = new StringBuffer();
		        for (int i = 0; i < messageDigest.length; i++) {
		            String h = Integer.toHexString(0xFF & messageDigest[i]);
		            while (h.length() < 2)
		                h = "0" + h;
		            hexString.append(h);
		        }
		        return hexString.toString();
		 
		    } catch (NoSuchAlgorithmException e) {
		        e.printStackTrace();
		    }
		    return "";
		}
		
		public HttpResponse executeHttpRequest(final String finalUrl, Bundle returnBundle) {
    		final String TAG = "CsACcountFragment.CsSessionBasedRequestTask.executeHttpRequest()";

			HttpResponse response = null;
            try {
                final HttpGet httpGet = new HttpGet(finalUrl);
                //final HttpPost httpPost = new HttpPost(finalUrl);
                
                response =  httpclient.execute(httpGet);

            } catch (ClientProtocolException e) {
            	ClLog.e(TAG, "got ClientProtocolException! [" + e.toString() +"]");
            	ClLog.e(TAG, e);
            	returnBundle.putString(CsAccountFragment.ASYNC_ERROR, e.toString());
    		} catch (IllegalArgumentException e) {
    			ClLog.e(TAG, "got IllegalArgumentException! [" + e.toString() +"]");
    			ClLog.e(TAG, e);
    			returnBundle.putString(CsAccountFragment.ASYNC_ERROR, e.toString());
    		} catch (UnknownHostException e) {
    			ClLog.e(TAG, "got UnknownHostException! [" + e.toString() +"]");
    			ClLog.e(TAG, e);
    			returnBundle.putString(CsAccountFragment.ASYNC_ERROR, "Specified URL is not a valid CloudStack instance.  This must be a proper host/URL.");
    		} catch (ConnectTimeoutException e) {
    			ClLog.e(TAG, "got ConnectTimeoutException! [" + e.toString() +"]");
    			ClLog.e(TAG, e);
    			returnBundle.putString(CsAccountFragment.ASYNC_ERROR, "Could not connect to the specified CloudStack instance.  Is the URL is correct?");
    		} catch (HttpHostConnectException e) {
    			ClLog.e(TAG, "got HttpHostConnectException! [" + e.toString() +"]");
    			ClLog.e(TAG, e);
    			returnBundle.putString(CsAccountFragment.ASYNC_ERROR, "Could not connect to the specified CloudStack instance.  Perhaps the URL is incorrect?");
    		} catch (SocketException e) {
    			ClLog.e(TAG, "got SocketException! [" + e.toString() +"]");
    			ClLog.e(TAG, e);
    			returnBundle.putString(CsAccountFragment.ASYNC_ERROR, "Could not found a route to the specified CloudStack instance.  Perhaps the port number is missing?");
    		} catch (IOException e) {
    			ClLog.e(TAG, "got IOException! [" + e.toString() +"]");
    			ClLog.e(TAG, e);
    			returnBundle.putString(CsAccountFragment.ASYNC_ERROR, e.toString());
    		}
    		
			return response;
		}
		
		public JsonNode extractUserObjFor(final String replyBody, final String username) {
			final String TAG = "CsACcountFragment.CsLoginTask.extractUserObjFor()";

			ObjectMapper om = new ObjectMapper();
			try {
				JsonNode replyObj = om.readTree(replyBody);
				ClLog.d(TAG, CsApiConstants.LISTACCOUNTS_PARAMS.USER+"="+username);
				JsonNode userList = replyObj.findValue(CsApiConstants.LISTACCOUNTS_PARAMS.USER);
				ClLog.d(TAG, "userList="+userList);
				if(userList!=null) {
					//each account may have multiple users, so search through the list users,
					//returning only the one that is the currently logged-in user
					for(JsonNode userObj : userList) {
						JsonNode usernameNode = userObj.findValue(CsApiConstants.LISTACCOUNTS_PARAMS.USERNAME);
						if(usernameNode!=null) {
							String usernameNodeAsStr = usernameNode.toString();
							usernameNodeAsStr = removeDoubleQuotes(usernameNodeAsStr);
							ClLog.d(TAG, "  usernameNodeAsStr="+usernameNodeAsStr);
							if(username.equalsIgnoreCase(usernameNodeAsStr)) {
								return userObj;
							}
						}
					}
				}
			} catch (IOException e) {
				ClLog.e(TAG, "got IOException parsing reading replyBody! [" + e.toString() +"]");
				ClLog.e(TAG, e);
			}
			return null;
		}

		public String removeDoubleQuotes(final String inStr) {
			String returnStr = null;
			if(inStr.startsWith("\"") && inStr.endsWith("\"")) {
				//remove any double-quotes around the value (Jackson tends to return the values with double-quotes instead of just the raw value)
				returnStr = inStr.substring(1, inStr.length()-1);
			}
			return returnStr;
		}
		
		public String extractValueForKey(JsonNode node, final String keyToExtract) {
			JsonNode value = node.findValue(keyToExtract);
			if(value==null) {
				return null;
			}
			return value.asText();
		}

		public String extractValueForKey(final String replyBody, final String keyToExtract) {
    		final String TAG = "CsACcountFragment.CsLoginTask.extractSessionKey()";

			String value = null;
			ObjectMapper om = new ObjectMapper();
			try {
				JsonNode replyObj = om.readTree(replyBody);
				value = replyObj.findValue(keyToExtract).asText();
			} catch (IOException e) {
				ClLog.e(TAG, "got IOException parsing reading replyBody! [" + e.toString() +"]");
				ClLog.e(TAG, e);
			}
			return value;
		}

		public String urlEncode(final String valueToEncode) {
			final String TAG = "CsAccountFragment.urlEncodeSessionKey.urlEncode()";
		
			try {
				return URLEncoder.encode(valueToEncode, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				ClLog.e(TAG, "We should not be getting this exception!  We are hosed if we are. [" + e.toString() +"]");
				ClLog.e(TAG, e);
			}
			return null;
		}

		public void showErrorAndEndProgress(final String asyncError) {
			//cache error so we can re-display in case user leave this screen and comes back
			SharedPreferences preferences = getActivity().getSharedPreferences(CloudStackAndroidClient.SHARED_PREFERENCES.PREFERENCES_NAME, Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = preferences.edit();
			editor.putString(CloudStackAndroidClient.SHARED_PREFERENCES.LOGINERROR_CACHE, asyncError);
			editor.remove(CloudStackAndroidClient.SHARED_PREFERENCES.LOGIN_INPROGRESS);
			editor.commit();
			
			//show error to user
			TextSwitcher ts = (TextSwitcher)getActivity().findViewById(R.id.loginerrorframe);
			if(ts!=null) { ts.setText(asyncError); }
			
			//auto-scroll to bottom to make sure error is on-screen
			ScrollView detailscrollview = (ScrollView)getActivity().findViewById(R.id.detailscrollview);
			if(detailscrollview!=null) { detailscrollview.smoothScrollTo(0, detailscrollview.getHeight()); }
			
			//revert progress-circle back to button icon
			switchLoginProgress();
			
		}

	}
	
    private class CsLoginTask extends CsSessionBasedRequestTask {

		@Override
		protected void onPreExecute() {
			SharedPreferences preferences = getActivity().getSharedPreferences(CloudStackAndroidClient.SHARED_PREFERENCES.PREFERENCES_NAME, Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = preferences.edit();
			editor.putBoolean(CloudStackAndroidClient.SHARED_PREFERENCES.LOGIN_INPROGRESS, true);
			editor.commit();
			
			super.onPreExecute();
		}
		
    	@Override
    	protected Bundle doInBackground(Bundle... params) {
    		Bundle apiParams = params[0];
    		Bundle returnBundle = new Bundle();
    		
    		//All session-based transactions (i.e. calls that don't go through CsRestService)
    		//with cs must start with a login call, so we will create the httpclient only when
    		//a login has been initiated.  We will use that same httpclient that keeps the valid
    		//JSESSIONID cookie for all intervening calls until a logout has been performed.
    		httpclient = new DefaultHttpClient();
    		
    		HttpResponse response = makeLoginCall(apiParams, returnBundle);
    		
			parseLoginResponse(response, returnBundle);
    		
			//add the input needed by later calls to the return bundle so chained rest requests can make use of them
			returnBundle.putString(CloudStackAndroidClient.SHARED_PREFERENCES.CLOUDSTACK_HOST_SETTING, apiParams.getString(CloudStackAndroidClient.SHARED_PREFERENCES.CLOUDSTACK_HOST_SETTING));
			returnBundle.putString(CsApiConstants.LOGIN_PARAMS.USERNAME, apiParams.getString(CsApiConstants.LOGIN_PARAMS.USERNAME));
    		return returnBundle;
    	}

		public HttpResponse makeLoginCall(Bundle apiParams, Bundle returnBundle) {
			final String TAG = "CsAccountFragment.CsLoginTask.makeLoginCall()";
				
			final String csHost = apiParams.getString(CloudStackAndroidClient.SHARED_PREFERENCES.CLOUDSTACK_HOST_SETTING);
			final String csDomain = apiParams.getString(CsApiConstants.LOGIN_PARAMS.DOMAIN);
    		final String username = apiParams.getString(CsApiConstants.LOGIN_PARAMS.USERNAME);
    		String password = apiParams.getString(CsApiConstants.LOGIN_PARAMS.PASSWORD);

	   		final String csHostUrl = CsRestService.makeHostIntoApiUrlIfNecessary(csHost);
			final String hashedPassword = md5(password);
    		String finalUrl = csHostUrl + "?" + "response=json"
    								+ "&"+CsRestService.COMMAND+"="+CsApiConstants.API.login
    								+ "&"+CsApiConstants.LOGIN_PARAMS.USERNAME+"="+urlEncode(username)
    								+ "&"+CsApiConstants.LOGIN_PARAMS.PASSWORD+"="+urlEncode(hashedPassword);
    		if(csDomain!=null) {
    			finalUrl += "&"+CsApiConstants.LOGIN_PARAMS.DOMAIN+"="+urlEncode(csDomain);
    		}
    		ClLog.d(TAG, "finalUrl="+finalUrl);
    		
    		HttpResponse response = executeHttpRequest(finalUrl, returnBundle);
			return response;
		}

		public void parseLoginResponse(HttpResponse response, Bundle returnBundle) {
			final String TAG = "CsAccountFragment.CsLoginTask.parseLoginResponse()";

			if(response==null) {
				return;
			}
			
			String replyBody = null;
			try {
				replyBody = CsRestService.getReplyBody(response).toString();
			} catch (IOException e) {
				ClLog.e(TAG, "got IOException parsing response! [" + e.toString() +"]");
				ClLog.e(TAG, e);
			}
			
			final int statusCode = response.getStatusLine().getStatusCode();
			final boolean callReturnedOk = statusCode==HttpStatus.SC_OK;
			final boolean ranInto404 = statusCode==HttpStatus.SC_NOT_FOUND;
			if(callReturnedOk) {
				final String sessionKey = extractValueForKey(replyBody, CsApiConstants.LOGIN_PARAMS.SESSIONKEY);
    			returnBundle.putString(CsAccountFragment.ASYNC_RESULT, sessionKey);
			} else if(ranInto404) {
				String errorText = "The specified CloudStack instance could not be found.  Perhaps the URL is incorrect?";
    			returnBundle.putString(CsAccountFragment.ASYNC_ERROR, errorText);
			} else {
				String errorText = extractValueForKey(replyBody, CsApiConstants.ERROR_PARAMS.ERRORTEXT);
				if(errorText==null) { errorText="Ran into an unknown error."; };
    			returnBundle.putString(CsAccountFragment.ASYNC_ERROR, errorText);
			}
		}

    	@Override
    	protected void onPostExecute(Bundle returnBundle) {
    		final String asyncResult = returnBundle.getString(CsAccountFragment.ASYNC_RESULT);
    		final String asyncError = returnBundle.getString(CsAccountFragment.ASYNC_ERROR);
    		
			if(asyncError!=null) {
				showErrorAndEndProgress(asyncError);
    			return;  //if we got an error abort the provisioning process
    		}

    		Bundle bundle = new Bundle();
    		//add the input needed by later calls to the params bundle so chained rest requests can make use of them
    		bundle.putString(CloudStackAndroidClient.SHARED_PREFERENCES.CLOUDSTACK_HOST_SETTING, returnBundle.getString(CloudStackAndroidClient.SHARED_PREFERENCES.CLOUDSTACK_HOST_SETTING));
    		bundle.putString(CsApiConstants.LOGIN_PARAMS.USERNAME, returnBundle.getString(CsApiConstants.LOGIN_PARAMS.USERNAME));
    		bundle.putString(CsApiConstants.LOGIN_PARAMS.SESSIONKEY, asyncResult);
    		provisionTaskHandle = new CsListAccountsTask();
    		provisionTaskHandle.execute(bundle);
    	}
    	
    }
    
    private class CsListAccountsTask extends CsSessionBasedRequestTask {

		@Override
		protected Bundle doInBackground(Bundle... params) {
			Bundle apiParams = params[0]; 
			Bundle returnBundle = new Bundle();

			HttpResponse response = makeListAccountsCall(apiParams, returnBundle);
			
			parseListAccountsResponse(response, apiParams, returnBundle);
    		
			//add csHost to the return bundle so chained rest requests can make use of it
			returnBundle.putString(CloudStackAndroidClient.SHARED_PREFERENCES.CLOUDSTACK_HOST_SETTING, apiParams.getString(CloudStackAndroidClient.SHARED_PREFERENCES.CLOUDSTACK_HOST_SETTING));
    		return returnBundle;
		}
		
		public HttpResponse makeListAccountsCall(Bundle apiParams, Bundle returnBundle) {
			final String TAG = "CsAccountFragment.CsListAccountsTask.makeListAccountsCall()";
			
			final String sessionKey = apiParams.getString(CsApiConstants.LOGIN_PARAMS.SESSIONKEY);
			String encodedSessionKey = urlEncode(sessionKey);
    		
			final String csHost = apiParams.getString(CloudStackAndroidClient.SHARED_PREFERENCES.CLOUDSTACK_HOST_SETTING);
	   		final String csHostUrl = CsRestService.makeHostIntoApiUrlIfNecessary(csHost);
    		final String finalUrl = csHostUrl + "?"+"response=json"
    								+ "&"+CsRestService.COMMAND+"="+CsApiConstants.API.listAccounts
    								+ "&"+CsApiConstants.LOGIN_PARAMS.SESSIONKEY+"="+encodedSessionKey;  //the url-encoded sessionkey returned from the LOGIN call must be included in the request to bypass CSRF protection
    		ClLog.d(TAG, "finalUrl="+finalUrl);
    		
    		HttpResponse response = executeHttpRequest(finalUrl, returnBundle);
			return response;
		}
		
		public void parseListAccountsResponse(HttpResponse response, Bundle apiParams, Bundle returnBundle) {
			final String TAG = "CsAccountFragment.CsListAccountsTask.parseListAccountsResponse()";

			if(response==null) {
				return;
			}
			
			final int statusCode = response.getStatusLine().getStatusCode();
			final boolean callReturnedOk = statusCode==HttpStatus.SC_OK;
			String replyBody = null;
			try {
				replyBody = CsRestService.getReplyBody(response).toString();
			} catch (IOException e) {
				ClLog.e(TAG, "got IOException parsing response! [" + e.toString() +"]");
				ClLog.e(TAG, e);
			}
			
			if(callReturnedOk) {
				final String loggedInUsername = apiParams.getString(CsApiConstants.LOGIN_PARAMS.USERNAME);
				final JsonNode userObject = extractUserObjFor(replyBody, loggedInUsername);
				if(userObject==null) {
					returnBundle.putString(CsAccountFragment.ASYNC_ERROR, "No matching user found.  Please double-check your username.");
					return;
				}
				
				final String apikey = extractValueForKey(userObject, CsApiConstants.LISTACCOUNTS_PARAMS.APIKEY);
				final String secretkey = extractValueForKey(userObject, CsApiConstants.LISTACCOUNTS_PARAMS.SECRETKEY);

				final boolean ifApiKeysHaveNotBeenGenerated = TextUtils.isEmpty(apikey) || TextUtils.isEmpty(secretkey);
				if(ifApiKeysHaveNotBeenGenerated) {
					returnBundle.putString(CsAccountFragment.ASYNC_ERROR, "You must first generate API/secret keys on CloudStack for LOGIN to complete.  Please ask your administrator for details and try again after the keys have been generated.");
					return;
				}

				//save the retrieved keys to prefs
				SharedPreferences preferences = getActivity().getSharedPreferences(CloudStackAndroidClient.SHARED_PREFERENCES.PREFERENCES_NAME, Context.MODE_PRIVATE);
				SharedPreferences.Editor preferencesEditor = preferences.edit();
				preferencesEditor.putString(CloudStackAndroidClient.SHARED_PREFERENCES.APIKEY_SETTING, apikey);
				preferencesEditor.putString(CloudStackAndroidClient.SHARED_PREFERENCES.SECRETKEY_SETTING, secretkey);
				preferencesEditor.commit();

    			returnBundle.putString(CsAccountFragment.ASYNC_RESULT, apikey);
			} else {
				String errorText = extractValueForKey(replyBody, CsApiConstants.ERROR_PARAMS.ERRORTEXT);
				if(errorText==null) { errorText="Sorry, encountered an unknown error."; };
    			returnBundle.putString(CsAccountFragment.ASYNC_ERROR, errorText);
			}
		}
    	
    	@Override
    	protected void onPostExecute(Bundle returnBundle) {
    		final String asyncResult = returnBundle.getString(CsAccountFragment.ASYNC_RESULT);
    		final String asyncError = returnBundle.getString(CsAccountFragment.ASYNC_ERROR);

    		provisionTaskHandle = null;
    		
    		final FragmentActivity activity = getActivity();
    		if(activity==null) {
    			return;
    		}
    		
    		SharedPreferences preferences = activity.getSharedPreferences(CloudStackAndroidClient.SHARED_PREFERENCES.PREFERENCES_NAME, Context.MODE_PRIVATE);
    		SharedPreferences.Editor editor = preferences.edit();
    		editor.remove(CloudStackAndroidClient.SHARED_PREFERENCES.LOGIN_INPROGRESS);
    		editor.commit();
    		
			if(asyncError!=null) {
				showErrorAndEndProgress(asyncError);
				return;  //if we got an error abort the provisioning process
			}
			
    		Bundle bundle = new Bundle();
    		//add csHost to the return bundle so chained rest requests can make use of it
    		bundle.putString(CloudStackAndroidClient.SHARED_PREFERENCES.CLOUDSTACK_HOST_SETTING, returnBundle.getString(CloudStackAndroidClient.SHARED_PREFERENCES.CLOUDSTACK_HOST_SETTING));
    		bundle.putString(CsApiConstants.LOGIN_PARAMS.SESSIONKEY, asyncResult);
    		new CsLogoutTask().execute(bundle);
    		
			//revert progress-circle back to button icon
			switchLoginProgress();
			switchToKeysScreen();
			
			//make listVms call to fill out vm data for user
			final String username = preferences.getString(CloudStackAndroidClient.SHARED_PREFERENCES.USERNAME_SETTING, null);
			if(username!=null) {
				//make the rest call to cs server for vm data
				final String action = CsRestService.TEST_CALL;   
				Bundle apiCmd = new Bundle();
				apiCmd.putString(CsRestService.COMMAND, "listVirtualMachines");
				apiCmd.putString(Vms.ACCOUNT, username);
		        apiCmd.putString(Transactions.CALLBACK_INTENT_FILTER, CsVmListFragment.INTENT_ACTION.CALLBACK_LISTVMS);
				Intent csRestServiceIntent = CsRestService.createCsRestServiceIntent(getActivity(), action, apiCmd);  //user api
				getActivity().startService(csRestServiceIntent);
			}
			
			//make listSnapshots call to fill out vm data for user
			if(username!=null) {
				//make the rest call to cs server for vm data
				final String action = CsRestService.TEST_CALL;   
				Bundle apiCmd = new Bundle();
				apiCmd.putString(CsRestService.COMMAND, "listSnapshots");
				apiCmd.putString(Vms.ACCOUNT, username);
		        apiCmd.putString(Transactions.CALLBACK_INTENT_FILTER, CsSnapshotListFragment.INTENT_ACTION.CALLBACK_LISTSNAPSHOTS);
				Intent csRestServiceIntent = CsRestService.createCsRestServiceIntent(getActivity(), action, apiCmd);  //user api
				getActivity().startService(csRestServiceIntent);
			}
    	}

		public void switchToKeysScreen() {
			final FragmentActivity activity = getActivity();
			
			//disable the slide-in animation for ui elements while ViewSwitcher animates flipping the screen
			RelativeLayout keysdisplayframe = (RelativeLayout)activity.findViewById(R.id.keysdisplayframe);
			if(keysdisplayframe!=null) { keysdisplayframe.setLayoutAnimation(null); }
			
			//fill out the dynamic text in the keys screen
			SharedPreferences preferences = activity.getSharedPreferences(CloudStackAndroidClient.SHARED_PREFERENCES.PREFERENCES_NAME, Context.MODE_PRIVATE);
			TextView readonlyusername = (TextView)activity.findViewById(R.id.username);
			readonlyusername.setText(preferences.getString(CloudStackAndroidClient.SHARED_PREFERENCES.USERNAME_SETTING, "<username missing>"));
			TextView readonlycshost = (TextView)activity.findViewById(R.id.cshost);
			readonlycshost.setText(preferences.getString(CloudStackAndroidClient.SHARED_PREFERENCES.CLOUDSTACK_HOST_SETTING, "<CloudStack host not configured>"));
			TextView apikey = (TextView)activity.findViewById(R.id.apikey);
			apikey.setText(preferences.getString(CloudStackAndroidClient.SHARED_PREFERENCES.APIKEY_SETTING, "<api key does not exist>"));
			TextView secretkey = (TextView)activity.findViewById(R.id.secretkey);
			secretkey.setText(preferences.getString(CloudStackAndroidClient.SHARED_PREFERENCES.SECRETKEY_SETTING, "<secret key does not exist>"));
			
			//just to be safe in case there has been scrolling, move scrollview back to beginning so user sees top of view after we flip
			ScrollView detailscrollview = (ScrollView)activity.findViewById(R.id.detailscrollview);
			if(detailscrollview!=null) { detailscrollview.smoothScrollTo(0, 0); }
			
			//switch the ui to "keys screen"
			flipBetweenLoginAndKeysScreens(CsAccountFragment.KEYS_SCREEN);
			
			//in case the focus was on something before the flip, clear focus here so we don't attract attention to any buttons from the get-go
			Button resetbutton = (Button)activity.findViewById(R.id.resetbutton);
			if(resetbutton!=null) { resetbutton.clearFocus(); }
			Button aboutbutton_loginscreen = (Button)activity.findViewById(R.id.aboutbutton_loginscreen);
			if(aboutbutton_loginscreen!=null) { aboutbutton_loginscreen.clearFocus(); }
			Button aboutbutton_keysscreen = (Button)activity.findViewById(R.id.aboutbutton_keysscreen);
			if(aboutbutton_keysscreen!=null) { aboutbutton_keysscreen.clearFocus(); }
			
			Button zbxaccountbutton = (Button)activity.findViewById(R.id.zbxaccountbutton);
			if(zbxaccountbutton!=null) { zbxaccountbutton.clearFocus(); }
		}

    }
    
    private class CsLogoutTask extends CsSessionBasedRequestTask {

    	@Override
    	protected Bundle doInBackground(Bundle... params) {
    		Bundle apiParams = params[0];
    		Bundle returnBundle = new Bundle();
    		
    		//we'll just make the logout call and not care too much whether it succeeded
    		//since the session will timeout eventually anyways.
    		makeLogoutCall(apiParams, returnBundle);
    		
    		//When httpclient is no longer needed, shut down the connection manager to ensure
    		//immediate deallocation of all system resources.  Since no other calls can succeed
    		//after a logout, we can shutdown the httpclient here and at the same time invalidate
    		//the active JSESSION.
    		httpclient.getConnectionManager().shutdown();

    		return returnBundle;  //return empty bundle since we have to return something
    	}

		public HttpResponse makeLogoutCall(Bundle apiParams, Bundle returnBundle) {
			final String TAG = "CsAccountFragment.CsLoginTask.makeLogoutCall()";
			
			final String sessionKey = apiParams.getString(CsApiConstants.LOGIN_PARAMS.SESSIONKEY);
    		String encodedSessionKey = urlEncode(sessionKey);
				
			final String csHost = apiParams.getString(CloudStackAndroidClient.SHARED_PREFERENCES.CLOUDSTACK_HOST_SETTING);
	   		final String csHostUrl = CsRestService.makeHostIntoApiUrlIfNecessary(csHost);
    		final String finalUrl = csHostUrl + "?" + "response=json"
    								+ "&"+CsRestService.COMMAND+"="+CsApiConstants.API.logout
    								+ "&"+CsApiConstants.LOGIN_PARAMS.SESSIONKEY+"="+encodedSessionKey;  //the url-encoded sessionkey returned from the LOGIN call must be included in the request to bypass CSRF protection
    		ClLog.d(TAG, "finalUrl="+finalUrl);
    		
    		HttpResponse response = executeHttpRequest(finalUrl, returnBundle);
			return response;
		}

    }
    

    public CsAccountFragment() {
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
		initTextSwitcher(R.id.hosterrorframe, shake, out);
		initTextSwitcher(R.id.usernamepassworderrorframe, shake, out);
		initTextSwitcher(R.id.loginerrorframe, shake, out);
        
		setAboutButtonOnClickHandler(activity, R.id.aboutbutton_loginscreen);
        setAboutButtonOnClickHandler(activity, R.id.aboutbutton_keysscreen);
        setResetButtonOnClickHandler(activity);
        
        setZbxAccountButtonOnClickHandler(activity, R.id.zbxaccountbutton);
        
		SharedPreferences preferences = getActivity().getSharedPreferences(CloudStackAndroidClient.SHARED_PREFERENCES.PREFERENCES_NAME, Context.MODE_PRIVATE);
		final String savedApiKey = preferences.getString(CloudStackAndroidClient.SHARED_PREFERENCES.APIKEY_SETTING, null);
        final boolean isProvisioned = savedApiKey!=null;
		if(isProvisioned) {
        	//if we are already provisioned, just show the "keys screen"
			ViewSwitcher loginscreenkeyscreenswitcher = (ViewSwitcher)activity.findViewById(R.id.loginscreenkeyscreenswitcher);
			loginscreenkeyscreenswitcher.setDisplayedChild(KEYS_SCREEN);

			final String savedCsHost = preferences.getString(CloudStackAndroidClient.SHARED_PREFERENCES.CLOUDSTACK_HOST_SETTING, null);
			final String savedUsername = preferences.getString(CloudStackAndroidClient.SHARED_PREFERENCES.USERNAME_SETTING, null);
			final String savedSecretKey = preferences.getString(CloudStackAndroidClient.SHARED_PREFERENCES.SECRETKEY_SETTING, null);
			
			setTextViewValue(R.id.cshost, savedCsHost);
			setTextViewValue(R.id.username, savedUsername);
			setTextViewValue(R.id.apikey, savedApiKey);
			setTextViewValue(R.id.secretkey, savedSecretKey);
        } else {
        	//if we aren't provisioned yet, we will get the "login screen" by default
        	final boolean loginIsInProgress = preferences.getBoolean(CloudStackAndroidClient.SHARED_PREFERENCES.LOGIN_INPROGRESS, false);
        	if(loginIsInProgress) {
        		//show progress-circle
        		switchLoginProgress();
        	}
        	
        	final String loginErrorMessage = preferences.getString(CloudStackAndroidClient.SHARED_PREFERENCES.LOGINERROR_CACHE, null);
        	if(loginErrorMessage!=null) {
        		//show any login error messages we may have got while off-screen
        		TextSwitcher ts = (TextSwitcher)getActivity().findViewById(R.id.loginerrorframe);
    			if(ts!=null) { ts.setText(loginErrorMessage); }
        	}
        }
		
		if(savedInstanceState!=null) {
			final boolean zbxAccountDialogWasVisibleBeforePause = savedInstanceState.getBoolean(CsAccountFragment.ZBX_ACCOUNT_DIALOG_VISIBLE);
			if(zbxAccountDialogWasVisibleBeforePause) {
				showZbxAccountDialog(activity);
			}
		}
		
        super.onActivityCreated(savedInstanceState);
	}

	public void showZbxAccountDialog(final FragmentActivity activity) {
		if(zbxAccountDialogGlobalHandle==null) {
			zbxAccountDialogGlobalHandle = createZbxAccountLoginDialog(activity);
		}
		zbxAccountDialogGlobalHandle.show();
	}

	public void setZbxAccountButtonOnClickHandler(final FragmentActivity activity, final int zbxAccountButtonId) {
		Button aboutbutton = (Button)activity.findViewById(zbxAccountButtonId);
		aboutbutton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showZbxAccountDialog(activity);
			}
		});
	}
	
	public void setAboutButtonOnClickHandler(final FragmentActivity activity, final int aboutButtonId) {
		Button aboutbutton = (Button)activity.findViewById(aboutButtonId);
		aboutbutton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent startAboutFragmentActivity = new Intent(activity, com.creationline.cloudstack.ui.AboutFragmentActivity.class);
		    	startActivity(startAboutFragmentActivity);
			}
		});
	}

	public void setResetButtonOnClickHandler(final FragmentActivity activity) {
		Button resetbutton = (Button)activity.findViewById(R.id.resetbutton);
        resetbutton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				View confirmdialog_resetaccount = activity.getLayoutInflater().inflate(R.layout.confirmdialog_resetaccount, null);
				
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
	
	public AlertDialog createZbxAccountLoginDialog(final FragmentActivity activity) {
		//create and show the zbx account/login ui as a fragment-in-dialog
		View zbxaccountdialog = activity.getLayoutInflater().inflate(R.layout.zbxaccountdialog, null);
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setView(zbxaccountdialog);
		AlertDialog dialogHandle = builder.create();
		return dialogHandle;
	}

	public void initTextSwitcher(final int textSwitcherId, final Animation inAnimation, final Animation outAnimation) {
		TextSwitcher hostErrorFrame = (TextSwitcher) getActivity().findViewById(textSwitcherId);
        hostErrorFrame.setFactory(this);
        hostErrorFrame.setInAnimation(inAnimation);
        hostErrorFrame.setOutAnimation(outAnimation);
	}
	
	@Override
	public void onResume() {
        
        super.onResume();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		if(zbxAccountDialogGlobalHandle!=null && zbxAccountDialogGlobalHandle.isShowing()) {
			//note that zbx account dialog is being shown so we can put it up again post orientation change
			outState.putBoolean(CsAccountFragment.ZBX_ACCOUNT_DIALOG_VISIBLE, true);
		}
		
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onPause() {
		
		super.onPause();
	}

	@Override
	public void onDestroy() {
		if(zbxAccountDialogGlobalHandle!=null) {
			//In general, we will leave the zbx account fragment around after it is created
			//as it is more robust in terms of transitioning between active and paused states.
			//We explicitly check for existence and manually clean it up here b/c the app
			//may be killed unexpected by android; not doing this here seems to leave some sort
			//of dangling reference that causes the app to choke due to duplicated ids next
			//time it tries to create the zbx account fragment anew.
			if(zbxAccountDialogGlobalHandle.isShowing()) {
				zbxAccountDialogGlobalHandle.dismiss();
			}
			zbxAccountDialogGlobalHandle = null;
		}
		
		if(provisionTaskHandle!=null) {
			provisionTaskHandle.cancel(false);
			removePreferenceSetting(CloudStackAndroidClient.SHARED_PREFERENCES.LOGIN_INPROGRESS);  //mark LOGIN as no longer in progress so we don't have a never-ending progress circle once we return. If the provision call does complete, we can always re-do it anyways
		}
		
		super.onDestroy();
	}

	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View csaccountfragment = inflater.inflate(R.layout.csaccountfragment, container, false);

		SharedPreferences preferences = getActivity().getSharedPreferences(CloudStackAndroidClient.SHARED_PREFERENCES.PREFERENCES_NAME, Context.MODE_PRIVATE);
		final String savedCsHost = preferences.getString(CloudStackAndroidClient.SHARED_PREFERENCES.CLOUDSTACK_HOST_SETTING, null);
		final String savedCsDomain = preferences.getString(CloudStackAndroidClient.SHARED_PREFERENCES.CLOUDSTACK_DOMAIN_SETTING, null);
		final String savedUsername = preferences.getString(CloudStackAndroidClient.SHARED_PREFERENCES.USERNAME_SETTING, null);
		
        setTextWatcherAndValueForEditText(csaccountfragment, R.id.cshostfield, CloudStackAndroidClient.SHARED_PREFERENCES.CLOUDSTACK_HOST_SETTING, savedCsHost);
        setTextWatcherAndValueForEditText(csaccountfragment, R.id.csdomainfield, CloudStackAndroidClient.SHARED_PREFERENCES.CLOUDSTACK_DOMAIN_SETTING, savedCsDomain);
        setTextWatcherAndValueForEditText(csaccountfragment, R.id.usernamefield, CloudStackAndroidClient.SHARED_PREFERENCES.USERNAME_SETTING, savedUsername);

        setLoginButtonClickHandler(csaccountfragment);
        
		return csaccountfragment;
	}

	public void setTextViewValue(final int idOfTextViewToSet, final String newValue) {
		TextView textView = (TextView)getActivity().findViewById(idOfTextViewToSet);
        if(textView!=null) { textView.setText(newValue); }
	}

	public void setTextWatcherAndValueForEditText(final View csaccountfragment, final int idOfEditTextToWatch, final String preferenceKey, final String previousValue) {
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

        EditText editText = (EditText)csaccountfragment.findViewById(idOfEditTextToWatch);
        if(editText!=null) {
	        editText.addTextChangedListener(textWatcher);
	        
	        if(previousValue!=null) {
	        	editText.setText(previousValue);
	        }
        }
	}

	public void setLoginButtonClickHandler(final View csaccountfragment) {
		final Button loginButton = (Button)csaccountfragment.findViewById(R.id.loginbutton);
        loginButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				loginButton.setClickable(false);
				
				//automatically close the soft keyboard
				final EditText passwordfield = (EditText)csaccountfragment.findViewById(R.id.passwordfield);
				InputMethodManager mgr = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
				mgr.hideSoftInputFromWindow(passwordfield.getWindowToken(), 0);
				
				makeLoginCall(csaccountfragment);
			}
		});
	}

	public void makeLoginCall(View itemView) {
		
		switchLoginProgress();
		
		SharedPreferences preferences = getActivity().getSharedPreferences(CloudStackAndroidClient.SHARED_PREFERENCES.PREFERENCES_NAME, Context.MODE_PRIVATE);
		final String csHost = preferences.getString(CloudStackAndroidClient.SHARED_PREFERENCES.CLOUDSTACK_HOST_SETTING, null);
		final String csDomain = preferences.getString(CloudStackAndroidClient.SHARED_PREFERENCES.CLOUDSTACK_DOMAIN_SETTING, null);
		final String username = preferences.getString(CloudStackAndroidClient.SHARED_PREFERENCES.USERNAME_SETTING, null);
		TextView passwordText = (TextView)getActivity().findViewById(R.id.passwordfield);
		final String password = passwordText.getText().toString();

		clearErrorFrames();
		if(emptyInputValidationFailed(csHost, username, password)) {
			switchLoginProgress();
			return;  //if input validation failed, short-circuit the rest of the method
		}
		final boolean csHostFieldHasInvalidInput = !validateTextInput(R.id.cshostfield, csHost);
		final boolean csDomainFieldHasInvalidInput = TextUtils.isEmpty(csDomain)? false : !validateTextInput(R.id.csdomainfield, csDomain); //csDomain field is optional, so don't bother validating if there is no input
		final boolean usernameFieldHasInvalidInput = !validateTextInput(R.id.usernamefield, username);
		if(csHostFieldHasInvalidInput || csDomainFieldHasInvalidInput || usernameFieldHasInvalidInput) {
			switchLoginProgress();
			return;  //if input validation failed, short-circuit the rest of the method
		}
			
		//make the rest call to login to cs server
        Bundle apiCmd = new Bundle();
        apiCmd.putString(CsRestService.COMMAND, CsApiConstants.API.login);
        apiCmd.putString(CloudStackAndroidClient.SHARED_PREFERENCES.CLOUDSTACK_HOST_SETTING, csHost);
        apiCmd.putString(CsApiConstants.LOGIN_PARAMS.USERNAME, username);
        apiCmd.putString(CsApiConstants.LOGIN_PARAMS.PASSWORD, password);
        if(!TextUtils.isEmpty(csDomain)) {
        	apiCmd.putString(CsApiConstants.LOGIN_PARAMS.DOMAIN, csDomain);
        };
        provisionTaskHandle = new CsLoginTask();
        provisionTaskHandle.execute(apiCmd);
	}
	
	public void clearErrorFrames() {
		showLoginErrorMessage(R.id.hosterrorframe, "");
		showLoginErrorMessage(R.id.usernamepassworderrorframe, "");
		showLoginErrorMessage(R.id.loginerrorframe, "");
		
		removePreferenceSetting(CloudStackAndroidClient.SHARED_PREFERENCES.LOGINERROR_CACHE);
	}

	public void removePreferenceSetting(final String keyToRemove) {
		SharedPreferences preferences = getActivity().getSharedPreferences(CloudStackAndroidClient.SHARED_PREFERENCES.PREFERENCES_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = preferences.edit();
		editor.remove(keyToRemove);
		editor.commit();
	}

	public boolean emptyInputValidationFailed(final String csHost, final String username, final String password) {
		boolean inputIsEmpty = false;
		
		if(TextUtils.isEmpty(csHost)) {
			showLoginErrorMessage(R.id.hosterrorframe, "please enter a valid CloudStack URL");
			inputIsEmpty = true;
		}
		
		if(TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
			showLoginErrorMessage(R.id.usernamepassworderrorframe, "enter your username and password");
			inputIsEmpty = true;
		}
		
		return inputIsEmpty;
	}
	
	public boolean validateTextInput(final int idOfEditTextBeingValidated, final String stringToValidate) {
		if(idOfEditTextBeingValidated==R.id.cshostfield){
			Matcher matcher = csHostPattern.matcher(stringToValidate);
			if(!matcher.matches()) { //csHost cannot contain 1-byte nor 2-byte spaces
				showLoginErrorMessage(R.id.hosterrorframe, "host value must be a valid URL");
				return false;
			} else {
				showLoginErrorMessage(R.id.hosterrorframe, "");
				return true;
			}
		} else if(idOfEditTextBeingValidated==R.id.csdomainfield){
			Matcher matcher = csHostPattern.matcher(stringToValidate);  //we'll re-use the csHost regex check here since we don't have a more stringent check for the domain specifically
			if(!matcher.matches()) { //csDomain cannot contain 1-byte nor 2-byte spaces
				showLoginErrorMessage(R.id.hosterrorframe, "domain value must not have spaces");
				return false;
			} else {
				showLoginErrorMessage(R.id.hosterrorframe, "");
				return true;
			}
		} else if(idOfEditTextBeingValidated==R.id.usernamefield) {
			Matcher matcher = usernamePattern.matcher(stringToValidate);
			if(!matcher.matches()) { //csHost cannot contain 1-byte nor 2-byte spaces
				showLoginErrorMessage(R.id.usernamepassworderrorframe, "username cannot contain special characters");
				return false;
			} else {
				showLoginErrorMessage(R.id.usernamepassworderrorframe, "");
				return true;
			}
		}
		return false;  //fail validation as default
	}
	
	private void showLoginErrorMessage(final int errorFrameId, final String message) {
		TextSwitcher ts = (TextSwitcher) getActivity().findViewById(errorFrameId);
		if(ts!=null) { ts.setText(message); }
	}
	
	private void resetAccount() {
		final FragmentActivity activity = getActivity();
		
		//give any existing zbx account dialog the chance to clean up after itself
		//(this will cause any logged-in zbx account dialogs to flip back to the
		// login screen the next time the dialog is opened (assuming it is not
		// destroyed in the meantime)
		ZbxAccountFragment zbxaccountfragment = (ZbxAccountFragment)getFragmentManager().findFragmentById(R.id.zbxaccountfragment);
		if(zbxaccountfragment!=null) { zbxaccountfragment.resetAccount(); };

		//delete all saved prefs
		SharedPreferences preferences = activity.getSharedPreferences(CloudStackAndroidClient.SHARED_PREFERENCES.PREFERENCES_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor preferencesEditor = preferences.edit();
		preferencesEditor.clear();
		preferencesEditor.commit();
		
		//delete all existing data (both cs and zabbix data since the cs-account reset wipes the whole app)
		CsRestContentProvider.deleteAllData(activity);
		ZbxRestContentProvider.deleteAllData(activity);  //need to explicitly wipe zabbix dbs here as the zbxaccountfragment.resetAccount() call above doesn't do it for some reason
		
		//just to be safe in case there has been scrolling, move scrollview back to beginning so user sees top of view after we flip
		ScrollView detailscrollview = (ScrollView)activity.findViewById(R.id.detailscrollview);
		if(detailscrollview!=null) { detailscrollview.fullScroll(ScrollView.FOCUS_UP); }
		
		//switch the ui to "login screen"
		flipBetweenLoginAndKeysScreens(CsAccountFragment.LOGIN_SCREEN);
		
		//clear the edittexts of previous values
		setTextViewValue(R.id.cshostfield, "");
		setTextViewValue(R.id.csdomainfield, "");
		setTextViewValue(R.id.usernamefield, "");
		setTextViewValue(R.id.passwordfield, "");
		
	}

	public void flipBetweenLoginAndKeysScreens(final int screenToFlipTo) {
		final FragmentActivity activity = getActivity();
		
		ViewSwitcher loginscreenkeyscreenswitcher = (ViewSwitcher)activity.findViewById(R.id.loginscreenkeyscreenswitcher);
		if(loginscreenkeyscreenswitcher.getDisplayedChild()!=screenToFlipTo) {
			final Animation flipout_withstartdelay = AnimationUtils.loadAnimation(activity, R.anim.flipout_withstartdelay);
			final Animation flipin_withstartdelay = AnimationUtils.loadAnimation(activity, R.anim.flipin_withstartdelay);

			loginscreenkeyscreenswitcher.setOutAnimation(flipout_withstartdelay);
			loginscreenkeyscreenswitcher.setInAnimation(flipin_withstartdelay);
			loginscreenkeyscreenswitcher.showNext();
		}
	}
	
	public void switchLoginProgress() {
		ViewSwitcher loginprogressswitcher = (ViewSwitcher)getActivity().findViewById(R.id.loginprogressswitcher);
		if(loginprogressswitcher!=null) {
			loginprogressswitcher.showNext();
			loginprogressswitcher.getCurrentView().setClickable(true);  //always set whatever child is in front to clickable; we want to make the LOGIN button re-clickable after it was made unclickable when pressed + we don't care if the progress circle is clickable
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
