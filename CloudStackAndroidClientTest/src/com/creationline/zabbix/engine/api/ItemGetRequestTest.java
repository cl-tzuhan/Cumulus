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
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import android.content.Intent;
import android.os.Bundle;
import android.test.AndroidTestCase;

import com.creationline.common.engine.RestServiceBase;
import com.creationline.zabbix.engine.ZbxRestService;

public class ItemGetRequestTest extends AndroidTestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testItemGetRequest() {
		final String sampleAuthToken = "some sample auth token";
		final String sampleHostid = "some sample host ID";
		final String sampleItemname = "some sample item name";
		final String sampleHostip = "some sample host IP";
		final String sampleTargetContentUri = "some sample target content URI";

		ItemGetRequest sampleItemGetRequest = new ItemGetRequest(sampleAuthToken, sampleHostid, sampleItemname, sampleHostip, sampleTargetContentUri);
		
		ObjectNode retrievedNode = sampleItemGetRequest.getPostData();
		{ //test base data
			JsonNode jsonrpcNode = retrievedNode.findPath(ZbxApiConstants.FIELDS.JSONRPC);
			assertEquals(ZbxApiConstants.VALUES.VER20, jsonrpcNode.getTextValue());
			JsonNode methodNode = retrievedNode.findPath(ZbxApiConstants.FIELDS.METHOD);
			assertEquals(ZbxApiConstants.API.ITEM.GET, methodNode.getTextValue());
			JsonNode authNode = retrievedNode.findPath(ZbxApiConstants.FIELDS.AUTH);
			assertEquals(sampleAuthToken, authNode.getTextValue());
		}
		
		JsonNode paramsNode = retrievedNode.findPath(ZbxApiConstants.FIELDS.PARAMS);
		{ //test params object
			JsonNode outputNode = paramsNode.findPath(ZbxApiConstants.FIELDS.OUTPUT);
			assertEquals(ZbxApiConstants.VALUES.REFER, outputNode.getTextValue());
		}

		JsonNode hostidsArray = retrievedNode.findPath(ZbxApiConstants.FIELDS.HOSTIDS);
		{ //test hostids object
			JsonNode hostidNode = hostidsArray.get(0);
			assertEquals(sampleHostid, hostidNode.getTextValue());
		}

