package com.creationline.cloudstack.engine.db;

import android.net.Uri;
import android.provider.BaseColumns;

import com.creationline.cloudstack.engine.CsRestContentProvider;

public class Vms implements BaseColumns {
	//This class defines the columns of the vms database, which are exactly the response tags for the listVirtualMachines API call.
	//The text description for each column will not be copied here to make it easier to sync things to the official CS API.
	//Please refer to the official documentation for descriptions.  This file is current as of CS API 2.2.11:
	//  http://download.cloud.com/releases/2.2.0/api_2.2.11/user/listVirtualMachines.html
	//Each member var will be considered a column in the created db (embedded classes are skipped).  All columns are of type TEXT only.
	//The name of the member var, in lower case, will be used as the column name, so please make sure to avoid
	//1) case-sensitive names, and 2) names that are SQL keywords.
	
	//table columns other than _ID (_ID already included in BaseColumns declaration).
	//the following member vars will be automatically created as columns for the vms db (names case insensitive).
	//value of var must match name of var exactly, except just lower-case.
	public static final String ID = "id";
	public static final String ACCOUNT = "account";
	public static final String CPUNUMBER = "cpunumber";
	public static final String CPUSPEED = "cpuspeed";
	public static final String CPUUSED = "cpuused";
	public static final String CREATED = "created";
	public static final String DISPLAYNAME = "displayname";
	public static final String DOMAIN = "domain";
	public static final String DOMAINID = "domainid";
	public static final String FORVIRTUALNETWORK = "forvirtualnetwork";
	public static final String GROUPA = "groupa";  //member var name purposefully named different than CS API property name b/c "group" is a keyword in SQL which causes statements with this word in it to choke
												   //(also see META_DATA.CS_ORIGINAL_GROUP_FIELD_NAME)
	public static final String GROUPID = "groupid";
	public static final String GUESTOSID = "guestosid";
	public static final String HAENABLE = "haenable";
	public static final String HOSTID = "hostid";
	public static final String HOSTNAME = "hostname";
	public static final String HYPERVISOR = "hypervisor";
	public static final String IPADDRESS = "ipaddress";
	public static final String ISODISPLAYTEXT = "isodisplaytext";
	public static final String ISOID = "isoid";
	public static final String ISONAME = "isoname";
	public static final String JOBID = "jobid";
	public static final String JOBSTATUS = "jobstatus";
	public static final String MEMORY = "memory";
	public static final String NAME = "name";
	public static final String NETWORKKBSREAD = "networkkbsread";
	public static final String NETWORKKBSWRITE = "networkkbswrite";
	public static final String PASSWORD = "password";
	public static final String PASSWORDENABLED = "passwordenabled";
	public static final String ROOTDEVICEID = "rootdeviceid";
	public static final String ROOTDEVICETYPE = "rootdevicetype";
	public static final String SERVICEOFFERINGID = "serviceofferingid";
	public static final String SERVICEOFFERINGNAME = "serviceofferingname";
	public static final String STATE = "state";
	public static final String TEMPLATEDISPLAYTEXT = "templatedisplaytext";
	public static final String TEMPLATEID = "templateid";
	public static final String TEMPLATENAME = "templatename";
	public static final String ZONEID = "zoneid";
	public static final String ZONENAME = "zonename";
	public static final String NIC = "nic";  //(*)the nic param is actually a list of possible Nic objects, so this field will just hold the json representation of the list-of-objects
	public static final String SECURITYGROUP = "securitygroup";  //(*)the securitygroup param is actually a list of possible SecurityGroup objects, so this field will just hold the json representation of the list-of-objects

	
	public static final class Nic {
		public static final String ID = "id";
		public static final String BROADCASTURI = "broadacsturi";
		public static final String IPADDRESS = "ipaddress";
		public static final String ISDEFAULT = "isdefault";
		public static final String ISOLATIONURI = "isolationuri";
		public static final String MACADDRESS = "macaddress";
		public static final String NETMASK = "netmask";
		public static final String NETWORKID = "networkid";
		public static final String TRAFFICTYPE = "traffictype";
		public static final String TYPE = "type";
	}

	
	public static final class SecurityGroup {
		public static final String ID = "id";
		public static final String ACCOUNT = "account";
		public static final String DESCRIPTION = "description";
		public static final String DOMAIN = "domain";
		public static final String DOMAINID = "domainid";
		public static final String JOBID = "jobid";
		public static final String JOBSTATUS = "jobstatus";
		public static final String NAME = "name";
		public static final String INGRESSRULE = "ingressrule";  //(*)the ingressrule param is actually a list of possible IngressRule objects, so this field will just hold the json representation of the list-of-objects

		public static final class IngressRule {
			public static final String ACCOUNT = "account";
			public static final String CIDR = "cidr";
			public static final String ENDPORT = "endport";
			public static final String ICMPCODE = "icmpcode";
			public static final String ICMPTYPE = "icmptype";
			public static final String PROTOCOL = "protocol";
			public static final String RULEID = "ruleid";
			public static final String SECURITYGROUPNAME = "securitygroupname";
			public static final String STARTPORT = "startport";
		}
	}
	
	public static final class STATE_VALUES {
		public static final String RUNNING = "Running";
		public static final String STOPPED = "Stopped";
		public static final String STARTING = "Starting";
		public static final String STOPPING = "Stopping";

		public static final String REBOOTING = "Rebooting";  //this is not a real CS API value; this used locally by CSAC
	}
	
	public static final class META_DATA {
		public static final String TABLE_NAME = "vms";
		public static final Uri CONTENT_URI = Uri.parse("content://"+CsRestContentProvider.AUTHORITY+"/"+TABLE_NAME);
		public static final String CS_ORIGINAL_GROUP_FIELD_NAME = "group";  //the real "group" field name used by the cs server/api
	}
	
}
