package com.creationline.cloudstack.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.widget.TextView;

public class DateTimeParser {

	private static SimpleDateFormat datetimeParser = null;
	private static SimpleDateFormat timePrinter = null;
	private static SimpleDateFormat datePrinter = null;
	
	/**
	 * Specifically sets the "created" and "createdTime" textviews in the specified view with
	 * parsed output from passed-in datetimeStr.  Format of datetimeStr is assumed to be fixed.
	 * In the case datetimeStr cannot be successfully parsed/formated, the textviews will not be set.
	 * @param dateText the textview which will get the parsed datestamp set as text
	 * @param timeText the textview which will get the parsed timestamp set as text
	 * @param datetimeStr string in pre-determined format of the datetime to parse
	 */
	public static void setParsedDateTime(TextView dateText, TextView timeText, String datetimeStr) {
		try {
			//lazy init & re-use
			if(datetimeParser==null) {datetimeParser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");};
			if(datePrinter==null) {datePrinter = new SimpleDateFormat("yyyy-MM-dd");};
			if(timePrinter==null) {timePrinter = new SimpleDateFormat("HH:mm:ss");};
			
			final Date date = datetimeParser.parse(datetimeStr);  //parse the date string
			
			dateText.setText(datePrinter.format(date));  //set just the date info
			
//			dateText = (TextView) view.findViewById(R.id.createdtime);
//			dateText.setText(timePrinter.format(date));  //set the time info separately from date info
			timeText.setText(timePrinter.format(date));  //set the time info separately from date info

		} catch (ParseException e) {
			//in the case of an un-parse-able datetime str, we will just display the str as is instead of trying to prettify it
			ClLog.e("setTextViewWithString():", "created timestamp could not be parsed; skipping");
			ClLog.e("setTextViewWithString():", e);
			dateText.setText(datetimeStr);
		}
	}
}
