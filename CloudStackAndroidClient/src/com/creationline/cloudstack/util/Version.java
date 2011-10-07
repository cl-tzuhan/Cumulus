package com.creationline.cloudstack.util;
///This Version class is just for display purposes, when you want to present a "nice" version number
///to the user that is easier to understand than the revno/revid combo.  The major/minor version
///has no specific link to the state of the repository.  The revision information in the
///Revision class is automatically generated from bzr, and thus is used as the actual
///version tracking info for development/debugging purposes).

public class Version {
	// MAJOR_VER and MINOR_VER can be edited by hand.
	public static final int MAJOR_VER = 0;
	public static final int MINOR_VER = 2;
	
    @Override
	public String toString() {
    	return asString();
	}
    
	public static String asString() {
    	return MAJOR_VER+"."+MINOR_VER;
	}

}
