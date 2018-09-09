package org.gheskio.queue;

import java.util.StringTokenizer;

import java.util.Date;
import java.io.*;
import java.text.*;

/** harvested sessions from SerialQRecords.
 * very much the same as SimpleQ, but without all the Android foo
 * and across all facilities
 */

public class SimpleSession {

        public String token_id = "";
        public String start_time = "";
        public String end_time = "";
        public long duration = 0;
        public String station_id = "";
        public String facility_id = "";

	public static String getCsvHeader() {
		return (new String("\"token_id\",\"start_wait\",\"end_wait\",\"minutes\",\"station_id\",\"facility_id\""));
	}

	public String toCsvString() {
		SimpleDateFormat myDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			Date beginDate = myDF.parse(start_time);
			Date endDate = myDF.parse(end_time);
			long beginUSecs = beginDate.getTime();
			long endUSecs = endDate.getTime();
			duration = (endUSecs - beginUSecs) / 60000;
		} catch (Exception e) {

		}
		int numSecs = (int)(duration / 1000);
		String retString = new String("\"" + token_id + "\",\"" + start_time + "\",\"" + end_time + 
			"\",\"" + duration + "\",\"" + station_id + "\",\"" + facility_id + "\"");
		return(retString);
	}

	public String toString() {
		// XXX gotta get this straight for MS SQLserver
                java.util.Date nextDate = new java.util.Date(start_time);
		int numSecs = (int)(duration / 1000);
		String retString = new String(token_id+"|"+nextDate+"|"+numSecs+"|"+station_id+"|"+facility_id);
		return(retString);
	}
}
