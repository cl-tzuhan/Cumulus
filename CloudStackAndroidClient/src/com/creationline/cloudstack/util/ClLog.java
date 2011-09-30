package com.creationline.cloudstack.util;

import android.util.Log;

@SuppressWarnings("unused")
public class ClLog {
	///Just like Log, but only logs messages depending on the set LOG_LEVEL

	public static final int LOG_LEVEL = android.util.Log.WARN;  //show log statements at this level or more detailed

	
	static public void v(String tag, String msg) {
		if(LOG_LEVEL<=android.util.Log.VERBOSE) {
			Log.v(tag, msg);
		}
	}
	
	static public void d(String tag, String msg) {
		if(LOG_LEVEL<=android.util.Log.DEBUG) {
			Log.d(tag, msg);
		}
	}
	
	static public void i(String tag, String msg) {
		if(LOG_LEVEL<=android.util.Log.INFO) {
			Log.i(tag, msg);
		}
	}

	static public void w(String tag, String msg) {
		if(LOG_LEVEL<=android.util.Log.WARN) {
			Log.w(tag, msg);
		}
	}
	
	
	static public void e(String tag, String msg) {
		if(LOG_LEVEL<=android.util.Log.ERROR) {
			Log.e(tag, msg);
		}
	}
	
	
}
