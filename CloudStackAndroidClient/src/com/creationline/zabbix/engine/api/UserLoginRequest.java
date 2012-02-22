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
import android.content.SharedPreferences;
import android.os.Bundle;

import com.creationline.cloudstack.CloudStackAndroidClient;
import com.creationline.common.engine.RestServiceBase;
import com.creationline.common.utils.ClLog;
import com.creationline.zabbix.engine.ZbxRestService;

public class UserLoginRequest extends ZbxApiRequest {
//	public static class INTENT_ACTION {
//		//NOTE: changing the value of these constants requires you to change any usage of the same string in Android.manifest!!!
//		public static final String BROADCAST_LOGINRESULT = "com.creationline.zabbix.engine.api.UserLoginRequest.BROADCAST_LOGINRESULT";
//	}
//	public static class FIELDS {
//		public static final String BROADCAST_SUCCESS = "com.creationline.zabbix.engine.api.UserLoginRequest.BROADCAST_RESULT";  //boolean param
//	}
	
	
	public UserLoginRequest(final String username, final String password) {
		super();  //parent constructor must be called first
		
		ObjectMapper objMapper = new ObjectMapper();
        ObjectNode paramsNode = objMapper.createObjectNode();
//        paramsNode.put(ZbxApiConstants.FIELDS.USER, "admin");
//        paramsNode.put(ZbxApiConstants.FIELDS.PASSWORD, "cladmin0987");
//        paramsNode.put(ZbxApiConstants.FIELDS.PASSWORD, "cladmin0987s");
        paramsNode.put(ZbxApiConstants.FIELDS.USER, username);
        paramsNode.put(ZbxApiConstants.FIELDS.PASSWORD, password);
        
		jsonRpcData.put(ZbxApiConstants.FIELDS.PARAMS, paramsNode);
        jsonRpcData.put(ZbxApiConstants.FIELDS.METHOD, ZbxApiConstants.API.USER.LOGIN);
        
	}

	public static Bundle createUserLoginActionPayload(Context context, Bundle followupAction) {
		Bundle payload = new Bundle();
		payload.putString(RestServiceBase.PAYLOAD_FIELDS.ACTION_ID, ZbxRestService.ACTIONS.CALL_ZBX_API);  //classify as zbx api call
        payload.putString(RestServiceBase.PAYLOAD_FIELDS.API_CMD, ZbxApiConstants.API.USER.LOGIN);  //zbx api to call
        if(followupAction!=null) { payload.putBundle(ZbxRestService.PAYLOAD_FIELDS.FOLLOWUP_ACTION, followupAction); }  //api call that should be started after this one

        return payload;
	}

	@Override
	public void doApiSpecificProcessing(Context context, JsonNode rootNode) {
		final String TAG = "UserLoginRequest.doApiSpecificProcessing()";

		final JsonNode resultNode = rootNode.path("result");
		final String authToken = resultNode.asText();
		ClLog.d(TAG, "authToken="+authToken);
		
		//save auth token for future use
		SharedPreferences preferences = context.getSharedPreferences(CloudStackAndroidClient.SHARED_PREFERENCES.PREFERENCES_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(CloudStackAndroidClient.SHARED_PREFERENCES.ZABBIX_AUTH_TOKEN, authToken);
		editor.commit();
		ClLog.d(TAG, "saved auth token to preferenecs");
		
		//if requested, broadcast result of login
//		Bundle broadcastLoginSuccessPayload = ZbxRestService.createBroadcastNotifPayload(context,
//				UserLoginRequest.INTENT_ACTION.BROADCAST_LOGINRESULT,
//				ZbxRestService.CALL_STATUS_VALUES.CALL_SUCCESS,
//				null,
//				null);
//		Intent broadcastLoginSuccessIntent = ZbxRestService.createZbxRestServiceIntent(context, broadcastLoginSuccessPayload);
//		context.startService(broadcastLoginSuccessIntent);
		Intent broadcastLoginSuccessIntent = ZbxRestService.createBroadcastIntent(context,
								 ZbxRestService.INTENT_ACTION.ZBXRESTSERVICE_BROADCAST,
								 ZbxRestService.CALL_STATUS_VALUES.CALL_SUCCESS,
								 null,
								 null);
		context.startService(broadcastLoginSuccessIntent);
		
	}

//	@Override
//	public Intent doRequestFailureProcessing(Context context, JsonNode rootNode) {
//		//for user.login call, we broadcast to a login-specific intent filter
//		return createCallFailureBroadcastIntentFor(context, UserLoginRequest.INTENT_ACTION.BROADCAST_LOGINRESULT, rootNode);
//	}

	

}
