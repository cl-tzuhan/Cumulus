package com.creationline.cloudstack.engine;

import java.lang.reflect.Field;
import java.util.List;

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

import com.creationline.cloudstack.engine.db.Errors;
import com.creationline.cloudstack.engine.db.Transactions;
import com.creationline.cloudstack.engine.db.Vms;
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
	
	public static final String AUTHORITY = "com.creationline.cloudstack.engine.csrestcontentprovider";
	
	public static final String DB_NAME = "CsRestTransaction.db";
	private	SQLiteDatabase sqlDb;
	private SQLiteDatabaseHelper sqlDbHelper;
	private static final int DB_VERSION = 1;
	
	
	public static class SQLiteDatabaseHelper extends SQLiteOpenHelper {
		///This helper class simplifies management of the sql db itself

		public SQLiteDatabaseHelper(Context context) {
			super(context, DB_NAME, null, DB_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			///Create db tables
			
			//creating the Transactions table manually b/c I want a specific ordering to the columns that's not alphabetical
			final String tableColumns = ", "
										+ Transactions.REQUEST+" TEXT, "
										+ Transactions.REQUEST_DATETIME+" TEXT, "
										+ Transactions.STATUS+" TEXT, "
										+ Transactions.REPLY+" TEXT,"
										+ Transactions.REPLY_DATETIME+" TEXT";
			db.execSQL("CREATE TABLE " + Transactions.META_DATA.TABLE_NAME + " ( "+Transactions._ID+" INTEGER PRIMARY KEY AUTOINCREMENT" + tableColumns + ");");
			
			//create the ui-use tables from the appropriate column definition classes
			db.execSQL(makeCreateTableSqlStr(new Vms()));
			
			
			//create the errors table
			db.execSQL(makeCreateTableSqlStr(new Errors()));
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			///destroy and re-create the db tables
			db.execSQL("DROP TABLE IF EXISTS " + Transactions.META_DATA.TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + Vms.META_DATA.TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + Errors.META_DATA.TABLE_NAME);
			onCreate(db);
		}
		
		public static String makeCreateTableSqlStr(Object columnDefinitionClass) {
			//Build an sql statement that creates a db based off of the name and member vars of the passed in class.
			
			StringBuilder sqlStr = new StringBuilder("CREATE TABLE ");
			sqlStr.append(columnDefinitionClass.getClass().getSimpleName().toLowerCase());
			sqlStr.append(" ( ").append(BaseColumns._ID).append(" INTEGER PRIMARY KEY AUTOINCREMENT, ");  //as an Android SQLite db, the _id column is always assumed to exist
			for(Field field : columnDefinitionClass.getClass().getDeclaredFields()) {
				sqlStr.append(field.getName().toLowerCase());
				sqlStr.append(" TEXT, ");
			}
			sqlStr.deleteCharAt(sqlStr.length()-2);  //remove the last comma
			sqlStr.append(");");
			
			return sqlStr.toString();
		}
		
	}
	

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int numDeleted = 0;
		
		if(uri==null) {
			ClLog.e(TAG, "delete(): uri is null!");
			return -1;
		}
		
		try {
			sqlDb = sqlDbHelper.getWritableDatabase();
			
			selection = transformIdToSelectionClause(uri, selection);
			List<String> pathSegments = uri.getPathSegments();
			String tableName = pathSegments.get(0);  //table name will always be first segment of path, regardless of whether uri has appended id or not
			
			numDeleted = sqlDb.delete(tableName, selection, selectionArgs);
			
			getContext().getContentResolver().notifyChange(uri, null);  //signal observers that something was deleted
		} catch (SQLiteException e) {
			ClLog.e(TAG, "delete(): getWritableDatabase() failed!");
			ClLog.e(TAG, e);
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
		
		if(uri==null) {
			ClLog.e(TAG, "insert(): uri is null!");
			return null;
		}
		
		Uri returnUri = null;
		try {
			sqlDb = sqlDbHelper.getWritableDatabase();
			
			List<String> pathSegments = uri.getPathSegments();
			String tableName = pathSegments.get(0);  //table name will always be first segment of path, regardless of whether uri has appended id or not

			long rowId = sqlDb.insert(tableName, null, values);  //insert the passed in data as a new row
			
			if(rowId<0){
				ClLog.e(TAG, "Exception inserting values=(" + values + ") into uri=" + uri);
				return null;
			}
			
			//create full uri appended with newly added row id for return
			returnUri = ContentUris.appendId(Transactions.META_DATA.CONTENT_URI.buildUpon(), rowId).build();
			getContext().getContentResolver().notifyChange(uri, null);  //signal observers that something was added
		} catch (SQLiteException e) {
			ClLog.e(TAG, "insert(): getWritableDatabase() failed!");
			ClLog.e(TAG, e);
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
		
		if(uri==null) {
			ClLog.e(TAG, "query(): uri is null!");
			return null;
		}
		
		Cursor c = null;
		try {
			sqlDb = sqlDbHelper.getReadableDatabase();
			
			selection = transformIdToSelectionClause(uri, selection);  //process any trailing id specifiers in the uri
			List<String> pathSegments = uri.getPathSegments();
			String tableName = pathSegments.get(0);  //table name will always be first segment of path, regardless of whether uri has appended id or not
			
			SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
			queryBuilder.setTables(tableName);
			c = queryBuilder.query(sqlDb, projection, selection, selectionArgs, null, null, sortOrder);
			
			c.setNotificationUri(getContext().getContentResolver(), uri);  //register to watch for content uri changes
			c.moveToFirst();  //hack?: for whatever reason, calling moveToFirst() here allows you to close sqlDb w/out affecting the output of the cursor in the calling method (if you don't, the calling method gets a cursor with no data)
		} catch (SQLiteException e) {
			ClLog.e(TAG, "query(): getReadableDatabase() failed!");
			ClLog.e(TAG, e);
		} finally {
			sqlDb.close();
		}
		
		return c;
	}

	private static String transformIdToSelectionClause(Uri uri, String selection) {
		//If the content uri contains a trailing id number, this method will
		//create and return a sql WHERE clause that will isolate that record/id.
		//If the uri contains no trailing id number, the WHERE clause is
		//returned unchanged.
		final String ID_EQUALS = Transactions._ID+"=";
		
		try {
			final long specifiedId = ContentUris.parseId(uri);
			if(specifiedId>-1) {
				//narrow query to specific row if id specified in uri
				//(any supplied selection (WHERE) clause will then apply only to that row (which is not of much use :P))
				final String idSelection = ID_EQUALS+specifiedId;
				selection = (selection==null)? idSelection : idSelection+" AND "+selection;
			}
		} catch (NumberFormatException e) {
			//if we got this exception, then uri did not have an id as the last segment so we don't need to create an id selection statement
		}
		return selection;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		int numRowsUpdated = 0;
		
		if(uri==null) {
			ClLog.e(TAG, "update(): uri is null!");
			return -1;
		}
		
		try {
			sqlDb = sqlDbHelper.getWritableDatabase();
			
			selection = transformIdToSelectionClause(uri, selection);  //process any trailing id specifiers in the uri
			List<String> pathSegments = uri.getPathSegments();
			String tableName = pathSegments.get(0);  //table name will always be first segment of path, regardless of whether uri has appended id or not
			
			numRowsUpdated = sqlDb.update(tableName, values, selection, selectionArgs);  //update existing rows using passed in data
			
			getContext().getContentResolver().notifyChange(uri, null);  //signal observers that something was updated
		} catch (SQLiteException e) {
			ClLog.e(TAG, "update(): getWritableDatabase() failed!");
			ClLog.e(TAG, e);
		} finally {
			sqlDb.close();
		}
		
		return numRowsUpdated;
	}

}
