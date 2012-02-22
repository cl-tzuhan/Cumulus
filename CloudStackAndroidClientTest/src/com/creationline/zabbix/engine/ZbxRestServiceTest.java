package com.creationline.zabbix.engine;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.test.ServiceTestCase;

import com.creationline.cloudstack.CloudStackAndroidClient;
import com.creationline.cloudstack.mock.CsacMockApplication;
import com.creationline.common.engine.RestServiceBase;
import com.creationline.zabbix.engine.api.HostGetRequest;
import com.creationline.zabbix.engine.api.ZbxApiConstants;
import com.creationline.zabbix.engine.api.ZbxApiRequest;
import com.creationline.zabbix.engine.db.Transactions;

public class ZbxRestServiceTest extends ServiceTestCase<ZbxRestService> {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		setApplication(new CsacMockApplication());
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public ZbxRestServiceTest() {
		super(ZbxRestService.class);
	}
	
	private ZbxRestService startZbxRestService() {
		Bundle emptyBundle = new Bundle();
        Intent zbxRestServiceIntent = ZbxRestService.createZbxRestServiceIntent(getContext(), emptyBundle);  //kicking ZbxRestService with empty payload causes it to start-up and do nothing
        startService(zbxRestServiceIntent);

		return getService();
	}
	
	public void testCreateGetCpuLoadDataIntent() {
		final String sampleIp = "!&()0=~POIUYTREFDS>GSATGRDSFpoiueytredsloiu' IP";
		final String sampleHost = "!&()0=~POIUYTREFDS>GSATGRDSFpoiueytredsloiu' host";
		final String[] sampleItemNames = new String[] { "North Blue", "East Blue", "West Blue", "South Blue", "Redline", "New World" };
		final String sampleTargetContenUri = "!&()0=~POIUYTREFDS>GSATGRDSFpoiueytredsloiu' target uri";
		
		Intent getDataIntent = ZbxRestService.createGetDataIntent(getContext(), sampleIp, sampleHost, sampleItemNames, sampleTargetContenUri);
		Bundle payload = getDataIntent.getExtras();

		{
			final String actionId = payload.getString(RestServiceBase.PAYLOAD_FIELDS.ACTION_ID);
			assertEquals(ZbxRestService.ACTIONS.BROADCAST_NOTIF, actionId);

			final String intentFilter = payload.getString(ZbxRestService.PAYLOAD_FIELDS.INTENT_FILTER);
			assertEquals(ZbxRestService.INTENT_ACTION.ZBXRESTSERVICE_BROADCAST, intentFilter);

			final int callStatus = payload.getInt(ZbxRestService.CALL_STATUS);
			assertEquals(ZbxRestService.CALL_STATUS_VALUES.CALL_STARTED, callStatus);
		}

		{
			Bundle followupPayload = payload.getBundle(ZbxRestService.PAYLOAD_FIELDS.FOLLOWUP_ACTION);

			final String retreivedApiCmd = followupPayload.getString(RestServiceBase.PAYLOAD_FIELDS.API_CMD);
			assertEquals(ZbxApiConstants.API.HOST.GET, retreivedApiCmd);

			final String retreivedIp = followupPayload.getString(ZbxApiConstants.FIELDS.IP);
			assertEquals(sampleIp, retreivedIp);
			
			final String retreivedHost = followupPayload.getString(ZbxApiConstants.FIELDS.HOST);
			assertEquals(sampleHost, retreivedHost);
			
			final String[] retreivedItemNames = followupPayload.getStringArray(ZbxRestService.PAYLOAD_FIELDS.ITEM_NAME_ARRAY);
			assertEquals(sampleItemNames, retreivedItemNames);
			
			final String retreivedTargetContentUri = followupPayload.getString(ZbxRestService.PAYLOAD_FIELDS.TARGET_CONTENT_URI);
			assertEquals(sampleTargetContenUri, retreivedTargetContentUri);

			final Bundle retreivedFollowupAction = followupPayload.getBundle(ZbxRestService.PAYLOAD_FIELDS.FOLLOWUP_ACTION);
			assertNull("no followup action should be set", retreivedFollowupAction);
		}
	}
	
	public void testCreateActionPayload_basic() {
		final String hostIp = "some host IP sample";
		final String hostName = "some host sample";
		final String[] itemNames = new String[] { "sample item 1", "sample item 2", "sample item 3", "sample item 4", "sample item 5" };
		final String targetContenUri = "some sample target content uri";
		final String followupAction = "some sample followup action";
		Bundle followupBundle = ZbxApiRequest.createActionPayload(getContext(), followupAction, null);
		Bundle testBundle = HostGetRequest.createHostGetActionPayload(getContext(), hostIp, hostName, itemNames, targetContenUri, followupBundle);
		
		final String retrievedActionId = testBundle.getString(RestServiceBase.PAYLOAD_FIELDS.ACTION_ID);
		assertEquals(ZbxRestService.ACTIONS.CALL_ZBX_API, retrievedActionId);

		final String retrievedApiCmd = testBundle.getString((RestServiceBase.PAYLOAD_FIELDS.API_CMD));
		assertEquals(ZbxApiConstants.API.HOST.GET, retrievedApiCmd);
		
		final String retrievedHostIp = testBundle.getString(ZbxApiConstants.FIELDS.IP);
		assertEquals(hostIp, retrievedHostIp);
		
		final String retrievedHostName = testBundle.getString(ZbxApiConstants.FIELDS.HOST);
		assertEquals(hostName, retrievedHostName);
		
		final String[] retrievedItemNameArray = testBundle.getStringArray(ZbxRestService.PAYLOAD_FIELDS.ITEM_NAME_ARRAY);
		for(int i=0; i<itemNames.length; i++) {
			assertEquals(itemNames[i], retrievedItemNameArray[i]);
		}
		
		final String retrievedTargetContenUri = testBundle.getString(ZbxRestService.PAYLOAD_FIELDS.TARGET_CONTENT_URI);
		assertEquals(targetContenUri, retrievedTargetContenUri);
		
		Bundle retrievedFollowupBundle = testBundle.getBundle(ZbxRestService.PAYLOAD_FIELDS.FOLLOWUP_ACTION);
		final String retrievedFollowupApiCmd = retrievedFollowupBundle.getString((RestServiceBase.PAYLOAD_FIELDS.API_CMD));
		assertEquals(followupAction, retrievedFollowupApiCmd);
	}

