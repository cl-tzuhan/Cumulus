package com.creationline.cloudstack.engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.ObjectMapper;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Base64;

import com.creationline.cloudstack.engine.db.Errors;
import com.creationline.cloudstack.engine.db.Transactions;
import com.creationline.cloudstack.engine.db.Vms;
import com.creationline.cloudstack.util.ClLog;

public class CsRestService extends IntentService {
	
	//Intent msg-related constants
	public static final String ACTION_ID = "com.creationline.engine.ACTION_ID";
	public static final String API_CMD = "com.creationline.engine.API_CMD";
	public static final String RESPONSE = "com.creationline.engine.RESPONSE";
	
	public static final String TEST_CALL = "com.creationline.cloudstack.engine.TEST_CALL";
	
	//constants matching params used by the CS API request format
	public static final String COMMAND = "command";
	
	
	private Uri inProgressTransaction = null;
	private static Time time = null;

	
	public CsRestService() {
		super("CsRestService");
		time = new Time();  //use default timezone
	}
	
	public static Intent createCsRestServiceIntent(Context context, String action, Bundle apiCmd) {
		
		Bundle payload = new Bundle();
        payload.putString(CsRestService.ACTION_ID, action);  //action stored under this key
        payload.putBundle(CsRestService.API_CMD, apiCmd);  //main part of REST request stored under this key
        
        Intent startCsRestServiceIntent = new Intent(context, CsRestService.class);
        startCsRestServiceIntent.putExtras(payload);
        
        return startCsRestServiceIntent;
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		//This is already running on a non-UI thread
		
		Bundle payload = intent.getExtras(); 
		Bundle apiCmd = payload.getBundle(CsRestService.API_CMD);
		
		initiateRestRequest(apiCmd);
		
		Intent broadcastIntent = new Intent(payload.getString(CsRestService.ACTION_ID));
		broadcastIntent.putExtra(CsRestService.RESPONSE, apiCmd+"Response");
		sendBroadcast(broadcastIntent);

	}
	
	@Override
	public void onDestroy() {
		final String TAG = "CsRestService.onDestory()";

		if(inProgressTransaction!=null) {
			ClLog.i(TAG, "App exited before request completed.  Marking "+inProgressTransaction+" as canceled");
			updateCallAsAbortedOnDb(inProgressTransaction);
		}
		
		super.onDestroy();
	}

	private void initiateRestRequest(Bundle apiCmd) {
		
		// ApiKey and secretKey as given by your CloudStack vendor
//		String apiKey = "namomNgZ8Qt5DuNFUWf3qpGlQmB4650tY36wFOrhUtrzK13d66qNpttKw52Brj02dbtIHs01y-lCLz1UOzTxVQ";    //thsu-account@192.168.3.11:8080 use
//		String secretKey = "Yt_9ZEIDGlmRIg63MiMatAri-1aRoo4l-82mnbYdR3d8JdG7jvXqrrB5TpmbLZB_8zK_j95VRSQWZwnu0153eQ"; //thsu-account@192.168.3.11:8080 use
//		String apiKey = "fUFqsJeECZcMawm9q376WKFKdFvd51GLHwgm3d9PD-r3mjNJUaXBYbkKxBoxCdF5EubJ-ypmT8vHihtAm-gZvA";    //iizuka1@72.52.126.24 use
//		String secretKey = "Q3s_-gMYzivbaaO9S_2ewdXHSXHvUg6ExP0W2yRWBZxFIbTDIKD3ADk-0NU6qhsD0K31e9Irchh_Z8yuRQTuqQ"; //iizuka1@72.52.126.24 use
		String apiKey = "cqLtNDMDYAeIZ6ZdZQG2QInyE5Sx4M914eSeb-rsJTewTvcCcGLRMe-zh_IPQQKmcIGJzNBa_UGrLDhS_LEy-g";    //rickson@219.117.239.169:8080/client/ use
		String secretKey = "lodAuMftOyg0nWiwU5JUy__nn9YO1uJ34oxE9PvdLplJQOTmrEzpoe3wXjG0u1-AsY2y9636GTGDs5LsinxK7Q"; //rickson@219.117.239.169:8080/client/ use

		//create complete url
		String finalUrl = buildFinalUrl(null, apiCmd, apiKey, secretKey);
		
		//save the request to db
		inProgressTransaction = saveRequestToDb(finalUrl);
		
		//send request to cs
		HttpResponse response = doRestCall(finalUrl, inProgressTransaction);
		
		//save reply to view data db
		saveReplyToDb(inProgressTransaction, response, apiCmd);
		
		inProgressTransaction = null;
		
	}

