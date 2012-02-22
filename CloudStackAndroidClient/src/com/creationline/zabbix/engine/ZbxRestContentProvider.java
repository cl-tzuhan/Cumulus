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
package com.creationline.zabbix.engine;

import java.lang.reflect.Field;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

import com.creationline.common.engine.RestContentProviderBase;
import com.creationline.zabbix.engine.db.CpuLoads;
import com.creationline.zabbix.engine.db.CpuUtilization;
import com.creationline.zabbix.engine.db.DiskUsage;
import com.creationline.zabbix.engine.db.NetworkUtilization;
import com.creationline.zabbix.engine.db.Transactions;

/**
 * ZbxRestContentProvider is used to persist the REST requests going to Zabbix as well as the replies/data that come back.
 * Along with other persisted metadata concerning the status/dateTime of the request, the data saved should
 * provide a complete picture of the REST transactions currently in progress as well as previously executed
 * at any time.  The persisted return data also provide an offline cache that can be used by the app without internet access.
 * 
 * @author thsu
 *
 */
public class ZbxRestContentProvider extends RestContentProviderBase {
	
	public static final String AUTHORITY = "com.creationline.zabbix.engine.zbxrestcontentprovider";
	public static final String DB_NAME = "ZbxRestTransaction.db";
	private static final int DB_VERSION = 13;
	
	public static class ZbxSQLiteDatabaseHelper extends SQLiteDatabaseHelper {
		///This helper class simplifies management of the sql db itself

		public ZbxSQLiteDatabaseHelper(Context context) {
			super(context, DB_NAME, null, DB_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			//creating the Transactions table manually b/c I want a specific ordering to the columns that's not alphabetical
			final String tableColumns = ", "
										+ Transactions.REQUEST+" TEXT, "
										+ Transactions.POSTDATA+" TEXT, "
										+ Transactions.REQUEST_DATETIME+" TEXT, "
										+ Transactions.STATUS+" TEXT, "
										+ Transactions.REPLY+" TEXT,"
										+ Transactions.REPLY_DATETIME+" TEXT, "
										+ Transactions.JOBID+" TEXT, "
										+ Transactions.CALLBACK_INTENT_FILTER+" TEXT";
			db.execSQL("CREATE TABLE " + Transactions.META_DATA.TABLE_NAME + " ( "+Transactions._ID+" INTEGER PRIMARY KEY AUTOINCREMENT" + tableColumns + ");");
			
			//create the graph-use tables from the appropriate column definition classes
			db.execSQL(makeCreateTableSqlStr(new CpuLoads()));
			db.execSQL(makeCreateTableSqlStr(new CpuUtilization()));
			db.execSQL(makeCreateTableSqlStr(new DiskUsage()));
			db.execSQL(makeCreateTableSqlStr(new NetworkUtilization()));
			//..add new tables here
			///NOTE: if adding new table here, also add it to onUpgrade() and deleteAllData()
			
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			///destroy and re-create the db tables
			db.execSQL("DROP TABLE IF EXISTS " + Transactions.META_DATA.TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + CpuLoads.META_DATA.TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + CpuUtilization.META_DATA.TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + DiskUsage.META_DATA.TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + NetworkUtilization.META_DATA.TABLE_NAME);
			onCreate(db);
		}
		
		public static String makeCreateTableSqlStr(Object columnDefinitionClass) {
			//Build an sql statement that creates a db based off of the name and member vars of the passed in class.
			
			StringBuilder sqlStr = new StringBuilder("CREATE TABLE ");
			sqlStr.append(columnDefinitionClass.getClass().getSimpleName().toLowerCase());
			sqlStr.append(" ( ").append(BaseColumns._ID).append(" INTEGER PRIMARY KEY AUTOINCREMENT, ");  //as an Android SQLite db, the _id column is always assumed to exist
			for(Field field : columnDefinitionClass.getClass().getDeclaredFields()) {
				sqlStr.append(field.getName().toLowerCase());
				sqlStr.append(" DOUBLE, ");
			}
			sqlStr.deleteCharAt(sqlStr.length()-2);  //remove the last comma
			sqlStr.append(");");
			
			return sqlStr.toString();
		}
		
	}
	

	@Override
	public boolean onCreate() {
		sqlDbHelper = new ZbxSQLiteDatabaseHelper(getContext());
		return (sqlDbHelper==null)? false : true;
	}

	public static void deleteAllData(Context context) {
		//erase all data from each table
		context.getContentResolver().delete(Transactions.META_DATA.CONTENT_URI, null, null);
		context.getContentResolver().delete(CpuLoads.META_DATA.CONTENT_URI, null, null);
		context.getContentResolver().delete(CpuUtilization.META_DATA.CONTENT_URI, null, null);
		context.getContentResolver().delete(DiskUsage.META_DATA.CONTENT_URI, null, null);
		context.getContentResolver().delete(NetworkUtilization.META_DATA.CONTENT_URI, null, null);
	}
	

}
