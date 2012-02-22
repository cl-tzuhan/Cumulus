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
package com.creationline.zabbix.engine;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;

import com.creationline.cloudstack.CloudStackAndroidClient;
import com.creationline.common.engine.RestServiceBase;
import com.creationline.common.utils.ClLog;
import com.creationline.zabbix.engine.api.HistoryGetRequest;
import com.creationline.zabbix.engine.api.HostGetRequest;
import com.creationline.zabbix.engine.api.ItemGetRequest;
import com.creationline.zabbix.engine.api.UserLoginRequest;
import com.creationline.zabbix.engine.api.ZbxApiConstants;
import com.creationline.zabbix.engine.api.ZbxApiRequest;
import com.creationline.zabbix.engine.db.Transactions;

public class ZbxRestService extends RestServiceBase {
	public static class INTENT_ACTION {
		//NOTE: changing the value of these constants requires you to change any usage of the same string in Android.manifest!!!
		public static final String ZBXRESTSERVICE_BROADCAST = "com.creationline.zabbix.engine.ZBXRESTSERVICE_BROADCAST";
	}

	public static final String CALL_STATUS = "com.creationline.zabbix.engine.ZbxRestService.CALL_STATUS";  //used with CALL_STATUS_VALUES
	public static class CALL_STATUS_VALUES {
		public static final int LOGIN_SUCCEEDED = 4;
		public static final int LOGIN_FAILED = 3;
		public static final int CALL_STARTED = 2;
		public static final int CALL_SUCCESS = 1;
		public static final int CALL_FAILURE = 0;
	}
	
	//Intent msg-related constants
	public static final class ACTIONS {
		//used to ask ZbxRestService to make the specified zabbix api call
		public static final String CALL_ZBX_API = "com.creationline.zabbix.engine.ZbxRestService.ACTIONS.CALL_ZBX_API";
		
		//used to ask ZbxRestService to make a broadcast with the specified intent filter
		public static final String BROADCAST_NOTIF = "com.creationline.zabbix.engine.ZbxRestService.ACTIONS.BROADCAST_NOTIF";
	}

	public static final class PAYLOAD_FIELDS {
		public static final String FOLLOWUP_ACTION = "com.creationline.zabbix.engine.ZbxRestService.PAYLOAD_FIELDS.FOLLOWUP_ACTION";
		public static final String INTENT_FILTER = "com.creationline.zabbix.engine.ZbxRestService.INTENT_FILTER";
		public static final String ITEM_NAME_ARRAY = "com.creationline.zabbix.engine.ZbxRestService.ITEM_LIST";
		public static final String TARGET_CONTENT_URI = "com.creationline.zabbix.engine.ZbxRestService.TARGET_CONTENT_URI";

	}	

	public static final int MAX_LENGTH_OF_STRING_TO_SAVE = 1000;  //max length of reply json text that will be saved (keeps size of transactions db down)
	public static final int TRANSACTIONS_TABLE_PRUNING_SIZE = 50;  //transactions table pruned to 1/2 its size if it grows above this limit
	
	public ZbxRestService() {
		super("ZbxRestService");
	}
	
	public static Intent createGetDataIntent(Context context, final String hostIp, final String hostName,
																	final String[] itemNames, final String targetContentUri) {
		Bundle hostGetActionPayload = HostGetRequest.createHostGetActionPayload(context,
																				   hostIp,
																				   hostName,
																				   itemNames,
																				   targetContentUri,
																				   null);
		Bundle broadcastDataDownloadInitiatedPayload = createBroadcastPayload(context,
																				   ZbxRestService.INTENT_ACTION.ZBXRESTSERVICE_BROADCAST,
																				   ZbxRestService.CALL_STATUS_VALUES.CALL_STARTED,
																				   null,
																				   hostGetActionPayload);
		Intent zbxHostGetActionIntent = ZbxRestService.createZbxRestServiceIntent(context, broadcastDataDownloadInitiatedPayload);
		return zbxHostGetActionIntent;
	}
	
