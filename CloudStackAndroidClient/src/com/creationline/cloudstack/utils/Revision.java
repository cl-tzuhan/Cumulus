package com.creationline.cloudstack.utils;
///This Revision.java files is not checked into Bazaar.
///It is preferable to have this class in source control as well,
///but currently there does not seem to be a way update
///REVISION_NO and REVISION_ID automatically upon commit _and_
///sync the newest values to bzr at the same time (this is an
///artifact of the way bzr's pre_commit and start_commit hooks work.
///We can use pre_commit to get the next revno/revid and write it to
///a file, but pre_commit does not allow changing of the contents of
///the commit.  start_commit can be used to change the contents of
///the commit before it is actually done (though cannot add new changes
///to the commit itself apparently?), but it does not have access
///to the next revno/revid that will be assigned once the commit
///actually goes through.  The end result of this is that the
///revision recorded in Revision.java will always be one revision
////behind the tip of the repository).
///
///Therefore, we will use the generateRevision.py script to
///automatically generate this Revision class upon pre_commit
///with the latest revision information embedded (the code for
///this file will also be saved inside generateRevision.py,
///which is under source control).  We will use this revision
///info as the developer-use version tracking information.
///
///Version.java, provided separately, provides a manual
///major/minor version number that can be arbitrarily edited by
///hand for end-user display purposes.  This file is sync-ed to bzr.

public class Revision {
	public static final int REVISION_NO = 141;
    public static final String REVISION_ID = "tzuhan@creationline.com-20120222072142-j0gys7a5zvq9luzf";

    @Override
	public String toString() {
		return asString();
	}

    public static String asString() {
    	return REVISION_NO+" ["+REVISION_ID+"]";
    }
}
