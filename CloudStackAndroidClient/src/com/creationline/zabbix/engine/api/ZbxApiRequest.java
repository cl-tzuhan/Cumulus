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

import java.io.IOException;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.creationline.common.engine.RestServiceBase;
import com.creationline.common.utils.ClLog;
import com.creationline.zabbix.engine.ZbxRestService;

public class ZbxApiRequest {
	
	static ObjectMapper objMapper = new ObjectMapper();
	ObjectNode jsonRpcData = null;
	
	Intent followupRequest = null;  //if non-null, is the request that should be executed immediately upon completion of this request

	public ZbxApiRequest() {
		jsonRpcData = objMapper.createObjectNode();
		jsonRpcData.put(ZbxApiConstants.FIELDS.JSONRPC, ZbxApiConstants.VALUES.VER20);
	}
	
	public ObjectNode getPostData() {
		return jsonRpcData;
	}
	
	/**
	 * Whatever intent is set as the followup request will be executed via startService() after the completion of this request.
	 * 
	 * @param startServiceIntent Intent to start ZbxRestService with
	 */
	public void setFollowUpRequest(Intent startServiceIntent) {
		followupRequest = startServiceIntent;
	}

	/**
	 * Assumes the passed in uri has an ending rowId, which is parsed and
	 * used as the "id" field of this Zabbix json rpc.
	 * 
	 * NOTE:
	 * We are using a String as the "id" here.  The Zabbix API reference doesn't
	 * state specifically, but uses an int here.  An actual Zabbix server seems
	 * to just return whatever value is provided here, regardless of type.
	 * Going with String for now since it is the least amount of conversion
	 * necessary.
	 * 
	 * @param uri uri of transaction table row (with row _id) where this rpc is saved
	 * @return json request data with newly added "id" value
	 */
	public ObjectNode setTransactionRowAsId(Uri uri) {
		jsonRpcData.put("id", uri.getLastPathSegment());
		return jsonRpcData;
	}
	
	public static JsonNode parseReplyTextAsJson(String replyBodyText) {
		final String TAG = "ZbxApiRequest.parseReplyTextAsJson()";

		JsonNode rootNode = null;
		try {
			ObjectMapper om = new ObjectMapper();
			rootNode = om.readTree(replyBodyText);
			
		} catch (JsonParseException e) {
			ClLog.e(TAG, "got Exception parsing json! [" + e.toString() +"]");
			ClLog.e(TAG, e);
		} catch (IOException e) {
			ClLog.e(TAG, "got IOException! [" + e.toString() +"]");
			ClLog.e(TAG, e);
		}
		
		return rootNode;
	}
	
	public void handleResponseFromServer(Context context, String replyBody) throws IOException {
		String TAG = "ZbxApiRequest.handleResponseFromServer()";
		if(replyBody==null) {
			return;
		}
		JsonNode rootNode = parseReplyTextAsJson(replyBody);
		if(rootNode==null) {
			return;
		}

		//find out if general auth-related error occurred
		Intent shortCircuitIntent = null;
		if(authTokenIsInvalid(rootNode)) {
			ClLog.i(TAG, "auth token has expired; making login call to server, followed by call to request we just failed auth on");
			shortCircuitIntent = doInvalidAuthTokenProcessing(context);  //will spawn a login call
		} else if(requestFailedWithInvalidParams(rootNode)) {
			ClLog.i(TAG, "login credentials incorrect; short-circuiting any follow-up actions");
			shortCircuitIntent = doRequestFailureProcessing(context, rootNode);  //will spawn a broadcast
		}
		if(shortCircuitIntent!=null) {
			context.startService(shortCircuitIntent);
			return;  //we skip any api-specific processing if we have an auth-related error
		}
		
		doApiSpecificProcessing(context, rootNode);
		initiateFollowupCall(context, followupRequest);
	}
	
	public static Bundle createActionPayload(Context context, String apiCmd, Bundle followupAction) {
		Bundle payload = new Bundle();
        if(apiCmd!=null) { payload.putString(RestServiceBase.PAYLOAD_FIELDS.API_CMD, apiCmd); }
        if(followupAction!=null) { payload.putBundle(ZbxRestService.PAYLOAD_FIELDS.FOLLOWUP_ACTION, followupAction); }  //api call that should be started after this one

        return payload;
	}
	