	public static Intent createZbxRestServiceIntent(Context context, Bundle payload) {
		Intent startZbxRestServiceIntent = new Intent(context, ZbxRestService.class);
        startZbxRestServiceIntent.putExtras(payload);
        
        return startZbxRestServiceIntent;
	}

	public static Bundle createBroadcastPayload(Context context, final String intentFilter,
														final int callStatus, final String data, final Bundle followupAction) {
		Bundle payload = new Bundle();
		payload.putString(RestServiceBase.PAYLOAD_FIELDS.ACTION_ID, ZbxRestService.ACTIONS.BROADCAST_NOTIF);
		payload.putString(ZbxRestService.PAYLOAD_FIELDS.INTENT_FILTER, intentFilter);
		payload.putInt(ZbxRestService.CALL_STATUS, callStatus);
		if(data!=null) { payload.putString(ZbxApiConstants.FIELDS.DATA, data); }  //data field of response for passing error messages and such
        if(followupAction!=null) { payload.putBundle(ZbxRestService.PAYLOAD_FIELDS.FOLLOWUP_ACTION, followupAction); }  //api call that should be started after this one
		
        return payload;
	}
	
	public static Intent createBroadcastIntent(Context context, final String intentFilter,
														final int callStatus, final String data, final Bundle followupAction) {
		Bundle broadcastLoginFailurePayload = ZbxRestService.createBroadcastPayload(context,
				intentFilter,
				callStatus,
				data,
				null);
		Intent broadcastLoginFailureIntent = ZbxRestService.createZbxRestServiceIntent(context, broadcastLoginFailurePayload);
		return broadcastLoginFailureIntent;
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		//This is already running on a non-UI thread
		
		Bundle payload = intent.getExtras();
		final String action = payload.getString(RestServiceBase.PAYLOAD_FIELDS.ACTION_ID);
		
		if(ZbxRestService.ACTIONS.CALL_ZBX_API.equalsIgnoreCase(action)) {
			//calls to the zabbix server api
			processCallApiIntent(payload);
		} else if(ZbxRestService.ACTIONS.BROADCAST_NOTIF.equalsIgnoreCase(action)) {
			//broadcasting results to other parts of the system
			processBroadcastNotifIntent(payload);
		} else {
			ClLog.w("ZbxRestService.onHandleIntent()", "skipping; got unrecognized action="+action);
			return;
		}
		
	}

