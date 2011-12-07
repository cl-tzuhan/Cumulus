/*******************************************************************************
 * Copyright 2011 Creationline,Inc.
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
package com.creationline.cloudstack.engine.db;

import android.net.Uri;
import android.provider.BaseColumns;

import com.creationline.cloudstack.engine.CsRestContentProvider;

public class Errors implements BaseColumns {
	//The errors table records the errors that the app has run into which are interesting to the user.
	//A running list of past errors is kept, with the latest entry as the most recent error.
	
	//table columns other than _ID (_ID already included in BaseColumns declaration)
	//the following member vars will be automatically created as columns for the vms db (names case insensitive).
	//value of var must match name of var exactly, except just lower-case.
	public static final String ERRORTEXT = "errortext";
	public static final String ERRORCODE = "errorcode";
	public static final String ORIGINATINGCALL = "originatingcall";
	public static final String OCCURRED = "occurred";
	public static final String UNREAD = "unread";  //boolean value on db

	
	public static final class META_DATA {
		public static final String TABLE_NAME = "errors";
		public static final Uri CONTENT_URI = Uri.parse("content://"+CsRestContentProvider.AUTHORITY+"/"+TABLE_NAME);
	}
}
