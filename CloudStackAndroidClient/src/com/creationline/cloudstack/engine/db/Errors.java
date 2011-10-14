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

	
	public static final class META_DATA {
		public static final String TABLE_NAME = "errors";
		public static final Uri CONTENT_URI = Uri.parse("content://"+CsRestContentProvider.AUTHORITY+"/"+TABLE_NAME);
	}
}