	public void testCreateActionPayload_empty() {
		Bundle testBundle = HostGetRequest.createHostGetActionPayload(getContext(), null, null, null, null, null);
		
		final String retrievedActionId = testBundle.getString(RestServiceBase.PAYLOAD_FIELDS.ACTION_ID);
		assertEquals(ZbxRestService.ACTIONS.CALL_ZBX_API, retrievedActionId);
		
		final String retrievedApiCmd = testBundle.getString(RestServiceBase.PAYLOAD_FIELDS.API_CMD);
		assertEquals(ZbxApiConstants.API.HOST.GET, retrievedApiCmd);
		
		final String retrievedHostIp = testBundle.getString(ZbxApiConstants.FIELDS.IP);
		assertNull(retrievedHostIp);
		
		final String retrievedHostName = testBundle.getString(ZbxApiConstants.FIELDS.HOST);
		assertNull(retrievedHostName);
		
		final String[] retrievedItemNameArray = testBundle.getStringArray(ZbxRestService.PAYLOAD_FIELDS.ITEM_NAME_ARRAY);
		assertNull(retrievedItemNameArray);
		
		final String retrievedTargetContenUri = testBundle.getString(ZbxRestService.PAYLOAD_FIELDS.TARGET_CONTENT_URI);
		assertNull(retrievedTargetContenUri);
		
		Bundle retrievedFollowupBundle = testBundle.getBundle(ZbxRestService.PAYLOAD_FIELDS.FOLLOWUP_ACTION);
		assertNull(retrievedFollowupBundle);
	}
	

	public void testCreateZbxRestServiceIntent1() {
		final String apiCmd = "1234567890-^qwp@[asd:]zxcvbn,.";
		final String followupAction = "followup 1234567890-^qwp@[asd:]zxcvbn,.";
		Bundle followupBundle = ZbxApiRequest.createActionPayload(getContext(), followupAction, null);
		
		Bundle payload = new Bundle();
        payload.putString(RestServiceBase.PAYLOAD_FIELDS.API_CMD, apiCmd);
        payload.putBundle(ZbxRestService.PAYLOAD_FIELDS.FOLLOWUP_ACTION, followupBundle);
        
        Intent createdIntent = ZbxRestService.createZbxRestServiceIntent(getContext(), payload);
        
        Bundle retrievedPayload = createdIntent.getExtras();
        final String retrievedApiCmd = retrievedPayload.getString((RestServiceBase.PAYLOAD_FIELDS.API_CMD));
		
		assertEquals(apiCmd, retrievedApiCmd);
		
		Bundle retrievedFollowupBundle = retrievedPayload.getBundle(ZbxRestService.PAYLOAD_FIELDS.FOLLOWUP_ACTION);
		final String retrievedFollowupApiCmd = retrievedFollowupBundle.getString((RestServiceBase.PAYLOAD_FIELDS.API_CMD));
		assertEquals(followupAction, retrievedFollowupApiCmd);
	}

