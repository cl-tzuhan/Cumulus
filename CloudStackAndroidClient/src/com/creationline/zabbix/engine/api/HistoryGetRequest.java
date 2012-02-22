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
package com.creationline.zabbix.engine.api;

import java.util.ArrayList;
import java.util.Calendar;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.creationline.common.engine.RestServiceBase;
import com.creationline.common.utils.ClLog;
import com.creationline.zabbix.engine.ZbxRestService;
import com.creationline.zabbix.engine.db.CpuLoads;

public class HistoryGetRequest extends ZbxApiRequest {
	
	public static class FIELDS {
		public static final String NUMHOURS = "com.creationline.zabbix.engine.api.HistoryGetRequest.NUMHOURS";
	}
	
	private String itemid = null;
	private String itemname = null;  //not used in zabbix request itself, but we will display it on the ui
	private int numHours = 0;  //number of hours worth of history to fetch
	private String host = null;  //not used in zabbix request itself, but used to associate data with this host in the db
	private String targetContentUri = null;

	
	public HistoryGetRequest(final String authToken, final String itemid, final String itemName,
											final int numHours, final String host, final String targetContentUri) {
		super();  //parent constructor must be called first
		
		ObjectMapper objMapper = new ObjectMapper();
		
		ArrayNode itemidsNode = objMapper.createArrayNode();
		itemidsNode.add(itemid);
		this.itemid = itemid;
		this.itemname = itemName;  //save for reference later
		this.host = host;  //save for use in db table name
		this.targetContentUri = targetContentUri;  //save for use later
		
		//calc timestamps representing the range of time from which to fetch history data
		this.numHours = numHours;
		Calendar rightNow = Calendar.getInstance();
		final long timeTillTimestamp = convertAndroidTimestampToUnixTimestamp(rightNow.getTimeInMillis());
		rightNow.add(Calendar.HOUR, (0-numHours));  //calc numHours into the past from right now (var name is now a mismatch)
		final long timeFromTimestamp = convertAndroidTimestampToUnixTimestamp(rightNow.getTimeInMillis());

        ObjectNode paramsNode = objMapper.createObjectNode();
        paramsNode.put(ZbxApiConstants.FIELDS.HISTORY, 0);
        paramsNode.put(ZbxApiConstants.FIELDS.OUTPUT, ZbxApiConstants.VALUES.EXTEND);
        paramsNode.put(ZbxApiConstants.FIELDS.ITEMIDS, itemidsNode);
        paramsNode.put(ZbxApiConstants.FIELDS.TIME_FROM, timeFromTimestamp);
        paramsNode.put(ZbxApiConstants.FIELDS.TIME_TILL, timeTillTimestamp);
        paramsNode.put(ZbxApiConstants.FIELDS.SORTFIELD, ZbxApiConstants.VALUES.CLOCK);
        paramsNode.put(ZbxApiConstants.FIELDS.SORTORDER, ZbxApiConstants.VALUES.ASC);
		
		jsonRpcData.put(ZbxApiConstants.FIELDS.PARAMS, paramsNode);
        jsonRpcData.put(ZbxApiConstants.FIELDS.METHOD, ZbxApiConstants.API.HISTORY.GET);
        jsonRpcData.put(ZbxApiConstants.FIELDS.AUTH, authToken);
	}
	
	public static long convertAndroidTimestampToUnixTimestamp(long androidTimestamp) {
		//android timestamps = time since 01/01/1970 in milliseconds
		//unix timetamps = time since 01/01/1970 in seconds
		return androidTimestamp/1000;
	}
	
	public static Bundle createHistoryGetActionPayload(Context context, final String itemid, final String itemName, final int numHours,
															final String host, final String targetContentUri, final Bundle followupAction) {
		Bundle payload = new Bundle();
		payload.putString(RestServiceBase.PAYLOAD_FIELDS.ACTION_ID, ZbxRestService.ACTIONS.CALL_ZBX_API);  //classify as zbx api call
        payload.putString(RestServiceBase.PAYLOAD_FIELDS.API_CMD, ZbxApiConstants.API.HISTORY.GET);  //zbix api to call
        if(itemid!=null) { payload.putString(ZbxApiConstants.FIELDS.ITEMID, itemid); }  //id of item to fetch
        if(itemName!=null) { payload.putString(ZbxApiConstants.FIELDS.KEY_, itemName); }  //name of item to fetch
        payload.putInt(HistoryGetRequest.FIELDS.NUMHOURS, numHours);  //#-of-hours of history data to fetch
        if(host!=null) { payload.putString(ZbxApiConstants.FIELDS.HOST, host); }  //name of target host
        if(targetContentUri!=null) { payload.putString(ZbxRestService.PAYLOAD_FIELDS.TARGET_CONTENT_URI, targetContentUri); }  //uri of db table to save data to
        if(followupAction!=null) { payload.putBundle(ZbxRestService.PAYLOAD_FIELDS.FOLLOWUP_ACTION, followupAction); }  //api call that should be started after this one

        return payload;
	}

	@Override
	public Intent doInvalidAuthTokenProcessing(Context context) {
		Intent zbxLoginIntent = createLoginCallFollowedByHistoryGetCall(context);
		return zbxLoginIntent;
	}

