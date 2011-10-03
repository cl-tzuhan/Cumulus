package com.creationline.cloudstack.engine;

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
		super.tearDown();
	}

	public void testInsertAndQuery_rowSpecifiedInUri() {
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
		Cursor c = getContext().getContentResolver().query(uriOfNewRow, columns, null, null, null);
		c.moveToFirst();
		assertEquals(1, c.getCount());
		assertEquals(testData1_request, c.getString(c.getColumnIndex(CsRestContentProvider.Transactions.REQUEST)));
		assertEquals(testData1_status, c.getString(c.getColumnIndex(CsRestContentProvider.Transactions.STATUS)));
		assertEquals(testData1_reply, c.getString(c.getColumnIndex(CsRestContentProvider.Transactions.REPLY)));
		assertEquals(testData1_request_dateTime, c.getString(c.getColumnIndex(CsRestContentProvider.Transactions.REQUEST_DATETIME)));
	}
	
	public void testInsertAndQuery_rowSpecifiedInParams() {
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
		Cursor c = getContext().getContentResolver().query(CsRestContentProvider.Transactions.CONTENT_URI, 
														   columns,
														   CsRestContentProvider.Transactions._ID+"=?",
														   new String[] {String.valueOf(ContentUris.parseId(uriOfNewRow))},
														   null);
		c.moveToFirst();
		assertEquals(1, c.getCount());
		assertEquals(testData1_request, c.getString(c.getColumnIndex(CsRestContentProvider.Transactions.REQUEST)));
		assertEquals(testData1_status, c.getString(c.getColumnIndex(CsRestContentProvider.Transactions.STATUS)));
		assertEquals(testData1_reply, c.getString(c.getColumnIndex(CsRestContentProvider.Transactions.REPLY)));
		assertEquals(testData1_request_dateTime, c.getString(c.getColumnIndex(CsRestContentProvider.Transactions.REQUEST_DATETIME)));
	}


}