	public void testCreateZbxRestServiceIntent2() {
		final String action = "1234567890-^qwp@[asd:]zxcvbn,.";
		final String followupHostIp = "followup !_?<KOP`)0=WEIOPASD+*ZXCVB<>IP";
		final String followupHostName = "followup !_?<KOP`)0=WEIOPASD+*ZXCVB<>Hostname";
		final String[] followupItemNames = new String[] { "followup !_?<KOP`)0=WEIOPASD+*ZXCVB<> item 1",
												  "followup !_?<KOP`)0=WEIOPASD+*ZXCVB<> item 2",
												  "followup !_?<KOP`)0=WEIOPASD+*ZXCVB<> item 3",
												  "followup !_?<KOP`)0=WEIOPASD+*ZXCVB<> item 4",
												  "followup !_?<KOP`)0=WEIOPASD+*ZXCVB<> item 5",
												  "followup !_?<KOP`)0=WEIOPASD+*ZXCVB<> item 6",
												  "followup !_?<KOP`)0=WEIOPASD+*ZXCVB<> item 7",
												  "followup !_?<KOP`)0=WEIOPASD+*ZXCVB<> item 8",
												  };
		final String followupTargetContenUri = "sfollowup !_?<KOP`)0=WEIOPASD+*ZXCVB<> content uri";
		Bundle followupBundle = HostGetRequest.createHostGetActionPayload(getContext(),
																		  followupHostIp,
																		  followupHostName,
																		  followupItemNames,
																		  followupTargetContenUri,
																		  null);
		
		Bundle createdActionPayload = ZbxApiRequest.createActionPayload(getContext(), action, followupBundle);
		Intent createdIntent = ZbxRestService.createZbxRestServiceIntent(getContext(), createdActionPayload);
		
		Bundle retrievedPayload = createdIntent.getExtras();
		final String retrievedApiCmd = retrievedPayload.getString((RestServiceBase.PAYLOAD_FIELDS.API_CMD));
		assertEquals(action, retrievedApiCmd);
		
		Bundle retrievedFollowupBundle = retrievedPayload.getBundle(ZbxRestService.PAYLOAD_FIELDS.FOLLOWUP_ACTION);
		final String retrievedFollowupApiCmd = retrievedFollowupBundle.getString((RestServiceBase.PAYLOAD_FIELDS.API_CMD));
		final String retrievedFollowupHostIp = retrievedFollowupBundle.getString((ZbxApiConstants.FIELDS.IP));
		assertEquals(ZbxApiConstants.API.HOST.GET, retrievedFollowupApiCmd);
		assertEquals(followupHostIp, retrievedFollowupHostIp);
	}
	
	public void testCreateZbxRestServiceIntent3() {
		final String hostIp = "!_?<KOP`)0=WEIOPASD+*ZXCVB<>--IP";
		final String hostName = "!_?<KOP`)0=WEIOPASD+*ZXCVB<>--HostName";
		final String[] itemNames = new String[] { "!_?<KOP`)0=WEIOPASD+*ZXCVB<>--item 1",
												  };
		final String targetContenUri = "!_?<KOP`)0=WEIOPASD+*ZXCVB<>--content uri";
		final String followupAction = "followup 1234567890-^qwp@[asd:]zxcvbn,.";
		Bundle followupBundle = ZbxApiRequest.createActionPayload(getContext(), followupAction, null);
		
        Bundle createdActionPayload = HostGetRequest.createHostGetActionPayload(getContext(), hostIp, hostName, itemNames, targetContenUri, followupBundle);
		Intent createdIntent = ZbxRestService.createZbxRestServiceIntent(getContext(), createdActionPayload);
        
        Bundle retrievedPayload = createdIntent.getExtras();
        final String retrievedApiCmd = retrievedPayload.getString((RestServiceBase.PAYLOAD_FIELDS.API_CMD));
		final String retrievedHostIp = retrievedPayload.getString((ZbxApiConstants.FIELDS.IP));
		assertEquals(ZbxApiConstants.API.HOST.GET, retrievedApiCmd);
		assertEquals(hostIp, retrievedHostIp);
		
		Bundle retrievedFollowupBundle = retrievedPayload.getBundle(ZbxRestService.PAYLOAD_FIELDS.FOLLOWUP_ACTION);
		final String retrievedFollowupApiCmd = retrievedFollowupBundle.getString((RestServiceBase.PAYLOAD_FIELDS.API_CMD));
		assertEquals(followupAction, retrievedFollowupApiCmd);
	}
	
	public void testGetSavedAuthToken() {
		ZbxRestService zbxRestService = startZbxRestService();
		final String sampleAuthToken = "There is a difference between using three swords... and using Santouryuu!";
		
		SharedPreferences preferences = getContext().getSharedPreferences(CloudStackAndroidClient.SHARED_PREFERENCES.PREFERENCES_NAME, Context.MODE_PRIVATE);
		final String originalAuthToken = preferences.getString(CloudStackAndroidClient.SHARED_PREFERENCES.ZABBIX_AUTH_TOKEN, null);
		SharedPreferences.Editor editor = preferences.edit();
		{
			editor.putString(CloudStackAndroidClient.SHARED_PREFERENCES.ZABBIX_AUTH_TOKEN, sampleAuthToken);
			editor.commit();

			final String retrievedAuthToken = zbxRestService.getSavedAuthToken();
			assertEquals(sampleAuthToken, retrievedAuthToken);
		}

		{
			editor.remove(CloudStackAndroidClient.SHARED_PREFERENCES.ZABBIX_AUTH_TOKEN);
			editor.commit();

			final String retrievedAuthToken = zbxRestService.getSavedAuthToken();
			assertNull("There should be no saved auth token", retrievedAuthToken);
		}
		editor.putString(CloudStackAndroidClient.SHARED_PREFERENCES.ZABBIX_AUTH_TOKEN, originalAuthToken);
		editor.commit();
	}
	
