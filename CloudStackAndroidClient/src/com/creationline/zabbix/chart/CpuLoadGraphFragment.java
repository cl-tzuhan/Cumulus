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

import org.achartengine.chart.PointStyle;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.creationline.cloudstack.R;
import com.creationline.zabbix.engine.api.ZbxApiConstants;
import com.creationline.zabbix.engine.db.CpuLoads;

public class CpuLoadGraphFragment extends AChartEngineGraphFragmentBase {
	
	//size of legendTitles determines how many lines are graphed,
	//and must be <= size of lineColors/.fillColors/.styles
	final static String[] legendTitles = new String[] { ZbxApiConstants.VALUES.SYSTEM_CPU_LOAD_AVG1,
													    ZbxApiConstants.VALUES.SYSTEM_CPU_LOAD_AVG5,
														ZbxApiConstants.VALUES.SYSTEM_CPU_LOAD_AVG15, };
	public String[] legendTitlesAlias() { return legendTitles; }
	public static String[] getLegendTitles() { return legendTitles; }
                                                //greenish                    //yellowish					 //reddish
	final static int[] lineColors = new int[] { Color.argb(200, 75, 171, 25), Color.argb(200, 227, 227, 30), Color.argb(200, 160, 40, 30), };
	final static int[] fillColors = new int[] { Color.argb(50, 75, 171, 25),  Color.argb(50, 227, 227, 30),  Color.argb(50, 160, 40, 30), };
	                                            //fill colors are more transparent versions of the line colors
	final static PointStyle[] styles = new PointStyle[] { PointStyle.CIRCLE, PointStyle.DIAMOND, PointStyle.TRIANGLE, };
	
	@Override
	protected String Y_AXIS_LABEL() { return "Load"; }

	@Override
	protected Uri DB_TABLE_CONTENT_URI() { return CpuLoads.META_DATA.CONTENT_URI; }

	
	public CpuLoadGraphFragment() {
		//empty constructor is needed by Android for automatically creating fragments from XML declarations
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.zbxcpuloadgraphfragment, container);
		return view;
	}
	
	
	
}