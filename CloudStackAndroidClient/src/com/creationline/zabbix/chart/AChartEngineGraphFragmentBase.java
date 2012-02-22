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
import java.util.Date;
import java.util.List;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.SeriesSelection;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.creationline.cloudstack.R;
import com.creationline.cloudstack.utils.QuickActionUtils;
import com.creationline.common.utils.ClLog;
import com.creationline.zabbix.engine.ZbxRestService;
import com.creationline.zabbix.engine.api.ZbxApiConstants;
import com.creationline.zabbix.engine.db.CpuLoads;

public class AChartEngineGraphFragmentBase extends Fragment {

	///members that subclasses should override
	protected String Y_AXIS_LABEL() { return ""; }  //sub-classes should implement this for their specific graph

	protected Uri DB_TABLE_CONTENT_URI() { return null; }  //sub-classes should implement this for their specific graph

	public String[] legendTitlesAlias() { return null; }  //sub-classes should implement this for their specific graph
	
	//(optional) subclasses should override if something other than the default colors/styles is desired
	protected int[] lineColors() { return new int[] { Color.argb(200, 75, 171, 25),  //greenish
													  Color.argb(200, 227, 227, 30), //yellowish
													  Color.argb(200, 160, 40, 30),  //reddish
													}; };
	protected int[] fillColors() { return new int[] { Color.argb(50, 75, 171, 25),  //light-greenish
													  Color.argb(50, 227, 227, 30), //light-yellowish
													  Color.argb(50, 160, 40, 30),  //light-reddish
													}; };
	protected PointStyle[] styles() { return new PointStyle[] { PointStyle.CIRCLE,
																PointStyle.DIAMOND,
																PointStyle.TRIANGLE,
															   }; };
	///end members that subclasses should override

	public static final String FINISHEDDOWNLOADINGDATA = "com.creationline.zabbix.chart.AChartEngineGraphFragmentBase.FINISHEDDOWNLOADINGDATA";
	public static final String LOGINFAILED = "com.creationline.zabbix.chart.AChartEngineGraphFragmentBase.LOGINFAILED";
	
	protected String host = null;

	private static GraphicalView graphView;
	private XYMultipleSeriesDataset dataset;
	private XYMultipleSeriesRenderer renderer;
	private BroadcastReceiver dataDownloadCompleteCallbackReceiver = null;
	private AlertDialog globalDialogHandle = null;
	private boolean finishedDownloadingData = false;
	private boolean loginFailed = false;

	/**
	 * Custom component derived from ProgressBar.
	 * The only thing it does differently from ProgressBar is to call graphView.repaint()
	 * whenever the progress bar graphic is animated.  This is a workaround for aChartEngine,
	 * which for whatever reason, will draw itself into the background of any other UI
	 * components laid-out around the actual GraphicalView itself.  Calling repaint()
	 * before changing surrounding UI components prevents aChartEngine from doing this;
	 * though why this is, I don't know.
	 * @author thsu
	 */
	public static class GraphViewAwareProgressBar extends ProgressBar {

		public GraphViewAwareProgressBar(Context context) {
			super(context);
		}

		public GraphViewAwareProgressBar(Context context, AttributeSet attrs) {
			super(context, attrs);
		}

		public GraphViewAwareProgressBar(Context context, AttributeSet attrs, int defStyle) {
			super(context, attrs, defStyle);
		}

		@Override
		protected synchronized void onDraw(Canvas canvas) {
			if(graphView!=null) { graphView.repaint(); };
			super.onDraw(canvas);
		}
		
	}