	public void testSaveReplyToDb() {
		ZbxRestService zbxRestService = startZbxRestService();

		final String sampleRequest = "Some random sample request";
		final String sampleStatus = "Some random sample status";
		ContentValues cv = new ContentValues();
		cv.put(Transactions.REQUEST, sampleRequest);
		cv.put(Transactions.STATUS, sampleStatus);
		final Uri testRow = getContext().getContentResolver().insert(Transactions.META_DATA.CONTENT_URI, cv);
		
		{
			zbxRestService.saveReplyToDb(null, null, null);  //calling saveReplyToDb with all null should do nothing to the saved row

			final String[] columns = new String[] {
					Transactions.REQUEST,
					Transactions.REPLY_DATETIME,
					Transactions.STATUS,
			};
			Cursor c = getContext().getContentResolver().query(testRow, columns, null, null, null);
			final String retrievedRequest = c.getString(c.getColumnIndex(Transactions.REQUEST));
			final String retrievedReplyDateTime = c.getString(c.getColumnIndex(Transactions.REPLY_DATETIME));
			final String retrievedStatus = c.getString(c.getColumnIndex(Transactions.STATUS));
			c.close();

			assertEquals(sampleRequest, retrievedRequest);
			assertNull(retrievedReplyDateTime);
			assertEquals(sampleStatus, retrievedStatus);
		}

		{
			zbxRestService.saveReplyToDb(testRow, null, null);  //calling saveReplyToDb with null HttpResponse should result in row marked as aborted

			final String[] columns = new String[] {
					Transactions.REQUEST,
					Transactions.REPLY_DATETIME,
					Transactions.STATUS,
			};
			Cursor c = getContext().getContentResolver().query(testRow, columns, null, null, null);
			final String retrievedRequest = c.getString(c.getColumnIndex(Transactions.REQUEST));
			final String retrievedReplyDateTime = c.getString(c.getColumnIndex(Transactions.REPLY_DATETIME));
			final String retrievedStatus = c.getString(c.getColumnIndex(Transactions.STATUS));
			c.close();

			assertEquals(sampleRequest, retrievedRequest);
			assertNotNull("When a row is marked as aborted, the reply_datetime should be inserted automatically", retrievedReplyDateTime);
			assertEquals(Transactions.STATUS_VALUES.ABORTED, retrievedStatus);
		}
		
	}
	
	
	public void testUpdateCallWithReplyOnDb() {
		ZbxRestService zbxRestService = startZbxRestService();

		final String sampleRequest = "Some random sample request";
		final String sampleStatus = "Some random sample status";
		ContentValues cv = new ContentValues();
		cv.put(Transactions.REQUEST, sampleRequest);
		cv.put(Transactions.STATUS, sampleStatus);
		final Uri testRow = getContext().getContentResolver().insert(Transactions.META_DATA.CONTENT_URI, cv);

		final String updatedSampleStatus = "Some random updated sample status";
		final StringBuilder replyBodyText = new StringBuilder("{\"jsonrpc\":\"2.0\",\"result\":[{\"maintenances\":[{\"maintenanceid\":\"0\"}],\"hostid\":\"10017\",\"proxy_hostid\":\"0\",\"host\":\"Zabbix server\",\"dns\":\"\",\"useip\":\"1\",\"ip\":\"127.0.0.1\",\"port\":\"10050\",\"status\":\"0\",\"disable_until\":\"0\",\"error\":\"\",\"available\":\"1\",\"errors_from\":\"0\",\"lastaccess\":\"0\",\"inbytes\":\"0\",\"outbytes\":\"0\",\"useipmi\":\"0\",\"ipmi_port\":\"623\",\"ipmi_authtype\":\"0\",\"ipmi_privilege\":\"2\",\"ipmi_username\":\"\",\"ipmi_password\":\"\",\"ipmi_disable_until\":\"0\",\"ipmi_available\":\"0\",\"snmp_disable_until\":\"0\",\"snmp_available\":\"0\",\"maintenanceid\":\"0\",\"maintenance_status\":\"0\",\"maintenance_type\":\"0\",\"maintenance_from\":\"0\",\"ipmi_ip\":\"\",\"ipmi_errors_from\":\"0\",\"snmp_errors_from\":\"0\",\"ipmi_error\":\"\",\"snmp_error\":\"\"}],\"id\":\"21\"}");
		zbxRestService.updateCallWithReplyOnDb(testRow, updatedSampleStatus, replyBodyText);

		{
			final String[] columns = new String[] {
					Transactions.REQUEST,
					Transactions.REPLY_DATETIME,
					Transactions.STATUS,
					Transactions.REPLY,
			};
			Cursor c = getContext().getContentResolver().query(testRow, columns, null, null, null);
			final String retrievedRequest = c.getString(c.getColumnIndex(Transactions.REQUEST));
			final String retrievedReplyDateTime = c.getString(c.getColumnIndex(Transactions.REPLY_DATETIME));
			final String retrievedStatus = c.getString(c.getColumnIndex(Transactions.STATUS));
			final String retrievedReply = c.getString(c.getColumnIndex(Transactions.REPLY));
			c.close();

			assertEquals(sampleRequest, retrievedRequest);
			assertNotNull("When a row is marked as aborted, the reply_datetime should be inserted automatically", retrievedReplyDateTime);
			assertEquals(updatedSampleStatus, retrievedStatus);
			if(replyBodyText.length()<ZbxRestService.MAX_LENGTH_OF_STRING_TO_SAVE) {
				assertEquals(replyBodyText.toString(), retrievedReply);
			} else {
				final String truncatedReplyBodyText = replyBodyText.substring(0, ZbxRestService.MAX_LENGTH_OF_STRING_TO_SAVE)+"...";
				assertEquals(truncatedReplyBodyText, retrievedReply);
			}
		}

	}
		
