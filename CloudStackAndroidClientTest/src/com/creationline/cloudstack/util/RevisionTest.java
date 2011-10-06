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
	    	Revision.toStaticString();  //being able to build this line itself is the test; if we can build and call toStaticString, Revision.java is there

	    	//if we get here, then Revision class exists and we pass this test
		} catch (Exception e) {
			fail("Revision class not found: this class is not strictly necessary for CloudStackAndroidClient to work, but is better if it is there (see RevisionTest.java comments for details).");
		}
	    
		
		/*----
		 * In case the Revision.java file has been lost somehow (as it is not meant to be sync-ed to bzr),
		 * providing a copy of the text here (up-to-date as of 2011/10/06) for backup purposes.
		 * This file should be located under the trunk in: ./CloudStackAndroidClient/src/com/creationline/cloudstack/util/Revision.java
		 * 
			package com.creationline.cloudstack.util;
			///This Revision.java files is not checked into Bazaar.  It is preferable to have this information in source control as well
			///but currently there does not seem to be a way update REVISION_NO and REVISION_ID automatically upon commit _as well as_
			///keep the newest values sync-ed to bzr (this is an artifact of the way bzr's pre_commit and start_commit hooks work.
			///We can use pre_commit to get the next revno/revid and write it to a file, but pre_commit does not allow changing of the
			///contents of the commit.  start_commit can be used to change the contents of the commit before it's actually done, but
			///it does not have access to the next revno/revid that will be assigned once the commit actually goes through.  The end
			///result of this is that the revision recorded in Revision.java will always be one revision behind the tip of the repository).
			///
			///So the way this is being handled is that Version.java will be sync-ed to bzr and will record just a numerical build_no
			///to keep track of individual builds.  For better information, we will write out the revno/revid as well to this Revision.java
			///file upon commit (and tie it to the build_no), but will not sync this file to bzr.
			
			public class Revision {
				//BUILD_NO is automatically copied from Version.java every time the source is committed to Bazaar.
				//REVISION_NO and REVISION_ID will be updated automatically every time the source is committed to Bazaar.
			    public static final int BUILD_NO = 61;  //this must be on its own line (updated by incrementVersion.py automatically)
				public static final int REVISION_NO = 23;  //this must be on its own line (updated by incrementRevision.py automatically)
			    public static final String REVISION_ID = "tzuhan@creationline.com-20111005112613-3i928yu72rcmso7b";  //this must be on its own line (updated by incrementRevision.py automatically)
			
			    @Override
				public String toString() {
					return REVISION_NO+"  ["+REVISION_ID+"]";
				}
			
			}
		 *
		 *----*/
					
	}

}