	public static AlertDialog showErrorDialog(Activity activity, final int errorDialogView, final String errorMessage) {
		View errorDialog = activity.getLayoutInflater().inflate(errorDialogView, null);
		
		if(errorMessage!=null && errorMessage.length()!=0) {
			//switch default message for specified message
			TextView dialogmessagedetail = (TextView)errorDialog.findViewById(R.id.dialogmessagedetail);
			dialogmessagedetail.setText(errorMessage);
		}
		
		AlertDialog.Builder builder = new AlertDialog.Builder(activity)
						.setNeutralButton("OK", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
			                    dialog.cancel();
			               }
			           	});
		builder.setView(errorDialog);
		AlertDialog dialogHandle = builder.create();
		dialogHandle.show();
		return dialogHandle;
	}


	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    Bundle extras = getActivity().getIntent().getExtras();
	    host = extras.getString(ZbxApiConstants.FIELDS.HOST);
	    
	    if(savedInstanceState!=null) {
	    	//if we are just doing an orientation change, maintain same state as to whether data has been downloaded, login failed, etc.
	    	finishedDownloadingData = savedInstanceState.getBoolean(FINISHEDDOWNLOADINGDATA, false);
	    	loginFailed = savedInstanceState.getBoolean(LOGINFAILED, false);
	    }
	    
	    registerDataDownloadCompleteCallbackReceiver();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(FINISHEDDOWNLOADINGDATA, finishedDownloadingData);
		outState.putBoolean(LOGINFAILED, loginFailed);
		super.onSaveInstanceState(outState);
	}

	/**
	 * Borrowed heavily from XYChartBuilder class of AChartEngine:
	 *   http://code.google.com/p/achartengine/
	 *   
	 */
	@Override
	public void onResume() {
		super.onResume();
		
	    TextView hostname = (TextView)getView().findViewById(R.id.hostname);
	    hostname.setText(host);
		
		if(finishedDownloadingData) {
			hideProgressComponents();
		} else {
			showProgressComponents();
		}
		
		dataset = buildDataset(legendTitlesAlias());
		renderer = buildRenderer(dataset, lineColors(), fillColors(), styles());
		graphView = buildGraph(dataset, renderer);
	
		if(loginFailed) {
			globalDialogHandle = showErrorDialog(getActivity(), R.layout.zbxerrordialog_loginincorrect, null);
		}
	}

	@Override
	public void onPause() {
		if(globalDialogHandle!=null) {
			globalDialogHandle.dismiss();
			globalDialogHandle = null;
		}
		super.onPause();
	}

	@Override
	public void onDestroy() {
		unregisterCallbackReceiver(dataDownloadCompleteCallbackReceiver);
	
		super.onDestroy();
	}

	public GraphicalView buildGraph(XYMultipleSeriesDataset dataset, XYMultipleSeriesRenderer renderer) {
		
		if(dataset==null || renderer==null) {
			return null;
		}
		
		final Activity activity = getActivity();
		final GraphicalView graphView = ChartFactory.getTimeChartView(activity, dataset, renderer, "MM/dd HH:mm");
		
		graphView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				///display the y-value of any point the user touches for better usability
				if(graphView!=null) { graphView.repaint(); };  //for whatever reason, a repaint is needed here first to prevent the setText() below from drawing the chart into the textview itself
				SeriesSelection seriesSelection = graphView.getCurrentSeriesAndPoint();
				TextView displayarea = (TextView)activity.findViewById(R.id.selectedvaluedisplayarea);
				if (seriesSelection == null) {
					displayarea.setText("");
				} else {
					final String clickedValue = "clicked value= "+seriesSelection.getValue();
					displayarea.setText(clickedValue);
				}
			}
		});
		
		//display the created chart
		LinearLayout layout = (LinearLayout)activity.findViewById(R.id.graph);
		layout.removeAllViews();
		layout.addView(graphView, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		return graphView;
	}

	public XYMultipleSeriesRenderer buildRenderer(XYMultipleSeriesDataset dataset, final int[] lineColors,
																final int[] fillColors, final PointStyle[] styles) {
		XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();

		configureRendererWithLineAttributes(lineColors, fillColors, styles, renderer);

		//figure out the max/min dates we have in this dataset across all series (ie. lines)
		double xMin_acrossAllSeries = Double.MAX_VALUE;
		double xMax_acrossAllSeries = Double.MIN_VALUE;
		double yMin_acrossAllSeries = 0;  //always at least show 0 value for y-axis
		double yMax_acrossAllSeries = Double.MIN_VALUE;
		final boolean thereIsData = dataset!=null && dataset.getSeriesCount()>0;
		if(thereIsData) {
			double xMin_thisSeries;
			double xMax_thisSeries;
			double yMin_thisSeries;
			double yMax_thisSeries;
			for(int i=0; i<dataset.getSeriesCount(); i++) {
				XYSeries xySeries = dataset.getSeriesAt(i);
				if(xySeries!=null) {
					xMin_thisSeries = xySeries.getMinX();
					if(xMin_thisSeries<xMin_acrossAllSeries) { xMin_acrossAllSeries = xMin_thisSeries; };
					xMax_thisSeries = xySeries.getMaxX();
					if(xMax_thisSeries>xMax_acrossAllSeries) { xMax_acrossAllSeries = xMax_thisSeries; };
					yMin_thisSeries = xySeries.getMinY();
					if(yMin_thisSeries<yMin_acrossAllSeries) { yMin_acrossAllSeries = yMin_thisSeries; };
					yMax_thisSeries = xySeries.getMaxY();
					if(yMax_thisSeries>yMax_acrossAllSeries) { yMax_acrossAllSeries = yMax_thisSeries; };
				}
			}
		}

		//set overall chart properties
		renderer.setApplyBackgroundColor(true);
		renderer.setBackgroundColor(Color.argb(100, 50, 50, 50));
		renderer.setGridColor(Color.argb(55, 255, 255, 255));
		renderer.setShowGrid(true);
		renderer.setMargins(new int[] { 0, 58, 75, 0 });  //{top, left, bottom, right}
		renderer.setClickEnabled(true);
		renderer.setSelectableBuffer(50);
		renderer.setPointSize(2);

		//set x/y axis properties
		renderer.setYTitle(Y_AXIS_LABEL());
		renderer.setXAxisMin(xMin_acrossAllSeries);
		renderer.setXAxisMax(xMax_acrossAllSeries);
		renderer.setYAxisMin(yMin_acrossAllSeries);
		renderer.setYAxisMax(yMax_acrossAllSeries);
		renderer.setAxisTitleTextSize(16);
		renderer.setAxesColor(Color.argb(25, 255, 255, 255));

		//set x/y axis label properties
		renderer.setXLabels(12);
		renderer.setYLabels(10);
		renderer.setXLabelsAngle(40);
		renderer.setXLabelsAlign(Align.LEFT);
		renderer.setYLabelsAlign(Align.RIGHT);
		renderer.setLabelsTextSize(19);
		renderer.setLabelsColor(Color.argb(255, 149, 149, 149));

		//set legend properties
		renderer.setLegendTextSize(17);
		renderer.setLegendHeight(100);

		//set pan/zoom properties
		final double xBuffer = (xMax_acrossAllSeries-xMin_acrossAllSeries) * 0.4;  //give user some whitespace on either side of the graphed line for better viewing
		final double yBuffer = (yMax_acrossAllSeries-yMin_acrossAllSeries) * 0.4;  //give user some whitespace above and below graphed line for better viewing
		final double[] limitsWithBuffer = new double[] { xMin_acrossAllSeries-xBuffer, xMax_acrossAllSeries+xBuffer, yMin_acrossAllSeries-yBuffer, yMax_acrossAllSeries+yBuffer, };
		renderer.setPanLimits(limitsWithBuffer);
		renderer.setZoomLimits(limitsWithBuffer);
		renderer.setZoomButtonsVisible(true);


		return renderer;
	}

	public void configureRendererWithLineAttributes(final int[] lineColors,
			final int[] fillColors, final PointStyle[] styles, XYMultipleSeriesRenderer renderer) {
		//set properties for each line to graph
		if(lineColors!=null && fillColors!=null && styles!=null) {
			for (int i=0; i < lineColors.length; i++) {
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
		
		for(int legendTitlesIndex=0; legendTitlesIndex<legendTitles.length; legendTitlesIndex++) {
			final String[] selectionArgs = new String[] { host, legendTitles[legendTitlesIndex] };
			Cursor c = getActivity().getContentResolver().query(DB_TABLE_CONTENT_URI(), columns, whereClause, selectionArgs, sortOrder);
	
			if(c==null) {
				ClLog.i("AChartEngineGraphFragmentBase.buildDataset()", "null cursor returned by query (most likely, specified db does not exist)");
				return emptyDataset;
			}
			
			Date[] dateData = new Date[c.getCount()];
			double[] measuredLoad = new double[c.getCount()];
			final int clockColumnIndex = c.getColumnIndex(CpuLoads.CLOCK);
			final int valueColumnIndex = c.getColumnIndex(CpuLoads.VALUE);
	
			//parsed retrieved data into arrays, converting the clock values into java date objects
			c.moveToFirst();
			for(int i=0; !c.isAfterLast(); c.moveToNext(), i++) {
				final double clockInMilliSec = c.getDouble(clockColumnIndex);  //timestamp saved to db is in _milliseconds_ from epoch
				dateData[i] = new Date((long)clockInMilliSec);
				measuredLoad[i] = c.getDouble(valueColumnIndex);
			}
			c.close();

			xValues.add(dateData);  //all lines have the exact same x values
			yValues.add(measuredLoad);
		}
		
		return buildDateDataset(legendTitles, xValues, yValues);
	}

	/**
	 * |Copied from AbstractDemoChart class of AChartEngine:
	 * |  http://code.google.com/p/achartengine/
	 * |Apache license, v2.0
	 * 
	 * Builds an XY multiple time dataset using the provided values.
	 * 
	 * @param titles the series titles
	 * @param xValues the values for the X axis
	 * @param yValues the values for the Y axis
	 * @return the XY multiple time dataset
	 */
	protected XYMultipleSeriesDataset buildDateDataset(String[] titles, List<Date[]> xValues, List<double[]> yValues) {
		XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
		int length = titles.length;
		for (int i = 0; i < length; i++) {
			TimeSeries series = new TimeSeries(titles[i]);
			Date[] xV = xValues.get(i);
			double[] yV = yValues.get(i);
			int seriesLength = xV.length;
			for (int k = 0; k < seriesLength; k++) {
				series.add(xV[k], yV[k]);
			}
			dataset.addSeries(series);
		}
		return dataset;
	}

	public void registerDataDownloadCompleteCallbackReceiver() {
	    dataDownloadCompleteCallbackReceiver = new BroadcastReceiver(){
	    	//This handles callback intents broadcasted by ZbxRestService
	    	@Override
	    	public void onReceive(Context contenxt, Intent intent) {
				//build the graph with whatever data has been saved to db
				dataset = buildDataset(legendTitlesAlias());
				renderer = buildRenderer(dataset, lineColors(), fillColors(), styles());
				graphView = buildGraph(dataset, renderer);
	
				Bundle payload = intent.getExtras();
				final int callStatus = payload.getInt(ZbxRestService.CALL_STATUS);
				if(callStatus==ZbxRestService.CALL_STATUS_VALUES.CALL_STARTED) {
					finishedDownloadingData = false;
					showProgressComponents();
				} else if(callStatus==ZbxRestService.CALL_STATUS_VALUES.LOGIN_SUCCEEDED) {
					//ignore any successful login broadcasts as automatic re-logins are part of the call-reply process
					return;
				} else {
					finishedDownloadingData = true;
					hideProgressComponents();
					
					if(callStatus==ZbxRestService.CALL_STATUS_VALUES.LOGIN_FAILED) {
						loginFailed = true;
						globalDialogHandle = showErrorDialog(getActivity(), R.layout.zbxerrordialog_loginincorrect, null);
					} else if(callStatus==ZbxRestService.CALL_STATUS_VALUES.CALL_FAILURE) {
						final String errorMessage = payload.getString(ZbxApiConstants.FIELDS.DATA);
						globalDialogHandle = showErrorDialog(getActivity(), R.layout.zbxerrordialog_datadownloaderror, errorMessage);
					} else if(callStatus==ZbxRestService.CALL_STATUS_VALUES.CALL_SUCCESS) {
						Toast.makeText(getActivity(), "data update complete", Toast.LENGTH_SHORT).show();
					}
				}
	    		
	    	}
	
	    };
	    getActivity().registerReceiver(dataDownloadCompleteCallbackReceiver, new IntentFilter(ZbxRestService.INTENT_ACTION.ZBXRESTSERVICE_BROADCAST));  //activity will now get intents broadcast by ZbxRestService (filtered by ZBXRESTSERVICE_BROADCAST action)
	}

	public void unregisterCallbackReceiver(BroadcastReceiver broadcastReceiver) {
		if(broadcastReceiver!=null) {
			//catch-all here as a safeguard against cases where the activity is exited before BroadcastReceiver.onReceive() has been called-back
			try {
				getActivity().unregisterReceiver(broadcastReceiver);
			} catch (IllegalArgumentException e) {
				//will get this exception if listSnapshotsCallbackReceiver has already been unregistered (or was never registered); will just ignore here
				;
			}
		}
	}

	public AChartEngineGraphFragmentBase() {
		super();
	}

	public void showProgressComponents() {
		GraphViewAwareProgressBar graphprogresscircle = (GraphViewAwareProgressBar)getActivity().findViewById(R.id.graphprogresscircle);
		TextView updatingdatalabel = (TextView)getActivity().findViewById(R.id.updatingdatalabel);
		//we only animate if the components are not invisible prevent unnecessary animation during orientation changes
		if(graphprogresscircle.getVisibility()!=View.VISIBLE && updatingdatalabel.getVisibility()!=View.VISIBLE) {
			//Note: we animate manually (instead of with viewswitcher or something similar) and with a graph repaint
			//      because of achartengine's unfortunate tendency of drawing into the layout box of surrounding elements
			//      that should be unrelated to the chart itself.  Below is the only way found so far to avoid these
			//      mis-drawn artifacts.
			if(graphView!=null) { graphView.repaint(); };  //hack for achartengine
			graphprogresscircle.startAnimation(QuickActionUtils.getFadein_decelerate());
			updatingdatalabel.startAnimation(QuickActionUtils.getFadein_decelerate());
			graphprogresscircle.setVisibility(View.VISIBLE);
			updatingdatalabel.setVisibility(View.VISIBLE);
		}
	}

	public void hideProgressComponents() {
		GraphViewAwareProgressBar graphprogresscircle = (GraphViewAwareProgressBar)getActivity().findViewById(R.id.graphprogresscircle);
		TextView updatingdatalabel = (TextView)getActivity().findViewById(R.id.updatingdatalabel);
		//we only animate if the components are not invisible prevent unnecessary animation during orientation changes
		if(graphprogresscircle.getVisibility()!=View.INVISIBLE && updatingdatalabel.getVisibility()!=View.INVISIBLE) {
			//Note: we animate manually (instead of with viewswitcher or something similar) and with a graph repaint
			//      because of achartengine's unfortunate tendency of drawing into the layout box of surrounding elements
			//      that should be unrelated to the chart itself.  Below is the only way found so far to avoid these
			//      mis-drawn artifacts.
			if(graphView!=null) { graphView.repaint(); };  //hack for achartengine
			graphprogresscircle.startAnimation(QuickActionUtils.getFadeout_decelerate());
			updatingdatalabel.startAnimation(QuickActionUtils.getFadeout_decelerate());
			graphprogresscircle.setVisibility(View.INVISIBLE);
			updatingdatalabel.setVisibility(View.INVISIBLE);
		}
	}

}