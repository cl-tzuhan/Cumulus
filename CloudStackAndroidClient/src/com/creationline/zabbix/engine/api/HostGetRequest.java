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

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.creationline.common.engine.RestServiceBase;
import com.creationline.common.utils.ClLog;
import com.creationline.zabbix.engine.ZbxRestService;
import com.creationline.zabbix.engine.db.CpuLoads;

public class HostGetRequest extends ZbxApiRequest {
	
	private String hostIp = null;
	private String hostName = null;
	private String[] itemNames = null;
	private String targetContentUri = null;  //uri of db table to persist data to

	
	public HostGetRequest(String authToken, final String ip, final String host, final String[] itemNames, final String targetContentUri) {
		super();  //parent constructor must be called first
		
		ObjectMapper objMapper = new ObjectMapper();

		ObjectNode filterNode = objMapper.createObjectNode();
		filterNode.put(ZbxApiConstants.FIELDS.IP, ip);
		this.hostIp = ip;
		filterNode.put(ZbxApiConstants.FIELDS.HOST, host);
		this.hostName = host;
		
        ObjectNode paramsNode = objMapper.createObjectNode();
        paramsNode.put(ZbxApiConstants.FIELDS.OUTPUT, ZbxApiConstants.VALUES.EXTEND);
        paramsNode.put(ZbxApiConstants.FIELDS.FILTER, filterNode);
		
		jsonRpcData.put(ZbxApiConstants.FIELDS.PARAMS, paramsNode);
        jsonRpcData.put(ZbxApiConstants.FIELDS.METHOD, ZbxApiConstants.API.HOST.GET);
        jsonRpcData.put(ZbxApiConstants.FIELDS.AUTH, authToken);

        this.itemNames = itemNames;
        this.targetContentUri = targetContentUri;
	}
	
	public static Bundle createHostGetActionPayload(Context context, final String hostIp, final String hostName,
														final String[] namesOfItemsToGet, final String targetContentUri, Bundle followupAction) {
		Bundle payload = new Bundle();
		payload.putString(RestServiceBase.PAYLOAD_FIELDS.ACTION_ID, ZbxRestService.ACTIONS.CALL_ZBX_API);  //classify as zbx api call
        payload.putString(RestServiceBase.PAYLOAD_FIELDS.API_CMD, ZbxApiConstants.API.HOST.GET);  //zbx api cmd to call 
        if(hostIp!=null) { payload.putString(ZbxApiConstants.FIELDS.IP, hostIp); }  //public IP of target host
        if(hostName!=null) { payload.putString(ZbxApiConstants.FIELDS.HOST, hostName); }  //name of target host
        if(namesOfItemsToGet!=null) { payload.putStringArray(ZbxRestService.PAYLOAD_FIELDS.ITEM_NAME_ARRAY, namesOfItemsToGet); }  //array of items whose data to download
        if(targetContentUri!=null) { payload.putString(ZbxRestService.PAYLOAD_FIELDS.TARGET_CONTENT_URI, targetContentUri); }  //uri of db table to save data retrieved by history.get to
        if(followupAction!=null) { payload.putBundle(ZbxRestService.PAYLOAD_FIELDS.FOLLOWUP_ACTION, followupAction); }  //api call that should be started after this one

        return payload;
	}

	@Override
	public Intent doInvalidAuthTokenProcessing(Context context) {
		Intent zbxLoginIntent = createLoginCallFollowedByHostGetCall(context);
		return zbxLoginIntent;
	}

	public Intent createLoginCallFollowedByHostGetCall(Context context) {
		//make a login call followed by a host.get call
		Bundle followupActionPayload = HostGetRequest.createHostGetActionPayload(context, hostIp, hostName, itemNames, targetContentUri, null);
		Bundle loginActionPayload = UserLoginRequest.createUserLoginActionPayload(context, followupActionPayload);
		Intent zbxLoginFollowedByHostGetIntent = ZbxRestService.createZbxRestServiceIntent(context, loginActionPayload);
		return zbxLoginFollowedByHostGetIntent;
	}

	@Override
	public void doApiSpecificProcessing(Context context, JsonNode rootNode) {
		final String TAG = "HostGetRequest.doApiSpecificProcessing()";

		///debug-use logging; only uncomment if you need to see response of each call
		//ClLog.d("HostGetRequest.doApiSpecificProcessing()", "result="+rootNode.path("result").toString());

		//retrieve the hostid from reply
		final JsonNode hostidNode = rootNode.findPath(ZbxApiConstants.FIELDS.HOSTID);
		final String hostid = hostidNode.getTextValue();
		ClLog.d(TAG, "from result, got hostid="+hostid);
		
		final boolean specifiedHostNotRegisteredOnZbxServer = hostid==null;
		if(specifiedHostNotRegisteredOnZbxServer) {
			//short-circuit the remaining data-download process since there is no data to get
			context.startService(doRequestFailureProcessing(context, "(is this host configured on your Zabbix server?)"));
			return;
		}
		
		//retrieve the host from reply
		final JsonNode hostNode = rootNode.findPath(ZbxApiConstants.FIELDS.HOST);
		final String host = hostNode.getTextValue();
		ClLog.d(TAG, "from result, got host="+host);
		
		//clear db of old data each time
		deleteOldSavedDataForHost(context, host);
		

		//start a call for every item we want to get, finishing with a broadcast that data download is done
		for(int i=0; i<itemNames.length; i++) {
			Bundle notifyChartOfSuccessPayload = null;
			final boolean isLastItem = i==itemNames.length-1;
			if(isLastItem) {
				//make a notify chart call
				notifyChartOfSuccessPayload = ZbxRestService.createBroadcastPayload(context,
																					ZbxRestService.INTENT_ACTION.ZBXRESTSERVICE_BROADCAST,
																					ZbxRestService.CALL_STATUS_VALUES.CALL_SUCCESS,
																					null,
																					null);
			}
			
			//make item.get call and execute
			Intent zbxItemGetIntent = createItemGetCallWithRetreivedHostId(context,
																		   hostid,
																		   itemNames[i],
																		   host,
																		   targetContentUri,
																		   notifyChartOfSuccessPayload);
			context.startService(zbxItemGetIntent);
			
		}
		
	}

	public void deleteOldSavedDataForHost(Context context, final String host) {
		final String whereClause = CpuLoads.HOST+"=?";
		final String[] selectionArgs = new String[] { host };
		Uri contentUri = Uri.parse(targetContentUri);
		final int num = context.getContentResolver().delete(contentUri, whereClause, selectionArgs);
		ClLog.i("HostGetRequest.deleteOldSavedDataForHost()", "clearing "+contentUri+" db of rows for host="+host+"; num of records deleted="+num);
	}

	public static Intent createItemGetCallWithRetreivedHostId(Context context, final String hostid, final String itemname,
																final String host, final String targetContentUri, final Bundle followupCmdPayload) {
		//create next call of the data download chain
		Bundle itemGetActionPayload = ItemGetRequest.createItemGetActionPayload(context, hostid, itemname, host, targetContentUri, followupCmdPayload);
		Intent zbxItemGetIntent = ZbxRestService.createZbxRestServiceIntent(context, itemGetActionPayload);
		return zbxItemGetIntent;
	}
	
	
	

}
