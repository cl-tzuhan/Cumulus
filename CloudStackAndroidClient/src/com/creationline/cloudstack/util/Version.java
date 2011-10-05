package com.creationline.cloudstack.util;

public class Version {
	// MAJOR_VER and MINOR_VER can be edited by hand.  REVISION_NO and REVISION_ID will be updated
	// automatically every time the source is committed to Bazaar.
	public static final int MAJOR_VER = 0;
	public static final int MINOR_VER = 1;
    public static final int BUILD_NO = 15;  //this must be on its own line (updated by incrementVersion.py automatically)
	
    @Override
	public String toString() {
    	String returnStr = MAJOR_VER+"."+MINOR_VER+"."+BUILD_NO;
    	try {
    		Class revisionClass = Class.forName("Revision");
    		returnStr += "  ("+revisionClass.toString()+")";
    	} catch(ClassNotFoundException e) {
    		;  //if Revision.java is not present, don't include revision info with version info
    	}
		return returnStr;
	}

}
