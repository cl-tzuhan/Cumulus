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

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.test.AndroidTestCase;

import com.creationline.common.engine.RestServiceBase;
import com.creationline.zabbix.engine.ZbxRestContentProvider;
import com.creationline.zabbix.engine.ZbxRestService;
import com.creationline.zabbix.engine.db.CpuLoads;

public class HostGetRequestTest extends AndroidTestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testHostGetRequest() {
		final String sampleAuthToken = "some sample AUTH TOKEN";
		final String sampleIp = "some sample IP.ADD.RESS";
		final String sampleHost = "some sample HOST";
		final String[] sampleItemNames = new String[] { "some sample ITEM1", "some sample ITEM2", "some sample ITEM3", "some sample ITEM4", };
		final String sampleTargetContentUri = "some sample TARGET CONTENT URI";

		HostGetRequest sampleHostGetRequest = new HostGetRequest(sampleAuthToken, sampleIp, sampleHost, sampleItemNames, sampleTargetContentUri);
		
		ObjectNode retrievedNode = sampleHostGetRequest.getPostData();
		{ //test base data
			JsonNode jsonrpcNode = retrievedNode.findPath(ZbxApiConstants.FIELDS.JSONRPC);
			assertEquals(ZbxApiConstants.VALUES.VER20, jsonrpcNode.getTextValue());
			JsonNode methodNode = retrievedNode.findPath(ZbxApiConstants.FIELDS.METHOD);
			assertEquals(ZbxApiConstants.API.HOST.GET, methodNode.getTextValue());
			JsonNode authNode = retrievedNode.findPath(ZbxApiConstants.FIELDS.AUTH);
			assertEquals(sampleAuthToken, authNode.getTextValue());
		}
		
		JsonNode paramsNode = retrievedNode.findPath(ZbxApiConstants.FIELDS.PARAMS);
		{ //test params object
			JsonNode outputNode = paramsNode.findPath(ZbxApiConstants.FIELDS.OUTPUT);
			assertEquals(ZbxApiConstants.VALUES.EXTEND, outputNode.getTextValue());
		}

