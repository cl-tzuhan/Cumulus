package com.creationline.cloudstack.engine.db;

import android.net.Uri;
import android.provider.BaseColumns;

import com.creationline.cloudstack.engine.CsRestContentProvider;

public class Transactions implements BaseColumns {
	
	//table columns other than _ID (_ID already included in BaseColumns declaration)
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
	
	public static final class META_DATA {
		public static final Uri CONTENT_URI = Uri.parse("content://"+CsRestContentProvider.AUTHORITY+"/"+CsRestContentProvider.TABLE_NAMES.TRANSACTIONS);
	}
	
}
