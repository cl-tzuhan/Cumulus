package com.creationline.cloudstack.engine;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.util.Iterator;
import java.util.Set;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.test.ServiceTestCase;

import com.creationline.cloudstack.engine.CsRestService.JsonNameNodePair;
import com.creationline.cloudstack.engine.db.Errors;
import com.creationline.cloudstack.engine.db.Snapshots;
import com.creationline.cloudstack.engine.db.Transactions;
import com.creationline.cloudstack.engine.db.Vms;
import com.creationline.cloudstack.mock.CsacMockApplication;

@SuppressWarnings("deprecation")
public class CsRestServiceTest extends ServiceTestCase<CsRestService> {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		setApplication(new CsacMockApplication());
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
	
	protected void deleteAllData() {
//		//Completely remove the db if it exists
//		if (getContext().databaseList().length<=0) {
//			return; //do nothing if there is no db to start with
//		}
//		assertTrue(getContext().deleteDatabase(CsRestContentProvider.DB_NAME));
		
		//erase all data from each table
		getContext().getContentResolver().delete(Transactions.META_DATA.CONTENT_URI, null, null);
		getContext().getContentResolver().delete(Vms.META_DATA.CONTENT_URI, null, null);
		getContext().getContentResolver().delete(Snapshots.META_DATA.CONTENT_URI, null, null);
		getContext().getContentResolver().delete(Errors.META_DATA.CONTENT_URI, null, null);
	}
	
	public void testSignRequest() {
		{
			///Tests for a specific request+key signing with pre-determined result
			final String apiKey = "namomNgZ8Qt5DuNFUWf3qpGlQmB4650tY36wFOrhUtrzK13d66qNpttKw52Brj02dbtIHs01y-lCLz1UOzTxVQ";
			final String sortedUrl = "account=thsu-account&apikey=namomngz8qt5dunfuwf3qpglqmb4650ty36wforhutrzk13d66qnpttkw52brj02dbtihs01y-lclz1uoztxvq&command=listvirtualmachines&domainid=2&response=json";
			final String expectedSignedResult = "anoAR%2FAaugrU6uemcuRUw%2Fma0RI%3D";

			String signedResult = CsRestService.signRequest(sortedUrl, apiKey);
			assertEquals(expectedSignedResult, signedResult);
		}

		{
			///Tests for a specific request+key signing with pre-determined result
			final String apiKey = "lodAuMftOyg0nWiwU5JUy__nn9YO1uJ34oxE9PvdLplJQOTmrEzpoe3wXjG0u1-AsY2y9636GTGDs5LsinxK7Q";
			final String sortedUrl = "account=rickson&apikey=cqltndmdyaeiz6zdzqg2qinye5sx4m914eseb-rsjtewtvcccglrme-zh_ipqqkmcigjznba_ugrldhs_ley-g&command=listsnapshots&response=json";
			final String expectedSignedResult = "3%2BkXi7q6hcOlD4oQFX1w6iCuabQ%3D";
			
			String signedResult = CsRestService.signRequest(sortedUrl, apiKey);
			assertEquals(expectedSignedResult, signedResult);
		}
	}
	
	public void testInputStreamToString() {
		CsRestService csRestService = startCsRestService();

		try {
			final String sampleText = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ 1234567890 !#$%&\'()-^\\@[;:],./=~|`{+*}<>?";
			InputStream sampleTextStream = new StringBufferInputStream(sampleText);
			StringBuilder result1;
			result1 = csRestService.inputStreamToString(sampleTextStream);
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
		} catch (IOException e) {
			fail("inputStreamToString ran into error trying to process stream");
			e.printStackTrace();
		}
	}
	