	public Intent doRequestFailureProcessing(Context context, JsonNode rootNodeWithError) {
		String data = null;
		if(rootNodeWithError!=null) {
			final JsonNode dataNode = rootNodeWithError.findPath(ZbxApiConstants.FIELDS.DATA);
			data = dataNode.getTextValue();  //in case of user.login call failure, data holds the error message from zabbix
			if(data!=null) {
				ClLog.d("ZbxApiRequest.doRequestFailureProcessing()", "reply body: data="+data);
			}
		}
		return createBroadcastIntentFor(context, ZbxRestService.INTENT_ACTION.ZBXRESTSERVICE_BROADCAST, ZbxRestService.CALL_STATUS_VALUES.CALL_FAILURE, data);
	}

	public Intent doRequestFailureProcessing(Context context, final String data) {
		
		return createBroadcastIntentFor(context, ZbxRestService.INTENT_ACTION.ZBXRESTSERVICE_BROADCAST, ZbxRestService.CALL_STATUS_VALUES.CALL_FAILURE, data);
	}

	/**
	 * Replaces any existing followup action with a broadcast notification that the request failed
	 * (we need to short-circuit any followup here or we might get into an infinite loop calling the
	 *  followup while failing, say, login, indefinitely).  Any specific zabbix response error can
	 * be included as rootNode, which will be parsed for the "data" field and contents included
	 * as the specific error.
	 * 
	 * @param context context in which to create broadcast
	 * @param intentFilter intent filter to send broadacst to
	 * @param rootNodeWithError json node with zabbix error
	 * @return Intent that broadcasts a call failure to the specified intent filter
	 */
	public Intent createBroadcastIntentFor(Context context, final String intentFilter, final int callStatus, final String data) {
		//replace any existing followup action we have with a broadcast notification that the request failed 
		//(we need to short-circuit any followup here or we might get into an infinite loop calling the followup while failing login indefinitely)
		followupRequest = null;
		return ZbxRestService.createBroadcastIntent(context, intentFilter, callStatus, data, null);
	}

	public Intent doInvalidAuthTokenProcessing(Context context) {
		//subclasses should override this method to implement specific processing for an invalid auth token
		return null;
	}

	public void doApiSpecificProcessing(Context context, JsonNode rootNode) {
		;  //subclasses should override this method to implement specific processing for handling the successful result of a particular api call
	}
	
	public void initiateFollowupCall(Context context, Intent followupRequest) {
		if(followupRequest!=null) {
			context.startService(followupRequest);  //initiate any request that is specified as following this one
		}
	}

	/**
	 * Looks in response node for pre-canned message+data value indicating that the supplied auth token in no longer valid.
	 * 
	 * @param replyBodyNode JsonNode of reply body from server
	 * @return true if the server indicated that the provided auth token is no longer valid; false otherwise
	 */
	public static boolean authTokenIsInvalid(JsonNode rootNode) {
		final boolean noValidAuthToken = invalidParamErrorDataEquals(rootNode, ZbxApiConstants.ERROR.DATA.NOTAUTHORIZED);
		return noValidAuthToken;
	}

	/**
	 * Parses replyBodyNode for "invalid params." error, and returns whether the specific error data
	 * matches the specified string.
	 * 
	 * @param replyBodyNode json node with zabbix error
	 * @param errorData specific error string to match
	 * @return true if error data matches; false otherwise
	 */
	public static boolean invalidParamErrorDataEquals(JsonNode replyBodyNode, final String errorData) {
		final String TAG = "ZbxApiRequest.invalidParamErrorDataEquals()";
		
		if(replyBodyNode==null || errorData==null) {
			return false;
		}
		
		final JsonNode dataNode = replyBodyNode.findPath(ZbxApiConstants.FIELDS.DATA);
		final String data = dataNode.getTextValue();
		if(data!=null) { ClLog.d(TAG, "reply body: data="+data); }
		
		final boolean errorDataMatches = requestFailedWithInvalidParams(replyBodyNode) && errorData.equalsIgnoreCase(data);
		return errorDataMatches;
	}
	
	/**
	 * Parses replyBodyNode for "invalid params." error.
	 * 
	 * @param replyBodyNode json node with zabbix error
	 * @return true if reply is invalid params error; false otherwise
	 */
	public static boolean requestFailedWithInvalidParams(JsonNode replyBodyNode) {
		final String TAG = "ZbxApiRequest.requestFailedWithInvalidParams()";
		
		if(replyBodyNode==null) {
			return false;
		}
	
		final JsonNode messageNode = replyBodyNode.findPath(ZbxApiConstants.FIELDS.MESSAGE);
		final String message = messageNode.getTextValue();
		if(message!=null) {
			ClLog.d(TAG, "reply body: message="+message);
		}
		
		final boolean errorDataMatches = ZbxApiConstants.ERROR.MESSAGE.INVALIDPARAMS.equalsIgnoreCase(message);
		return errorDataMatches;
	}

}
