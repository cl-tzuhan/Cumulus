package com.creationline.cloudstack.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.view.View;
import android.widget.TextView;

import com.creationline.cloudstack.R;

public class DateTimeParser {

	private static SimpleDateFormat datetimeParser = null;
	private static SimpleDateFormat timePrinter = null;
	private static SimpleDateFormat datePrinter = null;
	
	/**
	 * Specifically sets the "created" and "createdTime" textviews in the specified view with
	 * parsed output from passed-in datetimeStr.  Format of datetimeStr is assumed to be fixed.
	 * In the case datetimeStr cannot be successfully parsed/formated, the textviews will not be set.
	 * 
	 * @param view view which contains "created" and "createdTime" textviews to set with date/time text
	 * @param tv the "created" textview (this is passed in just to save processing since we have this already in the existing code flow)
	 * @param datetimeStr string in pre-determined format of the datetime to parse
	 */
	public static void setCreatedDateTime(View view, TextView tv, String datetimeStr) {
		try {
			//lazy init & re-use
			if(datetimeParser==null) {datetimeParser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");};
			if(datePrinter==null) {datePrinter = new SimpleDateFormat("yyyy-MM-dd");};
			if(timePrinter==null) {timePrinter = new SimpleDateFormat("HH:mm:ss");};
			
			final Date date = datetimeParser.parse(datetimeStr);  //parse the date string
			
			tv.setText(datePrinter.format(date));  //set just the date info
			
			tv = (TextView) view.findViewById(R.id.createdtime);
			tv.setText(timePrinter.format(date));  //set the time info separately from date info

		} catch (ParseException e) {
			//in the case of an un-parse-able datetime str, we will just display the str as is instead of trying to prettify it
			ClLog.e("setTextViewWithString():", "created timestamp could not be parsed; skipping");
			ClLog.e("setTextViewWithString():", e);
		}
	}
}
