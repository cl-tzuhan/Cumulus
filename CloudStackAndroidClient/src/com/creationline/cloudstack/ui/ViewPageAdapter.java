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
package com.creationline.cloudstack.ui;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.creationline.common.utils.ClLog;
import com.viewpagerindicator.TitleProvider;

public class ViewPageAdapter extends FragmentPagerAdapter implements TitleProvider {
	
	public static final int ACCOUNT_PAGE = 0;
	public static final int INSTANCES_PAGE = 1;
	public static final int SNAPSHOTS_PAGE = 2;


	public ViewPageAdapter(FragmentManager fm) {
		super(fm);
	}

	@Override
	public int getCount() {
		return 3;
	}
	
	@Override
	public Fragment getItem(int position) {
		//order of the views in csac is hard-coded
		switch(position) {
			case ACCOUNT_PAGE:
				return new CsAccountFragment();
			case INSTANCES_PAGE:
				return new CsVmListFragment();
			case SNAPSHOTS_PAGE:
				return new CsSnapshotListFragment();
			default:
				ClLog.e("ViewPageAdapter.getItem()", "Trying to swtich to non-existant position="+position);
				return null;
		}
	}

	@Override
	public String getTitle(int position) {
		//order of the views in csac is hard-coded
		switch(position) {
			case ACCOUNT_PAGE:
				return "Account";
			case INSTANCES_PAGE:
				return "Instances";
			case SNAPSHOTS_PAGE:
				return "Snapshots";
			default:
				ClLog.e("ViewPageAdapter.getTitle()", "Trying to swtich to non-existant position="+position);
				return null;
		}
	}

	
}
