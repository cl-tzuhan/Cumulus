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

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.creationline.cloudstack.R;
import com.creationline.zabbix.engine.api.ZbxApiConstants;
import com.creationline.zabbix.engine.db.CpuUtilization;


public class CpuUtilizationGraphFragment extends AChartEngineGraphFragmentBase {
	
	//size of legendTitles determines how many lines are graphed,
	//and must be <= size of lineColors/.fillColors/.styles
	final static String[] legendTitles = { ZbxApiConstants.VALUES.SYSTEM_CPU_UTIL_IDLE_AVG1,
										   ZbxApiConstants.VALUES.SYSTEM_CPU_UTIL_USER_AVG1,
										   ZbxApiConstants.VALUES.SYSTEM_CPU_UTIL_SYSTEM_AVG1, };
	public String[] legendTitlesAlias() { return legendTitles; }
	public static String[] getLegendTitles() { return legendTitles; }
	
	@Override
	protected String Y_AXIS_LABEL() { return "Utilization"; }

	@Override
	protected Uri DB_TABLE_CONTENT_URI() { return CpuUtilization.META_DATA.CONTENT_URI; }

	
	public CpuUtilizationGraphFragment() {
		//empty constructor is needed by Android for automatically creating fragments from XML declarations
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.zbxcpuutilizationgraphfragment, container);
		return view;
	}
	
	
	
}