	public void processCallApiIntent(final Bundle payload) {
		final String TAG = "ZbxRestService.processCallApiIntent()";

		final String apiCmd = payload.getString(RestServiceBase.PAYLOAD_FIELDS.API_CMD);
		ZbxApiRequest zbxRequest = null;

		final String authToken = getSavedAuthToken();
		if(ZbxApiConstants.API.USER.LOGIN.equalsIgnoreCase(apiCmd)) {
			ClLog.d(TAG, "making UserLoginRequest");
			//pulling directly from prefs here to avoid having to sent password through intent system
			SharedPreferences preferences = getSharedPreferences(CloudStackAndroidClient.SHARED_PREFERENCES.PREFERENCES_NAME, Context.MODE_PRIVATE);
			final String zbxUsername = preferences.getString(CloudStackAndroidClient.SHARED_PREFERENCES.ZABBIX_USERNAME_SETTING, null);
			final String zbxPassword = preferences.getString(CloudStackAndroidClient.SHARED_PREFERENCES.ZABBIX_PASSWORD_SETTING, null);
			zbxRequest = new UserLoginRequest(zbxUsername, zbxPassword);
		} else if(ZbxApiConstants.API.HOST.GET.equalsIgnoreCase(apiCmd)) {
			final String ip = payload.getString(ZbxApiConstants.FIELDS.IP);
			final String host = payload.getString(ZbxApiConstants.FIELDS.HOST);
			final String[] itemNames = payload.getStringArray(ZbxRestService.PAYLOAD_FIELDS.ITEM_NAME_ARRAY);
			final String targetContentUri = payload.getString(ZbxRestService.PAYLOAD_FIELDS.TARGET_CONTENT_URI);
			zbxRequest = new HostGetRequest(authToken, ip, host, itemNames, targetContentUri);
			ClLog.d(TAG, "making HostGetRequest with ip="+ip+" host="+host+" itemNames="+itemNames);
		} else if(ZbxApiConstants.API.ITEM.GET.equalsIgnoreCase(apiCmd)) {
			final String hostid = payload.getString(ZbxApiConstants.FIELDS.HOSTID);
			final String itemName = payload.getString(ZbxApiConstants.FIELDS.KEY_);
			final String host = payload.getString(ZbxApiConstants.FIELDS.HOST);
			final String targetContentUri = payload.getString(ZbxRestService.PAYLOAD_FIELDS.TARGET_CONTENT_URI);
			zbxRequest = new ItemGetRequest(authToken, hostid, itemName, host, targetContentUri);
			ClLog.d(TAG, "making ItemGetRequest with hostid="+hostid+" and itemName="+itemName+" and host="+host);
		} else if(ZbxApiConstants.API.HISTORY.GET.equalsIgnoreCase(apiCmd)) {
			final String itemid = payload.getString(ZbxApiConstants.FIELDS.ITEMID);
			final String itemName = payload.getString(ZbxApiConstants.FIELDS.KEY_);
			final int numDays = payload.getInt(HistoryGetRequest.FIELDS.NUMHOURS);
			final String host = payload.getString(ZbxApiConstants.FIELDS.HOST);
			final String targetContentUri = payload.getString(ZbxRestService.PAYLOAD_FIELDS.TARGET_CONTENT_URI);
			zbxRequest = new HistoryGetRequest(authToken, itemid, itemName, numDays, host, targetContentUri);
			ClLog.d(TAG, "making HistoryGetRequest with itemid="+itemid+" itemName="+itemName+" for numDays="+numDays+" and host="+host+" targetContentUri="+targetContentUri);
		} else {
			ClLog.e(TAG, "skipping; got unrecognized apiCmd="+apiCmd);
			return;
		}
		
		//set a follow-up intent if specified
		final Bundle followupAction = payload.getBundle(ZbxRestService.PAYLOAD_FIELDS.FOLLOWUP_ACTION);
		if(followupAction!=null) {
			Intent zbxGetCpuLoadDataIntent = ZbxRestService.createZbxRestServiceIntent(this, followupAction);
			zbxRequest.setFollowUpRequest(zbxGetCpuLoadDataIntent);
		}
		
		performRestRequest(zbxRequest);
	}
	
	public String getSavedAuthToken() {
		SharedPreferences preferences = getSharedPreferences(CloudStackAndroidClient.SHARED_PREFERENCES.PREFERENCES_NAME, Context.MODE_PRIVATE);
		final String authToken = preferences.getString(CloudStackAndroidClient.SHARED_PREFERENCES.ZABBIX_AUTH_TOKEN, null);
		
		return authToken;
	}
		
	private void performRestRequest(ZbxApiRequest zbxRequest) {
		Uri inProgressTransaction = null;
		try {
			//construct zabbix api url from user-specified host
			String zbxApiUrl = constructZbxApiUrl();

			//save the request to db
			inProgressTransaction = saveRequestToDb(zbxApiUrl, zbxRequest);
			inProgressTransactionList.add(inProgressTransaction);
			
			//though we don't save it to db, we will always assume the
			//_id value of the entry in the transactions table as the
			//"id" value of the json request to the zabbix server
			zbxRequest.setTransactionRowAsId(inProgressTransaction);

			//send request to Zabbix
			HttpResponse reply = doRestCall(zbxApiUrl, zbxRequest);

			//extract body text from response
			StringBuilder replyBody = getReplyBody(reply);

			//save reply to view data db
			saveReplyToDb(inProgressTransaction, reply, replyBody);

			//perform call-specific actions based on response
			handleResponseFromServer(zbxRequest, reply, replyBody);

		} catch (Exception e) {
			updateCallAsAbortedOnDb(inProgressTransaction);
			startService(zbxRequest.doRequestFailureProcessing(this, e.toString()));
			ClLog.e("ZbxRestService.performRestRequest()", e);
		}
		
		inProgressTransactionList.remove(inProgressTransaction);
		inProgressTransaction = null;
	}

