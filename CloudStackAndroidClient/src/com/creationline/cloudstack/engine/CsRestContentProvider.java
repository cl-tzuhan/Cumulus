package com.creationline.cloudstack.engine;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;

import com.creationline.cloudstack.util.ClLog;

/**
 * CsRestContentProvider is used to persist the REST requests going to CS as well as the replies that come back.
 * Along with other persisted metadata concerning the status/dateTime of the request, the data saved should
 * provide a complete picture of the REST transactions currently in progress as well as previously executed
 * at any time.
 * 
 * @author thsu
 *
 */
public class CsRestContentProvider extends ContentProvider {
	private static final String TAG = "ContentProvider";
	
	private static final String AUTHORITY = "com.creationline.cloudstack.engine.csrestcontentprovider";
	
	public static final String DB_NAME = "CsRestTransaction.db";
	private	SQLiteDatabase sqlDb;
	private SQLiteDatabaseHelper sqlDbHelper;
	private static final int DB_VERSION = 1;
	private static final String TABLE_NAME = "transactions";
	
	private static final String ID_EQUALS = CsRestContentProvider.Transactions._ID+"=";
	
	
	public static final class Transactions implements BaseColumns {
		
		public static final Uri CONTENT_URI = Uri.parse("content://"+AUTHORITY+"/"+TABLE_NAME);
		
		//table columns (_ID already included in BaseColumns declaration)
		public static final String REQUEST = "request";
		public static final String STATUS = "status";
		public static final String REPLY = "reply";
		public static final String REQUEST_DATETIME = "request_dateTime";
		public static final String REPLY_DATETIME = "reply_dateTime";

		
		//allowed values for the status column
		public static final class STATUS_VALUES {
			public static final String IN_PROGRESS = "in_progress"; //request accepted and is being executed
			public static final String SUCCESS = "success";         //request is finished and succeeded
			public static final String FAIL = "fail";               //request is finished and failed (with error from CS itself)
			public static final String ABORTED = "aborted";         //request itself did not make it to CS for some reason
			                            							//TODO: tie ABORTED to some error db that app can go to to get associated error msg
		}
		
	}
	
	private static class SQLiteDatabaseHelper extends SQLiteOpenHelper {
		///This helper class simplifies management of the sql db itself

		public SQLiteDatabaseHelper(Context context) {
			super(context, DB_NAME, null, DB_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			///create db table
			final String tableColumns = ", "
										+ Transactions.REQUEST+" TEXT, "
										+ Transactions.REQUEST_DATETIME+" TEXT, "
										+ Transactions.STATUS+" TEXT, "
										+ Transactions.REPLY+" TEXT,"
										+ Transactions.REPLY_DATETIME+" TEXT";
			db.execSQL("CREATE TABLE " + TABLE_NAME + " ( "+Transactions._ID+" INTEGER PRIMARY KEY AUTOINCREMENT" + tableColumns + ");");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			///destroy and re-create the db table
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
			onCreate(db);
		}
		
	}
	

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int numDeleted = 0;
		
		try {
			sqlDb = sqlDbHelper.getWritableDatabase();
			
			selection = transformIdToSelectionClause(uri, selection);
			
			numDeleted = sqlDb.delete(TABLE_NAME, selection, selectionArgs);
			
			getContext().getContentResolver().notifyChange(uri, null);  //signal observers that something was deleted
		} catch (SQLiteException e) {
			ClLog.e(TAG, "delete(): getWritableDatabase() failed!");
			e.printStackTrace();
		} finally {
			sqlDb.close();
		}
		
		return numDeleted;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		Uri returnUri = null;
		
		try {
			sqlDb = sqlDbHelper.getWritableDatabase();
			long rowId = sqlDb.insert(TABLE_NAME, null, values);  //insert the passed in data as a new row
			
			if(rowId<0){
				ClLog.e(TAG, "Exception inserting values=(" + values + ") into uri=" + uri);
				return null;
			}
			
			//create full uri appended with newly added row id for return
			returnUri = ContentUris.appendId(Transactions.CONTENT_URI.buildUpon(), rowId).build();
			getContext().getContentResolver().notifyChange(uri, null);  //signal observers that something was added
		} catch (SQLiteException e) {
			ClLog.e(TAG, "insert(): getWritableDatabase() failed!");
			e.printStackTrace();
		} finally {
			sqlDb.close();
		}
		
		return returnUri;
	}

	@Override
	public boolean onCreate() {
		sqlDbHelper = new SQLiteDatabaseHelper(getContext());
		return (sqlDbHelper==null)? false : true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		Cursor c = null;
		try {
			sqlDb = sqlDbHelper.getReadableDatabase();
			
			selection = transformIdToSelectionClause(uri, selection);  //process any trailing id specifiers in the uri
			
			SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
			queryBuilder.setTables(TABLE_NAME);
			c = queryBuilder.query(sqlDb, projection, selection, selectionArgs, null, null, sortOrder);
			
			c.setNotificationUri(getContext().getContentResolver(), uri);  //register to watch for content uri changes
			c.moveToFirst();  //hack?: for whatever reason, calling moveToFirst() here allows you to close sqlDb w/out affecting the output of the cursor in the calling method (if you don't, the calling method gets a cursor with no data)
		} catch (SQLiteException e) {
			ClLog.e(TAG, "query(): getReadableDatabase() failed!");
			e.printStackTrace();
		} finally {
			sqlDb.close();
		}
		
		return c;
	}

	private String transformIdToSelectionClause(Uri uri, String selection) {
		//If the content uri contains a trailing id number, this method will
		//create and return a sql WHERE clause that will isolate that record/id.
		//If the uri contains no trailing id number, the WHERE clause is
		//returned unchanged.
		
		try {
			final long specifiedId = ContentUris.parseId(uri);
			if(specifiedId>-1) {
				//narrow query to specific row if id specified in uri
				//(any supplied selection (WHERE) clause will then apply only to that row (which is not of much use :P))
				final String idSelection = ID_EQUALS+specifiedId;
				selection = (selection==null)? idSelection : idSelection+" AND "+selection;
			}
		} catch (NumberFormatException e) {
			//if we got this exception, then uri did not have an id as the last segment so we do nothing
		}
		return selection;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		int numRowsUpdated = 0;
		
		try {
			sqlDb = sqlDbHelper.getWritableDatabase();
			
			selection = transformIdToSelectionClause(uri, selection);  //process any trailing id specifiers in the uri
			
			numRowsUpdated = sqlDb.update(TABLE_NAME, values, selection, selectionArgs);  //update existing rows using passed in data
			
			getContext().getContentResolver().notifyChange(uri, null);  //signal observers that something was updated
		} catch (SQLiteException e) {
			ClLog.e(TAG, "update(): getWritableDatabase() failed!");
			e.printStackTrace();
		} finally {
			sqlDb.close();
		}
		
		return numRowsUpdated;
	}

}