		JsonNode filterNode = retrievedNode.findPath(ZbxApiConstants.FIELDS.FILTER);
		{ //test filter object
			JsonNode ipNode = filterNode.findPath(ZbxApiConstants.FIELDS.IP);
			assertEquals(sampleIp, ipNode.getTextValue());
		}
		
	}
	
	public void testCreateHostGetActionPayload() {
		final String sampleIp = "some sample IP.ADDRESS.001";
		final String sampleHost = "some sample HOST";
		final String[] sampleItemNames = new String[] { "some sample ITEM1", "some sample ITEM2", "some sample ITEM3", "some sample ITEM4", };
		final String sampleTargetContentUri = "some sample TARGET CONTENT URI";
		final String sampleFollowupKey1 = "followup key 1";
		final String sampleFollowupData1 = "followup data 1";
		final String sampleFollowupKey2 = "followup key 2";
		final String sampleFollowupData2 = "followup data 2";
		
		Bundle sampleFollowupBundle = new Bundle();  //making nonsense bundle just for test
		sampleFollowupBundle.putString(sampleFollowupKey1, sampleFollowupData1);
		sampleFollowupBundle.putString(sampleFollowupKey2, sampleFollowupData2);
		
		Bundle hostGetPayload = HostGetRequest.createHostGetActionPayload(getContext(), sampleIp, sampleHost, sampleItemNames, sampleTargetContentUri, sampleFollowupBundle);
		
		final String retrievedActionId = hostGetPayload.getString(RestServiceBase.PAYLOAD_FIELDS.ACTION_ID);
		assertEquals(ZbxRestService.ACTIONS.CALL_ZBX_API, retrievedActionId);

		final String retrievedApiCmd = hostGetPayload.getString(RestServiceBase.PAYLOAD_FIELDS.API_CMD);
		assertEquals(ZbxApiConstants.API.HOST.GET, retrievedApiCmd);
		
		final String retrievedIp = hostGetPayload.getString(ZbxApiConstants.FIELDS.IP);
		assertEquals(sampleIp, retrievedIp);

		final String retrievedHost = hostGetPayload.getString(ZbxApiConstants.FIELDS.HOST);
		assertEquals(sampleHost, retrievedHost);
		
		final String[] retrievedItemNameArray = hostGetPayload.getStringArray(ZbxRestService.PAYLOAD_FIELDS.ITEM_NAME_ARRAY);
		for(int i=0; i<sampleItemNames.length; i++) {
			assertEquals(sampleItemNames[i], retrievedItemNameArray[i]);
		}
		
		final String retrievedTargetContentUri = hostGetPayload.getString(ZbxRestService.PAYLOAD_FIELDS.TARGET_CONTENT_URI);
		assertEquals(sampleTargetContentUri, retrievedTargetContentUri);
		
		Bundle retrievedFollowupBundle = hostGetPayload.getBundle(ZbxRestService.PAYLOAD_FIELDS.FOLLOWUP_ACTION);
		final String retrievedData1 = retrievedFollowupBundle.getString(sampleFollowupKey1);
		final String retrievedData2 = retrievedFollowupBundle.getString(sampleFollowupKey2);
		assertEquals(sampleFollowupData1, retrievedData1);
		assertEquals(sampleFollowupData2, retrievedData2);
		
	}

	public void testcreateLoginCallFollowedByHostGetCall() {
		final String sampleAuthToken = "some sample auth token";
		final String sampleIp = "some sample IP.ADDRESS.002";
		final String sampleHost = "some sample HOST";
		final String[] sampleItemNames = new String[] { "some sample ITEM1", "some sample ITEM2", "some sample ITEM3", "some sample ITEM4", };
		final String sampleTargetContentUri = "some sample TARGET CONTENT URI";

		HostGetRequest sampleHostGetRequest = new HostGetRequest(sampleAuthToken, sampleIp, sampleHost, sampleItemNames, sampleTargetContentUri);
		Intent testIntent = sampleHostGetRequest.createLoginCallFollowedByHostGetCall(getContext());
		
		Bundle loginBundle = testIntent.getExtras();
		final String action = loginBundle.getString(RestServiceBase.PAYLOAD_FIELDS.ACTION_ID);
		assertEquals(ZbxRestService.ACTIONS.CALL_ZBX_API, action);
		final String retrievedLoginCmd = loginBundle.getString(RestServiceBase.PAYLOAD_FIELDS.API_CMD);
		assertEquals(ZbxApiConstants.API.USER.LOGIN, retrievedLoginCmd);
		
		Bundle followupBundle = loginBundle.getBundle(ZbxRestService.PAYLOAD_FIELDS.FOLLOWUP_ACTION);
		final String retrievedFollowupCmd = followupBundle.getString(RestServiceBase.PAYLOAD_FIELDS.API_CMD);
		assertEquals(ZbxApiConstants.API.HOST.GET, retrievedFollowupCmd);
		final String retrievedFollowupIp = followupBundle.getString(ZbxApiConstants.FIELDS.IP);
		assertEquals(sampleIp, retrievedFollowupIp);
		final String retrievedHost = followupBundle.getString(ZbxApiConstants.FIELDS.HOST);
		assertEquals(sampleHost, retrievedHost);
		final String[] retrievedItemNameArray = followupBundle.getStringArray(ZbxRestService.PAYLOAD_FIELDS.ITEM_NAME_ARRAY);
		for(int i=0; i<sampleItemNames.length; i++) {
			assertEquals(sampleItemNames[i], retrievedItemNameArray[i]);
		}
		final String retrievedTargetContentUri = followupBundle.getString(ZbxRestService.PAYLOAD_FIELDS.TARGET_CONTENT_URI);
		assertEquals(sampleTargetContentUri, retrievedTargetContentUri);
		
	}
	
	public void testCreateItemGetCallWithHostId() {
		final String replyForHostGetOnUnconnectedHost = "{\"jsonrpc\":\"2.0\",\"result\":[{\"maintenances\":[{\"maintenanceid\":\"0\"}],\"hostid\":\"10047\",\"proxy_hostid\":\"0\",\"host\":\"i-13-86-VM\",\"dns\":\"\",\"useip\":\"1\",\"ip\":\"219.117.239.181\",\"port\":\"10050\",\"status\":\"0\",\"disable_until\":\"1326258935\",\"error\":\"Get value from agent failed: *** Cannot connect to [[219.117.239.181]:10050]: [4] Interrupted system call\",\"available\":\"2\",\"errors_from\":\"1325754701\",\"lastaccess\":\"0\",\"inbytes\":\"0\",\"outbytes\":\"0\",\"useipmi\":\"0\",\"ipmi_port\":\"623\",\"ipmi_authtype\":\"-1\",\"ipmi_privilege\":\"2\",\"ipmi_username\":\"\",\"ipmi_password\":\"\",\"ipmi_disable_until\":\"0\",\"ipmi_available\":\"0\",\"snmp_disable_until\":\"0\",\"snmp_available\":\"0\",\"maintenanceid\":\"0\",\"maintenance_status\":\"0\",\"maintenance_type\":\"0\",\"maintenance_from\":\"0\",\"ipmi_ip\":\"\",\"ipmi_errors_from\":\"0\",\"snmp_errors_from\":\"0\",\"ipmi_error\":\"\",\"snmp_error\":\"\"}],\"id\":2}";
		final String testItemname1 = "some random test item name - one";
		final String testHostip1 = "some random test host ip - one";
		final String testTargetContentUri1 = "some random test content uri - one";
		testCreateItemGetCallWithHostId_helper(replyForHostGetOnUnconnectedHost, testItemname1, testHostip1, testTargetContentUri1);

		final String replyForHostGetOnConnectedHost = "[{\"maintenances\":[{\"maintenanceid\":\"0\"}],\"hostid\":\"10017\",\"proxy_hostid\":\"0\",\"host\":\"Zabbix server\",\"dns\":\"\",\"useip\":\"1\",\"ip\":\"127.0.0.1\",\"port\":\"10050\",\"status\":\"0\",\"disable_until\":\"0\",\"error\":\"\",\"available\":\"1\",\"errors_from\":\"0\",\"lastaccess\":\"0\",\"inbytes\":\"0\",\"outbytes\":\"0\",\"useipmi\":\"0\",\"ipmi_port\":\"623\",\"ipmi_authtype\":\"0\",\"ipmi_privilege\":\"2\",\"ipmi_username\":\"\",\"ipmi_password\":\"\",\"ipmi_disable_until\":\"0\",\"ipmi_available\":\"0\",\"snmp_disable_until\":\"0\",\"snmp_available\":\"0\",\"maintenanceid\":\"0\",\"maintenance_status\":\"0\",\"maintenance_type\":\"0\",\"maintenance_from\":\"0\",\"ipmi_ip\":\"\",\"ipmi_errors_from\":\"0\",\"snmp_errors_from\":\"0\",\"ipmi_error\":\"\",\"snmp_error\":\"\"}]";
		final String testItemname2 = "43586790-)('&%$#ERHGJH?><{`*} - two";
		final String testHostip2 = "43586790-)('&%$#ERHGJH?><{`*} - two";
		final String testTargetContentUri2 = "some random test content uri - two";
		testCreateItemGetCallWithHostId_helper(replyForHostGetOnConnectedHost, testItemname2, testHostip2, testTargetContentUri2);
	}
	
	public void testCreateItemGetCallWithHostId_helper(final String jsonData, final String itemname, final String hostip, final String targetContentUri) {
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
		
		final String sampleFollowupKey1 = "followup key 1";
		final String sampleFollowupData1 = "followup data 1";
		final String sampleFollowupKey2 = "followup key 2";
		final String sampleFollowupData2 = "followup data 2";
		Bundle sampleFollowupBundle = new Bundle();  //making nonsense bundle just for test
		sampleFollowupBundle.putString(sampleFollowupKey1, sampleFollowupData1);
		sampleFollowupBundle.putString(sampleFollowupKey2, sampleFollowupData2);
		
		//retrieve the hostid from json
		final JsonNode hostidNode = rootNode.findPath(ZbxApiConstants.FIELDS.HOSTID);
		final String hostid = hostidNode.getTextValue();
		//retrieve the host from json
		final JsonNode hostNode = rootNode.findPath(ZbxApiConstants.FIELDS.HOST);
		final String host = hostNode.getTextValue();
		
		Intent testIntent = HostGetRequest.createItemGetCallWithRetreivedHostId(getContext(), hostid, itemname, host, targetContentUri, sampleFollowupBundle);
		Bundle testPayload = testIntent.getExtras();
		final String retrievedApiCmd = testPayload.getString(RestServiceBase.PAYLOAD_FIELDS.API_CMD);
		assertEquals(ZbxApiConstants.API.ITEM.GET, retrievedApiCmd);
		final String retrievedHostId = testPayload.getString(ZbxApiConstants.FIELDS.HOSTID);
		assertEquals(rootNode.findPath(ZbxApiConstants.FIELDS.HOSTID).getTextValue(), retrievedHostId);
		final String retrievedItemname = testPayload.getString(ZbxApiConstants.FIELDS.KEY_);
		assertEquals(itemname, retrievedItemname);
		Bundle retrievedFollowupBundle = testPayload.getBundle(ZbxRestService.PAYLOAD_FIELDS.FOLLOWUP_ACTION);
		final String retrievedData1 = retrievedFollowupBundle.getString(sampleFollowupKey1);
		final String retrievedData2 = retrievedFollowupBundle.getString(sampleFollowupKey2);
		assertEquals(sampleFollowupData1, retrievedData1);
		assertEquals(sampleFollowupData2, retrievedData2);
	}
	
	public int insertSampleDataIntCpuLoadsTable(final String sampleHost, final int numRecsToInsert) {
		final String sampleItemname = "sample item name ";
		double sampleClock = 0;
		double sampleValue = 0;
		
		ContentValues[] cvArray = new ContentValues[numRecsToInsert];
		for(int i=0; i<numRecsToInsert; i++) {
			ContentValues cv = new ContentValues();
			cv.put(CpuLoads.HOST, sampleHost);
			cv.put(CpuLoads.ITEMNAME, sampleItemname+i);
			cv.put(CpuLoads.CLOCK, sampleClock+i);
			cv.put(CpuLoads.VALUE, sampleValue+i);
			cvArray[i] = cv;
		}
		return getContext().getContentResolver().bulkInsert(CpuLoads.META_DATA.CONTENT_URI, cvArray);
	}
	
	public int cpuloadTableSizeInRows() {
		final String[] columns = new String[] { CpuLoads._ID };
		Cursor c = getContext().getContentResolver().query(CpuLoads.META_DATA.CONTENT_URI, columns, null, null, null);
		final int numRows = c.getCount();
		c.close();
		return numRows;
	}
	
	public void testdeleteOldSavedDataForHostIp() {
		ZbxRestContentProvider.deleteAllData(getContext());

		final String sampleAuthToken = "some sample auth token";
		final String sampleHostId = "some sample host id";
		final String sampleIp1 = "some sample HOST.IP.ADDRESS 1";
		final String sampleHost1 = "some sample HOST 1";
		final String sampleHost2 = "some sample HOST 2";
		final String sampleHost3 = "some sample HOST 3";
		final String[] sampleItemNames1 = new String[] { "some sample ITEM1-1", "some sample ITEM1-2", "some sample ITEM1-3", "some sample ITEM1-4", };
		final String sampleTargetContentUri1 = CpuLoads.META_DATA.CONTENT_URI.toString();

		ObjectMapper objMapper = new ObjectMapper();
		ObjectNode fakeNodeForTheSolePurposeOfPreventingTheCallFromBarfing = objMapper.createObjectNode();
		fakeNodeForTheSolePurposeOfPreventingTheCallFromBarfing.put(ZbxApiConstants.FIELDS.HOSTID, sampleHostId);

		final int sampleIp1RecNum = 200;
		final int sampleIp2RecNum = 220;
		final int sampleIp3RecNum = 230;
		final int numInsertedSampleIp1Recs = insertSampleDataIntCpuLoadsTable(sampleHost1, sampleIp1RecNum);
		assertEquals(sampleIp1RecNum, numInsertedSampleIp1Recs);
		final int numInsertedSampleIp2Recs = insertSampleDataIntCpuLoadsTable(sampleHost2, sampleIp2RecNum);
		assertEquals(sampleIp2RecNum, numInsertedSampleIp2Recs);
		final int numInsertedSampleIp3Recs = insertSampleDataIntCpuLoadsTable(sampleHost3, sampleIp3RecNum);
		assertEquals(sampleIp3RecNum, numInsertedSampleIp3Recs);
		
		HostGetRequest sampleHostGetRequest = new HostGetRequest(sampleAuthToken, sampleIp1, sampleHost1, sampleItemNames1, sampleTargetContentUri1);
		sampleHostGetRequest.deleteOldSavedDataForHost(getContext(), sampleHost1);

		assertEquals((sampleIp3RecNum+sampleIp2RecNum), cpuloadTableSizeInRows());
		
		assertThereAreNoRowsWithThisIp(sampleHost1);
		
	}

	public void assertThereAreNoRowsWithThisIp(final String host) {
		final String[] columns = new String[] { CpuLoads.HOST };
		Cursor c = getContext().getContentResolver().query(CpuLoads.META_DATA.CONTENT_URI, columns, null, null, null);
		final int hostColumnIndex = c.getColumnIndex(CpuLoads.HOST);
		c.moveToFirst();
		while(!c.isAfterLast()) {
			final String retrievedHost = c.getString(hostColumnIndex);
			assertTrue("all entries for host='"+host+"' should have been deleted from the table by doApiSpecificProcessing()", !host.equals(retrievedHost));
			c.moveToNext();
		}
		c.close();
	}

	
	
	
}