	public Uri saveRequestToDb(String request) {
		final String TAG = "CsRestService.saveRequestToDb()";
		
		if(request==null) {
			return null;
		}
		
		ContentValues contentValues = new ContentValues();
		contentValues.put(Transactions.REQUEST, request);
		contentValues.put(Transactions.STATUS, Transactions.STATUS_VALUES.IN_PROGRESS);
		time.setToNow();
		contentValues.put(Transactions.REQUEST_DATETIME, time.format3339(false));
		///To change the saved REQUEST_DATETIME str back into a Time object, use the following:
		//    Time readTime = new Time();
		//    readTime.parse3339(timeStr);  //str was saved out using RFC3339 format, so needs to be read in as such
		//    readTime.switchTimezone("Asia/Tokyo");  //parse3339() automatically converts read in times to UTC.  We need to change it back to the default timezone of the handset (JST in this example)
		
		Uri newUri = getBaseContext().getContentResolver().insert(Transactions.META_DATA.CONTENT_URI, contentValues);
		ClLog.d(TAG, "added new API request to db at " + newUri);
		return newUri;
	}
	
	/**
	 * Adapted from:
	 *   http://download.cloud.com/releases/2.2.0/api/user/2.2api_examplecode_details.html
	 * @param host if not null, the CS server url to connect to
	 * @param apiCmd command to send to CS server (not including host and user api path)
	 *   
	 * @return finalized URL that can be used to call CS server; null if any step of the building process failed
	 */
//	public static String buildFinalUrl(String specifiedHost, String apiCmd, String apiKey, String secretKey) {
	public static String buildFinalUrl(String specifiedHost, Bundle apiCmd, String apiKey, String secretKey) {
		final String TAG = "CsRestService.buildFinalUrl()";
		
		//String HOST = "http://192.168.3.11:8080/client/api";  //CL CS user api base url
		//String HOST = "http://72.52.126.24/client/api";  //Citrix CS user api base url
		String HOST = "http://219.117.239.169:8080/client/api";  //SakauePark CS user api base url
		
		if (specifiedHost!=null) {HOST = specifiedHost;}  //use any caller-specified host over the default value
		
		// Command and Parameters
//		String apiUrl = "command=listVirtualMachines&account=thsu-account&domainid=2&response=json";

		
		/// These are the things that need to be done next!!!
		///
		/// TODO: need to find a way to have the user specify her own host
		/// TODO: need to find a way to have the user specify her apiKey/secretKey
		///

		try {
			if (apiCmd == null || apiCmd.isEmpty() || apiKey == null || secretKey == null) {
				return null;
			}
			
			//make sure we get reply in json 
//			apiCmd += JSON_PARAM;  //(will not check whether apiCmd already has response=json param for speed purposes, but having 2 of these params will cause call to fail)
			apiCmd.putString("response", "json");    //(will not check whether apiCmd already has response=json param for speed purposes, but having 2 of these params will cause call to fail)

			ClLog.d(TAG, "constructing API call to host='" + HOST + " and apiUrl='" + apiCmd + "' using apiKey='" + apiKey + "' and secretKey='" + secretKey + "'");
			
			// Step 1: Make sure your APIKey is toLowerCased and URL encoded
			String encodedApiKey = URLEncoder.encode(apiKey.toLowerCase(), "UTF-8"); //NOTE: URLEncoder will convert spaces to "+" instead of "%20" like CS prefers
			
			// Step 2: toLowerCase all the parameters, URL encode each parameter value, and the sort the parameters in alphabetical order
			// Please note that if any parameters with a '&' as a value will cause this test client to fail since we are using '&' to delimit 
			// the string
			List<String> sortedParams = new ArrayList<String>();
			sortedParams.add("apikey="+encodedApiKey);
//			StringTokenizer st = new StringTokenizer(apiCmd, "&");
//			while (st.hasMoreTokens()) {
//				String paramValue = st.nextToken().toLowerCase();
//				String param = paramValue.substring(0, paramValue.indexOf("="));
//				String value = URLEncoder.encode(paramValue.substring(paramValue.indexOf("=")+1, paramValue.length()), "UTF-8");   //NOTE: URLEncoder will convert spaces to "+" instead of "%20" like CS prefers
//				sortedParams.add(param + "=" + value);
//			}
			Iterator<String> keySetItr = apiCmd.keySet().iterator();
			StringBuilder apiCmdSb = new StringBuilder();
			while(keySetItr.hasNext()) {
				//go through apiCmd bundle and compile the strings we need for building the actual request url
				final String param = keySetItr.next();
				final String value = apiCmd.getString(param);
				
				apiCmdSb.append(param).append("=").append(value).append("&");  //create the un-encoded/un-sorted version of the apiCmd that goes into the final url
				
				sortedParams.add(param+"="+URLEncoder.encode(value.toLowerCase(), "UTF-8"));  //compile a encoded&sorted string from the apiCmd params/values that is used to sign the request
				   																			  //NOTE: URLEncoder will convert spaces to "+" instead of "%20" like CS prefers
			}
			Collections.sort(sortedParams);
			ClLog.d(TAG, "sorted Parameters= " + sortedParams);
			
			// Step 3: Construct the sorted URL and sign and URL encode the sorted URL with your secret key
			String sortedUrl = null;
			boolean first = true;
			for (String param : sortedParams) {
				if (first) {
					sortedUrl = param;
					first = false;
				} else {
					sortedUrl = sortedUrl + "&" + param;
				}
			}
			ClLog.d(TAG, "sorted URL: " + sortedUrl);
			final String encodedSignature = signRequest(sortedUrl, secretKey);
			
			// Step 4: Construct the final URL we want to send to the CloudStack Management Server
			// Final result should look like:
			// http(s)://client/api?&apiKey=&signature=
//			final String finalUrl = HOST + "?" + apiCmd + "&apiKey=" + apiKey + "&signature=" + encodedSignature;
			final String finalUrl = HOST + "?" + apiCmdSb.toString() + "apiKey=" + apiKey + "&signature=" + encodedSignature;
			ClLog.d(TAG, "finalURL= " + finalUrl);
//			System.out.println("CsRestService.callUserApi(): final URL: " + finalUrl);
			
			// Step 5: Perform a HTTP GET on this URL to execute the command
//			doRestCall(finalUrl);
			// Step 5: return final URL
			return finalUrl;
			
		} catch (Throwable t) {
			ClLog.e(TAG, "error occurred building api call: " + t.toString() + " ["+t.getMessage()+"]");
			ClLog.e(TAG, t);
			return null;
		}
	}
	
