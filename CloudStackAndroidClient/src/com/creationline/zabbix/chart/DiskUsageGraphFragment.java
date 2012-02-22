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
package com.creationline.zabbix.chart;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.creationline.cloudstack.R;
import com.creationline.common.utils.ClLog;
import com.creationline.zabbix.engine.api.ZbxApiConstants;
import com.creationline.zabbix.engine.db.CpuLoads;
import com.creationline.zabbix.engine.db.DiskUsage;


public class DiskUsageGraphFragment extends AChartEngineGraphFragmentBase {
	
	//size of legendTitles determines how many lines are graphed,
	//and must be <= size of lineColors/.fillColors/.styles
	final static String[] legendTitles = { ZbxApiConstants.VALUES.VFS_FS_SIZE_PFREE,
										   ZbxApiConstants.VALUES.VFS_FS_SIZE_PUSED, };
	public String[] legendTitlesAlias() { return legendTitles; }
	public static String[] getLegendTitles() { return legendTitles; }
	
	protected int[] lineColors() { return new int[] { Color.argb(255, 75, 171, 25),  //greenish
				  									  Color.argb(255, 160, 40, 30),  //reddish
													 }; };
	protected int[] fillColors() { return new int[] { Color.argb(255, 11, 97, 22),  //darkish green
													  Color.argb(255, 97, 11, 22),  //darkish red
													}; };
	protected PointStyle[] styles() { return new PointStyle[] { PointStyle.CIRCLE,
																PointStyle.TRIANGLE,
															   }; };
	
	@Override
	protected String Y_AXIS_LABEL() { return "Usage"; }

	@Override
	protected Uri DB_TABLE_CONTENT_URI() { return DiskUsage.META_DATA.CONTENT_URI; }

	
	public DiskUsageGraphFragment() {
		//empty constructor is needed by Android for automatically creating fragments from XML declarations
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.zbxdiskusagegraphfragment, container);
		return view;
	}
	
	/**
	 * Overriding for Disk Usage chart: this modified buildDataset() builds a stacked chart
	 * (default is just a line chart with the area-under-line filled).
	 * It does this by a) accumulating value points for neighboring series (note that for performance
	 * reasons, it does not actually check the time point to see that two values are for the exact
	 * same time.  This means that the result may be wrong if you give it two series with differing
	 * x-axis points), and 2) reversing the order of the saved series so that the lower value
	 * series will be drawn on top of the higher values series (this second point is a workaround
	 * for AChartEngine's default line&fill behavior to make it look like a stacked chart).
	 */
	@Override
	public XYMultipleSeriesDataset buildDataset(String[] legendTitles) {
		final XYMultipleSeriesDataset emptyDataset = new XYMultipleSeriesDataset();  //completely empty dataset for use as return value in case of error
		
		if(legendTitles==null) {
			return emptyDataset;
		}
		
		//query data from db
		final String[] columns = new String[] { CpuLoads.ITEMNAME, CpuLoads.CLOCK, CpuLoads.VALUE };
		final String whereClause = CpuLoads.HOST+"=? AND "+CpuLoads.ITEMNAME+"=?";
		final String sortOrder = CpuLoads.CLOCK;
	
		List<Date[]> xValues = new ArrayList<Date[]>();
		List<double[]> yValues = new ArrayList<double[]>();
		
		double[] previousMeasuredUsage = null;  //holds a copy of the last-parsed series
		for(int legendTitlesIndex=0; legendTitlesIndex<legendTitles.length; legendTitlesIndex++) {
			final String[] selectionArgs = new String[] { host, legendTitles[legendTitlesIndex] };
			Cursor c = getActivity().getContentResolver().query(DB_TABLE_CONTENT_URI(), columns, whereClause, selectionArgs, sortOrder);
	
			if(c==null) {
				ClLog.i("DiskUsageGraphFragment.buildDataset()", "null cursor returned by query (most likely, specified db does not exist)");
				return emptyDataset;
			}
			
			Date[] dateData = new Date[c.getCount()];
			double[] measuredUsage = new double[c.getCount()];
			final int clockColumnIndex = c.getColumnIndex(CpuLoads.CLOCK);
			final int valueColumnIndex = c.getColumnIndex(CpuLoads.VALUE);

			//parsed retrieved data into arrays, converting the clock values into java date objects
			c.moveToFirst();
			for(int i=0; !c.isAfterLast(); c.moveToNext(), i++) {
				final double clockInMilliSec = c.getDouble(clockColumnIndex);  //timestamp saved to db is in _milliseconds_ from epoch
				dateData[i] = new Date((long)clockInMilliSec);
				double usageValue = c.getDouble(valueColumnIndex);
				if(previousMeasuredUsage!=null && i<previousMeasuredUsage.length) {
					usageValue += previousMeasuredUsage[i];  //if this is not the 1st series, combine this value with value from the same time of the previous series
				}
				measuredUsage[i] = usageValue;
			}
			c.close();
	
			xValues.add(0, dateData);  //save to beginning to reverse the order the series are drawn
			yValues.add(0, measuredUsage);  //save to beginning to reverse the order the series are drawn
			
			previousMeasuredUsage = measuredUsage;
		}
		previousMeasuredUsage = null;
		
		return buildDateDataset(reversedArray(legendTitles), xValues, yValues);
	}
	
	/**
	 * Returns a reversed _copy_ of the passed-in array.
	 * @param array array to reverse
	 * @return copy of reversed array
	 */
	public String[] reversedArray(String[] array) {
		List<String> list = Arrays.asList(array.clone());  //using clone here, otherwise asList() would reflect subsequent changes to the original array
		Collections.reverse(list);
		String[] reversedArray = list.toArray(new String[]{});
		list = null;
		return reversedArray;
	}
	
	/**
	 * Overriding for Disk Usage chart: this modified buildDataset() builds a stacked chart
	 * (default is just a line chart with the area-under-line filled).
	 * It does this by reversing declared color/styles order so it matches the order of the
	 * series in the dataset (which is itself reversed so the smaller-valued series can be
	 * drawn over the larger value series).
	 */
	@Override
	public void configureRendererWithLineAttributes(final int[] lineColors,
			final int[] fillColors, final PointStyle[] styles, XYMultipleSeriesRenderer renderer) {
		//set properties for each line to graph
		if(lineColors!=null && fillColors!=null && styles!=null) {
			for (int i=lineColors.length-1; i >= 0; i--) {
				XYSeriesRenderer r = new XYSeriesRenderer();
				r.setColor(lineColors[i]);
				//r.setPointStyle(styles[i]);
				//r.setFillPoints(true);
				//r.setDisplayChartValues(true);
				r.setFillBelowLine(true);
				r.setFillBelowLineColor(fillColors[i]);
				renderer.addSeriesRenderer(r);
			}
		}
	}
	
}
