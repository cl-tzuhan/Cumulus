package com.creationline.cloudstack.ui;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.creationline.cloudstack.util.ClLog;
import com.viewpagerindicator.TitleProvider;

public class ViewPageAdapter extends FragmentPagerAdapter implements TitleProvider {


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
			case 0:
				return "Instances";
			case 1:
				return "Snapshots";
			default:
				ClLog.e("ViewPageAdapter.getTitle()", "Trying to swtich to non-existant position="+position);
				return null;
		}
	}

	
}
