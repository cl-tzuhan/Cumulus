package com.creationline.cloudstack.engine;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.util.Iterator;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.test.ServiceTestCase;

import com.creationline.cloudstack.engine.db.Errors;
import com.creationline.cloudstack.engine.db.Transactions;
import com.creationline.cloudstack.engine.db.Vms;

@SuppressWarnings("deprecation")
public class CsRestServiceTest extends ServiceTestCase<CsRestService> {

//	CsRestService csRestService = null;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
//		csRestService = new CsRestService();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public CsRestServiceTest() {
		super(CsRestService.class);
	}
	
	private CsRestService startCsRestService() {
		Bundle payload = new Bundle();
		Bundle emptyBundle = new Bundle();
        payload.putString(CsRestService.ACTION_ID, CsRestService.TEST_CALL);
        payload.putBundle(CsRestService.API_CMD, emptyBundle);  //sending an empty bundle causes CsRestService to start-up and do nothing
        
        Intent startCsRestServiceIntent = new Intent(getSystemContext(), CsRestService.class);
        startCsRestServiceIntent.putExtras(payload);
		startService(startCsRestServiceIntent);
		
		return getService();
	}
	
	protected void deleteDb() {
		//Completely remove the db if it exists
		
		if (getContext().databaseList().length<=0) {
			return; //do nothing if there is no db to start with
		}
		assertTrue(getContext().deleteDatabase(CsRestContentProvider.DB_NAME));
	}
	
	public void testSignRequest() {
		///Tests for a specific request+key signing with pre-determined result
		final String apiKey = "namomNgZ8Qt5DuNFUWf3qpGlQmB4650tY36wFOrhUtrzK13d66qNpttKw52Brj02dbtIHs01y-lCLz1UOzTxVQ";
		final String sortedUrl = "account=thsu-account&apikey=namomngz8qt5dunfuwf3qpglqmb4650ty36wforhutrzk13d66qnpttkw52brj02dbtihs01y-lclz1uoztxvq&command=listvirtualmachines&domainid=2&response=json";
		final String expectedSignedResult = "anoAR%2FAaugrU6uemcuRUw%2Fma0RI%3D";
		
		String signedResult = CsRestService.signRequest(sortedUrl, apiKey);
		assertEquals(expectedSignedResult, signedResult);
	}
	
	public void testInputStreamToString() {
		CsRestService csRestService = startCsRestService();

		final String sampleText = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ 1234567890 !#$%&\'()-^\\@[;:],./=~|`{+*}<>?";
		InputStream sampleTextStream = new StringBufferInputStream(sampleText);
		StringBuilder result1 = csRestService.inputStreamToString(sampleTextStream);
		assertEquals(sampleText, result1.toString());
		
		final String emptyStr = "";
		InputStream emptyStream = new StringBufferInputStream(emptyStr);
		StringBuilder result2 = csRestService.inputStreamToString(emptyStream);
		assertEquals(emptyStr, result2.toString());
		
		String bigStr = sampleText + sampleText + sampleText + sampleText + sampleText + sampleText + sampleText;
		bigStr = bigStr + bigStr + bigStr + bigStr + bigStr;
		InputStream bigStrStream = new StringBufferInputStream(bigStr);
		StringBuilder result3 = csRestService.inputStreamToString(bigStrStream);
		assertEquals(bigStr, result3.toString());
	}
	
	public void testBuildFinalUrl() {
		//Test for request+key signing with pre-determined result for thsu-account@192.168.3.11:8080
		String host = "http://192.168.3.11:8080/client/api";
//		String apiCmd = "command=listVirtualMachines&account=thsu-account&domainid=2";
		Bundle apiCmd = new Bundle();
			apiCmd.putString(CsRestService.COMMAND, "listVirtualMachines");
			apiCmd.putString("account", "thsu-account");
			apiCmd.putString("domainid", "2");
		String apiKey = "namomNgZ8Qt5DuNFUWf3qpGlQmB4650tY36wFOrhUtrzK13d66qNpttKw52Brj02dbtIHs01y-lCLz1UOzTxVQ";
		String secretKey = "Yt_9ZEIDGlmRIg63MiMatAri-1aRoo4l-82mnbYdR3d8JdG7jvXqrrB5TpmbLZB_8zK_j95VRSQWZwnu0153eQ";
		String expectedFinalUrl = "http://192.168.3.11:8080/client/api?response=json&command=listVirtualMachines&account=thsu-account&domainid=2&apiKey=namomNgZ8Qt5DuNFUWf3qpGlQmB4650tY36wFOrhUtrzK13d66qNpttKw52Brj02dbtIHs01y-lCLz1UOzTxVQ&signature=AZW5TbyF8QY07lPWxk0JZyMwFx0%3D";
		
		String finalizedRequest = CsRestService.buildFinalUrl(host, apiCmd, apiKey, secretKey);
		assertEquals(expectedFinalUrl, finalizedRequest);

		
		//Test for request+key signing with pre-determined result for iizuka1@72.52.126.24
		host = "http://72.52.126.24/client/api";
//		apiCmd = "command=listVirtualMachines&account=iizuka1";
		apiCmd = new Bundle();
			apiCmd.putString(CsRestService.COMMAND, "listVirtualMachines");
			apiCmd.putString("account", "iizuka1");
		apiKey = "fUFqsJeECZcMawm9q376WKFKdFvd51GLHwgm3d9PD-r3mjNJUaXBYbkKxBoxCdF5EubJ-ypmT8vHihtAm-gZvA";
		secretKey = "Q3s_-gMYzivbaaO9S_2ewdXHSXHvUg6ExP0W2yRWBZxFIbTDIKD3ADk-0NU6qhsD0K31e9Irchh_Z8yuRQTuqQ";
		expectedFinalUrl = "http://72.52.126.24/client/api?response=json&command=listVirtualMachines&account=iizuka1&apiKey=fUFqsJeECZcMawm9q376WKFKdFvd51GLHwgm3d9PD-r3mjNJUaXBYbkKxBoxCdF5EubJ-ypmT8vHihtAm-gZvA&signature=ueOd4fR%2BnPDv8Rcvb5qLNoGl80Y%3D";
		
		finalizedRequest = CsRestService.buildFinalUrl(host, apiCmd, apiKey, secretKey);
		assertEquals(expectedFinalUrl, finalizedRequest);
	}
	
	public void testParseListVirtualMachinesResult() {
		final String columns[] = new String[] {
				Vms.ID,
				Vms.NAME,
				Vms.DISPLAYNAME,
				Vms.ACCOUNT,
				Vms.DOMAINID,
				Vms.DOMAIN,
				Vms.CREATED,
				Vms.STATE,
				Vms.HAENABLE,
				Vms.ZONEID,
				Vms.ZONENAME,
				Vms.TEMPLATEID,
				Vms.TEMPLATENAME,
				Vms.TEMPLATEDISPLAYTEXT,
				Vms.PASSWORDENABLED,
				Vms.SERVICEOFFERINGID,
				Vms.SERVICEOFFERINGNAME,
				Vms.CPUNUMBER,
				Vms.CPUSPEED,
				Vms.MEMORY,
				Vms.CPUUSED,
				Vms.NETWORKKBSREAD,
				Vms.NETWORKKBSWRITE,
				Vms.GUESTOSID,
				Vms.ROOTDEVICEID,
				Vms.ROOTDEVICETYPE,
				Vms.NIC,
				Vms.HYPERVISOR};

		final String sampleBodyWith1Vm = "{ \"listvirtualmachinesresponse\" : { \"virtualmachine\" : [  {\"id\":2027,\"name\":\"i-39-2027-VM\",\"displayname\":\"i-39-2027-VM\",\"account\":\"iizuka\",\"domainid\":1,\"domain\":\"ROOT\",\"created\":\"2011-09-10T02:43:42-0700\",\"state\":\"Running\",\"haenable\":false,\"zoneid\":1,\"zonename\":\"San Jose\",\"templateid\":259,\"templatename\":\"CentOS 5.3 (64 bit) vSphere\",\"templatedisplaytext\":\"CentOS 5.3 (64 bit) vSphere Password enabled\",\"passwordenabled\":true,\"serviceofferingid\":1,\"serviceofferingname\":\"Small Instance\",\"cpunumber\":1,\"cpuspeed\":500,\"memory\":512,\"cpuused\":\"0%\",\"networkkbsread\":0,\"networkkbswrite\":0,\"guestosid\":12,\"rootdeviceid\":0,\"rootdevicetype\":\"NetworkFilesystem\",\"nic\":[{\"id\":2122,\"networkid\":247,\"netmask\":\"255.255.255.0\",\"gateway\":\"10.1.1.1\",\"ipaddress\":\"10.1.1.129\",\"traffictype\":\"Guest\",\"type\":\"Virtual\",\"isdefault\":true}],\"hypervisor\":\"VMware\"} ] } }";
		executeAndCheckParseListVirtualMachines(sampleBodyWith1Vm, columns);

		final String sampleBodyWith3Vms = "{ \"listvirtualmachinesresponse\" : { \"virtualmachine\" : [  {\"id\":1123,\"name\":\"AWS-i2236.ao\",\"displayname\":\"My big AWS VM\",\"account\":\"kamehameha\",\"domainid\":1000,\"domain\":\"ROOTS!\",\"created\":\"2001-01-01T00:00:00-0700\",\"state\":\"Stopped\",\"haenable\":false,\"zoneid\":12,\"zonename\":\"Mars (the planet)\",\"templateid\":1111111,\"templatename\":\"AWS Cloud OS version 0.0.0001x\",\"templatedisplaytext\":\"TOP SECRET OS!!  FOR YOUR EYES ONLY!!!\",\"passwordenabled\":true,\"serviceofferingid\":11111,\"serviceofferingname\":\"Cloud instance\",\"cpunumber\":11111,\"cpuspeed\":500001,\"memory\":51200123,\"cpuused\":\"10%\",\"networkkbsread\":1220,\"networkkbswrite\":45110,\"guestosid\":1232,\"rootdeviceid\":10,\"rootdevicetype\":\"IZUMOFilesystem\",\"nic\":[{\"id\":5522,\"networkid\":2475555,\"netmask\":\"113.25.255.0\",\"gateway\":\"10.0.0.1\",\"ipaddress\":\"100.100.100.129\",\"traffictype\":\"Guest\",\"type\":\"Virtual\",\"isdefault\":true}],\"hypervisor\":\"AWSware\"}, "
			+"{\"id\":2394,\"name\":\"i-39-2394-VM\",\"displayname\":\"Don't look at me\",\"account\":\"kamehameha\",\"domainid\":0,\"domain\":\"AROMA!\",\"created\":\"1034-10-10T10:10:10-0700\",\"state\":\"Running\",\"haenable\":true,\"zoneid\":986,\"zonename\":\"Mars (the symphony)\",\"templateid\":222222222,\"templatename\":\"Unknown OS\",\"templatedisplaytext\":\"If you are the owner of this OS, please contact 080-1456-2235\",\"passwordenabled\":false,\"serviceofferingid\":0,\"serviceofferingname\":\"Unknown instance\",\"cpunumber\":0,\"cpuspeed\":0,\"memory\":0,\"cpuused\":\"100%\",\"networkkbsread\":1098765,\"networkkbswrite\":56789,\"guestosid\":0,\"rootdeviceid\":333,\"rootdevicetype\":\"Unknown\",\"nic\":[{\"id\":883,\"networkid\":0,\"netmask\":\"0.0.0.0\",\"gateway\":\"0.0.0.0\",\"ipaddress\":\"0.0.0.0\",\"traffictype\":\"System\",\"type\":\"Real\",\"isdefault\":false}],\"hypervisor\":\"Megavisor!\"},"
			+"{\"id\":838272,\"name\":\"AWOL in the numerious battlefields of Kondak\",\"displayname\":\"_\",\"account\":\"kamehameha\",\"domainid\":99,\"domain\":\"none\",\"created\":\"2011-11-11T11:11:11-0700\",\"state\":\"Running\",\"haenable\":true,\"zoneid\":66346,\"zonename\":\"Mars (the candybar)\",\"templateid\":1,\"templatename\":\"Android OS for iPhone\",\"templatedisplaytext\":\"#%=(+%*?@$%')\",\"passwordenabled\":false,\"serviceofferingid\":259,\"serviceofferingname\":\"Android for iPhone instance\",\"cpunumber\":10000001,\"cpuspeed\":1340,\"memory\":5520,\"cpuused\":\"50%\",\"networkkbsread\":22,\"networkkbswrite\":4,\"guestosid\":20,\"rootdeviceid\":2,\"rootdevicetype\":\"ContentProvider store\",\"nic\":[{\"id\":883,\"networkid\":0,\"netmask\":\"0.0.0.0\",\"gateway\":\"0.0.0.0\",\"ipaddress\":\"0.0.0.0\",\"traffictype\":\"Admin\",\"type\":\"Worldly\",\"isdefault\":false}],\"hypervisor\":\"Googavisor!\"} ] } }";
		executeAndCheckParseListVirtualMachines(sampleBodyWith3Vms, columns);
	}
	
	private void executeAndCheckParseListVirtualMachines(final String jsonData, final String[] columns) {
		
		//ask CsRestService to parse the passed-in json; CsRestService will actually go and update the vms db for this
		CsRestService csRestService = startCsRestService();
		csRestService.parseReplyBody_listVirtualMachines(jsonData);
		
		//grab the data saved directly from db so we can check the saved values below
		Cursor c = getContext().getContentResolver().query(Vms.META_DATA.CONTENT_URI, columns, null, null, null);
		c.moveToFirst();
		
		//parse the original sample json data into a tree so we can use it to compare individual values below
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
		
		//grab the expected virtualmachine list inside listvirtualmachinesresponse; we will parse these VM objects, checking their values
		Iterator<JsonNode> listItr = rootNode.findValue("virtualmachine").getElements();
		
		int numVms = 0;
		while(listItr.hasNext()) {
			//go through each vm object returned and check every field to see if it matches the original value
			JsonNode vmNode = listItr.next();  //grab next parsed VM object to use as expected result
			numVms++;
			for(String columnName : c.getColumnNames()) {
				//check values of all columns and make sure read-back data match the original
				final String expectedValue = vmNode.findValue(columnName).toString();
				final String retrievedValue = c.getString(c.getColumnIndex(columnName));
				assertEquals(trimDoubleQuotes(expectedValue), trimDoubleQuotes(retrievedValue));
			}
			c.moveToNext();  //get next VM object read from db to check
		}
		
		assertEquals("Number of VM objects in db does not match expected number", numVms, c.getCount());
	}
	
	public void testParseErrorAndAddToDb() {
		deleteDb();
		
		final String sampleUriToUpdate = "content://com.creationline.cloudstack.engine.csrestcontentprovider/transactions/4";
		final int sampleStatusCode = 432;
		final String sampleErrorText = "The given command:listVirtualMachiness does not exist";
		final String sampleErrorResponseJson = "{ \"errorresponse\" : {\"errorcode\" : 432, \"errortext\" : \""+sampleErrorText+"\"}  }";
		
		//ask CsRestService to parse the sample error response; CsRestService will actually go and update the errors db for this
		CsRestService csRestService = startCsRestService();
		csRestService.parseErrorAndAddToDb(Uri.parse(sampleUriToUpdate), sampleStatusCode, new StringBuilder(sampleErrorResponseJson));
		
		final String columns[] = new String[] {
				 Errors.ERRORCODE,
				 Errors.ERRORTEXT,
				 Errors.ORIGINATINGCALL,
		};
		//grab the data saved directly from db so we can check the saved values below
		Cursor c = getContext().getContentResolver().query(Errors.META_DATA.CONTENT_URI, columns, null, null, null);
		c.moveToFirst();
		assertEquals("There should only be 1 row saved to errors db!", 1, c.getCount());
		
		assertEquals(sampleStatusCode, c.getInt(c.getColumnIndex(Errors.ERRORCODE)));
		assertEquals(sampleErrorText, c.getString(c.getColumnIndex(Errors.ERRORTEXT)));
		assertEquals(sampleUriToUpdate, c.getString(c.getColumnIndex(Errors.ORIGINATINGCALL)));
	}
	
	public void testUpdateCallWithReplyOnDb() {
		deleteDb();

		final String sampleRequest = "1234567890-^\\qwertyop@[asdfghjl;:]zxcvbnm,./_";
		final String sampleOriginalStatus = Transactions.STATUS_VALUES.IN_PROGRESS;
		
		ContentValues cv = new ContentValues();
		cv.put(Transactions.REQUEST, sampleRequest);
		cv.put(Transactions.STATUS, sampleOriginalStatus);
		Uri uriToUpdate = getContext().getContentResolver().insert(Transactions.META_DATA.CONTENT_URI, cv);
		
		final String sampleUpdatedStatus = Transactions.STATUS_VALUES.SUCCESS;
		final String sampleReplyBodyJson = "{ \"listvirtualmachinesresponse\" : { \"virtualmachine\" : [  {\"id\":2027,\"name\":\"i-39-2027-VM\",\"displayname\":\"i-39-2027-VM\",\"account\":\"iizuka\",\"domainid\":1,\"domain\":\"ROOT\",\"created\":\"2011-09-10T02:43:42-0700\",\"state\":\"Running\",\"haenable\":false,\"zoneid\":1,\"zonename\":\"San Jose\",\"templateid\":259,\"templatename\":\"CentOS 5.3 (64 bit) vSphere\",\"templatedisplaytext\":\"CentOS 5.3 (64 bit) vSphere Password enabled\",\"passwordenabled\":true,\"serviceofferingid\":1,\"serviceofferingname\":\"Small Instance\",\"cpunumber\":1,\"cpuspeed\":500,\"memory\":512,\"cpuused\":\"0%\",\"networkkbsread\":0,\"networkkbswrite\":0,\"guestosid\":12,\"rootdeviceid\":0,\"rootdevicetype\":\"NetworkFilesystem\",\"nic\":[{\"id\":2122,\"networkid\":247,\"netmask\":\"255.255.255.0\",\"gateway\":\"10.1.1.1\",\"ipaddress\":\"10.1.1.129\",\"traffictype\":\"Guest\",\"type\":\"Virtual\",\"isdefault\":true}],\"hypervisor\":\"VMware\"} ] } }";
		
		//ask CsRestService to update the db with new data; CsRestService will actually go and update the transactions db for this
		CsRestService csRestService = startCsRestService();
		csRestService.updateCallWithReplyOnDb(uriToUpdate, sampleUpdatedStatus, new StringBuilder(sampleReplyBodyJson));
		
		final String columns[] = new String[] {
				 Transactions.REQUEST,
				 Transactions.STATUS,
				 Transactions.REPLY,
				 Transactions.REPLY_DATETIME,
		};
		//grab the data saved directly from db so we can check the saved values below
		Cursor c = getContext().getContentResolver().query(Transactions.META_DATA.CONTENT_URI, columns, null, null, null);
		c.moveToFirst();
		assertEquals("There should only be 1 row saved+updated in transactions db!", 1, c.getCount());
		
		assertEquals(sampleRequest, c.getString(c.getColumnIndex(Transactions.REQUEST)));  //should have no change
		assertEquals(sampleUpdatedStatus, c.getString(c.getColumnIndex(Transactions.STATUS)));  //should have been changed
		assertEquals(sampleReplyBodyJson, c.getString(c.getColumnIndex(Transactions.REPLY)));  //should have been added
		assertNotNull("reply_datetime was not added automatically when it should have been!", c.getString(c.getColumnIndex(Transactions.REPLY_DATETIME)));  //can't get the exact timestamp that's added automatically by CsRestService, so will just check that it exists
	}
	
	public void testUpdateCallAsAbortedOnDb() {
		deleteDb();

		final String sampleRequest = "1234567890-^\\QWERTYUIOP@[ASDFGHJKL;:]ZXCVBNM,./_";
		final String sampleOriginalStatus = Transactions.STATUS_VALUES.IN_PROGRESS;
		
		ContentValues cv = new ContentValues();
		cv.put(Transactions.REQUEST, sampleRequest);
		cv.put(Transactions.STATUS, sampleOriginalStatus);
		Uri uriToUpdate = getContext().getContentResolver().insert(Transactions.META_DATA.CONTENT_URI, cv);
		
		//ask CsRestService to abort the call; CsRestService will actually go and update the transactions db for this
		CsRestService csRestService = startCsRestService();
		csRestService.updateCallAsAbortedOnDb(uriToUpdate);
		
		final String columns[] = new String[] {
				 Transactions.REQUEST,
				 Transactions.STATUS,
				 Transactions.REPLY,
				 Transactions.REPLY_DATETIME,
		};
		//grab the data saved directly from db so we can check the saved values below
		Cursor c = getContext().getContentResolver().query(Transactions.META_DATA.CONTENT_URI, columns, null, null, null);
		c.moveToFirst();
		assertEquals("There should only be 1 row saved+updated in transactions db!", 1, c.getCount());
		
		assertEquals(sampleRequest, c.getString(c.getColumnIndex(Transactions.REQUEST)));  //should have no change
		assertEquals(Transactions.STATUS_VALUES.ABORTED, c.getString(c.getColumnIndex(Transactions.STATUS)));  //should have been changed
		assertNull("Reply field should remain empty after a call abort", c.getString(c.getColumnIndex(Transactions.REPLY)));  //should have no change
		assertNotNull("reply_datetime was not added automatically when it should have been!", c.getString(c.getColumnIndex(Transactions.REPLY_DATETIME)));  //can't get the exact timestamp that's added automatically by CsRestService, so will just check that it exists
	}
	
	
	/**
	 * Copied from:
	 *   http://www.java2s.com/Code/Java/Data-Type/Trimsthequotes.htm
	 * Copyright (c) 2004 Actuate Corporation.
	 * All rights reserved. This program and the accompanying materials
	 * are made available under the terms of the Eclipse Public License v1.0
	 * which accompanies this distribution, and is available at
	 * http://www.eclipse.org/legal/epl-v10.html
	 * 
	 * @param value the string may have quotes
	 * @return the string without quotes
	 */
	public static String trimDoubleQuotes(String value)
	{
		if(value==null) {
			return value;
		}

		value = value.trim();
		if(value.startsWith("\"") && value.endsWith("\"")) {
			return value.substring(1, value.length()-1);
		}

		return value;
	}

	  
} //END CsRestServiceTest
