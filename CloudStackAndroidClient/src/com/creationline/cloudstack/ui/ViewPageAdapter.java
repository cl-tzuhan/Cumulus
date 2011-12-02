package com.creationline.cloudstack.ui;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.creationline.cloudstack.util.ClLog;
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
