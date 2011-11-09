package com.creationline.cloudstack.engine.db;

import android.net.Uri;
import android.provider.BaseColumns;

import com.creationline.cloudstack.engine.CsRestContentProvider;

public class Snapshots implements BaseColumns {
	//This class defines the columns of the snapshots database, which are exactly the response tags for the listSnapshots API call.
	//The text description for each column will not be copied here to make it easier to sync things to the official CS API.
	//Please refer to the official documentation for descriptions.  This file is current as of CS API 2.2.12:
	//  http://download.cloud.com/releases/2.2.0/api_2.2.12/user/listSnapshots.html
	//Each member var will be considered a column in the created db (embedded classes are skipped).  All columns are of type TEXT only.
	//The name of the member var, in lower case, will be used as the column name, so please make sure to avoid
	//1) case-sensitive names, and 2) names that are SQL keywords.
	
	//table columns other than _ID (_ID already included in BaseColumns declaration).
	//the following member vars will be automatically created as columns for the vms db (names case insensitive).
	//value of var must match name of var exactly, except just lower-case.
	public static final String ID = "id";
	public static final String ACCOUNT = "account";
	public static final String CREATED = "created";
	public static final String DOMAIN = "domain";
	public static final String DOMAINID = "domainid";
	public static final String INTERVALTYPE = "intervaltype";
	public static final String JOBID = "jobid";
	public static final String JOBSTATUS = "jobstatus";
	public static final String NAME = "name";
	public static final String SNAPSHOTTYPE = "snapshottype";
	public static final String STATE = "state";
	public static final String VOLUMEID = "volumeid";
	public static final String VOLUMENAME = "volumename";
	public static final String VOLUMETYPE = "volumetype";
	
	public static final class STATE_VALUES {
		public static final String CREATING = "Creating";
		public static final String BACKINGUP = "BackingUp";
		public static final String BACKEDUP = "BackedUp";
	}
	
	public static final class META_DATA {
		public static final String TABLE_NAME = "snapshots";
		public static final Uri CONTENT_URI = Uri.parse("content://"+CsRestContentProvider.AUTHORITY+"/"+TABLE_NAME);
	}
	
}