	public String constructZbxApiUrl() {

		SharedPreferences preferences = getSharedPreferences(CloudStackAndroidClient.SHARED_PREFERENCES.PREFERENCES_NAME, Context.MODE_PRIVATE);
		String zbxUrl = preferences.getString(CloudStackAndroidClient.SHARED_PREFERENCES.ZABBIX_HOST_SETTING, "http://no.host");

		if(!zbxUrl.startsWith("http://")) {
			zbxUrl = "http://"+zbxUrl;
		}
		if(zbxUrl.endsWith("/")) {
			zbxUrl = zbxUrl.substring(0, zbxUrl.length()-1);
		}
		if(zbxUrl.endsWith("/zabbix")) {
			zbxUrl += "/api_jsonrpc.php";
		}
		if(!zbxUrl.endsWith("/zabbix/api_jsonrpc.php")) {
			zbxUrl += "/zabbix/api_jsonrpc.php";
		}
		//the final form of the url should be along the lines of: "http://some.zabbix.server/zabbix/api_jsonrpc.php"
		ClLog.d("ZbxRestService.constructZbxApiUrl()", "constructed zabbix api url: "+zbxUrl);
		
		return zbxUrl;
	}
	
	@Override
	public void onDestroy() {
		// When HttpClient instance is no longer needed,
        // shut down the connection manager to ensure
        // immediate deallocation of all system resources
		if(httpclient!=null) {
			ClientConnectionManager connectionManager = httpclient.getConnectionManager();
			if(connectionManager!=null) {
				connectionManager.shutdown();
			}
		}
		
		pruneTransactionsDb();
		
		super.onDestroy();
	}

	/**
	 * If the transactions database table size is greater than the fixed TRANSACTIONS_TABLE_PRUNING_SIZE size (#-of-rows),
	 * this method will delete rows from the beginning of the table (ie. oldest rows first) so the table becomes
	 * half its original size.
	 */
	public void pruneTransactionsDb() {
		final String TAG = "ZbxRestService.pruneTransactionsDb()";
		ContentResolver contentResolver = getContentResolver();

		final String[] columns = new String[] { Transactions._ID };
		final String sortOrder = Transactions._ID;
		Cursor c = contentResolver.query(Transactions.META_DATA.CONTENT_URI, columns, null, null, sortOrder);
		final int tableSize = c.getCount();
		if(tableSize>TRANSACTIONS_TABLE_PRUNING_SIZE) {
			final int numToDelete = tableSize/2;
			ClLog.i("ZbxRestService.onDestory()", "transactions db has grown to size="+tableSize);
			
			//create a batch operation with all of the rows we want to delete
			final int idColumnIndex = c.getColumnIndex(Transactions._ID);
			final String whereClause = Transactions._ID+"=?";
			ArrayList<ContentProviderOperation> deleteFirstHalfOfDb = new ArrayList<ContentProviderOperation>();
			c.moveToFirst();
			for( ; deleteFirstHalfOfDb.size()<numToDelete; c.moveToNext()) {
				ContentProviderOperation deleteOperation = ContentProviderOperation.newDelete(Transactions.META_DATA.CONTENT_URI)
																				   .withSelection(whereClause, new String[] { String.valueOf(c.getInt(idColumnIndex)) })
																				   .build();
				deleteFirstHalfOfDb.add(deleteOperation);
			}
			ClLog.i("ZbxRestService.onDestory()", "pruning "+deleteFirstHalfOfDb.size()+" rows from the beginning of transactions db");
			//delete front half of db in one go
			try {
				contentResolver.applyBatch(ZbxRestContentProvider.AUTHORITY, deleteFirstHalfOfDb);
			} catch (RemoteException e) {
				ClLog.e(TAG, "got RemoteException! [" + e.toString() +"]");
	        	ClLog.e(TAG, e);
			} catch (OperationApplicationException e) {
				ClLog.e(TAG, "could not execute batch op! [" + e.toString() +"]");
	        	ClLog.e(TAG, e);
			}
		}
		c.close();
	}
	
