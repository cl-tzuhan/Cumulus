/*******************************************************************************
 * Copyright 2011-2012 Creationline,Inc.
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
package com.creationline.zabbix.engine.api;

public class ZbxApiConstants {

	public static class API {
		public static class USER {
			public static final String LOGIN = "user.login";
		}

		public static class HOST {
			public static final String GET = "host.get";
		}

		public static class ITEM {
			public static final String GET = "item.get";
		}

		public static class HISTORY {
			public static final String GET = "history.get";
		}

	}

	public static class FIELDS {
		public static final String JSONRPC = "jsonrpc";
		public static final String PARAMS = "params";
		public static final String METHOD = "method";
		public static final String AUTH = "auth";

		public static final String USER = "user";
		public static final String PASSWORD = "password";

		public static final String OUTPUT = "output";
		public static final String FILTER = "filter";

		public static final String IP = "ip";
		
		public static final String HOST = "host";
		public static final String HOSTID = "hostid";
		public static final String HOSTIDS = "hostids";

		public static final String ITEMID = "itemid";
		public static final String ITEMIDS = "itemids";
		public static final String HISTORY = "history";
		public static final String TIME_FROM = "time_from";
		public static final String TIME_TILL = "time_till";
		public static final String SORTFIELD = "sortfield";
		public static final String SORTORDER = "sortorder";

		public static final String SEARCH = "search";
		public static final String KEY_ = "key_";
		
		public static final String MESSAGE = "message";
		public static final String DATA = "data";
		
	}
	
	public static class VALUES {
		public static final String VER20 = "2.0";
		public static final String REFER = "refer";
		public static final String EXTEND = "extend";

		public static final String SYSTEM_CPU_LOAD_AVG1 = "system.cpu.load[,avg1]";
		public static final String SYSTEM_CPU_LOAD_AVG5 = "system.cpu.load[,avg5]";
		public static final String SYSTEM_CPU_LOAD_AVG15 = "system.cpu.load[,avg15]";

		public static final String SYSTEM_CPU_UTIL_IDLE_AVG1 = "system.cpu.util[,idle,avg1]";
		public static final String SYSTEM_CPU_UTIL_USER_AVG1 = "system.cpu.util[,user,avg1]";
		public static final String SYSTEM_CPU_UTIL_SYSTEM_AVG1 = "system.cpu.util[,system,avg1]";

		public static final String VFS_FS_SIZE_PUSED = "vfs.fs.size[/,pused]";
		public static final String VFS_FS_SIZE_PFREE = "vfs.fs.size[/,pfree]";
		
		public static final String NET_IF_IN_ETH0 = "net.if.in[eth0,bytes]";
		public static final String NET_IF_OUT_ETH0 = "net.if.out[eth0,bytes]";

		public static final String CLOCK = "clock";
		
		public static final String ASC = "ASC";

	}
	
	public static class ERROR {
		public static class MESSAGE {
			public static final String INVALIDPARAMS = "Invalid params.";
		}
		
		public static class DATA {
			public static final String NOTAUTHORIZED = "Not authorized";
			public static final String LOGININCORRECT = "Login name or password is incorrect";
		}
	}
	
}