	public Intent createLoginCallFollowedByHistoryGetCall(Context context) {
		//make a login call followed by a history call
		Bundle followupActionPayload = HistoryGetRequest.createHistoryGetActionPayload(context, itemid, itemname, numHours, host, targetContentUri, null);
		Bundle loginActionPayload = UserLoginRequest.createUserLoginActionPayload(context,followupActionPayload);
		Intent zbxLoginFollowedByHistoryGetIntent = ZbxRestService.createZbxRestServiceIntent(context, loginActionPayload);
		return zbxLoginFollowedByHistoryGetIntent;
	}
	
	@Override
	public void doApiSpecificProcessing(Context context, JsonNode rootNode) {
		final String TAG = "HistoryGetRequest.doApiSpecificProcessing()";

		//extract result from json response
		final JsonNode resultNode = rootNode.path("result");
		ClLog.d(TAG, "got history.get result (refrain from logging as it may be huge)");
		
		ArrayList<ContentValues> graphValueArray = new ArrayList<ContentValues>();
		final int numSamples = resultNode.size();  //total number of graph points retrieved from zabbix
		final int NUM_SAMPLES_TO_GRAPH_HINT = 200;  //number of points we actually want to show on the graph (hint because the sampling algorithm does not attempt to match this number exactly, just closely)
		final int SAMPLE_RATE = (numSamples>NUM_SAMPLES_TO_GRAPH_HINT)? numSamples/NUM_SAMPLES_TO_GRAPH_HINT : 1;  //factor of the points we need to throw away
																												   //(we lower-bound the factor at 1 in cases we have less
																												   // number of samples than the hint; otherwise we run
																												   // into an infinite loop below due to the factor being 0)
		double value, clockInSec, clockInMilliSec;  //using shared vars for each for pass as optimization
		try {
			double previousPreviousValue = Double.NaN;  //used by optimization
			double previousValue = Double.NaN;  //used by optimization
			
			for(int i=0; i<numSamples; i+=SAMPLE_RATE) {
				JsonNode node = resultNode.get(i);

//				final double value = Double.valueOf(node.path("value").getTextValue());
//				double clockInSec = Double.valueOf(node.path("clock").getTextValue());  //zabbix timestamp is _seconds_ from epoch
//				double clockInMilliSec = (long)clockInSec*1000L;  //we will use _milliseconds_ from epoch
				value = Double.valueOf(node.path("value").getTextValue());
				clockInSec = Double.valueOf(node.path("clock").getTextValue());  //zabbix timestamp is _seconds_ from epoch
				clockInMilliSec = (long)clockInSec*1000L;  //we will use _milliseconds_ from epoch
				ClLog.v(TAG, "  clockInMilliSec="+clockInMilliSec+"  value="+value);
				
				//collect parsed graph values
				ContentValues graphValue = new ContentValues();
				graphValue.put(CpuLoads.HOST, host);
				graphValue.put(CpuLoads.ITEMNAME, itemname);
				graphValue.put(CpuLoads.CLOCK, clockInMilliSec);
				graphValue.put(CpuLoads.VALUE, value);

				//optimization: throw away data points that map the same value when possible.
				//              If there are already two data points that map a straight
				//              horizontal line, any other data point in between those two
				//              points are unnecessary for plotting the line, so we will just
				//              throw them away (ie. values that are repeated consecutively
				//              3 or more times are truncated to just the beginning and ending
				//              2 values).  The caveat is that as we are saving to db
				//              in batches, if the consecutive repeating values cross a batch
				//              boundry, we ignore it for simplicity-of-processing's sake.
				int indexOfPreviousValue = -1;
				if(value==previousValue && value==previousPreviousValue) {
					indexOfPreviousValue = graphValueArray.size()-1;
				}
				if(indexOfPreviousValue>=0) {
					graphValueArray.set(indexOfPreviousValue, graphValue);  //for repeating, consecutive values >2, throw away the middle value
				} else {
					graphValueArray.add(graphValue);  //for all other cases, just process data point as is
				}
				previousPreviousValue = previousValue;
				previousValue = value;

				//save parsed graph values in batches
				final int PERSIST_BATCH_SIZE = 200;  //currently, this batch size is the same as NUM_SAMPLES_TO_GRAPH_HINT,
													 //so usually there will only be one batch :P
													 //(the size-in-bytes of a 200 item batch is about 12K,
													 // which is deemed an ok size in the interest of lowering
													 // processing time)
				if(graphValueArray.size()>=PERSIST_BATCH_SIZE) {
					//save current batch and clear temp array
					persistArrayValues(context, graphValueArray);
					graphValueArray.clear();
					
					//reset optimization-detection for each batch
					previousPreviousValue = Double.NaN;
					previousValue = Double.NaN;
				}
			}
		} catch(OutOfMemoryError e) {
			ClLog.v(TAG, "Not enough memory to parse all of returned sample data; aborting");
		}
		//save any remaining parsed values that we may have left
		if(!graphValueArray.isEmpty()) {
			persistArrayValues(context, graphValueArray);
		}
		
	}

	public void persistArrayValues(Context context, ArrayList<ContentValues> graphValueArray) {
		//saved collected graph values
		//(using array size of 1 to force toArray() to build an array of type ContentValues for us)
		Uri contentUri = Uri.parse(targetContentUri);
//		int numRowsInserted = context.getContentResolver().bulkInsert(contentUri, graphValueArray.toArray(new ContentValues[1]));
		int numRowsInserted = context.getContentResolver().bulkInsert(contentUri, graphValueArray.toArray(new ContentValues[] {}));
		ClLog.d("HistoryGetRequest.persistArrayValues()", "  numRowsInserted="+numRowsInserted);
		
	}
	
	
	

}
