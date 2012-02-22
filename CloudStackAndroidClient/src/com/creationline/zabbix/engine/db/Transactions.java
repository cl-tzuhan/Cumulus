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
package com.creationline.zabbix.engine.db;

import android.net.Uri;
import android.provider.BaseColumns;

import com.creationline.zabbix.engine.ZbxRestContentProvider;

public class Transactions implements BaseColumns {
	//The transactions table stores the actual REST calls made against the Zabbix server
	//by this app, along with any reply and various meta-data concerning the call itself.
	
	//table columns other than _ID (_ID already included in BaseColumns declaration)
	//value of var must match name of var exactly, except just lower-case.
	public static final String REQUEST = "request";
	public static final String POSTDATA = "postdata";
	public static final String STATUS = "status";
	public static final String REPLY = "reply";
	public static final String REQUEST_DATETIME = "request_dateTime";
	public static final String REPLY_DATETIME = "reply_dateTime";
	public static final String JOBID = "jobid";
	public static final String CALLBACK_INTENT_FILTER = "callback_intent_filter";

	
	//allowed values for the status column
	public static final class STATUS_VALUES {
		public static final String IN_PROGRESS = "in_progress"; //request accepted and is being executed
		public static final String SUCCESS = "success";         //request is finished and succeeded
		public static final String FAIL = "fail";               //request is finished and failed (with error from CS itself)
		public static final String ABORTED = "aborted";         //request itself did not make it to CS for some reason
	}
	
	public static final class META_DATA {
		public static final String TABLE_NAME = "transactions";
		public static final Uri CONTENT_URI = Uri.parse("content://"+ZbxRestContentProvider.AUTHORITY+"/"+TABLE_NAME);
	}
	
}
