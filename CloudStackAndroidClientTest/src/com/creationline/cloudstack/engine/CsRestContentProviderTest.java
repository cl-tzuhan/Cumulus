package com.creationline.cloudstack.engine;

import com.creationline.cloudstack.engine.CsRestContentProvider.Transactions;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.test.AndroidTestCase;

public class CsRestContentProviderTest extends AndroidTestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		deleteDb();  //clean-up so we don't leave test data around
		
		super.tearDown();
	}
	
	protected void deleteDb() {
		//Completely remove the db if it exists
		
		if (getContext().databaseList().length<=0) {
			return; //do nothing if there is no db to start with
		}
		assertTrue(getContext().deleteDatabase(CsRestContentProvider.DB_NAME));
	}

	public void testInsertAndQuery_rowSpecifiedInUri() {
		deleteDb();

		final String testData1_request = "This is a sample request";
		final String testData1_status = "This is a sample status";
		final String testData1_reply = "This is a sample reply";
		final String testData1_request_dateTime = "This is a sample request dateTime";
		
		ContentValues contentValues = new ContentValues();
		contentValues.put(CsRestContentProvider.Transactions.REQUEST, testData1_request);
		contentValues.put(CsRestContentProvider.Transactions.STATUS, testData1_status);
		contentValues.put(CsRestContentProvider.Transactions.REPLY, testData1_reply);
		contentValues.put(CsRestContentProvider.Transactions.REQUEST_DATETIME, testData1_request_dateTime);
		
		Uri uriOfNewRow = getContext().getContentResolver().insert(CsRestContentProvider.Transactions.CONTENT_URI, contentValues);
		assertNotNull("insert of data failed!!", uriOfNewRow);
		
		String columns[] = new String[] {CsRestContentProvider.Transactions.REQUEST,
										 CsRestContentProvider.Transactions.STATUS,
										 CsRestContentProvider.Transactions.REPLY,
										 CsRestContentProvider.Transactions.REQUEST_DATETIME};
		final ContentResolver cr = getContext().getContentResolver();
		Cursor resultOfQueryForSingleRecord = cr.query(uriOfNewRow, columns, null, null, null);
		resultOfQueryForSingleRecord.moveToFirst();
		assertEquals(1, resultOfQueryForSingleRecord.getCount());
		assertEquals(columns.length, resultOfQueryForSingleRecord.getColumnCount());
		//check to see the returned data is the what was saved
		assertEquals(testData1_request, resultOfQueryForSingleRecord.getString(resultOfQueryForSingleRecord.getColumnIndex(CsRestContentProvider.Transactions.REQUEST)));
		assertEquals(testData1_status, resultOfQueryForSingleRecord.getString(resultOfQueryForSingleRecord.getColumnIndex(CsRestContentProvider.Transactions.STATUS)));
		assertEquals(testData1_reply, resultOfQueryForSingleRecord.getString(resultOfQueryForSingleRecord.getColumnIndex(CsRestContentProvider.Transactions.REPLY)));
		assertEquals(testData1_request_dateTime, resultOfQueryForSingleRecord.getString(resultOfQueryForSingleRecord.getColumnIndex(CsRestContentProvider.Transactions.REQUEST_DATETIME)));
		
		Cursor resultOfQueryForAllRecords = cr.query(CsRestContentProvider.Transactions.CONTENT_URI, null, null, null, null);
		resultOfQueryForAllRecords.moveToFirst();
		assertEquals(1, resultOfQueryForAllRecords.getCount());  //there should be only 1 record in the db
	}
	
	public void testInsertAndQuery_rowSpecifiedInParams() {
		deleteDb();

		final String testData1_request = "This is a sample request";
		final String testData1_status = "This is a sample status";
		final String testData1_reply = "This is a sample reply";
		final String testData1_request_dateTime = "This is a sample request dateTime";
		
		ContentValues contentValues = new ContentValues();
		contentValues.put(CsRestContentProvider.Transactions.REQUEST, testData1_request);
		contentValues.put(CsRestContentProvider.Transactions.STATUS, testData1_status);
		contentValues.put(CsRestContentProvider.Transactions.REPLY, testData1_reply);
		contentValues.put(CsRestContentProvider.Transactions.REQUEST_DATETIME, testData1_request_dateTime);
		
		Uri uriOfNewRow = getContext().getContentResolver().insert(CsRestContentProvider.Transactions.CONTENT_URI, contentValues);
		assertNotNull("insert of data failed!!", uriOfNewRow);
		
		String columns[] = new String[] {CsRestContentProvider.Transactions.REQUEST,
										 CsRestContentProvider.Transactions.STATUS,
										 CsRestContentProvider.Transactions.REPLY,
										 CsRestContentProvider.Transactions.REQUEST_DATETIME};
		final ContentResolver cr = getContext().getContentResolver();
		Cursor resultOfQueryForSingleRecord = cr.query(CsRestContentProvider.Transactions.CONTENT_URI, 
														   columns,
														   CsRestContentProvider.Transactions._ID+"=?",
														   new String[] {String.valueOf(ContentUris.parseId(uriOfNewRow))},
														   null);
		resultOfQueryForSingleRecord.moveToFirst();
		assertEquals(1, resultOfQueryForSingleRecord.getCount());
		assertEquals(columns.length, resultOfQueryForSingleRecord.getColumnCount());
		//check to see the returned data is the what was saved
		assertEquals(testData1_request, resultOfQueryForSingleRecord.getString(resultOfQueryForSingleRecord.getColumnIndex(CsRestContentProvider.Transactions.REQUEST)));
		assertEquals(testData1_status, resultOfQueryForSingleRecord.getString(resultOfQueryForSingleRecord.getColumnIndex(CsRestContentProvider.Transactions.STATUS)));
		assertEquals(testData1_reply, resultOfQueryForSingleRecord.getString(resultOfQueryForSingleRecord.getColumnIndex(CsRestContentProvider.Transactions.REPLY)));
		assertEquals(testData1_request_dateTime, resultOfQueryForSingleRecord.getString(resultOfQueryForSingleRecord.getColumnIndex(CsRestContentProvider.Transactions.REQUEST_DATETIME)));
		
		Cursor resultOfQueryForAllRecords = cr.query(CsRestContentProvider.Transactions.CONTENT_URI, null, null, null, null);
		resultOfQueryForAllRecords.moveToFirst();
		assertEquals(1, resultOfQueryForAllRecords.getCount());  //there should be only 1 record in the db
		
	}
	
	public void testQuery_edgeCases() {
		deleteDb();
		
		final ContentResolver cr = getContext().getContentResolver();
		
		Cursor emptyResults = cr.query(CsRestContentProvider.Transactions.CONTENT_URI, null, null, null, null);
		assertEquals(0, emptyResults.getCount());
		
		
		Uri nonexistentUri = ContentUris.appendId(Transactions.CONTENT_URI.buildUpon(), 1).build();  //look for arbitrary _id=1 record, which does not exist
		emptyResults = cr.query(nonexistentUri, null, null, null, null);
		assertEquals(0, emptyResults.getCount());
	}


}
