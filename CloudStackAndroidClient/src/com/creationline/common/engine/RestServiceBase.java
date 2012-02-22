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
package com.creationline.common.engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.text.format.Time;

import com.creationline.common.utils.ClLog;

/**
 * A collection of common variables and methods that can be used as a base to
 * implement a particular *RestService which connections to a specific REST API backend.
 * In general, though not enforced, http-related functionality tend to be common
 * so are generalized here.  DB-related and specific request/response handling functionality
 * tend to be more backend-specific, so are implemented in the specific subclasses.
 * 
 * @author thsu
 *
 */
public class RestServiceBase extends IntentService {

	public static final class PAYLOAD_FIELDS {
		public static final String ACTION_ID = "com.creationline.common.engine.ACTION_ID";
		public static final String API_CMD = "com.creationline.common.engine.API_CMD";
		public static final String RESPONSE = "com.creationline.common.engine.RESPONSE";
		public static final String UPDATED_URI = "com.creationline.common.engine.UPDATED_URI";
	}
	
	protected static List<Uri> inProgressTransactionList = new ArrayList<Uri>();
	protected static Time time = null;
	protected HttpClient httpclient = null;

	/**
	 * Copied with slight modification from:
	 *   http://www.androidsnippets.com/GET-the-content-from-a-httpresponse-or-any-inputstream-as-a-string
	 *   
	 * @param is InputStream with data to read out
	 * @return all the data in specified InputStream
	 */
	public static StringBuilder inputStreamToString(final InputStream is) throws IOException {
	
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
			throw e;
		}
	    
	    // Return full string
	    return total;
	}

	public RestServiceBase(String name) {
		super(name);
		time = new Time();  //use default timezone
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
		
		super.onDestroy();
	}
	
	public String getCurrentTimeAsString() {
		time.setToNow();
		return time.format3339(false);
		///To change this str back into a Time object, use the following:
		//    Time readTime = new Time();
		//    readTime.parse3339(timeStr);  //str was saved out using RFC3339 format, so needs to be read in as such
		//    readTime.switchTimezone("Asia/Tokyo");  //parse3339() automatically converts read in times to UTC.  We need to change it back to the default timezone of the handset (JST in this example)
	}

	public static StringBuilder getReplyBody(final HttpResponse reply) throws IOException {
		final String TAG = "RestServiceBase.getReplyBody()";
		
		if(reply==null) {
			return null;
		}

		final int statusCode = reply.getStatusLine().getStatusCode();
		final HttpEntity entity = reply.getEntity();
		
		InputStream replyBodyObject = null;
		try {
			replyBodyObject = entity.getContent();
			
		} catch (IllegalStateException e) {
			ClLog.e(TAG, "got IllegalStateException! [" + e.toString() +"]");
			ClLog.e(TAG, e);
		} catch (IOException e) {
			ClLog.e(TAG, "got IOException! [" + e.toString() +"]");
			ClLog.e(TAG, e);
		}
		
		StringBuilder replyBody;
		try {
			//we can use org.apache.http.util.EntityUtils.toString(), here to do the same thing
			//but going with inputStreamToString() (with some quick tests, it seems
			//EntityUtils.toString() itself is somewhat faster than inputStreamToString(),
			//but the overall path of request<->update-UI does not show that much difference
			//in user-response speed; so we will go in favor of not changing working code)
			replyBody = inputStreamToString(replyBodyObject);
		} catch (IOException e) {
			ClLog.e(TAG, "failure occurred trying to parse entity so aborting.  entity="+entity);
			throw e;
		}
		ClLog.d(TAG, "parsed reply: statusCode="+statusCode+"  body="+replyBody);
		return replyBody;
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		//sub-classes should implement
	}

}
