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
package com.creationline.cloudstack.engine;

public class CsApiConstants {

	public static class API {
		//Virtual Machine
		public static final String listVirtualMachines = "listVirtualMachines";
		public static final String startVirtualMachine = "startVirtualMachine";
		public static final String stopVirtualMachine = "stopVirtualMachine";
		public static final String rebootVirtualMachine = "rebootVirtualMachine";
		
		//Snapshot
		public static final String deleteSnapshot = "deleteSnapshot";
		
		//Other
		public static final String login = "login";
		public static final String listAccounts = "listAccounts";
		public static final String logout = "logout";
		
	}

	public static class LOGIN_PARAMS {
		public static final String USERNAME = "username";
		public static final String PASSWORD = "password";
		public static final String DOMAIN = "domain";
		
		public static final String SESSIONKEY = "sessionkey";
	}

	public static class LISTACCOUNTS_PARAMS {
		public static final String USER = "user";
		public static final String USERNAME = "username";
		public static final String APIKEY = "apikey";
		public static final String SECRETKEY = "secretkey";
	}
	
	public static class ERROR_PARAMS {
		public static final String ERRORTEXT = "errortext";
	}
	
}
