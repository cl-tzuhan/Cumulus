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
package com.creationline.common.utils;

import android.util.Log;

@SuppressWarnings("unused")
public class ClLog {
	///Just like Log, but only logs messages depending on the set LOG_LEVEL

	//use the same log level definitions as android.util.Log to make things easy
	//(had to re-define as we couldn't inherit from Log)
	public static final int VERBOSE = android.util.Log.VERBOSE;
	public static final int DEBUG = android.util.Log.DEBUG;
	public static final int INFO = android.util.Log.INFO;
	public static final int WARN = android.util.Log.WARN;
	public static final int ERROR = android.util.Log.ERROR;

	//show log statements at this level or more detailed
	public static final int LOG_LEVEL = WARN;  // release builds should be level WARN
	
	static public void v(String tag, String msg) {
		if(LOG_LEVEL<=VERBOSE) {
			Log.v(tag, msg);
		}
	}
	
	static public void d(String tag, String msg) {
		if(LOG_LEVEL<=DEBUG) {
			Log.d(tag, msg);
		}
	}
	
	static public void i(String tag, String msg) {
		if(LOG_LEVEL<=INFO) {
			Log.i(tag, msg);
		}
	}

	static public void w(String tag, String msg) {
		if(LOG_LEVEL<=WARN) {
			Log.w(tag, msg);
		}
	}
	
	
	static public void e(String tag, String msg) {
		if(LOG_LEVEL<=ERROR) {
			Log.e(tag, msg);
		}
	}
	
	static public void e(String tag, Throwable t) {
		if(LOG_LEVEL<=ERROR) {
			Log.e(tag, Log.getStackTraceString(t));
		}
	}
	
}
