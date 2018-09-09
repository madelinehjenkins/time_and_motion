package org.gheskio.queue;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.BaseColumns;

import java.util.StringTokenizer;

/** A simple record to store the id of a scanned QRtoken,
 * when it was given, and when it was taken
 * 
 * @author cgn
 *
 */
public class SimpleQRecord implements BaseColumns {
	
	public static final String VERSION = "0.1";
	
	public static final String TABLE_NAME = "simpleqrecord";
	
	/** QRcode printed on the plastic card */
	public static final String COLUMN_TOKEN_ID = "token_id";

	/** by convention a time of zero means uninitialized */
	
	public static final String COLUMN_STATION_ID = "station_id";
	public static final String COLUMN_FACILITY_ID = "facility_id";	
	public static final String COLUMN_WORKER_ID = "worker_id";	
	public static final String COLUMN_COMMENTS = "comments";	
	
	public static final String COLUMN_EVENT_TIME = "event_time";	
	public static final String COLUMN_EVENT_TYPE = "event_type";
	
	public static final String[] COLUMNS_PROJECTION = {"_ID", COLUMN_TOKEN_ID, COLUMN_STATION_ID, COLUMN_FACILITY_ID, 
			COLUMN_WORKER_ID, COLUMN_COMMENTS, COLUMN_EVENT_TIME, COLUMN_EVENT_TYPE}; 

	
	public static final String GIVE_EVENT = "give";
	public static final String TAKE_EVENT = "take";
	public static final String SHOW_EVENT = "show";
		
	public long record_id = 0;
	public String token_id = "";
	public long event_time = 0;
	public String event_type = "";

	public String station_id = "";
	public String facility_id = "";
	public String worker_id = "";
	public String comments = "";
	
	public SimpleQRecord() { }
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		// XXX - should hex encode for weirdness, multilingual
		comments = comments.replaceAll("|",  "_");
				
		sb.append(VERSION+"|"+record_id+"|"+token_id+"|"+event_time+"|"+event_type+"|");
		sb.append(station_id+"|"+facility_id+"|"+worker_id+"|"+comments);
		return(sb.toString());
		
	}
	
	public static SimpleQRecord parseString(String pString) {
		
		SimpleQRecord retRec = null;
		try {
			
			retRec = new SimpleQRecord();
			StringTokenizer st = new StringTokenizer(pString, "|");
			String vString = st.nextToken();
			retRec.record_id = Long.parseLong(st.nextToken());
			retRec.token_id = st.nextToken();
			retRec.event_time = Long.parseLong(st.nextToken());
			retRec.event_type = st.nextToken();
			retRec.station_id = st.nextToken();
			retRec.facility_id = st.nextToken();
			retRec.worker_id = st.nextToken();
			retRec.comments = st.nextToken();
			
		} catch (Exception e) {
			e.printStackTrace();
			retRec = null;
		}
		return(retRec);
	}
	
	public static String getCreateStatement() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("create table if not exists " +  TABLE_NAME + " (");
		
		sb.append("_ID	INTEGER PRIMARY KEY,\n");
		sb.append(COLUMN_TOKEN_ID + " TEXT,\n");
		sb.append(COLUMN_EVENT_TIME + " INTEGER,\n");
		sb.append(COLUMN_EVENT_TYPE + " TEXT,\n");
		sb.append(COLUMN_FACILITY_ID + " TEXT,\n");
		sb.append(COLUMN_STATION_ID + " TEXT,\n");
		sb.append(COLUMN_WORKER_ID + " TEXT,\n");
		sb.append(COLUMN_COMMENTS + " TEXT");
		
		sb.append(");");
		
		return(sb.toString());
	}
	
	/** initialize one of these with a physical, printed ID, and what one plans
	 * to do with it...
	 * 
	 * @param printedId what is printed on the card
	 * @param eventType one of "give" "take" or "show"
	 */
	
	public SimpleQRecord(String printedId,  String pComments, String eventType) {

		String workerVal = MainActivity.sharedPref.getString("WORKER_ID", "");
		String stationVal = MainActivity.sharedPref.getString("STATION_ID", "");
		String facilityVal = MainActivity.sharedPref.getString("FACILITY_ID", "");
		
		long nowTime = new java.util.Date().getTime();
						
		// get ready to insert an event into the SimpleQRecord log
		ContentValues simpleQRvalues = new ContentValues();

		simpleQRvalues.put(COLUMN_TOKEN_ID, printedId);				
		simpleQRvalues.put(COLUMN_COMMENTS, pComments);			
		simpleQRvalues.put(COLUMN_EVENT_TIME, new Long(nowTime));
		simpleQRvalues.put(COLUMN_EVENT_TYPE, eventType);
		simpleQRvalues.put(COLUMN_FACILITY_ID, facilityVal);
		simpleQRvalues.put(COLUMN_STATION_ID, stationVal);
		simpleQRvalues.put(COLUMN_WORKER_ID, workerVal);
		
		System.out.println("simpleqrecord:" + facilityVal + ":" + stationVal + ":" + workerVal );
			
		long newRowId;
		
		// just record the event
		newRowId = MainActivity.myDB.insert(TABLE_NAME, null, simpleQRvalues);
			
		// presume, if 'start_wait', we enter a new item in the Q.
		//
		// note, we get into an interesting situation after an upload, when there still
		// might be people waiting in the Q, but the corresponding events have been uploaded
		// and cleared. 
		//
		// All of this comes into play because we want to generate stats for
		// the average wait, based on what's in the phone at the time.
		// but to do that, we need completed waits. And to that, we need to keep track 
		// of what's outstanding. So if we take a token, asa the first thing after an upload,
		// we can remember it ... *BUT* ... back at the server, we only see the boundary
		// events *1* time.
		
		
		if ("start_wait".equals(eventType)) {				
				// insert a new row into SimpleQ
				
				ContentValues qvalues = new ContentValues();
				
				qvalues.put(SimpleQ.COLUMN_GIVE_TIME, nowTime);
				qvalues.put(SimpleQ.COLUMN_TOKENID, printedId);
				qvalues.put(SimpleQ.COLUMN_COMMENTS, pComments);
				qvalues.put(SimpleQ.COLUMN_DURATION, 0);

				
				// mark it initially as not a fragment.
				// any exist during an upload, they are marked as a fragment.
				// soas to not have double counting
				
				newRowId = MainActivity.myDB.insert(SimpleQ.TABLE_NAME, null, qvalues);							
		}
	}
}
