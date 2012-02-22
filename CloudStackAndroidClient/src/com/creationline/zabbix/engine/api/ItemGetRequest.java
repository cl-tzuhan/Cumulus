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
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.creationline.common.engine.RestServiceBase;
import com.creationline.common.utils.ClLog;
import com.creationline.zabbix.engine.ZbxRestService;

public class ItemGetRequest extends ZbxApiRequest {
	
	private String hostid = null;
	private String itemname = null;
	private String host = null;  //not used in zabbix request itself, but we will use it eventually in the db table to associate data with this host
	private String targetContentUri = null;  //not used by item.get, but need to pass it to the next step

	
	public ItemGetRequest(final String authToken, final String hostid, final String itemname, final String host, final String targetContentUri) {
		super();  //parent constructor must be called first
		
		ObjectMapper objMapper = new ObjectMapper();
		
		ArrayNode hostidsNode = objMapper.createArrayNode();
		hostidsNode.add(hostid);
		this.hostid = hostid;
		this.itemname = itemname;
		this.host = host;

		ObjectNode searchNode = objMapper.createObjectNode();
		searchNode.put(ZbxApiConstants.FIELDS.KEY_, itemname);
		
        ObjectNode paramsNode = objMapper.createObjectNode();
        paramsNode.put(ZbxApiConstants.FIELDS.OUTPUT, ZbxApiConstants.VALUES.REFER);
        paramsNode.put(ZbxApiConstants.FIELDS.HOSTIDS, hostidsNode);
        paramsNode.put(ZbxApiConstants.FIELDS.SEARCH, searchNode);
		
		jsonRpcData.put(ZbxApiConstants.FIELDS.PARAMS, paramsNode);
        jsonRpcData.put(ZbxApiConstants.FIELDS.METHOD, ZbxApiConstants.API.ITEM.GET);
        jsonRpcData.put(ZbxApiConstants.FIELDS.AUTH, authToken);
        
        this.targetContentUri = targetContentUri;
	}
	
	public static Bundle createItemGetActionPayload(Context context, final String hostid, final String itemName,
														final String host, final String contentUri, final Bundle followupAction) {
		Bundle payload = new Bundle();
		payload.putString(RestServiceBase.PAYLOAD_FIELDS.ACTION_ID, ZbxRestService.ACTIONS.CALL_ZBX_API);  //classify as zbx api call
        payload.putString(RestServiceBase.PAYLOAD_FIELDS.API_CMD, ZbxApiConstants.API.ITEM.GET);  //zbx api to call
        if(hostid!=null) { payload.putString(ZbxApiConstants.FIELDS.HOSTID, hostid); }  //target host id
        if(itemName!=null) { payload.putString(ZbxApiConstants.FIELDS.KEY_, itemName); }  //name of item to get
        if(host!=null) { payload.putString(ZbxApiConstants.FIELDS.HOST, host); }  //target host name
        if(contentUri!=null) { payload.putString(ZbxRestService.PAYLOAD_FIELDS.TARGET_CONTENT_URI, contentUri); }  //uri of db table to save data retrieved by history.get to
        if(followupAction!=null) { payload.putBundle(ZbxRestService.PAYLOAD_FIELDS.FOLLOWUP_ACTION, followupAction); }  //api call that should be started after this one

        return payload;
	}

	@Override
	public Intent doInvalidAuthTokenProcessing(Context context) {
		Intent zbxLoginIntent = createLoginCallFollowedByItemGetCall(context);
//		context.startService(zbxLoginIntent);  //initiate login->item.get call
		return zbxLoginIntent;
	}

	public Intent createLoginCallFollowedByItemGetCall(Context context) {
		//make a login call followed by a item.get call
		Bundle followupActionPayload = ItemGetRequest.createItemGetActionPayload(context, hostid, itemname, host, targetContentUri, null);
		Bundle loginActionPayload = UserLoginRequest.createUserLoginActionPayload(context,followupActionPayload);
		Intent zbxLoginFollowedByItemGetIntent = ZbxRestService.createZbxRestServiceIntent(context, loginActionPayload);
		return zbxLoginFollowedByItemGetIntent;
	}
	
	@Override
	public void doApiSpecificProcessing(Context context, JsonNode rootNode) {

		///debug-use logging; only uncomment if you need to see response of each call
		//ClLog.d("ItemGetRequest.doApiSpecificProcessing()", "result="+rootNode.path("result").toString());

		//initiate next call of getCpuLoadData chain
		Intent zbxHistoryGetIntent = createHistoryGetCallWithRetreivedItemId(context, rootNode);
		context.startService(zbxHistoryGetIntent);
	}
	
	public Intent createHistoryGetCallWithRetreivedItemId(Context context, JsonNode rootNode) {
		//retrieve the itemid from reply
		final JsonNode itemidNode = rootNode.findPath(ZbxApiConstants.FIELDS.ITEMID);
		final String itemid = itemidNode.getTextValue();
		ClLog.d("HostGetRequest.createHistoryGetCallWithItemId()", "from result, got itemid="+itemid);

		//create next call of getData chain
		final int NUM_HOURS_OF_DATA_TO_GET = 6;
		Bundle historyGetActionPayload = HistoryGetRequest.createHistoryGetActionPayload(context, itemid, itemname, NUM_HOURS_OF_DATA_TO_GET, host, targetContentUri, null);
		Intent zbxHistoryGetIntent = ZbxRestService.createZbxRestServiceIntent(context, historyGetActionPayload);
		return zbxHistoryGetIntent;
	}
	
	

}