	public void testUpdateCallWithReplyOnDbShouldTruncatedVeryLongReplies() {
		ZbxRestService zbxRestService = startZbxRestService();

		final String sampleRequest = "Some random sample request";
		final String sampleStatus = "Some random sample status";
		ContentValues cv = new ContentValues();
		cv.put(Transactions.REQUEST, sampleRequest);
		cv.put(Transactions.STATUS, sampleStatus);
		final Uri testRow = getContext().getContentResolver().insert(Transactions.META_DATA.CONTENT_URI, cv);

		final String updatedSampleStatus = "Some random updated sample status";
		final StringBuilder replyBodyText = new StringBuilder("{\"jsonrpc\":\"2.0\",\"result\":[{\"itemid\":\"18468\",\"clock\":\"1327683663\",\"value\":\"0.0500\"},{\"itemid\":\"18468\",\"clock\":\"1327683668\",\"value\":\"0.0500\"},{\"itemid\":\"18468\",\"clock\":\"1327683673\",\"value\":\"0.0500\"},{\"itemid\":\"18468\",\"clock\":\"1327683678\",\"value\":\"0.0400\"},{\"itemid\":\"18468\",\"clock\":\"1327683683\",\"value\":\"0.0400\"},{\"itemid\":\"18468\",\"clock\":\"1327683688\",\"value\":\"0.0400\"},{\"itemid\":\"18468\",\"clock\":\"1327683693\",\"value\":\"0.0300\"},{\"itemid\":\"18468\",\"clock\":\"1327683698\",\"value\":\"0.0300\"},{\"itemid\":\"18468\",\"clock\":\"1327683703\",\"value\":\"0.0300\"},{\"itemid\":\"18468\",\"clock\":\"1327683708\",\"value\":\"0.0300\"},{\"itemid\":\"18468\",\"clock\":\"1327683713\",\"value\":\"0.0200\"},{\"itemid\":\"18468\",\"clock\":\"1327683718\",\"value\":\"0.0200\"},{\"itemid\":\"18468\",\"clock\":\"1327683723\",\"value\":\"0.0200\"},{\"itemid\":\"18468\",\"clock\":\"1327683728\",\"value\":\"0.0200\"},{\"itemid\":\"18468\",\"clock\":\"1327683733\",\"value\":\"0.0200\"},{\"itemid\":\"18468\",\"clock\":\"1327683738\",\"value\":\"0.0200\"},{\"itemid\":\"18468\",\"clock\":\"1327683743\",\"value\":\"0.0100\"},{\"itemid\":\"18468\",\"clock\":\"1327683748\",\"value\":\"0.0100\"},{\"itemid\":\"18468\",\"clock\":\"1327683753\",\"value\":\"0.0100\"},{\"itemid\":\"18468\",\"clock\":\"1327683758\",\"value\":\"0.0100\"},{\"itemid\":\"18468\",\"clock\":\"1327683763\",\"value\":\"0.0100\"},{\"itemid\":\"18468\",\"clock\":\"1327683768\",\"value\":\"0.0100\"},{\"itemid\":\"18468\",\"clock\":\"1327683773\",\"value\":\"0.0100\"},{\"itemid\":\"18468\",\"clock\":\"1327683778\",\"value\":\"0.0100\"},{\"itemid\":\"18468\",\"clock\":\"1327683783\",\"value\":\"0.0100\"},{\"itemid\":\"18468\",\"clock\":\"1327683788\",\"value\":\"0.0100\"},{\"itemid\":\"18468\",\"clock\":\"1327683793\",\"value\":\"0.0100\"},{\"itemid\":\"18468\",\"clock\":\"1327683798\",\"value\":\"0.0100\"},{\"itemid\":\"18468\",\"clock\":\"1327683803\",\"value\":\"0.0100\"},{\"itemid\":\"18468\",\"clock\":\"1327683808\",\"value\":\"0.0000\"},{\"itemid\":\"18468\",\"clock\":\"1327683813\",\"value\":\"0.0000\"},{\"itemid\":\"18468\",\"clock\":\"1327683818\",\"value\":\"0.0000\"},{\"itemid\":\"18468\",\"clock\":\"1327683823\",\"value\":\"0.0000\"},{\"itemid\":\"18468\",\"clock\":\"1327683828\",\"value\":\"0.0000\"},{\"itemid\":\"18468\",\"clock\":\"1327683833\",\"value\":\"0.0000\"},{\"itemid\":\"18468\",\"clock\":\"1327683838\",\"value\":\"0.0000\"},{\"itemid\":\"18468\",\"clock\":\"1327683843\",\"value\":\"0.0000\"},{\"itemid\":\"18468\",\"clock\":\"1327683848\",\"value\":\"0.0000\"},{\"itemid\":\"18468\",\"clock\":\"1327683853\",\"value\":\"0.0000\"},{\"itemid\":\"18468\",\"clock\":\"1327683858\",\"value\":\"0.0000\"},{\"itemid\":\"18468\",\"clock\":\"1327683863\",\"value\":\"0.0000\"},{\"itemid\":\"18468\",\"clock\":\"1327683868\",\"value\":\"0.0000\"},{\"itemid\":\"18468\",\"clock\":\"1327683873\",\"value\":\"0.0000\"},{\"itemid\":\"18468\",\"clock\":\"1327683878\",\"value\":\"0.0000\"},{\"itemid\":\"18468\",\"clock\":\"1327683883\",\"value\":\"0.0000\"},{\"itemid\":\"18468\",\"clock\":\"1327683888\",\"value\":\"0.0000\"},{\"itemid\":\"18468\",\"clock\":\"1327683893\",\"value\":\"0.0000\"},{\"itemid\":\"18468\",\"clock\":\"1327683898\",\"value\":\"0.0000\"},{\"itemid\":\"18468\",\"clock\":\"1327683903\",\"value\":\"0.0000\"},{\"itemid\":\"18468\",\"clock\":\"1327683908\",\"value\":\"0.0000\"},{\"itemid\":\"18468\",\"clock\":\"1327683913\",\"value\":\"0.0000\"},{\"itemid\":\"18468\",\"clock\":\"1327683918\",\"value\":\"0.0000\"},{\"itemid\":\"18468\",\"clock\":\"1327683923\",\"value\":\"0.0000\"},{\"itemid\":\"18468\",\"clock\":\"1327683928\",\"value\":\"0.0000\"},{\"itemid\":\"18468\",\"clock\":\"1327683933\",\"value\":\"0.0000\"},{\"itemid\":\"18468\",\"clock\":\"1327683938\",\"value\":\"0.0000\"},{\"itemid\":\"18468\",\"clock\":\"1327683943\",\"value\":\"0.0000\"},{\"itemid\":\"18468\",\"clock\":\"1327683948\",\"value\":\"0.0000\"},{\"itemid\":\"18468\",\"clock\":\"1327683953\",\"value\":\"0.0000\"},{\"itemid\":\"18468\",\"clock\":\"1327683958\",\"value\":\"0.0000\"},{\"itemid\":\"18468\",\"clock\":\"1327683963\",\"value\":\"0.0000\"},{\"itemid\":\"18468\",\"clock\":\"1327683968\",\"value\":\"0.0000\"},{\"itemid\":\"18468\",\"clock\":\"1327683973\",\"value\":\"0.0000\"},{\"itemid\":\"18468\",\"clock\":\"1327683978\",\"value\":\"0.0000\"},{\"itemid\":\"18468\",\"clock\":\"1327683983\",\"value\":\"0.0000\"},{\"itemid\":\"18468\",\"clock\":\"1327683988\",\"value\":\"0.0000\"},{\"itemid\":\"18468\",\"clock\":\"1327683993\",\"value\":\"0.0000\"},{\"itemid\":\"18468\",\"clock\":\"1327683998\",\"value\":\"0.0000\"},{\"itemid\":\"18468\",\"clock\":\"1327684003\",\"value\":\"0.0000\"},{\"itemid\":\"18468\",\"clock\":\"1327684008\",BLAH BLAH BLAH BLAH");
		zbxRestService.updateCallWithReplyOnDb(testRow, updatedSampleStatus, replyBodyText);

		{
			final String[] columns = new String[] {
					Transactions.REQUEST,
					Transactions.REPLY_DATETIME,
					Transactions.STATUS,
					Transactions.REPLY,
			};
			Cursor c = getContext().getContentResolver().query(testRow, columns, null, null, null);
			final String retrievedRequest = c.getString(c.getColumnIndex(Transactions.REQUEST));
			final String retrievedReplyDateTime = c.getString(c.getColumnIndex(Transactions.REPLY_DATETIME));
			final String retrievedStatus = c.getString(c.getColumnIndex(Transactions.STATUS));
			final String retrievedReply = c.getString(c.getColumnIndex(Transactions.REPLY));
			c.close();

			assertEquals(sampleRequest, retrievedRequest);
			assertNotNull("When a row is marked as aborted, the reply_datetime should be inserted automatically", retrievedReplyDateTime);
			assertEquals(updatedSampleStatus, retrievedStatus);
			if(replyBodyText.length()<ZbxRestService.MAX_LENGTH_OF_STRING_TO_SAVE) {
				assertEquals(replyBodyText.toString(), retrievedReply);
			} else {
				final String truncatedReplyBodyText = replyBodyText.substring(0, ZbxRestService.MAX_LENGTH_OF_STRING_TO_SAVE)+"...";
				assertEquals(truncatedReplyBodyText, retrievedReply);
			}

		}
	}
	
