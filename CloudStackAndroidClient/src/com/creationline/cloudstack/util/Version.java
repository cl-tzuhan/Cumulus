package com.creationline.cloudstack.util;

public class Version {
	// MAJOR_VER and MINOR_VER can be edited by hand.  REVISION_NO and REVISION_ID will be updated
	// automatically every time the source is committed to Bazaar.
	public static final int MAJOR_VER = 0;
	public static final int MINOR_VER = 1;
	public static final int REVISION_NO = 15;  //this must be on its own line (updated by incrementVersion.py automatically)
    public static final String REVISION_ID = "tzuhan@creationline.com-20111005080206-jyy3jgk9rgh2yiyh";  //this must be on its own line (updated by incrementVersion.py automatically)

    @Override
	public String toString() {
		return MAJOR_VER+"."+MINOR_VER+"."+REVISION_NO+"  ["+REVISION_ID+"]";
	}

}
