/*******************************************************************************
 * Copyright 2011 Creationline,Inc.
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
package com.creationline.cloudstack.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.creationline.common.utils.ClLog;

import android.text.format.Time;
import android.util.TimeFormatException;
import android.widget.TextView;

public class DateTimeParser {

	private static SimpleDateFormat datetimeParser = null;
	private static SimpleDateFormat timePrinter = null;
	private static SimpleDateFormat datePrinter = null;
	
	/**
	 * Specifically sets the specified textviews with parsed output from passed-in datetimeStr.
	 * Format of datetimeStr is assumed to be fixed.
	 * In the case datetimeStr cannot be successfully parsed/formated,
	 * the date textview will be set with the unparsed datetimeStr
	 * @param dateText the textview which will GET the parsed datestamp set as text
	 * @param timeText the textview which will GET the parsed timestamp set as text
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
			timeText.setText(timePrinter.format(date));  //set the time info separately from date info

		} catch (ParseException e) {
			//in the case of an un-parse-able datetime str, we will just display the str as is instead of trying to prettify it
			ClLog.e("setParsedDateTime():", "created timestamp could not be parsed; skipping");
			ClLog.e("setParsedDateTime():", e);
			dateText.setText(datetimeStr);
		}
	}
	
	/**
	 * Specifically sets the specified textviews with parsed output from passed-in datetimeStr.
	 * Format of datetimeStr is assumed to be in RFC 3339 format.
	 * In the case datetimeStr cannot be successfully parsed/formated,
	 * the date textview will be set with the unparsed datetimeStr
	 * @param dateText the textview which will GET the parsed datestamp set as text
	 * @param timeText the textview which will GET the parsed timestamp set as text
	 * @param datetimeStr string in pre-determined format of the datetime to parse
	 */
	public static void setParsedDateTime3999(TextView dateText, TextView timeText, String datetimeStr) {
	    Time readTime = new Time();
	    try {
			if(readTime.parse3339(datetimeStr)) {  //str was saved out using RFC3339 format, so needs to be read in as such
			    readTime.switchTimezone("Asia/Tokyo");  //parse3339() automatically converts read in times to UTC.  We need to change it back to the default timezone of the handset (JST in this example)

			    dateText.setText(readTime.format("%Y-%m-%d"));
			    timeText.setText(readTime.format("%X"));
			}
		} catch (TimeFormatException e) {
			//in the case of an un-parse-able datetime str, we will just display the str as is instead of trying to prettify it
			ClLog.e("setParsedDateTime3999():", "created timestamp could not be parsed; skipping");
			ClLog.e("setParsedDateTime3999():", e);
			dateText.setText(datetimeStr);
		}
	}
}
