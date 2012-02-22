package com.creationline.cloudstack.utils;

import com.creationline.common.utils.ClLog;

import android.test.AndroidTestCase;

public class ClLogTest extends AndroidTestCase {

    public void testLogLevel() {
    	//Specifically testing log level here to minimize accidentally
    	//releasing a DEBUG level app into the wild.
    	assertEquals("ClLog.LOG_LEVEL should be set to WARN for release builds!", ClLog.WARN, ClLog.LOG_LEVEL);
    }

}
