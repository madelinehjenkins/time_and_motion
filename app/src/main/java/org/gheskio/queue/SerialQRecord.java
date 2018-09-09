package org.gheskio.queue;

import java.util.StringTokenizer;

import java.sql.Date;
import java.io.*;

public class SerialQRecord implements Comparable {

	public java.sql.Date receive_time = null;
	public String receive_ip = null;
	public long record_id = 0;
        public String app_name = "";
        public String token_id = "";
        public long event_time = 0;
        public String event_type = "";
        public String station_id = "";
        public String facility_id = "";
        public String worker_id = "";
        public String comments = "";
	public String dateInString = "";

	public SerialQRecord() { }

	public static String getCsvHeader() {
		return new String("\"token_id\",\"event_time\",\"event_type\",\"station_id\",\"facility_id\",\"worker_id\",\"comments\"");
	}

	public String toCsvString() {
		return (new String("\"" + token_id+"\",\""+dateInString+"\",\""+event_type+"\",\""+station_id+"\",\""+facility_id+"\",\""+worker_id+"\",\""+comments+"\""));
	}

	public String toString() {
		// XXX gotta get this straight for MS SQLserver
                java.util.Date nextDate = new java.util.Date(event_time);
		String retString = new String(receive_time+"|"+receive_ip+"|"+record_id+"|"+app_name+"|"+token_id+"|"+nextDate+"|"+event_type+"|"+station_id+"|"+facility_id+"|"+worker_id+"|"+comments);
		return(retString);
	}

	public String nextDateString = "";

	public int compareTo(Object other) {
		SerialQRecord otherSQR = (SerialQRecord)other;
		int retVal = token_id.compareTo(otherSQR.token_id);
		if (retVal == 0) {
			retVal = (int)(event_time - otherSQR.event_time);
			if (retVal == 0) {
				retVal = facility_id.compareTo(otherSQR.facility_id);
				if (retVal == 0) {
					retVal = station_id.compareTo(otherSQR.station_id);
				}
			}
		}
		return(retVal);
	}

	// these come in the form:
	/* 			SimpleQRecord.COLUMN_TOKEN_ID + ", " +
                                SimpleQRecord.COLUMN_EVENT_TIME + ", " +
                                SimpleQRecord.COLUMN_COMMENTS + ", " +
                                SimpleQRecord.COLUMN_EVENT_TYPE + ", " +
                                SimpleQRecord.COLUMN_WORKER_ID + ", " +
                                SimpleQRecord.COLUMN_STATION_ID + ", " +
                                SimpleQRecord.COLUMN_FACILITY_ID 
	*/

	/** constructor allows for two forms: if remoteAddr is null,
	 * this is a test record from the maps.geography.uc.edu test site
	 * otherwise, the remoteAddress is from the servlet, and the time is now. 
	 *
	 * a typical line looks like:
	 * "waiting_time_app| 0.1| 2345| 1425161766117| | give| chris| 1| 2| "
	 */

	public SerialQRecord(String serializedSQR, String remoteAddr) throws Exception {
		// deal with null fields
		serializedSQR = serializedSQR.replaceAll("\\|", "| ");

		StringTokenizer st = new StringTokenizer(serializedSQR, "|", false);

		// XXX - must be compatible with: org.gheskio.queue.SimpleQRecord.toString()

		receive_time = new java.sql.Date(new java.util.Date().getTime()); 
		if (remoteAddr != null) {
			receive_ip = remoteAddr;
		}

		app_name = st.nextToken().trim();

		// throw an exception here if mismatch..???
		String versionId = st.nextToken();

		token_id = st.nextToken().trim();
		String eventTimeString = st.nextToken().trim();

		// XXX gotta get this straight for MS SQLserver
		event_time = Long.parseLong(eventTimeString);
		java.util.Date nextDate = new java.util.Date(event_time);

		comments = st.nextToken().trim();
		event_type = st.nextToken().trim();
		worker_id = st.nextToken().trim();
		station_id = st.nextToken().trim();
		facility_id = st.nextToken().trim();
	}

	public static void usage() {
		System.out.println("usage: java org.gheskio.SerialQRecord <file to parse>");
	}

	public static void main(String args[]) {

		try {
			SerialQRecord.usage();
			FileReader fr = new FileReader(args[0]);
			BufferedReader br = new BufferedReader(fr);
			String nextLine = br.readLine() ;
			while (nextLine != null) {
				try {
					SerialQRecord sqr = new SerialQRecord(nextLine, null);
				} catch (Exception e) {
					System.out.println("problem: " + nextLine);
				}
				nextLine = br.readLine() ;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