	public Uri saveRequestToDb(final String requestUrl, ZbxApiRequest zbxRequest) {
		final String TAG = "ZbxRestService.saveRequestToDb()";
		
		if(requestUrl==null) {
			return null;
		}
		
		ContentValues contentValues = new ContentValues();
		contentValues.put(Transactions.REQUEST, requestUrl);
		contentValues.put(Transactions.POSTDATA, zbxRequest.getPostData().toString());
		contentValues.put(Transactions.STATUS, Transactions.STATUS_VALUES.IN_PROGRESS);
		contentValues.put(Transactions.REQUEST_DATETIME, getCurrentTimeAsString());
		
		Uri newUri = getBaseContext().getContentResolver().insert(Transactions.META_DATA.CONTENT_URI, contentValues);
		ClLog.d(TAG, "added new API request to db at " + newUri);
		
		return newUri;
	}
	
	/**
	 * Adapted from:
	 *   http://www.devdaily.com/java/jwarehouse/commons-httpclient-4.0.3/httpclient/src/examples/org/apache/http/examples/client/ClientWithResponseHandler.java.shtml
	 *   
	 * @param url url to send an http POST to
	 * @throws IOException 
	 */
	private HttpResponse doRestCall(final String url, ZbxApiRequest zbxRequest) throws IOException {
		final String TAG = "ZbxRestService.doRestCall()";
		
		if(url==null) {
			return null;
		}

		try {
            //final HttpGet httpGet = new HttpGet(url);
            final HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Content-Type", "application/json-rpc");
            
            ClLog.d(TAG, "zbxRequest.getPostData().toString()="+zbxRequest.getPostData().toString());
            httpPost.setEntity(new StringEntity(zbxRequest.getPostData().toString()));
           
            if(httpclient==null) {
            	httpclient = new DefaultHttpClient();  //lazy init
            }
            
            HttpResponse response =  httpclient.execute(httpPost);
            return response;
            
        } catch (ClientProtocolException e) {
        	ClLog.e(TAG, e);
        	throw e;
		} catch (IllegalArgumentException e) {
			ClLog.e(TAG, e);
			throw e;
		} catch (IOException e) {
			ClLog.e(TAG, e);
			throw e;
		} catch (IllegalStateException e) {
			ClLog.e(TAG, e);
			throw e;
		} finally {
			//nothing done here currently
		}
	}
	
	public void saveReplyToDb(final Uri uriToUpdate, final HttpResponse reply, final StringBuilder replyBody) {
		final String TAG = "ZbxRestService.saveReplyToDb()";

		if(uriToUpdate==null || reply==null) {
			ClLog.e(TAG, "required params are null, so aborting.  uriToUpdate="+uriToUpdate+"  reply="+reply);
			
			if(uriToUpdate!=null && reply==null) {
				updateCallAsAbortedOnDb(uriToUpdate);
			}
			return;
		}
		
		//parse the reply for data
		final int statusCode = reply.getStatusLine().getStatusCode();
		final boolean callReturnedOk = statusCode==HttpStatus.SC_OK;
		final String status = (callReturnedOk)? Transactions.STATUS_VALUES.SUCCESS : Transactions.STATUS_VALUES.FAIL;
		
		updateCallWithReplyOnDb(uriToUpdate, status, replyBody);
		
	}
	
	public void updateCallAsAbortedOnDb(Uri uriToUpdate) {
		final String TAG = "ZbxRestService.updateCallAsAborted()";

		//mark transaction as aborted
		ContentValues cvForTransactionsTable = new ContentValues();
		cvForTransactionsTable.put(Transactions.STATUS, Transactions.STATUS_VALUES.ABORTED);
		cvForTransactionsTable.put(Transactions.REPLY_DATETIME, getCurrentTimeAsString());
		
		int rowsUpdated = getContentResolver().update(uriToUpdate, cvForTransactionsTable, null, null);
		assert(rowsUpdated==1);  //sanity check: this should only ever update a single row
		
		ClLog.d(TAG, "marked as aborted " + uriToUpdate);
		
	}
	
