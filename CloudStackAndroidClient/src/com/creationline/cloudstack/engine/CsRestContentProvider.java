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
package com.creationline.cloudstack.engine;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.creationline.cloudstack.engine.db.Errors;
import com.creationline.cloudstack.engine.db.Snapshots;
import com.creationline.cloudstack.engine.db.Transactions;
import com.creationline.cloudstack.engine.db.Vms;
import com.creationline.common.engine.RestContentProviderBase;

/**
 * CsRestContentProvider is used to persist the REST requests going to CloudStack as well as the replies/data that come back.
 * Along with other persisted metadata concerning the status/dateTime of the request, the data saved should
 * provide a complete picture of the REST transactions currently in progress as well as previously executed
 * at any time.  The persisted return data also provide an offline cache that can be used by the app without internet access.
 * 
 * @author thsu
 *
 */
public class CsRestContentProvider extends RestContentProviderBase {
	public static final String AUTHORITY = "com.creationline.cloudstack.engine.csrestcontentprovider";
	
	public static final String DB_NAME = "CsRestTransaction.db";
	private static final int DB_VERSION = 7;
	
	
	public static class CsSQLiteDatabaseHelper extends SQLiteDatabaseHelper {
		///This helper class simplifies management of the sql db itself

		public CsSQLiteDatabaseHelper(Context context) {
			super(context, DB_NAME, null, DB_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			//creating the Transactions table manually b/c I want a specific ordering to the columns that's not alphabetical
			final String tableColumns = ", "
										+ Transactions.REQUEST+" TEXT, "
										+ Transactions.REQUEST_DATETIME+" TEXT, "
										+ Transactions.STATUS+" TEXT, "
										+ Transactions.REPLY+" TEXT,"
										+ Transactions.REPLY_DATETIME+" TEXT, "
										+ Transactions.JOBID+" TEXT, "
										+ Transactions.CALLBACK_INTENT_FILTER+" TEXT";
			db.execSQL("CREATE TABLE " + Transactions.META_DATA.TABLE_NAME + " ( "+Transactions._ID+" INTEGER PRIMARY KEY AUTOINCREMENT" + tableColumns + ");");
			
			//create the ui-use tables from the appropriate column definition classes
			db.execSQL(makeCreateTableSqlStr(new Vms()));
			db.execSQL(makeCreateTableSqlStr(new Snapshots()));
			
			//create the errors table
			db.execSQL(makeCreateTableSqlStr(new Errors()));
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			///destroy and re-create the db tables
			db.execSQL("DROP TABLE IF EXISTS " + Transactions.META_DATA.TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + Vms.META_DATA.TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + Snapshots.META_DATA.TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + Errors.META_DATA.TABLE_NAME);
			onCreate(db);
		}
		
	}
	

	@Override
	public boolean onCreate() {
		sqlDbHelper = new CsSQLiteDatabaseHelper(getContext());
		return (sqlDbHelper==null)? false : true;
	}

	public static void deleteAllData(Context context) {
		//erase all data from each table
		context.getContentResolver().delete(Transactions.META_DATA.CONTENT_URI, null, null);
		context.getContentResolver().delete(Vms.META_DATA.CONTENT_URI, null, null);
		context.getContentResolver().delete(Snapshots.META_DATA.CONTENT_URI, null, null);
		context.getContentResolver().delete(Errors.META_DATA.CONTENT_URI, null, null);
	}
	

}