	public int insertSampleRecordsIntoTransactionsTable(final int numRecsToInsert) {
		final String sampleRequest = "sample request ";
		final String sampleStatus = "sample status ";
		final String samplePostdata = "sample post data ";
		final String sampleRequestDatetime = "sample request datetime ";
		final String sampleReply = "sample reply ";
		final String sampleReplyDatetime = "sample reply datetime ";
		final String sampleJobid = "sample job id ";
		final String sampleCallbackIntentFilter = "sample callback intent filter ";
		
		ContentValues[] cvArray = new ContentValues[numRecsToInsert];
		for(int i=0; i<numRecsToInsert; i++) {
			ContentValues cv = new ContentValues();
			cv.put(Transactions.REQUEST, sampleRequest+i);
			cv.put(Transactions.STATUS, sampleStatus+i);
			cv.put(Transactions.POSTDATA, samplePostdata+i);
			cv.put(Transactions.REQUEST_DATETIME, sampleRequestDatetime+i);
			cv.put(Transactions.REPLY, sampleReply+i);
			cv.put(Transactions.REPLY_DATETIME, sampleReplyDatetime+i);
			cv.put(Transactions.JOBID, sampleJobid+i);
			cv.put(Transactions.CALLBACK_INTENT_FILTER, sampleCallbackIntentFilter+i);
			cvArray[i] = cv;
		}
		return getContext().getContentResolver().bulkInsert(Transactions.META_DATA.CONTENT_URI, cvArray);
	}

