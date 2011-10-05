package com.creationline.cloudstack.util;

public class Version {
	// MAJOR_VER and MINOR_VER can be edited by hand.  REVISION_NO and REVISION_ID will be updated
	// automatically every time the source is committed to Bazaar.
	public static final int MAJOR_VER = 0;
	public static final int MINOR_VER = 1;
	public static final int REVISION_NO = -1;  //this must be on its own line (updated by incrementVersion.py automatically)
    public static final String REVISION_ID = "xxxx";  //this must be on its own line (updated by incrementVersion.py automatically)

    @Override
	public String toString() {
		return MAJOR_VER+"."+MINOR_VER+"."+REVISION_NO+"  ["+REVISION_ID+"]";
	}

}
