package com.creationline.cloudstack.engine;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.test.AndroidTestCase;

public class CsRestContentProviderTest extends AndroidTestCase {
	///Note: Running these tests will wipe out any existing transaction!!
	//       If you have data you want to keep around in the db, back it up before running these unit tests.

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
		final String testData1_request_dateTime = "This is a sample request dateTime";
		final String testData1_status = "This is a sample status";
		final String testData1_reply = "This is a sample reply";
		final String testData1_reply_dateTime = "This is a sample reply dateTime";
		
		ContentValues contentValues = new ContentValues();
		contentValues.put(CsRestContentProvider.Transactions.REQUEST, testData1_request);
		contentValues.put(CsRestContentProvider.Transactions.REQUEST_DATETIME, testData1_request_dateTime);
		contentValues.put(CsRestContentProvider.Transactions.STATUS, testData1_status);
		contentValues.put(CsRestContentProvider.Transactions.REPLY, testData1_reply);
		contentValues.put(CsRestContentProvider.Transactions.REPLY_DATETIME, testData1_reply_dateTime);
		
		Uri uriOfNewRow = getContext().getContentResolver().insert(CsRestContentProvider.Transactions.CONTENT_URI, contentValues);
		assertNotNull("insert of data failed!!", uriOfNewRow);
		
		String columns[] = new String[] {CsRestContentProvider.Transactions.REQUEST,
										 CsRestContentProvider.Transactions.REQUEST_DATETIME,
										 CsRestContentProvider.Transactions.STATUS,
										 CsRestContentProvider.Transactions.REPLY,
										 CsRestContentProvider.Transactions.REPLY_DATETIME};
		final ContentResolver cr = getContext().getContentResolver();
		Cursor resultOfQueryForSingleRecord = cr.query(uriOfNewRow, columns, null, null, null);
		resultOfQueryForSingleRecord.moveToFirst();
		assertEquals(1, resultOfQueryForSingleRecord.getCount());
		assertEquals(columns.length, resultOfQueryForSingleRecord.getColumnCount());
		//check to see the returned data is the what was saved
		assertEquals(testData1_request, resultOfQueryForSingleRecord.getString(resultOfQueryForSingleRecord.getColumnIndex(CsRestContentProvider.Transactions.REQUEST)));
		assertEquals(testData1_request_dateTime, resultOfQueryForSingleRecord.getString(resultOfQueryForSingleRecord.getColumnIndex(CsRestContentProvider.Transactions.REQUEST_DATETIME)));
		assertEquals(testData1_status, resultOfQueryForSingleRecord.getString(resultOfQueryForSingleRecord.getColumnIndex(CsRestContentProvider.Transactions.STATUS)));
		assertEquals(testData1_reply, resultOfQueryForSingleRecord.getString(resultOfQueryForSingleRecord.getColumnIndex(CsRestContentProvider.Transactions.REPLY)));
		assertEquals(testData1_reply_dateTime, resultOfQueryForSingleRecord.getString(resultOfQueryForSingleRecord.getColumnIndex(CsRestContentProvider.Transactions.REPLY_DATETIME)));
		resultOfQueryForSingleRecord.close();
		
