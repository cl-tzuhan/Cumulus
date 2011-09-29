package com.creationline.cloudstack.engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;

public class CsRestService extends IntentService {
	
	public static final String MSG_ID = "com.creationline.engine.CALL_ID";
	public static final String API_URL = "com.creationline.engine.API_URL";
//	public static final String PAYLOAD = "com.creationline.engine.PAYLOAD";
	public static final String RESPONSE = "com.creationline.engine.RESPONSE";
	
	public static final String TEST_CALL = "com.creationline.cloudstack.engine.TEST_CALL";

	public CsRestService() {
		super("CsRestService");
	}
	
	public static Intent createCsRestServiceIntent(Context context, String action, String url) {
		
		Bundle payload = new Bundle();
        payload.putString(CsRestService.MSG_ID, action); //action always stored under MSG_ID key
        payload.putString(CsRestService.API_URL, url);   //url always stored under API_URL key
        
        Intent startCsRestServiceIntent = new Intent(context, CsRestService.class);
        startCsRestServiceIntent.putExtras(payload);
        
        return startCsRestServiceIntent;
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		//This is already running on a non-UI thread
		
		Bundle payload = intent.getExtras(); 
		String apiUrl = payload.getString(CsRestService.API_URL);
		System.out.println("CsRestService.onHandleIntent: will send API ["+apiUrl+"] call to server here");
		
		//doRestCall(apiUrl);
		callUserApi(null);
		
		Intent broadcastIntent = new Intent(payload.getString(CsRestService.MSG_ID));
		broadcastIntent.putExtra(CsRestService.RESPONSE, apiUrl+"Response");
		sendBroadcast(broadcastIntent);

	}
	
	/**
	 * Adapted from:
	 *   http://www.devdaily.com/java/jwarehouse/commons-httpclient-4.0.3/httpclient/src/examples/org/apache/http/examples/client/ClientWithResponseHandler.java.shtml
	 *   
	 * @param url url to send an http GET to
	 */
	private void doRestCall(final String url) {
		
        HttpClient httpclient = new DefaultHttpClient();
        try {
            final HttpGet httpget = new HttpGet(url);

            System.out.println("CsRestService.doRestCall(): executing request " + httpget.getURI());

            // Create a response handler
            HttpResponse response =  httpclient.execute(httpget);
            final int statusCode = response.getStatusLine().getStatusCode();
            final HttpEntity entity = response.getEntity();
            final InputStream responseBody = entity.getContent();

            if (statusCode != 200) {
                // responseBody will have the error response
            }
            
            
            final StringBuilder responseText = inputStreamToString(responseBody);
            
            System.out.println("----------------------------------------");
            System.out.println(responseText);
            System.out.println("----------------------------------------");

        } catch (ClientProtocolException e) {
        	System.out.println("CsRestService.doRestCall(): got ClientProtocolException! [" + e.toString() +"]");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("CsRestService.doRestCall(): got IOException! [" + e.toString() +"]");
			e.printStackTrace();
		} finally {
            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
            httpclient.getConnectionManager().shutdown();
        }
	}
	
	/**
	 * Adapted from:
	 *   http://download.cloud.com/releases/2.2.0/api/user/2.2api_examplecode_details.html
	 *   
	 * @param url 
	 */
	private void callUserApi(final String url) {
		// Host
		String host = "http://192.168.3.11:8080/client/api";
		
		// Command and Parameters
		String apiUrl = "command=listVirtualMachines&account=thsu-account&domainid=2&response=json";

		// ApiKey and secretKey as given by your CloudStack vendor
		String apiKey = "namomNgZ8Qt5DuNFUWf3qpGlQmB4650tY36wFOrhUtrzK13d66qNpttKw52Brj02dbtIHs01y-lCLz1UOzTxVQ";
		String secretKey = "Yt_9ZEIDGlmRIg63MiMatAri-1aRoo4l-82mnbYdR3d8JdG7jvXqrrB5TpmbLZB_8zK_j95VRSQWZwnu0153eQ";
		
		/// These are the things that need to be done next!!!
		///
		/// TODO: need to replace the above host/apiUrl with the passed-in url instead.
		/// TODO: need to find a way to have the user specify her apiKey/secretKey
		/// TODO: find a way to parse the json as data instead of just output as text
		///

		try {
			if (apiUrl == null || apiKey == null || secretKey == null) {
				return;
			}

			System.out.println("CsRestService.callUserApi(): Constructing API call to host = '" + host + "' with API command = '" + apiUrl + "' using apiKey = '" + apiKey + "' and secretKey = '" + secretKey + "'");
			
			// Step 1: Make sure your APIKey is toLowerCased and URL encoded
			String encodedApiKey = URLEncoder.encode(apiKey.toLowerCase(), "UTF-8"); //NOTE: URLEncoder will convert spaces to "+" instead of "%20" like CS prefers
			
			// Step 2: toLowerCase all the parameters, URL encode each parameter value, and the sort the parameters in alphabetical order
			// Please note that if any parameters with a '&' as a value will cause this test client to fail since we are using '&' to delimit 
			// the string
			List<String> sortedParams = new ArrayList<String>();
			sortedParams.add("apikey="+encodedApiKey);
			StringTokenizer st = new StringTokenizer(apiUrl, "&");
			while (st.hasMoreTokens()) {
				String paramValue = st.nextToken().toLowerCase();
				String param = paramValue.substring(0, paramValue.indexOf("="));
				String value = URLEncoder.encode(paramValue.substring(paramValue.indexOf("=")+1, paramValue.length()), "UTF-8");   //NOTE: URLEncoder will convert spaces to "+" instead of "%20" like CS prefers
				sortedParams.add(param + "=" + value);
			}
			Collections.sort(sortedParams);
			System.out.println("CsRestService.callUserApi(): Sorted Parameters: " + sortedParams);
			
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
			System.out.println("CsRestService.callUserApi(): sorted URL : " + sortedUrl);
			final String encodedSignature = signRequest(sortedUrl, secretKey);
			
			// Step 4: Construct the final URL we want to send to the CloudStack Management Server
			// Final result should look like:
			// http(s)://://client/api?&apiKey=&signature=
			final String finalUrl = host + "?" + apiUrl + "&apiKey=" + apiKey + "&signature=" + encodedSignature;
			System.out.println("CsRestService.callUserApi(): final URL: " + finalUrl);
			
			// Step 5: Perform a HTTP GET on this URL to execute the command
			doRestCall(finalUrl);
			
		} catch (Throwable t) {
			System.out.println(t);
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
		try {
			Mac mac = Mac.getInstance("HmacSHA1");
			SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), "HmacSHA1");	
			mac.init(keySpec);
			mac.update(request.getBytes());
			final byte[] encryptedBytes = mac.doFinal();
			 //NOTE: URLEncoder will convert spaces to "+" instead of "%20" like CS prefers
			return URLEncoder.encode(Base64.encodeToString(encryptedBytes, Base64.NO_WRAP), "UTF-8"); //use NO_WRAP so no extraneous CR/LFs are added onto the end of the base64-ed string
		} catch (Exception ex) {
			System.out.println(ex);
		}
		return null;
	}
	
	/**
	 * Copied with slight modification from:
	 *   http://www.androidsnippets.com/get-the-content-from-a-httpresponse-or-any-inputstream-as-a-string
	 *   
	 * @param is InputStream with data to read out
	 * @return all the data in specified InputStream
	 */
	private StringBuilder inputStreamToString(final InputStream is) {
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
	    // Return full string
	    return total;
	}
	
	

}
