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
package com.creationline.cloudstack.util;
///This Version class is just for display purposes, when you want to present a "nice" version number
///to the user that is easier to understand than the revno/revid combo.  The major/minor version
///has no specific link to the state of the repository.  The revision information in the
///Revision class is automatically generated from bzr, and thus is used as the actual
///version tracking info for development/debugging purposes).

public class Version {
	// MAJOR_VER and MINOR_VER can be edited by hand.
	public static final int MAJOR_VER = 1;
	public static final int MINOR_VER = 0;
	
    @Override
	public String toString() {
    	return asString();
	}
    
	public static String asString() {
    	return MAJOR_VER+"."+MINOR_VER;
	}

}