		Cursor resultOfQueryForAllRecords = cr.query(CsRestContentProvider.Transactions.CONTENT_URI, null, null, null, null);
		resultOfQueryForAllRecords.moveToFirst();
		assertEquals(1, resultOfQueryForAllRecords.getCount());  //there should be only 1 record in the db
		resultOfQueryForAllRecords.close();
	}
	
	public void testInsertAndQuery_rowSpecifiedInParams() {
		deleteDb();

		final String testData1_request = "This is a sample request";
		final String testData1_request_dateTime = "This is a sample request dateTime";
		final String testData1_status = "This is a sample status";
		final String testData1_reply = "This is a sample reply";
		final String testData1_reply_dateTime = "This is a sample reply dateTime";
		
		ContentValues contentValues = new ContentValues();
		contentValues.put(CsRestContentProvider.Transactions.REQUEST, testData1_request);
		contentValues.put(CsRestContentProvider.Transactions.REQUEST_DATETIME, testData1_request_dateTime);
		contentValues.put(CsRestContentProvider.Transactions.STATUS, testData1_status);
		contentValues.put(CsRestContentProvider.Transactions.REPLY, testData1_reply);
		contentValues.put(CsRestContentProvider.Transactions.REPLY_DATETIME, testData1_reply_dateTime);
		
		Uri uriOfNewRow = getContext().getContentResolver().insert(CsRestContentProvider.Transactions.CONTENT_URI, contentValues);
		assertNotNull("insert of data failed!!", uriOfNewRow);
		
		String columns[] = new String[] {CsRestContentProvider.Transactions.REQUEST,
										 CsRestContentProvider.Transactions.REQUEST_DATETIME,
										 CsRestContentProvider.Transactions.STATUS,
										 CsRestContentProvider.Transactions.REPLY,
										 CsRestContentProvider.Transactions.REPLY_DATETIME};
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
		assertEquals(testData1_request_dateTime, resultOfQueryForSingleRecord.getString(resultOfQueryForSingleRecord.getColumnIndex(CsRestContentProvider.Transactions.REQUEST_DATETIME)));
		assertEquals(testData1_status, resultOfQueryForSingleRecord.getString(resultOfQueryForSingleRecord.getColumnIndex(CsRestContentProvider.Transactions.STATUS)));
		assertEquals(testData1_reply, resultOfQueryForSingleRecord.getString(resultOfQueryForSingleRecord.getColumnIndex(CsRestContentProvider.Transactions.REPLY)));
		assertEquals(testData1_reply_dateTime, resultOfQueryForSingleRecord.getString(resultOfQueryForSingleRecord.getColumnIndex(CsRestContentProvider.Transactions.REPLY_DATETIME)));
		resultOfQueryForSingleRecord.close();
		
		Cursor resultOfQueryForAllRecords = cr.query(CsRestContentProvider.Transactions.CONTENT_URI, null, null, null, null);
		resultOfQueryForAllRecords.moveToFirst();
		assertEquals(1, resultOfQueryForAllRecords.getCount());  //there should be only 1 record in the db
		resultOfQueryForAllRecords.close();
		
	}
	
	public void testQuery_edgeCases() {
		deleteDb();
		
		final ContentResolver cr = getContext().getContentResolver();
		
		Cursor emptyResults = cr.query(CsRestContentProvider.Transactions.CONTENT_URI, null, null, null, null);
		assertEquals(0, emptyResults.getCount());
		
		
		Uri nonexistentUri = ContentUris.appendId(CsRestContentProvider.Transactions.CONTENT_URI.buildUpon(), 1).build();  //look for arbitrary _id=1 record, which does not exist
		emptyResults = cr.query(nonexistentUri, null, null, null, null);
		assertEquals(0, emptyResults.getCount());
		
		emptyResults.close();
	}

	public void testUpdate() {
		deleteDb();
		
		final int TEST_SIZE = 6;
		Uri[] addedUris = new Uri[TEST_SIZE];

		final String testData_request = "dummy request data ";
		final String testData_request_dateTime = "dummy request_dateTime data ";
		final String testData_status = "dummy status data ";
		final String testData_reply = "dummy reply data ";
		final String testData_reply_dateTime = "dummy reply_dateTime data ";
		
		ContentResolver cr = getContext().getContentResolver();
		
		//Add base data
		for(int i=1; i<TEST_SIZE; i++) {
			ContentValues contentValues = new ContentValues();
			contentValues.put(CsRestContentProvider.Transactions.REQUEST, testData_request+i);
			contentValues.put(CsRestContentProvider.Transactions.REQUEST_DATETIME, testData_request_dateTime+i);
			contentValues.put(CsRestContentProvider.Transactions.STATUS, testData_status+i);
			contentValues.put(CsRestContentProvider.Transactions.REPLY, testData_reply+i);
			contentValues.put(CsRestContentProvider.Transactions.REPLY_DATETIME, testData_reply_dateTime+i);

			addedUris[i] = cr.insert(CsRestContentProvider.Transactions.CONTENT_URI, contentValues);
		}
		
		{//Test updating just one field of each record
			final String UPDATED = "UPDATED";
			for(int i=1; i<TEST_SIZE; i++) {
				ContentValues contentValues = new ContentValues();
				contentValues.put(CsRestContentProvider.Transactions.REQUEST, testData_request+UPDATED);
				contentValues.put(CsRestContentProvider.Transactions.REPLY, testData_reply+UPDATED);
				int numUpdated = cr.update(addedUris[i], contentValues, null, null);
				assertEquals(1, numUpdated);
			}
			Cursor c = cr.query(CsRestContentProvider.Transactions.CONTENT_URI, null, null, null, CsRestContentProvider.Transactions._ID);  //query all data from db sorted by id
			c.moveToFirst();
			for(int i=1; !c.isAfterLast(); i++) {
				//check to see that only the request & reply fields are updated, and all other fields remain unaffected
				assertEquals(testData_request+UPDATED, c.getString(c.getColumnIndex(CsRestContentProvider.Transactions.REQUEST)));
				assertEquals(testData_request_dateTime+i, c.getString(c.getColumnIndex(CsRestContentProvider.Transactions.REQUEST_DATETIME)));
				assertEquals(testData_status+i, c.getString(c.getColumnIndex(CsRestContentProvider.Transactions.STATUS)));
				assertEquals(testData_reply+UPDATED, c.getString(c.getColumnIndex(CsRestContentProvider.Transactions.REPLY)));
				assertEquals(testData_reply_dateTime+i, c.getString(c.getColumnIndex(CsRestContentProvider.Transactions.REPLY_DATETIME)));
				
				c.moveToNext();
			}
			c.close();
		}
		
		final String CHANGED_ME = "CHANGED_ME";
		{//Test changing status of records with odd digit ids
			ContentValues contentValues = new ContentValues();
			contentValues.put(CsRestContentProvider.Transactions.STATUS, CHANGED_ME);
			for(int d=1; d<TEST_SIZE; d+=2) {
				int numUpdated = cr.update(addedUris[d], contentValues, null, null);
				assertEquals(1, numUpdated);
			}
			for(int i=1; i<TEST_SIZE; i++) {
				//check to see that odd id-ed records are changed, but even id-ed records are unaffected
				Cursor c = cr.query(addedUris[i], new String[]{CsRestContentProvider.Transactions.STATUS}, null, null, CsRestContentProvider.Transactions._ID);
				if(i%2==0) {
					assertEquals(testData_status+i, c.getString(c.getColumnIndex(CsRestContentProvider.Transactions.STATUS)));
				} else {
					assertEquals(CHANGED_ME, c.getString(c.getColumnIndex(CsRestContentProvider.Transactions.STATUS)));
				}
				c.close();
			}
		}
		final String _AGAIN = "_AGAIN";
		{//Test updating multiple records at once
			ContentValues contentValues = new ContentValues();
			contentValues.put(CsRestContentProvider.Transactions.STATUS, CHANGED_ME+_AGAIN);
			String where = CsRestContentProvider.Transactions.STATUS+"=?";
			String[] whereArgs = new String[]{CHANGED_ME};
			int numUpdated = cr.update(CsRestContentProvider.Transactions.CONTENT_URI, contentValues, where, whereArgs);
			assertEquals(TEST_SIZE/2, numUpdated);
			
			for(int i=1; i<TEST_SIZE; i++) {
				//check to see that odd id-ed records are changed, but even id-ed records are unaffected
				Cursor c = cr.query(addedUris[i], new String[]{CsRestContentProvider.Transactions.STATUS}, null, null, CsRestContentProvider.Transactions._ID);
				if(i%2==0) {
					assertEquals(testData_status+i, c.getString(c.getColumnIndex(CsRestContentProvider.Transactions.STATUS)));
				} else {
					assertEquals(CHANGED_ME+_AGAIN, c.getString(c.getColumnIndex(CsRestContentProvider.Transactions.STATUS)));
				}
				c.close();
			}
		}
		
		{
			ContentValues contentValues = new ContentValues();
			contentValues.put(CsRestContentProvider.Transactions.STATUS, "Afro Samurai");
			int numUpdated = cr.update(CsRestContentProvider.Transactions.CONTENT_URI, contentValues, "status='NO_SUCH_STATUS'", null);
			assertEquals(0, numUpdated);
			
			for(int i=1; i<TEST_SIZE; i++) {
				//check to see no records have been changed by the above update call
				Cursor c = cr.query(addedUris[i], new String[]{CsRestContentProvider.Transactions.STATUS}, null, null, null);
				if(i%2==0) {
					assertEquals(testData_status+i, c.getString(c.getColumnIndex(CsRestContentProvider.Transactions.STATUS)));
				} else {
					assertEquals(CHANGED_ME+_AGAIN, c.getString(c.getColumnIndex(CsRestContentProvider.Transactions.STATUS)));
				}
				c.close();
			}
		}
		
	}
	
	public void testUpdate_emptyCase() {
		deleteDb();
		
		ContentResolver cr = getContext().getContentResolver();
		
		ContentValues contentValues = new ContentValues();
		contentValues.put(CsRestContentProvider.Transactions.REQUEST_DATETIME, "Afro Samurai time!");
		int numUpdated = cr.update(CsRestContentProvider.Transactions.CONTENT_URI, contentValues, "status='NO_SUCH_STATUS'", null);
		assertEquals(0, numUpdated);
		
		Cursor emptyResult = cr.query(CsRestContentProvider.Transactions.CONTENT_URI, null, null, null, null);
		assertEquals(0, emptyResult.getCount());
		emptyResult.close();
	}

	
	public void testDelete() {
		deleteDb();
		
		final int DATA_SET1_SIZE = 6;
		final int TOTAL_DATA_SET_SIZE = 11;
		Uri[] addedUris = new Uri[TOTAL_DATA_SET_SIZE];

		final String testData_request = "dummy request data ";
		final String testData_request_dateTime = "dummy request_dateTime data ";
		final String testData_status = "dummy status data ";
		final String testData_reply = "dummy reply data ";
		final String testData_reply_dateTime = "dummy reply_dateTime data ";
		
		ContentResolver cr = getContext().getContentResolver();
		
		//Add base data
		for(int i=1; i<DATA_SET1_SIZE; i++) {
			ContentValues contentValues = new ContentValues();
			contentValues.put(CsRestContentProvider.Transactions.REQUEST, testData_request+"1");
			contentValues.put(CsRestContentProvider.Transactions.REQUEST_DATETIME, testData_request_dateTime+"1");
			contentValues.put(CsRestContentProvider.Transactions.STATUS, testData_status+"1");
			contentValues.put(CsRestContentProvider.Transactions.REPLY, testData_reply+"1");
			contentValues.put(CsRestContentProvider.Transactions.REPLY_DATETIME, testData_reply_dateTime+"1");

			addedUris[i] = cr.insert(CsRestContentProvider.Transactions.CONTENT_URI, contentValues);
		}
		for(int i=6; i<TOTAL_DATA_SET_SIZE; i++) {
			ContentValues contentValues = new ContentValues();
			contentValues.put(CsRestContentProvider.Transactions.REQUEST, testData_request+"2");
			contentValues.put(CsRestContentProvider.Transactions.REQUEST_DATETIME, testData_request_dateTime+"2");
			contentValues.put(CsRestContentProvider.Transactions.STATUS, testData_status+"2");
			contentValues.put(CsRestContentProvider.Transactions.REPLY, testData_reply+"2");
			contentValues.put(CsRestContentProvider.Transactions.REPLY_DATETIME, testData_reply_dateTime+"2");
			
			addedUris[i] = cr.insert(CsRestContentProvider.Transactions.CONTENT_URI, contentValues);
		}
		
		int numDeleted = 0;
		Cursor c = null;
		
		//check single delete (first item)
		numDeleted = cr.delete(addedUris[1], null, null);
		assertEquals(1, numDeleted);
		c = cr.query(CsRestContentProvider.Transactions.CONTENT_URI, null, null, null, null);
		assertEquals(9, c.getCount());
		c.close();
		
		//check single delete (last item)
		numDeleted = cr.delete(addedUris[TOTAL_DATA_SET_SIZE-1], null, null);
		assertEquals(1, numDeleted);
		c = cr.query(CsRestContentProvider.Transactions.CONTENT_URI, null, null, null, null);
		assertEquals(8, c.getCount());
		c.close();
		
		//check deletion based on matching a WHERE clause
		String selection = CsRestContentProvider.Transactions.REQUEST_DATETIME+"=?";
		String[] selectionArgs = new String[]{testData_request_dateTime+"2"};
		numDeleted = cr.delete(CsRestContentProvider.Transactions.CONTENT_URI, selection, selectionArgs);
		assertEquals(TOTAL_DATA_SET_SIZE-DATA_SET1_SIZE-1, numDeleted);  //numDeleted should be the size of the "+2" dataset
		c = cr.query(CsRestContentProvider.Transactions.CONTENT_URI, null, null, null, null);
		assertEquals(4, c.getCount());
		c.moveToFirst();
		while(!c.isAfterLast()) {
			assertEquals(testData_request+"1", c.getString(c.getColumnIndex(CsRestContentProvider.Transactions.REQUEST)));
			assertEquals(testData_request_dateTime+"1", c.getString(c.getColumnIndex(CsRestContentProvider.Transactions.REQUEST_DATETIME)));
			assertEquals(testData_status+"1", c.getString(c.getColumnIndex(CsRestContentProvider.Transactions.STATUS)));
			assertEquals(testData_reply+"1", c.getString(c.getColumnIndex(CsRestContentProvider.Transactions.REPLY)));
			assertEquals(testData_reply_dateTime+"1", c.getString(c.getColumnIndex(CsRestContentProvider.Transactions.REPLY_DATETIME)));
			
			c.moveToNext();
		}
		c.close();
		
		//check deletion of all items
		numDeleted = cr.delete(CsRestContentProvider.Transactions.CONTENT_URI, null, null);
		assertEquals(4, numDeleted);
		c = cr.query(CsRestContentProvider.Transactions.CONTENT_URI, null, null, null, null);
		assertEquals(0, c.getCount());
		c.close();
	}
	
}