		JsonNode searchNode = retrievedNode.findPath(ZbxApiConstants.FIELDS.SEARCH);
		{ //test search object
			JsonNode keyNode = searchNode.findPath(ZbxApiConstants.FIELDS.KEY_);
			assertEquals(sampleItemname, keyNode.getTextValue());
		}
		
	}
	
	public void testCreateItemGetActionPayload() {
		final String sampleHostid = "some sample host ID";
		final String sampleFollowupKey1 = "followup key 1";
		final String sampleFollowupData1 = "followup data 1";
		final String sampleFollowupKey2 = "followup key 2";
		final String sampleFollowupData2 = "followup data 2";
		final String sampleItemname = "some sample item name";
		final String sampleHostName = "some sample host name";
		final String sampleTargetContentUri = "some sample target content URI";

		
		Bundle sampleFollowupBundle = new Bundle();  //making nonsense bundle just for test
		sampleFollowupBundle.putString(sampleFollowupKey1, sampleFollowupData1);
		sampleFollowupBundle.putString(sampleFollowupKey2, sampleFollowupData2);
		
		Bundle itemGetPayload = ItemGetRequest.createItemGetActionPayload(getContext(), sampleHostid, sampleItemname, sampleHostName, sampleTargetContentUri, sampleFollowupBundle);
		
		final String retrievedApiCmd = itemGetPayload.getString(RestServiceBase.PAYLOAD_FIELDS.API_CMD);
		assertEquals(ZbxApiConstants.API.ITEM.GET, retrievedApiCmd);
		
		final String retrievedHostid = itemGetPayload.getString(ZbxApiConstants.FIELDS.HOSTID);
		assertEquals(sampleHostid, retrievedHostid);

		final String retrievedItemname = itemGetPayload.getString(ZbxApiConstants.FIELDS.KEY_);
		assertEquals(sampleItemname, retrievedItemname);

		final String retrievedHostName = itemGetPayload.getString(ZbxApiConstants.FIELDS.HOST);
		assertEquals(sampleHostName, retrievedHostName);

		final String retrievedTargetContentUri = itemGetPayload.getString(ZbxRestService.PAYLOAD_FIELDS.TARGET_CONTENT_URI);
		assertEquals(sampleTargetContentUri, retrievedTargetContentUri);
		
		Bundle retrievedFollowupBundle = itemGetPayload.getBundle(ZbxRestService.PAYLOAD_FIELDS.FOLLOWUP_ACTION);
		final String retrievedData1 = retrievedFollowupBundle.getString(sampleFollowupKey1);
		final String retrievedData2 = retrievedFollowupBundle.getString(sampleFollowupKey2);
		assertEquals(sampleFollowupData1, retrievedData1);
		assertEquals(sampleFollowupData2, retrievedData2);
		
	}

	public void testcreateLoginCallFollowedByItemGetCall() {
		final String sampleAuthToken = "some sample auth token";
		final String sampleHostid = "some sample host ID";
		final String sampleItemname = "some sample item name";
		final String sampleHostName = "some sample host name";
		final String sampleTargetContentUri = "some sample target content URI";


		ItemGetRequest sampleItemGetRequest = new ItemGetRequest(sampleAuthToken, sampleHostid, sampleItemname, sampleHostName, sampleTargetContentUri);
		Intent testIntent = sampleItemGetRequest.createLoginCallFollowedByItemGetCall(getContext());
		
		Bundle loginBundle = testIntent.getExtras();
		final String retrievedLoginApiCmd = loginBundle.getString(RestServiceBase.PAYLOAD_FIELDS.API_CMD);
		assertEquals(ZbxApiConstants.API.USER.LOGIN, retrievedLoginApiCmd);
		
		Bundle followupBundle = loginBundle.getBundle(ZbxRestService.PAYLOAD_FIELDS.FOLLOWUP_ACTION);
		final String retrievedFollowupApiCmd = followupBundle.getString(RestServiceBase.PAYLOAD_FIELDS.API_CMD);
		assertEquals(ZbxApiConstants.API.ITEM.GET, retrievedFollowupApiCmd);
		final String retrievedFollowupHostid = followupBundle.getString(ZbxApiConstants.FIELDS.HOSTID);
		assertEquals(sampleHostid, retrievedFollowupHostid);
		final String retrievedFollowupItemname = followupBundle.getString(ZbxApiConstants.FIELDS.KEY_);
		assertEquals(sampleItemname, retrievedFollowupItemname);
		final String retrievedFollowupHostName = followupBundle.getString(ZbxApiConstants.FIELDS.HOST);
		assertEquals(sampleHostName, retrievedFollowupHostName);
		final String retrievedFollowupTargetContentUri = followupBundle.getString(ZbxRestService.PAYLOAD_FIELDS.TARGET_CONTENT_URI);
		assertEquals(sampleTargetContentUri, retrievedFollowupTargetContentUri);
		
	}
	
	public void testCreateHistoryGetCallWithItemId() {
		final String replyForItemGet = "{\"jsonrpc\":\"2.0\",\"result\":[{\"hosts\":[{\"hostid\":\"10047\"}],\"itemid\":\"22194\",\"hostid\":\"10047\"}],\"id\":2}";
		testCreateHistoryGetCallWithItemId_helper(replyForItemGet);
	}
	
	public void testCreateHistoryGetCallWithItemId_helper(String jsonData) {
		//parse the original sample json data into a tree so we can use it in the call below
	    ObjectMapper om = new ObjectMapper();
	    JsonNode rootNode = null;
		try {
			rootNode = om.readTree(jsonData);
		} catch (JsonProcessingException e) {
			fail("om.readTree() processing failed!");
			e.printStackTrace();
		} catch (IOException e) {
			fail("om.readTree() failed!");
			e.printStackTrace();
		}
		
		final String sampleAuthToken = "some sample auth token";
		final String sampleHostid = "some sample host ID";
		final String sampleItemname = "some sample item name";
		final String sampleHostName = "some sample host name";
		final String sampleTargetContentUri = "some sample target content URI";
		ItemGetRequest sampleItemGetRequest = new ItemGetRequest(sampleAuthToken, sampleHostid, sampleItemname, sampleHostName, sampleTargetContentUri);
		
		Intent testIntent = sampleItemGetRequest.createHistoryGetCallWithRetreivedItemId(getContext(), rootNode);
		Bundle testPayload = testIntent.getExtras();
		String retrievedApiCmd = testPayload.getString(RestServiceBase.PAYLOAD_FIELDS.API_CMD);
		assertEquals(ZbxApiConstants.API.HISTORY.GET, retrievedApiCmd);
		String retrievedItemId = testPayload.getString(ZbxApiConstants.FIELDS.ITEMID);
		assertEquals(rootNode.findPath(ZbxApiConstants.FIELDS.ITEMID).getTextValue(), retrievedItemId);
		final String retrievedFollowupItemname = testPayload.getString(ZbxApiConstants.FIELDS.KEY_);
		assertEquals(sampleItemname, retrievedFollowupItemname);
		final String retrievedFollowupHostName = testPayload.getString(ZbxApiConstants.FIELDS.HOST);
		assertEquals(sampleHostName, retrievedFollowupHostName);
		final String retrievedFollowupTargetContentUri = testPayload.getString(ZbxRestService.PAYLOAD_FIELDS.TARGET_CONTENT_URI);
		assertEquals(sampleTargetContentUri, retrievedFollowupTargetContentUri);
	}
	
}
