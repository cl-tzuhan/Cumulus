package com.creationline.cloudstack.ui;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.creationline.cloudstack.util.ClLog;

public class ViewPageAdapter extends FragmentPagerAdapter {


	public ViewPageAdapter(FragmentManager fm) {
		super(fm);
	}

	@Override
	public int getCount() {
		return 2;
	}
	
	@Override
	public Fragment getItem(int position) {
		//order of the views in csac is hard-coded
		switch(position) {
			case 0:
				return new CsVmListFragment();
			case 1:
				return new CsSnapshotList();
			default:
				ClLog.e("ViewPageAdapter.getItem()", "Trying to swtich to non-existant position="+position);
				return null;
		}
	}

	
}
