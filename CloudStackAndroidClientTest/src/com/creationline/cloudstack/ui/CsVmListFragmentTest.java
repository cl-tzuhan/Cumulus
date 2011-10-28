package com.creationline.cloudstack.ui;

import android.test.AndroidTestCase;


public class CsVmListFragmentTest extends AndroidTestCase {
	
////NOTE: commenting this out for now as I don't know how to setup a Fragment for testing
///       (createRunningAndStoppedStateQuickActions() require the fragment to be attached
///        to an activity to work)
//	private CsVmListFragment csVmListFragment = null;
//	
//
//	protected void setUp() throws Exception {
//		super.setUp();
//		
//		csVmListFragment = new CsVmListFragment();
//		csVmListFragment.createRunningAndStoppedStateQuickActions();
//	}
//
//	protected void tearDown() throws Exception {
//		super.tearDown();
//	}
//	
//	
//	public void testConfigureAttributesBasedOnState() {
//		LayoutInflater inflater = (LayoutInflater)   getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE); 
//		View csVmListItem = inflater.inflate(R.layout.csvmlistitem, null);
//		
//		TextView stateTextView = (TextView)csVmListItem.findViewById(R.id.state);
//		stateTextView.setText("running");
//		
//		csVmListFragment.adapter.configureAttributesBasedOnState(csVmListItem, R.id.state, R.id.quickactionicon);
//		final ColorStateList stateTextColor = stateTextView.getTextColors();
//		final ColorStateList expectedTextColor = csVmListFragment.getResources().getColorStateList(R.color.vmrunning_color_selector);
//		
//		assertTrue("ColorStateList that was set is not the expected one", stateTextColor.equals(expectedTextColor));
//		
//		
//	}
	
	

}
