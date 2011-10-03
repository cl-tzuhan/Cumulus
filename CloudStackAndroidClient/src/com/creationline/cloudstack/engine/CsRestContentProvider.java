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
	
	private	SQLiteDatabase sqlDb;
	private SQLiteDatabaseHelper sqlDbHelper;
	private static final String DB_NAME = "CsRestTransaction.db";
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
										+ Transactions.STATUS+" TEXT, "
										+ Transactions.REPLY+" TEXT,"
										+ Transactions.REQUEST_DATETIME+" TEXT";
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
	public int delete(Uri arg0, String arg1, String[] arg2) {
		// TODO Auto-generated method stub
		return 0;
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
			getContext().getContentResolver().notifyChange(returnUri, null);  //signal observers that something was added
		} catch (SQLiteException e) {
			ClLog.e(TAG, "getWritableDatabase() failed!");
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
			
			try {
				final long specifiedId = ContentUris.parseId(uri);
				if(specifiedId>-1) {
					//narrow query to specific row if id specified in uri
					//(any supplied selection (WHERE) clause will then apply only to that row (which is not of much use :P))
					final String idSelection = ID_EQUALS+specifiedId;
					selection = (selection==null)? idSelection : idSelection+" AND "+selection;
				}
			} catch (NumberFormatException e) {
				//if we got this exception, then uri did not have an id as the last segment;
				//just process the rest of the query without it.
			}
			
			SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
			queryBuilder.setTables(TABLE_NAME);
			c = queryBuilder.query(sqlDb, projection, selection, selectionArgs, null, null, sortOrder);
			
			c.setNotificationUri(getContext().getContentResolver(), uri);  //register to watch for content uri changes
			c.moveToFirst();  //hack?: for whatever reason, calling moveToFirst() here allows you to close sqlDb w/out affecting the output of the cursor in the calling method (if you don't, the calling method gets a cursor with no data)
		} catch (SQLiteException e) {
			ClLog.e(TAG, "getReadableDatabase() failed!");
			e.printStackTrace();
		} finally {
			sqlDb.close();
		}
		
		return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

}