	public int transactionsTableSizeInRows() {
		final String[] columns = new String[] { Transactions._ID };
		Cursor c = getContext().getContentResolver().query(Transactions.META_DATA.CONTENT_URI, columns, null, null, null);
		final int numRows = c.getCount();
		c.close();
		return numRows;
	}
		
	public void testTransactionsPruningShouldNotFaillWithEmptyDb() {
		ZbxRestContentProvider.deleteAllData(getContext());
		
		ZbxRestService zbxRestService = startZbxRestService();
		zbxRestService.pruneTransactionsDb();
		final int numRowsInDbAfterPruneCall = transactionsTableSizeInRows();
		assertEquals(0, numRowsInDbAfterPruneCall);
	}

	public void testTransactionsPruningShouldNotOccurWhenUnderLimit() {
		ZbxRestContentProvider.deleteAllData(getContext());
		
		final int numRecsToInsert = ZbxRestService.TRANSACTIONS_TABLE_PRUNING_SIZE;
		final int numRecsInserted = insertSampleRecordsIntoTransactionsTable(numRecsToInsert);
		assertEquals(numRecsToInsert, numRecsInserted);
		
		ZbxRestService zbxRestService = startZbxRestService();
		zbxRestService.pruneTransactionsDb();
		
		final int numRowsInDbAfterPruneCall = transactionsTableSizeInRows();
		assertEquals(numRecsToInsert, numRowsInDbAfterPruneCall);

		{//pruning again should not have a different effect
			zbxRestService.pruneTransactionsDb();
			
			final int numRowsInDbAfterPruneCall2 = transactionsTableSizeInRows();
			assertEquals(numRecsToInsert, numRowsInDbAfterPruneCall2);
		}
	}
	
	public void testTransactionsPruningShouldOccurWhenOverLimit() {
		ZbxRestContentProvider.deleteAllData(getContext());
		
		final int numRecsToInsert = ZbxRestService.TRANSACTIONS_TABLE_PRUNING_SIZE+1;
		final int numRecsInserted = insertSampleRecordsIntoTransactionsTable(numRecsToInsert);
		assertEquals(numRecsToInsert, numRecsInserted);
		
		ZbxRestService zbxRestService = startZbxRestService();
		zbxRestService.pruneTransactionsDb();
		
		final int numRowsInDbAfterPruneCall = transactionsTableSizeInRows();
		final int halfedSizeOfDb = numRecsToInsert - (numRecsToInsert/2);
		assertEquals(halfedSizeOfDb, numRowsInDbAfterPruneCall);

		{//pruning again should not have an affect the second time around
			zbxRestService.pruneTransactionsDb();

			final int numRowsInDbAfterPruneCall2 = transactionsTableSizeInRows();
			assertEquals(halfedSizeOfDb, numRowsInDbAfterPruneCall2);
		}
	}

	public void testTransactionsPruningShouldOccurRepeatedlyWhenWayOverLimit() {
		ZbxRestContentProvider.deleteAllData(getContext());
		
		final int numRecsToInsert = (ZbxRestService.TRANSACTIONS_TABLE_PRUNING_SIZE*3);
		final int numRecsInserted = insertSampleRecordsIntoTransactionsTable(numRecsToInsert);
		assertEquals(numRecsToInsert, numRecsInserted);
		
		ZbxRestService zbxRestService = startZbxRestService();
		final int halfedSizeOfDb = numRecsToInsert - (numRecsToInsert/2);
		{//first prune should half the db size, but will still be over pruning limit
			zbxRestService.pruneTransactionsDb();

			final int numRowsInDbAfterPruneCall = transactionsTableSizeInRows();
			assertEquals(halfedSizeOfDb, numRowsInDbAfterPruneCall);
		}
		
		final int halfedHalvedSizeOfDb = halfedSizeOfDb - (halfedSizeOfDb/2);
		{//pruning again should again half db size, this time putting it under the pruning limit
			zbxRestService.pruneTransactionsDb();
			
			final int numRowsInDbAfterPruneCall = transactionsTableSizeInRows();
			assertEquals(halfedHalvedSizeOfDb, numRowsInDbAfterPruneCall);
		}

		{//pruning this last time should have no affect on db as its already under the limit
			zbxRestService.pruneTransactionsDb();
			
			final int numRowsInDbAfterPruneCall = transactionsTableSizeInRows();
			assertEquals(halfedHalvedSizeOfDb, numRowsInDbAfterPruneCall);
		}
	}