	public void testBuildFinalUrl_listVirtualMachines() {
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

	public void testBuildFinalUrl_listSnapshots() {
		//Test for request+key signing with pre-determined result for ricksont@219.117.239.169:8080
		String host = "http://219.117.239.169:8080/client/api";
		Bundle apiCmd = new Bundle();
		apiCmd.putString(CsRestService.COMMAND, "listSnapshots");
		apiCmd.putString("account", "rickson");
		String apiKey = "cqLtNDMDYAeIZ6ZdZQG2QInyE5Sx4M914eSeb-rsJTewTvcCcGLRMe-zh_IPQQKmcIGJzNBa_UGrLDhS_LEy-g";
		String secretKey = "lodAuMftOyg0nWiwU5JUy__nn9YO1uJ34oxE9PvdLplJQOTmrEzpoe3wXjG0u1-AsY2y9636GTGDs5LsinxK7Q";
		String expectedFinalUrl = "http://219.117.239.169:8080/client/api?response=json&command=listSnapshots&account=rickson&apiKey=cqLtNDMDYAeIZ6ZdZQG2QInyE5Sx4M914eSeb-rsJTewTvcCcGLRMe-zh_IPQQKmcIGJzNBa_UGrLDhS_LEy-g&signature=3%2BkXi7q6hcOlD4oQFX1w6iCuabQ%3D";
		
		String finalizedRequest = CsRestService.buildFinalUrl(host, apiCmd, apiKey, secretKey);
		assertEquals(expectedFinalUrl, finalizedRequest);
		
	}
	
	public void testProcessAndSaveJsonReplyData_listVirtualMachines() {
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
		executeAndCheck_listVirtualMachines(sampleBodyWith1Vm, columns);

		final String sampleBodyWith3Vms = "{ \"listvirtualmachinesresponse\" : { \"virtualmachine\" : [  {\"id\":1123,\"name\":\"AWS-i2236.ao\",\"displayname\":\"My big AWS VM\",\"account\":\"kamehameha\",\"domainid\":1000,\"domain\":\"ROOTS!\",\"created\":\"2001-01-01T00:00:00-0700\",\"state\":\"STOPPED\",\"haenable\":false,\"zoneid\":12,\"zonename\":\"Mars (the planet)\",\"templateid\":1111111,\"templatename\":\"AWS Cloud OS version 0.0.0001x\",\"templatedisplaytext\":\"TOP SECRET OS!!  FOR YOUR EYES ONLY!!!\",\"passwordenabled\":true,\"serviceofferingid\":11111,\"serviceofferingname\":\"Cloud instance\",\"cpunumber\":11111,\"cpuspeed\":500001,\"memory\":51200123,\"cpuused\":\"10%\",\"networkkbsread\":1220,\"networkkbswrite\":45110,\"guestosid\":1232,\"rootdeviceid\":10,\"rootdevicetype\":\"IZUMOFilesystem\",\"nic\":[{\"id\":5522,\"networkid\":2475555,\"netmask\":\"113.25.255.0\",\"gateway\":\"10.0.0.1\",\"ipaddress\":\"100.100.100.129\",\"traffictype\":\"Guest\",\"type\":\"Virtual\",\"isdefault\":true}],\"hypervisor\":\"AWSware\"}, "
			+"{\"id\":2394,\"name\":\"i-39-2394-VM\",\"displayname\":\"Don't look at me\",\"account\":\"kamehameha\",\"domainid\":0,\"domain\":\"AROMA!\",\"created\":\"1034-10-10T10:10:10-0700\",\"state\":\"Running\",\"haenable\":true,\"zoneid\":986,\"zonename\":\"Mars (the symphony)\",\"templateid\":222222222,\"templatename\":\"Unknown OS\",\"templatedisplaytext\":\"If you are the owner of this OS, please contact 080-1456-2235\",\"passwordenabled\":false,\"serviceofferingid\":0,\"serviceofferingname\":\"Unknown instance\",\"cpunumber\":0,\"cpuspeed\":0,\"memory\":0,\"cpuused\":\"100%\",\"networkkbsread\":1098765,\"networkkbswrite\":56789,\"guestosid\":0,\"rootdeviceid\":333,\"rootdevicetype\":\"Unknown\",\"nic\":[{\"id\":883,\"networkid\":0,\"netmask\":\"0.0.0.0\",\"gateway\":\"0.0.0.0\",\"ipaddress\":\"0.0.0.0\",\"traffictype\":\"System\",\"type\":\"Real\",\"isdefault\":false}],\"hypervisor\":\"Megavisor!\"},"
			+"{\"id\":838272,\"name\":\"AWOL in the numerious battlefields of Kondak\",\"displayname\":\"_\",\"account\":\"kamehameha\",\"domainid\":99,\"domain\":\"none\",\"created\":\"2011-11-11T11:11:11-0700\",\"state\":\"Running\",\"haenable\":true,\"zoneid\":66346,\"zonename\":\"Mars (the candybar)\",\"templateid\":1,\"templatename\":\"Android OS for iPhone\",\"templatedisplaytext\":\"#%=(+%*?@$%')\",\"passwordenabled\":false,\"serviceofferingid\":259,\"serviceofferingname\":\"Android for iPhone instance\",\"cpunumber\":10000001,\"cpuspeed\":1340,\"memory\":5520,\"cpuused\":\"50%\",\"networkkbsread\":22,\"networkkbswrite\":4,\"guestosid\":20,\"rootdeviceid\":2,\"rootdevicetype\":\"ContentProvider store\",\"nic\":[{\"id\":883,\"networkid\":0,\"netmask\":\"0.0.0.0\",\"gateway\":\"0.0.0.0\",\"ipaddress\":\"0.0.0.0\",\"traffictype\":\"Admin\",\"type\":\"Worldly\",\"isdefault\":false}],\"hypervisor\":\"Googavisor!\"} ] } }";
		executeAndCheck_listVirtualMachines(sampleBodyWith3Vms, columns);
	}
	
	private void executeAndCheck_listVirtualMachines(final String jsonData, final String[] columns) {
		
		//ask CsRestService to parse the passed-in json; CsRestService will actually go and update the vms db for this
		CsRestService csRestService = startCsRestService();
		csRestService.processAndSaveJsonReplyData(null, jsonData);  //uriToUpdate parameter not used for listVirtualMachines call
		
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

	public void testProcessAndSaveJsonReplyData_startVirtualMachines() {
		final String columns[] = new String[] {
				Transactions.JOBID,
				};
		
		final String sampleBody = "{ \"startvirtualmachineresponse\" : {\"jobid\":383} }";
		executeAndCheck_startOrStopOrRebootVirtualMachines(sampleBody, columns);
	}

	public void testProcessAndSaveJsonReplyData_stopVirtualMachines() {
		final String columns[] = new String[] {
				Transactions.JOBID,
		};
		
		final String sampleBody = "{ \"stopvirtualmachineresponse\" : {\"jobid\":423} }";
		executeAndCheck_startOrStopOrRebootVirtualMachines(sampleBody, columns);
	}

	public void testProcessAndSaveJsonReplyData_rebootVirtualMachines() {
		final String columns[] = new String[] {
				Transactions.JOBID,
		};
		
		final String sampleBody = "{ \"rebootvirtualmachineresponse\" : {\"jobid\":424} }";
		executeAndCheck_startOrStopOrRebootVirtualMachines(sampleBody, columns);
	}
	
	private void executeAndCheck_startOrStopOrRebootVirtualMachines(final String jsonData, final String[] columns) {
		deleteAllData();
		
		//insert sample record so we can test whether it is successfully updated below
		ContentValues cv = new ContentValues();
		cv.put(Transactions.REQUEST, "test request");
		cv.put(Transactions.REQUEST_DATETIME, "test request datetime");
		cv.put(Transactions.STATUS, "test status");
		final Uri uriToUpdate = getContext().getContentResolver().insert(Transactions.META_DATA.CONTENT_URI, cv);
		
		//ask CsRestService to parse the passed-in json; CsRestService will actually go and update the transactions db for this
		CsRestService csRestService = startCsRestService();
		csRestService.processAndSaveJsonReplyData(uriToUpdate, jsonData);
		
		//grab the data saved directly from db so we can check the saved values below
		Cursor c = getContext().getContentResolver().query(uriToUpdate, columns, null, null, null);
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
		
		final String expectedJobid = rootNode.findValue("jobid").asText();
		final String retreivedJobid = c.getString(c.getColumnIndex(Transactions.JOBID));
		
		assertEquals("Jobids do not match!", expectedJobid, retreivedJobid);
	}
	
	public void testProcessAndSaveJsonReplyData_queryAsyncJobResult_success() {
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
			Vms.HYPERVISOR,
		};
		
		//sample reply from a startVirtualMachine call
		final String vmid1 = "51";
		final String sampleBody1 = "{ \"queryasyncjobresultresponse\" : {\"jobid\":423,\"jobstatus\":1,\"jobprocstatus\":0,\"jobresultcode\":0,\"jobresulttype\":\"object\",\"jobresult\":{\"virtualmachine\":{\"id\":"+vmid1+",\"name\":\"i-13-51-VM\",\"displayname\":\"èCìl\",\"account\":\"rickson\",\"domainid\":1,\"domain\":\"ROOT\",\"created\":\"2011-10-20T17:02:16+0900\",\"state\":\"Stopped\",\"haenable\":false,\"groupid\":9,\"group\":\"MMA\",\"zoneid\":2,\"zonename\":\"zone2\",\"templateid\":2,\"templatename\":\"CentOS 5.3(64-bit) no GUI (XenServer)\",\"templatedisplaytext\":\"CentOS 5.3(64-bit) no GUI (XenServer)\",\"passwordenabled\":false,\"serviceofferingid\":10,\"serviceofferingname\":\"mini Instance\",\"cpunumber\":1,\"cpuspeed\":140,\"memory\":256,\"cpuused\":\"0.02%\",\"networkkbsread\":154373515,\"networkkbswrite\":111849502,\"guestosid\":12,\"rootdeviceid\":0,\"rootdevicetype\":\"NetworkFilesystem\",\"securitygroup\":[],\"nic\":[{\"id\":113,\"networkid\":220,\"netmask\":\"255.255.255.0\",\"gateway\":\"10.1.1.1\",\"ipaddress\":\"10.1.1.45\",\"traffictype\":\"Guest\",\"type\":\"Virtual\",\"isdefault\":true,\"macaddress\":\"02:00:62:2d:00:03\"}],\"hypervisor\":\"XenServer\"}}} }";
		executeAndCheck_queryAsyncJobResult_success(sampleBody1, columns, vmid1);
		
		//sample reply form a rebootVirtualMachine call
		final String vmid2 = "49";
		final String sampleBody2 = "{ \"queryasyncjobresultresponse\" : {\"jobid\":424,\"jobstatus\":1,\"jobprocstatus\":0,\"jobresultcode\":0,\"jobresulttype\":\"object\",\"jobresult\":{\"virtualmachine\":{\"id\":"+vmid2+",\"name\":\"i-13-49-VM\",\"displayname\":\"Brazilian Jujitsu\",\"account\":\"rickson\",\"domainid\":1,\"domain\":\"ROOT\",\"created\":\"2011-10-20T16:30:14+0900\",\"state\":\"Running\",\"haenable\":false,\"groupid\":9,\"group\":\"MMA\",\"zoneid\":2,\"zonename\":\"zone2\",\"templateid\":2,\"templatename\":\"CentOS 5.3(64-bit) no GUI (XenServer)\",\"templatedisplaytext\":\"CentOS 5.3(64-bit) no GUI (XenServer)\",\"passwordenabled\":false,\"serviceofferingid\":11,\"serviceofferingname\":\"sakaue instance\",\"cpunumber\":1,\"cpuspeed\":400,\"memory\":512,\"cpuused\":\"0.1%\",\"networkkbsread\":8957853,\"networkkbswrite\":8948577,\"guestosid\":12,\"rootdeviceid\":0,\"rootdevicetype\":\"NetworkFilesystem\",\"securitygroup\":[],\"nic\":[{\"id\":109,\"networkid\":220,\"netmask\":\"255.255.255.0\",\"gateway\":\"10.1.1.1\",\"ipaddress\":\"10.1.1.64\",\"traffictype\":\"Guest\",\"type\":\"Virtual\",\"isdefault\":true,\"macaddress\":\"02:00:2b:20:00:01\"}],\"hypervisor\":\"XenServer\"}}} }";
		executeAndCheck_queryAsyncJobResult_success(sampleBody2, columns, vmid2);
	}
	private void executeAndCheck_queryAsyncJobResult_success(final String jsonData, final String[] columns, final String vmid) {
		deleteAllData();
		
		//insert sample record so we can test whether it is successfully updated below
		ContentValues cv = new ContentValues();
		cv.put(Vms.ID, vmid);  //all the other data is arbitrary, but this id must match the id of the virtualmachine data in the jsonData
		cv.put(Vms.ACCOUNT, "fake account data");
		cv.put(Vms.CPUNUMBER, "fake CPU number data");
		cv.put(Vms.DISPLAYNAME, "fake display name data");
		cv.put(Vms.GUESTOSID, "fake guest OS ID data");
		cv.put(Vms.HOSTNAME, "fake hostname data");
		cv.put(Vms.SERVICEOFFERINGID, "fake service offering id data");
		cv.put(Vms.STATE, "fake state data");
		getContext().getContentResolver().insert(Vms.META_DATA.CONTENT_URI, cv);
		
		//ask CsRestService to parse the passed-in json; CsRestService will actually go and update the vms db for this
		CsRestService csRestService = startCsRestService();
		csRestService.processAndSaveJsonReplyData(null, jsonData);  //uriToUpdate parameter not used for queryAsyncJobResult call
		
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
		
		//grab the expected virtualmachine list inside queryasyncjobresultresponse
		JsonNode vmNode = rootNode.findPath("virtualmachine");
		assertNotNull("virtualmachine field/objectnode in queryasyncjobresultresponse not found!", vmNode);
		
		for(String columnName : c.getColumnNames()) {
			//check values of all columns and make sure read-back data match the original
			final String expectedValue = vmNode.findValue(columnName).toString();
			final String retrievedValue = c.getString(c.getColumnIndex(columnName));
			assertEquals(trimDoubleQuotes(expectedValue), trimDoubleQuotes(retrievedValue));
		}
	}

	public void testProcessAndSaveJsonReplyData_queryAsyncJobResult_failure() {
		final String columns[] = new String[] {
				Errors.ERRORCODE,
				Errors.ERRORTEXT,
				Errors.ORIGINATINGCALL,
				};

		final String jobid = "163";
		final String sampleBody = "{ \"queryasyncjobresultresponse\" : {\"jobid\":"+jobid+",\"jobstatus\":2,\"jobprocstatus\":0,\"jobresultcode\":530,\"jobresulttype\":\"object\",\"jobresult\":{\"errorcode\":530,\"errortext\":\"Internal error executing command, please contact your system administrator\"}} }";
		executeAndCheck_queryAsyncJobResult_failure(sampleBody, columns, jobid);
	}
	private void executeAndCheck_queryAsyncJobResult_failure(final String jsonData, final String[] columns, final String jobid) {
		deleteAllData();
		
		//insert sample record so we can test whether it is successfully updated below
		final String sampleRequest = "http://cha.la.head.cha.la/";
		ContentValues cv = new ContentValues();
		cv.put(Transactions.REQUEST, sampleRequest);
		cv.put(Transactions.REQUEST_DATETIME, "test request datetime");
		cv.put(Transactions.STATUS, "test status");
		cv.put(Transactions.JOBID, jobid);  //all the other data is arbitrary, but this jobid must match the jobid of the data in the jsonData
		getContext().getContentResolver().insert(Transactions.META_DATA.CONTENT_URI, cv);
		
		//ask CsRestService to parse the passed-in json; CsRestService will actually go and update the errors db for this
		CsRestService csRestService = startCsRestService();
		csRestService.processAndSaveJsonReplyData(null, jsonData);  //uriToUpdate parameter not used for queryAsyncJobResult call
		
		//grab the data saved directly from db so we can check the saved values below
		Cursor c = getContext().getContentResolver().query(Errors.META_DATA.CONTENT_URI, columns, null, null, null);
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
		
		final String expectedErrorCode = rootNode.findPath("jobresultcode").asText();
		final String expectedErrorText = rootNode.findPath("errortext").asText()+" ("+expectedErrorCode+")";
		
		assertEquals(expectedErrorCode, c.getString(c.getColumnIndex(Errors.ERRORCODE)));
		assertEquals(expectedErrorText, c.getString(c.getColumnIndex(Errors.ERRORTEXT)));
		assertEquals(sampleRequest, c.getString(c.getColumnIndex(Errors.ORIGINATINGCALL)));
	}
	
	public void testProcessAndSaveJsonReplyData_queryAsyncJobResult_failure_failedToRebootVmInstance() {
		final String jobid = "163";
		final String sampleBody = "{ \"queryasyncjobresultresponse\" : {\"jobid\":"+jobid+",\"jobstatus\":2,\"jobprocstatus\":0,\"jobresultcode\":530,\"jobresulttype\":\"object\",\"jobresult\":{\"errorcode\":530,\"errortext\":\"Failed to reboot vm instance\"}} }";
		executeAndCheck_queryAsyncJobResult_failure_failedToRebootVmInstance(sampleBody, jobid);
	}
	private void executeAndCheck_queryAsyncJobResult_failure_failedToRebootVmInstance(final String jsonData, final String jobid) {
		deleteAllData();
		
		//insert sample record required by the process below
		final String vmid = "51";
		final String sampleRequest = "http://219.117.239.169:8080/client/api?response=json&command=rebootVirtualMachine&id="+vmid+"&apiKey=cqLtNDMDYAeIZ6ZdZQG2QInyE5Sx4M914eSeb-rsJTewTvcCcGLRMe-zh_IPQQKmcIGJzNBa_UGrLDhS_LEy-g&signature=acAL1qG0DOycLvvsbysbhlNTUrM%3D";
		ContentValues TransactionsCv = new ContentValues();
		TransactionsCv.put(Transactions.REQUEST, sampleRequest);
		TransactionsCv.put(Transactions.REQUEST_DATETIME, "test request datetime");
		TransactionsCv.put(Transactions.STATUS, "test status");
		TransactionsCv.put(Transactions.JOBID, jobid);  //all the other data is arbitrary, but this jobid must match the jobid of the data in the jsonData
		getContext().getContentResolver().insert(Transactions.META_DATA.CONTENT_URI, TransactionsCv);
		
		//insert sample record so we can test whether it is successfully updated below
		ContentValues VmsCv = new ContentValues();
		VmsCv.put(Vms.ID, vmid);  //all the other data is arbitrary, but this vmid must match the vmid of the data in the sampleRequest
		VmsCv.put(Vms.ACCOUNT, "fake account data");
		VmsCv.put(Vms.CPUNUMBER, "fake CPU number data");
		VmsCv.put(Vms.DISPLAYNAME, "fake display name data");
		VmsCv.put(Vms.GUESTOSID, "fake guest OS ID data");
		VmsCv.put(Vms.HOSTNAME, "fake hostname data");
		VmsCv.put(Vms.SERVICEOFFERINGID, "fake service offering id data");
		VmsCv.put(Vms.STATE, "fake state data");
		final Uri insertedVmsRow = getContext().getContentResolver().insert(Vms.META_DATA.CONTENT_URI, VmsCv);
		
		//ask CsRestService to parse the passed-in json; CsRestService will actually go and update the errors & vms dbs for this
		CsRestService csRestService = startCsRestService();
		csRestService.processAndSaveJsonReplyData(null, jsonData);  //uriToUpdate parameter not used for queryAsyncJobResult call
		
		final String[] vmsStateColumn = new String[] {
				Vms.STATE,
		};
		Cursor c = getContext().getContentResolver().query(insertedVmsRow, vmsStateColumn, null, null, null);
		c.moveToFirst();
		
		assertEquals(1, c.getCount());
		assertEquals("rebooting VMs getting this error should be automatically marked as stopped", Vms.STATE_VALUES.STOPPED, c.getString(c.getColumnIndex(Vms.STATE)));
	}
	
	public void testProcessAndSaveJsonReplyData_listSnapshots() {
		final String columns[] = new String[] {
				Snapshots.ID,
				Snapshots.ACCOUNT,
				Snapshots.CREATED,
				Snapshots.DOMAIN,
				Snapshots.DOMAINID,
				Snapshots.INTERVALTYPE,
				Snapshots.JOBID,
				Snapshots.JOBSTATUS,
				Snapshots.NAME,
				Snapshots.SNAPSHOTTYPE,
				Snapshots.STATE,
				Snapshots.VOLUMEID,
				Snapshots.VOLUMENAME,
				Snapshots.VOLUMETYPE
		};

		final String sampleBodyWith3Snapshots = "{ \"listsnapshotsresponse\" : { \"count\":3 ,\"snapshot\" : [  {\"id\":6,\"account\":\"rickson\",\"domainid\":1,\"domain\":\"ROOT\",\"snapshottype\":\"MANUAL\",\"volumeid\":60,\"volumename\":\"DATA-51\",\"volumetype\":\"DATADISK\",\"created\":\"2011-10-26T11:15:21+0900\",\"name\":\"i-13-51-VM_DATA-51_20111026021521\",\"intervaltype\":\"MANUAL\",\"state\":\"BackedUp\"}, {\"id\":5,\"account\":\"rickson\",\"domainid\":1,\"domain\":\"ROOT\",\"snapshottype\":\"MANUAL\",\"volumeid\":59,\"volumename\":\"ROOT-51\",\"volumetype\":\"ROOT\",\"created\":\"2011-10-26T11:15:16+0900\",\"name\":\"i-13-51-VM_ROOT-51_20111026021516\",\"intervaltype\":\"MANUAL\",\"state\":\"BackedUp\"}, {\"id\":4,\"account\":\"rickson\",\"domainid\":1,\"domain\":\"ROOT\",\"snapshottype\":\"MANUAL\",\"volumeid\":59,\"volumename\":\"ROOT-51\",\"volumetype\":\"ROOT\",\"created\":\"2011-10-21T12:45:25+0900\",\"name\":\"i-13-51-VM_ROOT-51_20111021034525\",\"intervaltype\":\"MANUAL\",\"state\":\"BackedUp\"} ] } }";
		executeAndCheck_listSnapshots(sampleBodyWith3Snapshots, columns);
	}
	
	private void executeAndCheck_listSnapshots(final String jsonData, final String[] columns) {
		
		//ask CsRestService to parse the passed-in json; CsRestService will actually go and update the snapshots db for this
		CsRestService csRestService = startCsRestService();
		csRestService.processAndSaveJsonReplyData(null, jsonData);  //uriToUpdate parameter not used for listVirtualMachines call
		
		//grab the data saved directly from db so we can check the saved values below
		Cursor c = getContext().getContentResolver().query(Snapshots.META_DATA.CONTENT_URI, columns, null, null, null);
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
		
		//grab the expected snapshots list inside listsnapshotsresponse; we will parse these snapshot objects, checking their values
		Iterator<JsonNode> listItr = rootNode.findValue("snapshot").getElements();
		
		int numVms = 0;
		while(listItr.hasNext()) {
			//go through each snapshot object returned and check every field to see if it matches the original value
			JsonNode snapshotNode = listItr.next();  //grab next parsed snapshot object to use as expected result
			numVms++;
			for(String columnName : c.getColumnNames()) {
				//check values of all columns and make sure read-back data match the original
				final String retrievedValue = c.getString(c.getColumnIndex(columnName));
				final JsonNode jn = snapshotNode.findValue(columnName);
				if(jn==null) {
					//if the expected value is a null...
					if(retrievedValue==null) {
						continue;  //...pass if the db also has no value for this column
					} else {
						fail("field="+columnName+" should exist, but is not found in output");  //...fail if it db contains a value for this column
					}
				}
				final String expectedValue = jn.toString();
				assertEquals(trimDoubleQuotes(expectedValue), trimDoubleQuotes(retrievedValue));
			}
			c.moveToNext();  //get next snapshot object read from db to check
		}
		
		assertEquals("Number of snapshot objects in db does not match expected number", numVms, c.getCount());
	}
	
	public void testParseErrorAndAddToDb() {
		deleteAllData();
		
		final String sampleUriToUpdate = "content://com.creationline.cloudstack.engine.csrestcontentprovider/transactions/4";
		final int sampleStatusCode = 432;
		final String sampleErrorText = "The given command:listVirtualMachiness does not exist";
		final String sampleErrorResponseJson = "{ \"errorresponse\" : {\"errorcode\" : 432, \"errortext\" : \""+sampleErrorText+"\"}  }";
		
		//ask CsRestService to parse the sample error response; CsRestService will actually go and update the errors db for this
		CsRestService csRestService = startCsRestService();
		csRestService.parseErrorAndAddToDb(Uri.parse(sampleUriToUpdate), sampleStatusCode, sampleErrorResponseJson);
		
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
		deleteAllData();

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
		deleteAllData();

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
	
	public void testUnpackAndSaveReplyBodyData() {
		CsRestService csRestService = startCsRestService();
		
		final Uri uri = Uri.parse("");
		final StringBuilder replyBody = new StringBuilder();
		
		csRestService.unpackAndSaveReplyBodyData(uri, null, replyBody);
		//we'll consider this a pass if we don't crash
		
		csRestService.unpackAndSaveReplyBodyData(null, null, replyBody);
		//we'll consider this a pass if we don't crash
		
		csRestService.unpackAndSaveReplyBodyData(uri, null, null);
		//we'll consider this a pass if we don't crash
		
		csRestService.unpackAndSaveReplyBodyData(null, null, null);
		//we'll consider this a pass if we don't crash
	}
	
	public void testFindTransactionRequest() {
		deleteAllData();
		CsRestService csRestService = startCsRestService();

		//set up data in transactions db
		Bundle testData = new Bundle();
		testData.putString("1", "http://ja.wikipedia.org/wiki/%E3%83%A2%E3%83%B3%E3%82%AD%E3%83%BC%E3%83%BBD%E3%83%BB%E3%83%AB%E3%83%95%E3%82%A3");
		testData.putString("2", "http://ja.wikipedia.org/wiki/%E3%83%AD%E3%83%AD%E3%83%8E%E3%82%A2%E3%83%BB%E3%82%BE%E3%83%AD");
		testData.putString("3", "http://ja.wikipedia.org/wiki/%E3%83%8A%E3%83%9F_(ONE_PIECE)");
		testData.putString("4", "http://ja.wikipedia.org/wiki/%E3%82%A6%E3%82%BD%E3%83%83%E3%83%97");
		testData.putString("5", "http://ja.wikipedia.org/wiki/%E3%82%B5%E3%83%B3%E3%82%B8");
		testData.putString("6", "http://ja.wikipedia.org/wiki/%E3%83%88%E3%83%8B%E3%83%BC%E3%83%88%E3%83%8B%E3%83%BC%E3%83%BB%E3%83%81%E3%83%A7%E3%83%83%E3%83%91%E3%83%BC");
		testData.putString("7", "http://ja.wikipedia.org/wiki/%E3%83%8B%E3%82%B3%E3%83%BB%E3%83%AD%E3%83%93%E3%83%B3");
		testData.putString("8", "http://ja.wikipedia.org/wiki/%E3%83%95%E3%83%A9%E3%83%B3%E3%82%AD%E3%83%BC_(ONE_PIECE)");
		testData.putString("9", "http://ja.wikipedia.org/wiki/%E3%83%96%E3%83%AB%E3%83%83%E3%82%AF_(ONE_PIECE)");
		testData.putString("100", "http://shonenjump.com/j/rensai/onepiece/index.html");
		testData.putString("200", "http://www.j-onepiece.com/");
		testData.putString("300", "http://www.toei-anim.co.jp/tv/onep/");
		testData.putString("400", "http://www.fujitv.co.jp/b_hp/onepiece/");
		testData.putString("500", "http://mv.avex.jp/onepiece/");
		testData.putString("600", "http://www.bandaigames.channel.or.jp/list/one_main/");
		Set<String> keys = testData.keySet();
		for(String key : keys) {
			ContentValues cv = new ContentValues();
			cv.put(Transactions.JOBID, key);
			cv.put(Transactions.REQUEST, testData.getString(key));
			getContext().getContentResolver().insert(Transactions.META_DATA.CONTENT_URI, cv);
		}
		//for good measure, insert some data that have no jobid
		for(int i=0; i<7; i++) {
			ContentValues cv = new ContentValues();
			cv.put(Transactions.REQUEST, "http://world.gov/"+i);
			getContext().getContentResolver().insert(Transactions.META_DATA.CONTENT_URI, cv);
		}
		
		//test each jobid to see if the fetched request is correct
		for(String key : keys) {
			final String request = csRestService.findTransactionRequest(Transactions.META_DATA.CONTENT_URI, key);
			assertEquals(testData.getString(key), request);
		}
	}
	
	public void testFindTransactionRequestForRow() {
		CsRestService csRestService = startCsRestService();

		{
			final String request = "http://www.oricon.co.jp/news/ranking/81934/full/";
			ContentValues cv = new ContentValues();
			cv.put(Transactions.REQUEST, request);
			Uri insertUri = getContext().getContentResolver().insert(Transactions.META_DATA.CONTENT_URI, cv);
			final String retrievedRequest = csRestService.findTransactionRequestForRow(insertUri);
			assertEquals(request, retrievedRequest);
		}

		{
			final String request = "http://www.geocities.jp/wj_log/rank/";
			ContentValues cv = new ContentValues();
			cv.put(Transactions.REQUEST, request);
			cv.put(Transactions.JOBID, "meaningless jobid");
			Uri insertUri = getContext().getContentResolver().insert(Transactions.META_DATA.CONTENT_URI, cv);
			final String retrievedRequest = csRestService.findTransactionRequestForRow(insertUri);
			assertEquals(request, retrievedRequest);
		}

		{
			final String request = "http://mantan-web.jp/2011/11/04/20111104dog00m200003000c.html";
			ContentValues cv = new ContentValues();
			cv.put(Transactions.REQUEST, request);
			cv.put(Transactions.REPLY, "meaningless reply");
			cv.put(Transactions.JOBID, "meaningless jobid");
			Uri insertUri = getContext().getContentResolver().insert(Transactions.META_DATA.CONTENT_URI, cv);
			final String retrievedRequest = csRestService.findTransactionRequestForRow(insertUri);
			assertEquals(request, retrievedRequest);
		}

		{
			final String retrievedRequest = csRestService.findTransactionRequestForRow(null);
			assertNull(retrievedRequest);
		}
	}
	
	public void testFindRequestForJobid_edegeCases() {
		CsRestService csRestService = startCsRestService();
		
		assertNull("findRequestForJobid() should fail gracefully with null", csRestService.findTransactionRequestForJobid(null));
		
		deleteAllData();
		assertNull("findRequestForJobid() should fail gracefully with non-existent jobid", csRestService.findTransactionRequestForJobid("63934"));
	}
	
	public void testAddToErrorLog() {
		{
			final String errorCode = "134567890-^^[@poiuytrewsdfjkl;.,mnbvc";
			final String errorText = "DSAFAEYN&UQOHLAPV$LT=GIVOBLPE LHG#`LG PT$VK 0GK KG";
			final String originatingTransactionUri = "!QASZ23ewdfsvcx&RTUDHGFN87uj)OL<<uo78iBNGFDTY$&XZdqZDFE!";
			Cursor c = addToErrorLogAndReturnQueryFromErrorsDb(errorCode, errorText, originatingTransactionUri);
			assertEquals(1, c.getCount());
			c.moveToFirst();
			assertEquals(errorCode, c.getString(c.getColumnIndex(Errors.ERRORCODE)));
			assertEquals(errorText, c.getString(c.getColumnIndex(Errors.ERRORTEXT)));
			assertEquals(originatingTransactionUri, c.getString(c.getColumnIndex(Errors.ORIGINATINGCALL)));
		}

		{
			final String errorCode = null;
			final String errorText = "DSAFAEYN&UQOHLAPV$LT=GIVOBLPE LHG#`LG PT$VK 0GK KG";
			final String originatingTransactionUri = "!QASZ23ewdfsvcx&RTUDHGFN87uj)OL<<uo78iBNGFDTY$&XZdqZDFE!";
			Cursor c = addToErrorLogAndReturnQueryFromErrorsDb(errorCode, errorText, originatingTransactionUri);
			assertEquals(1, c.getCount());
			c.moveToFirst();
			assertTrue("no errorCode value should have been inserted", c.isNull(c.getColumnIndex((Errors.ERRORCODE))));
			assertEquals(errorText, c.getString(c.getColumnIndex(Errors.ERRORTEXT)));
			assertEquals(originatingTransactionUri, c.getString(c.getColumnIndex(Errors.ORIGINATINGCALL)));
		}

		{
			final String errorCode = "134567890-^^[@poiuytrewsdfjkl;.,mnbvc";
			final String errorText = null;
			final String originatingTransactionUri = "!QASZ23ewdfsvcx&RTUDHGFN87uj)OL<<uo78iBNGFDTY$&XZdqZDFE!";
			Cursor c = addToErrorLogAndReturnQueryFromErrorsDb(errorCode, errorText, originatingTransactionUri);
			assertEquals(1, c.getCount());
			c.moveToFirst();
			assertEquals(errorCode, c.getString(c.getColumnIndex(Errors.ERRORCODE)));
			assertTrue("no errorText value should have been inserted", c.isNull(c.getColumnIndex((Errors.ERRORTEXT))));
			assertEquals(originatingTransactionUri, c.getString(c.getColumnIndex(Errors.ORIGINATINGCALL)));
		}
		
		{
			final String errorCode = "134567890-^^[@poiuytrewsdfjkl;.,mnbvc";
			final String errorText = "DSAFAEYN&UQOHLAPV$LT=GIVOBLPE LHG#`LG PT$VK 0GK KG";
			final String originatingTransactionUri = null;
			Cursor c = addToErrorLogAndReturnQueryFromErrorsDb(errorCode, errorText, originatingTransactionUri);
			assertEquals(1, c.getCount());
			c.moveToFirst();
			assertEquals(errorCode, c.getString(c.getColumnIndex(Errors.ERRORCODE)));
			assertEquals(errorText, c.getString(c.getColumnIndex(Errors.ERRORTEXT)));
			assertTrue("no originatingTransactionUri value should have been inserted", c.isNull(c.getColumnIndex((Errors.ORIGINATINGCALL))));
		}
	}
	public Cursor addToErrorLogAndReturnQueryFromErrorsDb(
			final String errorCode, final String errorText,
			final String originatingTransactionUri) {
		deleteAllData();
		CsRestService csRestService = startCsRestService();
		
		final Uri uri = csRestService.addToErrorLog(errorCode, errorText, originatingTransactionUri);
		assertNotNull(uri);
		assertTrue("returned uri is not for errors db table!", uri.toString().contains(Errors.META_DATA.CONTENT_URI.toString()));
		
		final String[] columns = new String[] {
				Errors.ERRORCODE,
				Errors.ERRORTEXT,
				Errors.ORIGINATINGCALL,
		};
		Cursor c = getContext().getContentResolver().query(uri, columns, null, null, null);
		return c;
	}
	
	public void testExtractFirstFieldValuePair_simpleEmbeddedObject() {
		final String valueJson = "{\"samplevalue\" : \"some text\" }";
		final String topFieldName = "sampleresponse";
		final String jsonToExtractFieldValuePairFrom = "{\""+topFieldName+"\" : "+valueJson+" }";
		
		ObjectMapper om = new ObjectMapper();
	    JsonNode dataNode = null;
	    JsonNode valueNode = null;
		try {
			dataNode = om.readTree(jsonToExtractFieldValuePairFrom);
			valueNode = om.readTree(valueJson);
		} catch (JsonProcessingException e) {
			fail("om.readTree() processing failed!");
			e.printStackTrace();
		} catch (IOException e) {
			fail("om.readTree() failed!");
			e.printStackTrace();
		}
		
		CsRestService csRestService = startCsRestService();
		JsonNameNodePair nodePair = csRestService.extractFirstFieldValuePair(dataNode);
		
		assertEquals(topFieldName, nodePair.getFieldName());
		assertEquals(valueNode, nodePair.getValueNode());
	}

	public void testExtractFirstFieldValuePair_embeddedList() {
		final String valueJson = "[{\"samplevalue1\" : \"some text\" } , {\"samplevalue2\" : 123456789 } , {\"samplevalue3\" : {\"embeddedObject\" : 31} }]";
		final String topFieldName = "sampleresponsethatisreallyreallyreallyreallyreallyreallyreallyreallylong with some spaces as well";
		final String jsonToExtractFieldValuePairFrom = "{\""+topFieldName+"\" : "+valueJson+" }";
		
		ObjectMapper om = new ObjectMapper();
		JsonNode dataNode = null;
		JsonNode valueNode = null;
		try {
			dataNode = om.readTree(jsonToExtractFieldValuePairFrom);
			valueNode = om.readTree(valueJson);
		} catch (JsonProcessingException e) {
			fail("om.readTree() processing failed!");
			e.printStackTrace();
		} catch (IOException e) {
			fail("om.readTree() failed!");
			e.printStackTrace();
		}
		
		CsRestService csRestService = startCsRestService();
		JsonNameNodePair nodePair = csRestService.extractFirstFieldValuePair(dataNode);
		
		assertEquals(topFieldName, nodePair.getFieldName());
		assertEquals(valueNode, nodePair.getValueNode());
	}

	public void testExtractFirstFieldValuePair_emptyEmbeddedList() {
		final String valueJson = "[ ]";
		final String topFieldName = "a   b   c    d      e        f             g                h";
		final String jsonToExtractFieldValuePairFrom = "{\""+topFieldName+"\" : "+valueJson+" }";
		
		ObjectMapper om = new ObjectMapper();
		JsonNode dataNode = null;
		JsonNode valueNode = null;
		try {
			dataNode = om.readTree(jsonToExtractFieldValuePairFrom);
			valueNode = om.readTree(valueJson);
		} catch (JsonProcessingException e) {
			fail("om.readTree() processing failed!");
			e.printStackTrace();
		} catch (IOException e) {
			fail("om.readTree() failed!");
			e.printStackTrace();
		}
		
		CsRestService csRestService = startCsRestService();
		JsonNameNodePair nodePair = csRestService.extractFirstFieldValuePair(dataNode);
		
		assertEquals(topFieldName, nodePair.getFieldName());
		assertEquals(valueNode, nodePair.getValueNode());
	}

	public void testExtractFirstFieldValuePair_topLevelList() {
		final String jsonToExtractFieldValuePairFrom = "[ {\"firstObj\" : \"qwerty\" } , {\"secondObj\" : 123} ]";
		
		ObjectMapper om = new ObjectMapper();
		JsonNode dataNode = null;
		try {
			dataNode = om.readTree(jsonToExtractFieldValuePairFrom);
		} catch (JsonProcessingException e) {
			fail("om.readTree() processing failed!");
			e.printStackTrace();
		} catch (IOException e) {
			fail("om.readTree() failed!");
			e.printStackTrace();
		}
		
		CsRestService csRestService = startCsRestService();
		JsonNameNodePair nodePair = csRestService.extractFirstFieldValuePair(dataNode);

		assertNull("a list has no top-level field/valule pair, so extractFirstFieldValuePair should return null", nodePair);
	}

	public void testExtractFirstFieldValuePair_emptyList() {
		final String jsonToExtractFieldValuePairFrom = "[ ]";
		
		ObjectMapper om = new ObjectMapper();
		JsonNode dataNode = null;
		try {
			dataNode = om.readTree(jsonToExtractFieldValuePairFrom);
		} catch (JsonProcessingException e) {
			fail("om.readTree() processing failed!");
			e.printStackTrace();
		} catch (IOException e) {
			fail("om.readTree() failed!");
			e.printStackTrace();
		}
		
		CsRestService csRestService = startCsRestService();
		JsonNameNodePair nodePair = csRestService.extractFirstFieldValuePair(dataNode);
		
		assertNull("an empty list has no top-level field/valule pair, so extractFirstFieldValuePair should return null", nodePair);
	}
	
	public void testHandleJobresultBasedOnApi_deleteSnapshot() {
		final String snapshotId = "25";
		final String request = " http://219.117.239.169:8080/client/api?response=json&command=deleteSnapshot&id="+snapshotId+"&apiKey=cqLtNDMDYAeIZ6ZdZQG2QInyE5Sx4M914eSeb-rsJTewTvcCcGLRMe-zh_IPQQKmcIGJzNBa_UGrLDhS_LEy-g&signature=YA0v%2BrUSF8%2B%2Fubqj7WxvY9iVvSM%3D";
		final String jobIdOfRequest = "591";

		//set up test data needed by handleJobresultBasedOnApi()
		deleteAllData();
		ContentValues transactionsCv = new ContentValues();
		transactionsCv.put(Transactions.REQUEST, request);
		transactionsCv.put(Transactions.JOBID, jobIdOfRequest);
		getContext().getContentResolver().insert(Transactions.META_DATA.CONTENT_URI, transactionsCv);

		//set up test data needed by handleJobresultBasedOnApi()
		ContentValues snapshotsCv = new ContentValues();
		snapshotsCv.put(Snapshots.ID, snapshotId);  //the other fields are arbitrary, only this id field needs to match the id in the request
		snapshotsCv.put(Snapshots.NAME, "sample snapshot");
		Uri snapshotUri = getContext().getContentResolver().insert(Snapshots.META_DATA.CONTENT_URI, snapshotsCv);
		
		
		CsRestService csRestService = startCsRestService();
		
		csRestService.handleJobresultBasedOnApi(jobIdOfRequest, false);  //handling a failed result resets animations on the ui, but should not affect the db
		Cursor c = getContext().getContentResolver().query(snapshotUri, null, null, null, null);
		assertNotNull(c);
		assertEquals(1, c.getCount());
		c.moveToFirst();
		assertEquals(snapshotId, c.getString(c.getColumnIndex(Snapshots.ID)));

		csRestService.handleJobresultBasedOnApi(jobIdOfRequest, true);  //handling a success result should remove the snapshot from db
		Cursor shouldBeEmpty = getContext().getContentResolver().query(snapshotUri, null, null, null, null);
		assertEquals("the previously saved snapshot should no longer exist", 0, shouldBeEmpty.getCount());
		final String whereClause = Snapshots.ID+"="+snapshotId;
		shouldBeEmpty = getContext().getContentResolver().query(Snapshots.META_DATA.CONTENT_URI, null, whereClause, null, null);
		assertEquals("double checking a snapshot with the same id does not exist", 0, shouldBeEmpty.getCount());
	}
	
	public void testUpdateVmState() {
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
				Vms.HYPERVISOR,
			};
		final String sampleText = "sample snapshot text";
		final String vmid = "123456789";
		
		//set up test data needed by updateVmState()
		ContentValues cv = new ContentValues();
		for(String field : columns) {
			cv.put(field, sampleText);
		}
		cv.put(Vms.ID, vmid);  //the other fields are arbitrary, only this id field needs to be specific
		Uri uri = getContext().getContentResolver().insert(Vms.META_DATA.CONTENT_URI, cv);
		
		CsRestService csRestService = startCsRestService();
		
		{ //updating to meaningless state and checking whether it took
			final String luffyState = "Gear Third";
			final int numUpdated = csRestService.updateVmState(vmid, luffyState);
			assertEquals(1, numUpdated);
			Cursor c = getContext().getContentResolver().query(uri, null, null, null, null);
			c.moveToFirst();
			for(String field : columns) {
				final String retrievedValue = c.getString(c.getColumnIndex(field));
				if(Vms.ID.equals(field)) {
					assertEquals(vmid, retrievedValue);
				} else if(Vms.STATE.equals(field)) {
					assertEquals(luffyState, retrievedValue);
				} else {
					assertEquals(sampleText, retrievedValue);
				}
			}
		}

		{ //updating again to meaningless state and checking whether it took
			final String roronoaState = "Oni giri";
			final int numUpdated = csRestService.updateVmState(vmid, roronoaState);
			assertEquals(1, numUpdated);
			Cursor c = getContext().getContentResolver().query(uri, null, null, null, null);
			c.moveToFirst();
			for(String field : columns) {
				final String retrievedValue = c.getString(c.getColumnIndex(field));
				if(Vms.ID.equals(field)) {
					assertEquals(vmid, retrievedValue);
				} else if(Vms.STATE.equals(field)) {
					assertEquals(roronoaState, retrievedValue);
				} else {
					assertEquals(sampleText, retrievedValue);
				}
			}
		}
		
		{ //updating again to no state and checking whether it took
			final int numUpdated = csRestService.updateVmState(vmid, null);
			assertEquals(1, numUpdated);
			Cursor c = getContext().getContentResolver().query(uri, null, null, null, null);
			c.moveToFirst();
			for(String field : columns) {
				final String retrievedValue = c.getString(c.getColumnIndex(field));
				if(Vms.ID.equals(field)) {
					assertEquals(vmid, retrievedValue);
				} else if(Vms.STATE.equals(field)) {
					assertNull("the state field should no longer have a value after this udpate", retrievedValue);
				} else {
					assertEquals(sampleText, retrievedValue);
				}
			}
		}
		
	}
	
	public void testDeleteSnapshotWithId() {
		CsRestService csRestService = startCsRestService();

		{
			//set up test data needed by handleJobresultBasedOnApi()
			final String snapshotId = "0";
			ContentValues snapshotsCv = new ContentValues();
			snapshotsCv.put(Snapshots.ID, snapshotId);  //the other fields are arbitrary, only this id field needs to match the id in the request
			snapshotsCv.put(Snapshots.NAME, "sample snapshot");
			Uri snapshotUri = getContext().getContentResolver().insert(Snapshots.META_DATA.CONTENT_URI, snapshotsCv);

			final int numDeleted = csRestService.deleteSnapshotWithId(snapshotId);
			assertEquals(1, numDeleted);
			Cursor c = getContext().getContentResolver().query(snapshotUri, null, null, null, null);
			assertEquals(0, c.getCount());
		}

		{
			//set up test data needed by handleJobresultBasedOnApi()
			final String snapshotId = "100000000000000000000000000";
			ContentValues snapshotsCv = new ContentValues();
			snapshotsCv.put(Snapshots.ID, snapshotId);  //the other fields are arbitrary, only this id field needs to match the id in the request
			snapshotsCv.put(Snapshots.NAME, "sample snapshot");
			Uri snapshotUri = getContext().getContentResolver().insert(Snapshots.META_DATA.CONTENT_URI, snapshotsCv);
			
			final int numDeleted = csRestService.deleteSnapshotWithId(snapshotId);
			assertEquals(1, numDeleted);
			Cursor c = getContext().getContentResolver().query(snapshotUri, null, null, null, null);
			assertEquals(0, c.getCount());
		}
	}

	public void testDeleteSnapshotWithId_multipleRows() {
		deleteAllData();
		CsRestService csRestService = startCsRestService();
		
		//insert multiple rows of data
		final int numRowsCap = 21;
		for(int i=0; i<=numRowsCap; i++) {
			ContentValues cv = new ContentValues();
			cv.put(Snapshots.ID, String.valueOf(i));
			cv.put(Snapshots.NAME, "sample snapshot "+i);
			getContext().getContentResolver().insert(Snapshots.META_DATA.CONTENT_URI, cv);
		}
		
		//execute deleteSnapshopWithId() on one at a time and check
		//to see that the remaining data still exists at each step
		for(int i=0; i<=numRowsCap; i++) {
			final int numDeleted = csRestService.deleteSnapshotWithId(String.valueOf(i));
			assertEquals(1, numDeleted);
			Cursor c = getContext().getContentResolver().query(Snapshots.META_DATA.CONTENT_URI, null, null, null, null);
			assertEquals("move than 1 row was deleted by deleteSnapshotWithId()!", numRowsCap-i, c.getCount());
			c.moveToFirst();
			for(int k=i+1; k<=numRowsCap; k++) {
				//go through each remaining row and check to see the id what we expect to be undeleted
				final String whereClause = Snapshots.ID+"=?";
				final String[] selectionArgs = new String[] { String.valueOf(k) };
				Cursor specificRow = getContext().getContentResolver().query(Snapshots.META_DATA.CONTENT_URI, null, whereClause, selectionArgs, null);
				assertEquals("row for this specific id was deleted when it shouldn't have been", 1, specificRow.getCount());
			}
		}
	}
	
	public void testExtractParamValueFromUriStr() {
		CsRestService csRestService = startCsRestService();
		
		String testUri1 = "http://192.168.3.11:8080/client/api?response=json&command=listVirtualMachines&account=thsu-account&domainid=2&apiKey=namomNgZ8Qt5DuNFUWf3qpGlQmB4650tY36wFOrhUtrzK13d66qNpttKw52Brj02dbtIHs01y-lCLz1UOzTxVQ&signature=AZW5TbyF8QY07lPWxk0JZyMwFx0%3D";
		assertEquals("json", csRestService.extractParamValueFromUriStr(testUri1, "response"));
		assertEquals("listVirtualMachines", csRestService.extractParamValueFromUriStr(testUri1, "command"));
		assertEquals("thsu-account", csRestService.extractParamValueFromUriStr(testUri1, "account"));
		assertEquals("2", csRestService.extractParamValueFromUriStr(testUri1, "domainid"));
		assertEquals("namomNgZ8Qt5DuNFUWf3qpGlQmB4650tY36wFOrhUtrzK13d66qNpttKw52Brj02dbtIHs01y-lCLz1UOzTxVQ", csRestService.extractParamValueFromUriStr(testUri1, "apiKey"));
		assertEquals("AZW5TbyF8QY07lPWxk0JZyMwFx0%3D", csRestService.extractParamValueFromUriStr(testUri1, "signature"));

		String testUri2 = "response=json&command=deleteSnapshot&id=25&apiKey=cqLtNDMDYAeIZ6ZdZQG2QInyE5Sx4M914eSeb-rsJTewTvcCcGLRMe-zh_IPQQKmcIGJzNBa_UGrLDhS_LEy-g&signature=YA0v%2BrUSF8%2B%2Fubqj7WxvY9iVvSM%3D";
		assertEquals("json", csRestService.extractParamValueFromUriStr(testUri2, "response"));
		assertEquals("deleteSnapshot", csRestService.extractParamValueFromUriStr(testUri2, "command"));
		assertEquals("25", csRestService.extractParamValueFromUriStr(testUri2, "id"));
		assertEquals("cqLtNDMDYAeIZ6ZdZQG2QInyE5Sx4M914eSeb-rsJTewTvcCcGLRMe-zh_IPQQKmcIGJzNBa_UGrLDhS_LEy-g", csRestService.extractParamValueFromUriStr(testUri2, "apiKey"));
		assertEquals("YA0v%2BrUSF8%2B%2Fubqj7WxvY9iVvSM%3D", csRestService.extractParamValueFromUriStr(testUri2, "signature"));

		assertNull(csRestService.extractParamValueFromUriStr(testUri1, "non-existent field"));
		assertNull(csRestService.extractParamValueFromUriStr(null, "response"));
		assertNull(csRestService.extractParamValueFromUriStr(null, null));
	}
	
	public void testparseAndSaveReply_returnedErrorResult() {
		deleteAllData();
		CsRestService csRestService = startCsRestService();
		
		final String errorObjJson = "{\"errorcode\":530,\"errortext\":\"Internal error executing command, please contact your system administrator\"}";
		ObjectMapper om = new ObjectMapper();
		JsonNode dataNode = null;
		try {
			dataNode = om.readTree(errorObjJson);
		} catch (JsonProcessingException e) {
			fail("om.readTree() processing failed!");
			e.printStackTrace();
		} catch (IOException e) {
			fail("om.readTree() failed!");
			e.printStackTrace();
		}
		csRestService.parseAndSaveReply(dataNode.traverse(), Errors.META_DATA.CONTENT_URI, CsRestService.INSERT_DATA);
		
		Cursor c = getContext().getContentResolver().query(Errors.META_DATA.CONTENT_URI, null, null, null, null);
		assertEquals(1, c.getCount());
		c.moveToFirst();
		
		assertEquals("530", c.getString(c.getColumnIndex(Errors.ERRORCODE)));
		assertEquals("Internal error executing command, please contact your system administrator", c.getString(c.getColumnIndex(Errors.ERRORTEXT)));
	}

	public void testParseAndSaveReply_vmList() {
		deleteAllData();
		CsRestService csRestService = startCsRestService();
		
		{ //do the insertion test first
			final String listOfVmsJson = "[  " +
			"{\"id\":2027,\"name\":\"i-39-2027-VM\",\"displayname\":\"i-39-2027-VM\",\"account\":\"iizuka\",\"domainid\":1,\"domain\":\"ROOT\",\"created\":\"2011-09-10T02:49:42-0700\",\"state\":\"Running\",\"haenable\":false,\"zoneid\":1,\"zonename\":\"San Jose\",\"templateid\":259,\"templatename\":\"CentOS 5.3 (64 bit) vSphere\",\"templatedisplaytext\":\"CentOS 5.3 (64 bit) vSphere Password enabled\",\"passwordenabled\":true,\"serviceofferingid\":101,\"serviceofferingname\":\"Smallest Instance\",\"cpunumber\":1,\"cpuspeed\":500,\"memory\":512,\"cpuused\":\"0%\",\"networkkbsread\":0,\"networkkbswrite\":0,\"guestosid\":12,\"rootdeviceid\":0,\"rootdevicetype\":\"NetworkFilesystem\",\"nic\":[{\"id\":2122,\"networkid\":247,\"netmask\":\"255.255.255.0\",\"gateway\":\"10.1.1.1\",\"ipaddress\":\"10.1.1.129\",\"traffictype\":\"Guest\",\"type\":\"Virtual\",\"isdefault\":true}],\"hypervisor\":\"VMware\"} , " +
			"{\"id\":2028,\"name\":\"i-39-2028-VM\",\"displayname\":\"i-39-2028-VM\",\"account\":\"iizuka\",\"domainid\":1,\"domain\":\"ROOT\",\"created\":\"2011-01-11T08:00:42-0700\",\"state\":\"Stopped\",\"haenable\":false,\"zoneid\":1,\"zonename\":\"San Jose\",\"templateid\":300,\"templatename\":\"CentOS 5.4 (128 bit) vSphere\",\"templatedisplaytext\":\"CentOS 5.4 (128 bit) vSphere Password enabled\",\"passwordenabled\":true,\"serviceofferingid\":102,\"serviceofferingname\":\"Smaller Instance\",\"cpunumber\":10,\"cpuspeed\":5000,\"memory\":12,\"cpuused\":\"10%\",\"networkkbsread\":0,\"networkkbswrite\":0,\"guestosid\":12,\"rootdeviceid\":0,\"rootdevicetype\":\"NetworkFilesystem\",\"nic\":[{\"id\":2122,\"networkid\":247,\"netmask\":\"255.255.255.0\",\"gateway\":\"10.1.1.1\",\"ipaddress\":\"10.1.1.130\",\"traffictype\":\"Guest\",\"type\":\"Virtual\",\"isdefault\":true}],\"hypervisor\":\"VMware N'xt\"} , " +
			"{\"id\":2029,\"name\":\"i-39-2029-VM\",\"displayname\":\"i-39-2029-VM\",\"account\":\"iizuka\",\"domainid\":1,\"domain\":\"ROOT\",\"created\":\"2000-11-10T12:52:42-0700\",\"state\":\"Stopping\",\"haenable\":false,\"zoneid\":1,\"zonename\":\"San Jose\",\"templateid\":25,\"templatename\":\"CentOS 5.5 (256 bit) vSphere\",\"templatedisplaytext\":\"CentOS 5.5 (256 bit) vSphere Password enabled\",\"passwordenabled\":true,\"serviceofferingid\":103,\"serviceofferingname\":\"Small Instance\",\"cpunumber\":100,\"cpuspeed\":50000,\"memory\":52,\"cpuused\":\"100%\",\"networkkbsread\":0,\"networkkbswrite\":0,\"guestosid\":12,\"rootdeviceid\":0,\"rootdevicetype\":\"NetworkFilesystem\",\"nic\":[{\"id\":2122,\"networkid\":247,\"netmask\":\"255.255.255.0\",\"gateway\":\"10.1.1.1\",\"ipaddress\":\"10.1.1.131\",\"traffictype\":\"Guest\",\"type\":\"Virtual\",\"isdefault\":true}],\"hypervisor\":\"VMware B'yond\"}   " +
			"]";
			ObjectMapper om = new ObjectMapper();
			JsonNode dataNode = null;
			try {
				dataNode = om.readTree(listOfVmsJson);
			} catch (JsonProcessingException e) {
				fail("om.readTree() processing failed!");
				e.printStackTrace();
			} catch (IOException e) {
				fail("om.readTree() failed!");
				e.printStackTrace();
			}
			dataNode.traverse();
			csRestService.parseAndSaveReply(dataNode.traverse(), Vms.META_DATA.CONTENT_URI, CsRestService.INSERT_DATA);

			Cursor c = getContext().getContentResolver().query(Vms.META_DATA.CONTENT_URI, null, null, null, Vms.ID);
			assertEquals(3, c.getCount());

			{ //the cursor results are ordered by id, so go through and check some fields to make sure they are what we expect
				c.moveToFirst();
				assertEquals("2027", c.getString(c.getColumnIndex(Vms.ID)));
				assertEquals("i-39-2027-VM", c.getString(c.getColumnIndex(Vms.NAME)));
				assertEquals("2011-09-10T02:49:42-0700", c.getString(c.getColumnIndex(Vms.CREATED)));
				assertEquals("San Jose", c.getString(c.getColumnIndex(Vms.ZONENAME)));
				assertEquals("CentOS 5.3 (64 bit) vSphere Password enabled", c.getString(c.getColumnIndex(Vms.TEMPLATEDISPLAYTEXT)));
				assertEquals("1", c.getString(c.getColumnIndex(Vms.CPUNUMBER)));
				assertEquals("500", c.getString(c.getColumnIndex(Vms.CPUSPEED)));
				assertEquals("[{\"id\":2122,\"networkid\":247,\"netmask\":\"255.255.255.0\",\"gateway\":\"10.1.1.1\",\"ipaddress\":\"10.1.1.129\",\"traffictype\":\"Guest\",\"type\":\"Virtual\",\"isdefault\":true}]", c.getString(c.getColumnIndex(Vms.NIC)));
				assertEquals("VMware", c.getString(c.getColumnIndex(Vms.HYPERVISOR)));

				c.moveToNext();
				assertEquals("2028", c.getString(c.getColumnIndex(Vms.ID)));
				assertEquals("i-39-2028-VM", c.getString(c.getColumnIndex(Vms.NAME)));
				assertEquals("2011-01-11T08:00:42-0700", c.getString(c.getColumnIndex(Vms.CREATED)));
				assertEquals("San Jose", c.getString(c.getColumnIndex(Vms.ZONENAME)));
				assertEquals("CentOS 5.4 (128 bit) vSphere Password enabled", c.getString(c.getColumnIndex(Vms.TEMPLATEDISPLAYTEXT)));
				assertEquals("10", c.getString(c.getColumnIndex(Vms.CPUNUMBER)));
				assertEquals("5000", c.getString(c.getColumnIndex(Vms.CPUSPEED)));
				assertEquals("[{\"id\":2122,\"networkid\":247,\"netmask\":\"255.255.255.0\",\"gateway\":\"10.1.1.1\",\"ipaddress\":\"10.1.1.130\",\"traffictype\":\"Guest\",\"type\":\"Virtual\",\"isdefault\":true}]", c.getString(c.getColumnIndex(Vms.NIC)));
				assertEquals("VMware N'xt", c.getString(c.getColumnIndex(Vms.HYPERVISOR)));

				c.moveToNext();
				assertEquals("2029", c.getString(c.getColumnIndex(Vms.ID)));
				assertEquals("i-39-2029-VM", c.getString(c.getColumnIndex(Vms.NAME)));
				assertEquals("2000-11-10T12:52:42-0700", c.getString(c.getColumnIndex(Vms.CREATED)));
				assertEquals("San Jose", c.getString(c.getColumnIndex(Vms.ZONENAME)));
				assertEquals("CentOS 5.5 (256 bit) vSphere Password enabled", c.getString(c.getColumnIndex(Vms.TEMPLATEDISPLAYTEXT)));
				assertEquals("100", c.getString(c.getColumnIndex(Vms.CPUNUMBER)));
				assertEquals("50000", c.getString(c.getColumnIndex(Vms.CPUSPEED)));
				assertEquals("[{\"id\":2122,\"networkid\":247,\"netmask\":\"255.255.255.0\",\"gateway\":\"10.1.1.1\",\"ipaddress\":\"10.1.1.131\",\"traffictype\":\"Guest\",\"type\":\"Virtual\",\"isdefault\":true}]", c.getString(c.getColumnIndex(Vms.NIC)));
				assertEquals("VMware B'yond", c.getString(c.getColumnIndex(Vms.HYPERVISOR)));
			}
		}
		
		{ //after vms have been inserted above, now do the update tests
			final String listOfUpdatedVmsJson = "[  " +
				"{\"id\":2027,\"name\":\"i-39-2027-VM edited!\",\"displayname\":\"i-39-2027-VM\",\"account\":\"iizuka\",\"domainid\":1,\"domain\":\"ROOT\",\"created\":\"2011-09-10T02:49:42-0700\",\"state\":\"Unknown\",\"haenable\":false,\"zoneid\":1,\"zonename\":\"San Jose\",\"templateid\":259,\"templatename\":\"CentOS 5.3 (64 bit) vSphere\",\"templatedisplaytext\":\"CentOS 5.3 (64 bit) vSphere Password enabled\",\"passwordenabled\":true,\"serviceofferingid\":101,\"serviceofferingname\":\"Smallest Instance\",\"cpunumber\":2,\"cpuspeed\":2500,\"memory\":512,\"cpuused\":\"0%\",\"networkkbsread\":0,\"networkkbswrite\":0,\"guestosid\":12,\"rootdeviceid\":0,\"rootdevicetype\":\"NetworkFilesystem\",\"nic\":[{\"id\":2122,\"networkid\":247,\"netmask\":\"255.255.255.0\",\"gateway\":\"10.1.1.1\",\"ipaddress\":\"10.1.1.129\",\"traffictype\":\"Guest\",\"type\":\"Virtual\",\"isdefault\":true}],\"hypervisor\":\"VMware\"} , " +
				"{\"id\":2028,\"name\":\"i-39-2028-VM edited!\",\"displayname\":\"i-39-2028-VM\",\"account\":\"iizuka\",\"domainid\":1,\"domain\":\"ROOT\",\"created\":\"2011-01-11T08:00:42-0700\",\"state\":\"Unknown\",\"haenable\":false,\"zoneid\":1,\"zonename\":\"San Jose\",\"templateid\":300,\"templatename\":\"CentOS 5.4 (128 bit) vSphere\",\"templatedisplaytext\":\"CentOS 5.4 (128 bit) vSphere Password enabled\",\"passwordenabled\":true,\"serviceofferingid\":102,\"serviceofferingname\":\"Smaller Instance\",\"cpunumber\":20,\"cpuspeed\":25000,\"memory\":12,\"cpuused\":\"10%\",\"networkkbsread\":0,\"networkkbswrite\":0,\"guestosid\":12,\"rootdeviceid\":0,\"rootdevicetype\":\"NetworkFilesystem\",\"nic\":[{\"id\":2122,\"networkid\":247,\"netmask\":\"255.255.255.0\",\"gateway\":\"10.1.1.1\",\"ipaddress\":\"10.1.1.130\",\"traffictype\":\"Guest\",\"type\":\"Virtual\",\"isdefault\":true}],\"hypervisor\":\"VMware N'xt\"} , " +
				"{\"id\":2029,\"name\":\"i-39-2029-VM edited!\",\"displayname\":\"i-39-2029-VM\",\"account\":\"iizuka\",\"domainid\":1,\"domain\":\"ROOT\",\"created\":\"2000-11-10T12:52:42-0700\",\"state\":\"Unknown\",\"haenable\":false,\"zoneid\":1,\"zonename\":\"San Jose\",\"templateid\":25,\"templatename\":\"CentOS 5.5 (256 bit) vSphere\",\"templatedisplaytext\":\"CentOS 5.5 (256 bit) vSphere Password enabled\",\"passwordenabled\":true,\"serviceofferingid\":103,\"serviceofferingname\":\"Small Instance\",\"cpunumber\":200,\"cpuspeed\":250000,\"memory\":52,\"cpuused\":\"100%\",\"networkkbsread\":0,\"networkkbswrite\":0,\"guestosid\":12,\"rootdeviceid\":0,\"rootdevicetype\":\"NetworkFilesystem\",\"nic\":[{\"id\":2122,\"networkid\":247,\"netmask\":\"255.255.255.0\",\"gateway\":\"10.1.1.1\",\"ipaddress\":\"10.1.1.131\",\"traffictype\":\"Guest\",\"type\":\"Virtual\",\"isdefault\":true}],\"hypervisor\":\"VMware B'yond\"}   " +
				"]";
			
			ObjectMapper om = new ObjectMapper();
			JsonNode dataNode = null;
			try {
				dataNode = om.readTree(listOfUpdatedVmsJson);
			} catch (JsonProcessingException e) {
				fail("om.readTree() processing failed!");
				e.printStackTrace();
			} catch (IOException e) {
				fail("om.readTree() failed!");
				e.printStackTrace();
			}
			dataNode.traverse();
			csRestService.parseAndSaveReply(dataNode.traverse(), Vms.META_DATA.CONTENT_URI, CsRestService.UPDATE_DATA_WITH_ID);
			
			Cursor c = getContext().getContentResolver().query(Vms.META_DATA.CONTENT_URI, null, null, null, Vms.ID);
			assertEquals(3, c.getCount());

			{ //the cursor results are ordered by id, so go through and check some fields to make sure they are what we expect
				c.moveToFirst();
				assertEquals("2027", c.getString(c.getColumnIndex(Vms.ID)));
				assertEquals("i-39-2027-VM edited!", c.getString(c.getColumnIndex(Vms.NAME)));
				assertEquals("2011-09-10T02:49:42-0700", c.getString(c.getColumnIndex(Vms.CREATED)));
				assertEquals("San Jose", c.getString(c.getColumnIndex(Vms.ZONENAME)));
				assertEquals("CentOS 5.3 (64 bit) vSphere Password enabled", c.getString(c.getColumnIndex(Vms.TEMPLATEDISPLAYTEXT)));
				assertEquals("2", c.getString(c.getColumnIndex(Vms.CPUNUMBER)));
				assertEquals("2500", c.getString(c.getColumnIndex(Vms.CPUSPEED)));
				assertEquals("[{\"id\":2122,\"networkid\":247,\"netmask\":\"255.255.255.0\",\"gateway\":\"10.1.1.1\",\"ipaddress\":\"10.1.1.129\",\"traffictype\":\"Guest\",\"type\":\"Virtual\",\"isdefault\":true}]", c.getString(c.getColumnIndex(Vms.NIC)));
				assertEquals("VMware", c.getString(c.getColumnIndex(Vms.HYPERVISOR)));
				assertEquals("Unknown", c.getString(c.getColumnIndex(Vms.STATE)));

				c.moveToNext();
				assertEquals("2028", c.getString(c.getColumnIndex(Vms.ID)));
				assertEquals("i-39-2028-VM edited!", c.getString(c.getColumnIndex(Vms.NAME)));
				assertEquals("2011-01-11T08:00:42-0700", c.getString(c.getColumnIndex(Vms.CREATED)));
				assertEquals("San Jose", c.getString(c.getColumnIndex(Vms.ZONENAME)));
				assertEquals("CentOS 5.4 (128 bit) vSphere Password enabled", c.getString(c.getColumnIndex(Vms.TEMPLATEDISPLAYTEXT)));
				assertEquals("20", c.getString(c.getColumnIndex(Vms.CPUNUMBER)));
				assertEquals("25000", c.getString(c.getColumnIndex(Vms.CPUSPEED)));
				assertEquals("[{\"id\":2122,\"networkid\":247,\"netmask\":\"255.255.255.0\",\"gateway\":\"10.1.1.1\",\"ipaddress\":\"10.1.1.130\",\"traffictype\":\"Guest\",\"type\":\"Virtual\",\"isdefault\":true}]", c.getString(c.getColumnIndex(Vms.NIC)));
				assertEquals("VMware N'xt", c.getString(c.getColumnIndex(Vms.HYPERVISOR)));
				assertEquals("Unknown", c.getString(c.getColumnIndex(Vms.STATE)));

				c.moveToNext();
				assertEquals("2029", c.getString(c.getColumnIndex(Vms.ID)));
				assertEquals("i-39-2029-VM edited!", c.getString(c.getColumnIndex(Vms.NAME)));
				assertEquals("2000-11-10T12:52:42-0700", c.getString(c.getColumnIndex(Vms.CREATED)));
				assertEquals("San Jose", c.getString(c.getColumnIndex(Vms.ZONENAME)));
				assertEquals("CentOS 5.5 (256 bit) vSphere Password enabled", c.getString(c.getColumnIndex(Vms.TEMPLATEDISPLAYTEXT)));
				assertEquals("200", c.getString(c.getColumnIndex(Vms.CPUNUMBER)));
				assertEquals("250000", c.getString(c.getColumnIndex(Vms.CPUSPEED)));
				assertEquals("[{\"id\":2122,\"networkid\":247,\"netmask\":\"255.255.255.0\",\"gateway\":\"10.1.1.1\",\"ipaddress\":\"10.1.1.131\",\"traffictype\":\"Guest\",\"type\":\"Virtual\",\"isdefault\":true}]", c.getString(c.getColumnIndex(Vms.NIC)));
				assertEquals("VMware B'yond", c.getString(c.getColumnIndex(Vms.HYPERVISOR)));
				assertEquals("Unknown", c.getString(c.getColumnIndex(Vms.STATE)));
			}
		}
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
