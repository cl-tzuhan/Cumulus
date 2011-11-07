package com.creationline.cloudstack.mock;

import android.test.mock.MockApplication;

public class CsacMockApplication extends MockApplication {

	///Note: This is necessary as the MockApplication used by the Junit test environment
	///      does not contain any system services.  CsRestService needs the AlarmManager
	///      service for scheduling tasks to check up on async job status.
	///      Luckily, we aren't testing the actual request that goes out to the Internet,
	///      so we are ok w/ just returning null for a service here so that CsRestService
	///      can skip the alarm-related code.  This only applies for unit testing.  The
	///      AlarmManager is required for production code, otherwise CsRestService won't be
	///      able to wake itself up to check on async job status.
	public Object getSystemService(String name) {
		return null;  //for now we will just return null for any system service requested
	}
}