	public void testConstructZbxApiUrl() {
		ZbxRestService zbxRestService = startZbxRestService();
		final String correctUrl = "http://176.34.60.193/zabbix/api_jsonrpc.php";
		
		//save any current value so we can restore after the tests
		SharedPreferences preferences = zbxRestService.getSharedPreferences(CloudStackAndroidClient.SHARED_PREFERENCES.PREFERENCES_NAME, Context.MODE_PRIVATE);
		final String originalValue = preferences.getString(CloudStackAndroidClient.SHARED_PREFERENCES.ZABBIX_HOST_SETTING, null);
		
		{  //bare urls should work
			final String bareUrl = "176.34.60.193";
			SharedPreferences.Editor editor = preferences.edit();
			editor.putString(CloudStackAndroidClient.SHARED_PREFERENCES.ZABBIX_HOST_SETTING, bareUrl);
			editor.commit();
			
			final String constructedUrl = zbxRestService.constructZbxApiUrl();
			assertEquals(correctUrl, constructedUrl);
		}

		{  //bare urls with slash should work
			final String bareUrl = "176.34.60.193/";
			SharedPreferences.Editor editor = preferences.edit();
			editor.putString(CloudStackAndroidClient.SHARED_PREFERENCES.ZABBIX_HOST_SETTING, bareUrl);
			editor.commit();
			
			final String constructedUrl = zbxRestService.constructZbxApiUrl();
			assertEquals(correctUrl, constructedUrl);
		}

		{  //http urls should work
			final String bareUrl = "http://176.34.60.193";
			SharedPreferences.Editor editor = preferences.edit();
			editor.putString(CloudStackAndroidClient.SHARED_PREFERENCES.ZABBIX_HOST_SETTING, bareUrl);
			editor.commit();
			
			final String constructedUrl = zbxRestService.constructZbxApiUrl();
			assertEquals(correctUrl, constructedUrl);
		}
		
		{  //http urls with slash should work
			final String bareUrl = "http://176.34.60.193/";
			SharedPreferences.Editor editor = preferences.edit();
			editor.putString(CloudStackAndroidClient.SHARED_PREFERENCES.ZABBIX_HOST_SETTING, bareUrl);
			editor.commit();
			
			final String constructedUrl = zbxRestService.constructZbxApiUrl();
			assertEquals(correctUrl, constructedUrl);
		}

		{  //http zabbix urls should work
			final String bareUrl = "http://176.34.60.193/zabbix";
			SharedPreferences.Editor editor = preferences.edit();
			editor.putString(CloudStackAndroidClient.SHARED_PREFERENCES.ZABBIX_HOST_SETTING, bareUrl);
			editor.commit();
			
			final String constructedUrl = zbxRestService.constructZbxApiUrl();
			assertEquals(correctUrl, constructedUrl);
		}

		{  //http zabbix urls with slash should work
			final String bareUrl = "http://176.34.60.193/zabbix/";
			SharedPreferences.Editor editor = preferences.edit();
			editor.putString(CloudStackAndroidClient.SHARED_PREFERENCES.ZABBIX_HOST_SETTING, bareUrl);
			editor.commit();
			
			final String constructedUrl = zbxRestService.constructZbxApiUrl();
			assertEquals(correctUrl, constructedUrl);
		}

		{  //http zabbix api urls should work
			final String bareUrl = "http://176.34.60.193/zabbix/api_jsonrpc.php";
			SharedPreferences.Editor editor = preferences.edit();
			editor.putString(CloudStackAndroidClient.SHARED_PREFERENCES.ZABBIX_HOST_SETTING, bareUrl);
			editor.commit();
			
			final String constructedUrl = zbxRestService.constructZbxApiUrl();
			assertEquals(correctUrl, constructedUrl);
		}

		{  //http zabbix api urls with slash should work
			final String bareUrl = "http://176.34.60.193/zabbix/api_jsonrpc.php/";
			SharedPreferences.Editor editor = preferences.edit();
			editor.putString(CloudStackAndroidClient.SHARED_PREFERENCES.ZABBIX_HOST_SETTING, bareUrl);
			editor.commit();
			
			final String constructedUrl = zbxRestService.constructZbxApiUrl();
			assertEquals(correctUrl, constructedUrl);
		}

		{  //zabbix api urls with slash should work
			final String bareUrl = "176.34.60.193/zabbix/api_jsonrpc.php/";
			SharedPreferences.Editor editor = preferences.edit();
			editor.putString(CloudStackAndroidClient.SHARED_PREFERENCES.ZABBIX_HOST_SETTING, bareUrl);
			editor.commit();
			
			final String constructedUrl = zbxRestService.constructZbxApiUrl();
			assertEquals(correctUrl, constructedUrl);
		}

		{  //zabbix api urls should work
			final String bareUrl = "176.34.60.193/zabbix/api_jsonrpc.php";
			SharedPreferences.Editor editor = preferences.edit();
			editor.putString(CloudStackAndroidClient.SHARED_PREFERENCES.ZABBIX_HOST_SETTING, bareUrl);
			editor.commit();
			
			final String constructedUrl = zbxRestService.constructZbxApiUrl();
			assertEquals(correctUrl, constructedUrl);
		}

		{  //zabbix urls with slash should work
			final String bareUrl = "176.34.60.193/zabbix/";
			SharedPreferences.Editor editor = preferences.edit();
			editor.putString(CloudStackAndroidClient.SHARED_PREFERENCES.ZABBIX_HOST_SETTING, bareUrl);
			editor.commit();
			
			final String constructedUrl = zbxRestService.constructZbxApiUrl();
			assertEquals(correctUrl, constructedUrl);
		}
		
		//restore the original host value saved in prefes
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(CloudStackAndroidClient.SHARED_PREFERENCES.ZABBIX_HOST_SETTING, originalValue);
		editor.commit();
	}
	

}
