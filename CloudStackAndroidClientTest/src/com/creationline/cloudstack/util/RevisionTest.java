package com.creationline.cloudstack.util;

import android.test.AndroidTestCase;

public class RevisionTest extends AndroidTestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testRevisionFile() {
		//Test to see if the expected Provision.java file exists
	    try {
	    	Revision.asString();  //being able to build this line itself is the test; if we can build and call asString(), Revision.java is there

	    	//if we get here, then Revision class exists and we pass this test
		} catch (Exception e) {
			fail("Revision class not found: this class is not strictly necessary for CloudStackAndroidClient to work, but should be automatically generated (see generateRevision.py comments for details).");
		}					
	}

}