	public void updateCallWithReplyOnDb(final Uri uriToUpdate, final String status, final StringBuilder replyBodyText) {
		final String TAG = "ZbxRestService.updateCallWithReplyOnDb()";

		//save the parsed data to db
		ContentValues contentValues = new ContentValues();
		contentValues.put(Transactions.STATUS, status);
		contentValues.put(Transactions.REPLY_DATETIME, getCurrentTimeAsString());
		
		ClLog.d(TAG, "replyBodyText.length()="+replyBodyText.length());
		if(replyBodyText.length()<MAX_LENGTH_OF_STRING_TO_SAVE) {
			contentValues.put(Transactions.REPLY, replyBodyText.toString());
		} else {
			//because history.get calls can return data on the range of 100KB to 1MB and more, we will arbitrarily
			//truncate the reply body that gets persisted to prevent the db from growing too quickly
			//(we still use the complete un-truncated reply body for processing in memory)
			final String truncatedReplyBodyText = truncateStringToLength(replyBodyText, MAX_LENGTH_OF_STRING_TO_SAVE);
			contentValues.put(Transactions.REPLY, truncatedReplyBodyText);
			ClLog.d(TAG, "truncatedReplyBodyText.length()="+truncatedReplyBodyText.length());
		}
		
		int rowsUpdated = getContentResolver().update(uriToUpdate, contentValues, null, null);
		assert(rowsUpdated==1);  //sanity check: this should only every update a single row
		
		ClLog.d(TAG, "updated request/reply record for " + uriToUpdate);
	}

	public String truncateStringToLength(final StringBuilder replyBodyText, final int length) {
		//truncates string to specified length and post-pends a "..." _after_ the truncated string
		return replyBodyText.substring(0, length).concat("...");
	}
	
	public void handleResponseFromServer(ZbxApiRequest zbxRequest, HttpResponse reply, StringBuilder replyBody) throws IOException {
		if(reply==null) {
			return;
		}
		
		final int statusCode = reply.getStatusLine().getStatusCode();
		final boolean callReturnedOk = statusCode==HttpStatus.SC_OK;
		final boolean ranInto404 = statusCode==HttpStatus.SC_NOT_FOUND;
		if(callReturnedOk) {
			zbxRequest.handleResponseFromServer(getApplicationContext(), replyBody.toString());
		} else if(ranInto404){
			throw new IOException("Zabbix instance not found at saved URL (404)");
		} else {
			throw new IOException("encountered unknown network error (HttpStatus="+statusCode+")");
		}
	}
	
	public void processBroadcastNotifIntent(final Bundle payload) {
		informCallerFragmentOfDownloadCompletion(payload);
	
		//set a follow-up intent if specified
		final Bundle followupAction = payload.getBundle(ZbxRestService.PAYLOAD_FIELDS.FOLLOWUP_ACTION);
		if(followupAction!=null) {
			Intent startFollowupActionIntent = ZbxRestService.createZbxRestServiceIntent(this, followupAction);
			startService(startFollowupActionIntent);
		}
		
	}
	
	public void informCallerFragmentOfDownloadCompletion(final Bundle payload) {
		//inform calling fragment of the result of the call
		final String intentFilter = payload.getString(ZbxRestService.PAYLOAD_FIELDS.INTENT_FILTER);
		final int callStatus = payload.getInt(ZbxRestService.CALL_STATUS);
		final String data = payload.getString(ZbxApiConstants.FIELDS.DATA);
		
		Intent broadcastIntent = new Intent(intentFilter);
		Bundle bundle = new Bundle();
		bundle.putInt(ZbxRestService.CALL_STATUS, callStatus);
		if(data!=null) { bundle.putString(ZbxApiConstants.FIELDS.DATA, data); };
		
		broadcastIntent.putExtras(bundle);
		sendBroadcast(broadcastIntent);
	}

}