	/**
	 * Adapted from:
	 *   http://download.cloud.com/releases/2.2.0/api/user/2.2api_examplecode_details.html
	 *   
	 * 1. Signs a string with a secret key using SHA-1
	 * 2. Base64 encode the result
	 * 3. URL encode the final result
	 * 
	 * @param request data to sign
	 * @param key secret key to sign data with
	 * @return requested signed with specified key
	 */
	public static String signRequest(final String request, final String key) {
		final String TAG = "CsRestService.signRequest()";
		
		try {
			Mac mac = Mac.getInstance("HmacSHA1");
			SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), "HmacSHA1");	
			mac.init(keySpec);
			mac.update(request.getBytes());
			final byte[] encryptedBytes = mac.doFinal();
			 //NOTE: URLEncoder will convert spaces to "+" instead of "%20" like CS prefers
			return URLEncoder.encode(Base64.encodeToString(encryptedBytes, Base64.NO_WRAP), "UTF-8"); //use NO_WRAP so no extraneous CR/LFs are added onto the end of the base64-ed string
		} catch (Exception ex) {
			ClLog.e(TAG, "got Exception! [" + ex.toString() +"]");
			ClLog.e(TAG, ex);
		}
		return null;
	}
	
	/**
	 * Adapted from:
	 *   http://www.devdaily.com/java/jwarehouse/commons-httpclient-4.0.3/httpclient/src/examples/org/apache/http/examples/client/ClientWithResponseHandler.java.shtml
	 *   
	 * @param url url to send an http GET to
	 * @param originatingTransactionUri uri of row in transactions db table that records this call (used to link an error in the errors db table to the specific transaction in the case of a failure)
	 */
	private HttpResponse doRestCall(final String url, final Uri originatingTransactionUri) {
		final String TAG = "CsRestService.doRestCall()";
		
		if(url==null) {
			return null;
		}
		
        HttpClient httpclient = new DefaultHttpClient();
        try {
            final HttpGet httpGet = new HttpGet(url);
            //final HttpPost httpPost = new HttpPost(url);
            
            // Create a response handler
            HttpResponse response =  httpclient.execute(httpGet);
            return response;
            
        } catch (ClientProtocolException e) {
        	ClLog.e(TAG, "got ClientProtocolException! [" + e.toString() +"]");
        	ClLog.e(TAG, e);
			return null;
		} catch (IllegalArgumentException e) {
			ClLog.e(TAG, "got IllegalArgumentException! [" + e.toString() +"]");
			ClLog.e(TAG, e);
		} catch (IOException e) {
			ClLog.e(TAG, "got IOException! [" + e.toString() +"]");
			ClLog.e(TAG, e);
			//save the error to errors db as well
			ContentValues cv = new ContentValues();
			cv.put(Errors.ERRORTEXT, e.getMessage());
			cv.put(Errors.ORIGINATINGCALL, originatingTransactionUri.toString());
			getContentResolver().insert(Errors.META_DATA.CONTENT_URI, cv);
		} finally {
            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
            httpclient.getConnectionManager().shutdown();
        }
		
		return null;
	}
	
	public void saveReplyToDb(Uri uriToUpdate, HttpResponse reply, Bundle apiCmd) {
		final String TAG = "CsRestService.saveReplyToDb()";

		if(uriToUpdate==null || reply==null) {
			ClLog.e(TAG, "required params are null, so aborting.  uriToUpdate="+uriToUpdate+"  reply="+reply);
			
			if(uriToUpdate!=null && reply==null) {
				updateCallAsAbortedOnDb(uriToUpdate);
			}
			return;
		}
		
		//parse the reply for data
		final int statusCode = reply.getStatusLine().getStatusCode();
		final HttpEntity entity = reply.getEntity();
		InputStream replyBody = null;
		try {
			replyBody = entity.getContent();
		} catch (IllegalStateException e) {
			ClLog.e(TAG, "got IllegalStateException! [" + e.toString() +"]");
			ClLog.e(TAG, e);
		} catch (IOException e) {
			ClLog.e(TAG, "got IOException! [" + e.toString() +"]");
			ClLog.e(TAG, e);
		}
		final boolean callReturnedOk = statusCode==HttpStatus.SC_OK;
		final String status = (callReturnedOk)? Transactions.STATUS_VALUES.SUCCESS : Transactions.STATUS_VALUES.FAIL;
		final StringBuilder replyBodyText = inputStreamToString(replyBody);
		ClLog.d(TAG, "parsed reply: statusCode="+statusCode+"  body="+replyBodyText);

		updateCallWithReplyOnDb(uriToUpdate, status, replyBodyText);
		
		if(callReturnedOk) {
			processAndSaveJsonReplyData(apiCmd, replyBodyText.toString());
		} else {
			parseErrorAndAddToDb(uriToUpdate, statusCode, replyBodyText);
		}
	}

	public void parseErrorAndAddToDb(final Uri uriToUpdate, final int statusCode, final StringBuilder replyBodyText) {
		final String TAG = "CsRestService.parseErrorAndAddToDb()";

		//extract the error details from the reply, defaulting to unknown if parse fails
		String errorText = "unknown error";
		ObjectMapper om = new ObjectMapper();
		try {
			JsonNode errorObj = om.readTree(replyBodyText.toString());
			errorText = errorObj.findValue("errortext").asText();
		} catch (JsonParseException e) {
			ClLog.e(TAG, "expected errorresponse not well-formed! [" + e.toString() +"]");
			ClLog.e(TAG, e);
		} catch (IOException e) {
			ClLog.e(TAG, "got IOException parsing errorresponse! [" + e.toString() +"]");
			ClLog.e(TAG, e);
		}
		
		ContentValues cv = new ContentValues();
		cv.put(Errors.ERRORTEXT, errorText);
		cv.put(Errors.ERRORCODE, statusCode);
		cv.put(Errors.ORIGINATINGCALL, uriToUpdate.toString());
		//save the error to errors db
		getContentResolver().insert(Errors.META_DATA.CONTENT_URI, cv);
	}

	public void updateCallWithReplyOnDb(final Uri uriToUpdate, final String status, final StringBuilder replyBodyText) {
		final String TAG = "CsRestService.updateCallWithReplyOnDb()";

		//save the parsed data to db
		ContentValues contentValues = new ContentValues();
		contentValues.put(Transactions.STATUS, status);
		contentValues.put(Transactions.REPLY, replyBodyText.toString());
		time.setToNow();
		contentValues.put(Transactions.REPLY_DATETIME, time.format3339(false));
		///To change the saved REQUEST_DATETIME str back into a Time object, use the following:
		//    Time readTime = new Time();
		//    readTime.parse3339(timeStr);  //str was saved out using RFC3339 format, so needs to be read in as such
		//    readTime.switchTimezone("Asia/Tokyo");  //parse3339() automatically converts read in times to UTC.  We need to change it back to the default timezone of the handset (JST in this example)
		
		int rowsUpdated = getContentResolver().update(uriToUpdate, contentValues, null, null);
		assert(rowsUpdated==1);  //sanity check: this should only every update a single row
		
		ClLog.d(TAG, "updated request/reply record for " + uriToUpdate);
	}

	public void updateCallAsAbortedOnDb(Uri uriToUpdate) {
		final String TAG = "CsRestService.updateCallAsAborted()";

		//mark transaction as aborted
		ContentValues cvForTransactionsTable = new ContentValues();
		cvForTransactionsTable.put(Transactions.STATUS, Transactions.STATUS_VALUES.ABORTED);
		time.setToNow();
		cvForTransactionsTable.put(Transactions.REPLY_DATETIME, time.format3339(false));
		
		int rowsUpdated = getContentResolver().update(uriToUpdate, cvForTransactionsTable, null, null);
		assert(rowsUpdated==1);  //sanity check: this should only ever update a single row
		
		
		//add error msg to errors table so user sees an error msg
//		ContentValues cvForErrorsTable = new ContentValues();
//		cvForErrorsTable.put(Errors.ERRORTEXT, "Connection could not be made; transaction aborted.");
//		cvForErrorsTable.put(Errors.ORIGINATINGCALL, uriToUpdate.toString());
//		getContentResolver().insert(Errors.META_DATA.CONTENT_URI, cvForErrorsTable);
		
		ClLog.d(TAG, "marked as aborted " + uriToUpdate);
	}

	/**
	 * Copied with slight modification from:
	 *   http://www.androidsnippets.com/get-the-content-from-a-httpresponse-or-any-inputstream-as-a-string
	 *   
	 * @param is InputStream with data to read out
	 * @return all the data in specified InputStream
	 */
	public StringBuilder inputStreamToString(final InputStream is) {

	    String line = "";
	    StringBuilder total = new StringBuilder();
	    
	    // Wrap a BufferedReader around the InputStream
	    BufferedReader rd = new BufferedReader(new InputStreamReader(is));

	    // Read response until the end
	    try {
			while ((line = rd.readLine()) != null) { 
			    total.append(line); 
			}
		} catch (IOException e) {
			ClLog.e("inputStreamToString", e);
		}
	    
	    // Return full string
	    return total;
	}
	
	private void processAndSaveJsonReplyData(Bundle apiCmd, String replyBodyText) {
		final String TAG = "CsRestService.processAndSaveJsonReplyData()";
	
		final String cmd = apiCmd.getString(CsRestService.COMMAND);
		if("listVirtualMachines".equals(cmd)) {
			//parse listVirtualMachine results and save to vms table
			parseReplyBody_listVirtualMachines(replyBodyText);
			
			
			///////////////////////////////////////////////////////////////////
			//TODO: other API calls to handle will go below here as an else-if
			///////////////////////////////////////////////////////////////////
		} else {
			//no such api call!
			ClLog.e(TAG, "No such CloudStack API call exists [cmd="+cmd+"].  No data saved to datastore.");
		}
		
	}

	public void parseReplyBody_listVirtualMachines(final String replyBodyText) {
		final String TAG = "CsRestService.parseListVirtualMachinesResult()";

		try {
			ObjectMapper om = new ObjectMapper();
			JsonNode rootNode = om.readTree(replyBodyText);
			//JsonNode vmNode = rootNode.findPath("virtualmachine");
			JsonNode vmNode = rootNode.path("listvirtualmachinesresponse").path("virtualmachine");  //extract the virtualmachine list, which contains the actual vm data
			JsonParser jsonParser = vmNode.traverse();
			
			int num = getContentResolver().delete(Vms.META_DATA.CONTENT_URI, null, null);
			ClLog.i(TAG, "clearing vms db before adding new data; num of records deleted=" + num);
			
			JsonToken token = null;
			ContentValues contentValues = new ContentValues();
			for(token = jsonParser.nextToken(); token!=null && token!=JsonToken.END_ARRAY; token = jsonParser.nextToken()) {
				
				if(token==JsonToken.START_ARRAY || token==JsonToken.END_ARRAY) {
					continue;  //ignore any array delimiters
				}
				
				if(token==JsonToken.START_OBJECT) {
					contentValues.clear();  //if we are at the start of an vm item, prepare a clean contentValues to save parsed data
					continue;
				}

				if(token==JsonToken.END_OBJECT) {
					Uri newUri = getContentResolver().insert(Vms.META_DATA.CONTENT_URI, contentValues);  //once we have parsed all the vm data, save it to db
					if(newUri!=null) {
						ClLog.i(TAG, "successfully inserted "+newUri+" into vms db");
					}
					continue;
				}
				
				String fieldName = jsonParser.getCurrentName();
				jsonParser.nextToken();  //now on value of this field
				String fieldValue = jsonParser.getText();
				if(jsonParser.isExpectedStartArrayToken()) {
					//parse & save any object/array values as plain json;
					//the app will have the responsibility of re-creating the obj from the json if it is needed
					JsonNode complexDataNode = om.readTree(jsonParser);
					fieldValue = om.writeValueAsString(complexDataNode);
				}

				if(fieldName.equals(Vms.META_DATA.CS_ORIGINAL_GROUP_FIELD_NAME)) {
					//this is an unfortunate special check being done to map any "group" fields from cs to
					//a client-specific "groupa" fieldname (arbitrary name).  This is necessary b/c "group"
					//is an sql keyword so we cannot use it as a field name or it cases sql statements to choke.
					fieldName = Vms.GROUPA;
				}
				
				contentValues.put(fieldName, fieldValue);
			}
			
			jsonParser.close();
			
		} catch (JsonParseException e) {
			ClLog.e(TAG, "got Exception parsing json! [" + e.toString() +"]");
			ClLog.e(TAG, e);
		} catch (IOException e) {
			ClLog.e(TAG, "got IOException! [" + e.toString() +"]");
			ClLog.e(TAG, e);
		}
	}
	

} ///END CsRestService
