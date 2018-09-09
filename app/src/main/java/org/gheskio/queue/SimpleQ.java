package org.gheskio.queue;


import android.database.Cursor;
import android.provider.BaseColumns;


/** supposedly things that have been given, but not taken, yet...
 * 
 * @author cgn
 *
 */
public class SimpleQ implements BaseColumns {
	
	public static final String TABLE_NAME = "simpleq";
	
	public static final String COLUMN_TOKENID = "token_id";
	public static final String COLUMN_GIVE_TIME = "give_time";
	
	// a duration of -1 means it is still in the queue...
	public static final String COLUMN_DURATION = "duration";
	public static final String COLUMN_COMMENTS = "comments";

	
	public static final String[] COLUMNS_PROJECTION = {"_ID", COLUMN_TOKENID, COLUMN_GIVE_TIME, COLUMN_DURATION, COLUMN_COMMENTS}; 

	public static final String sortOrder = COLUMN_GIVE_TIME + " DESC";

	/** used in the "skip" functionality of Main...
	 * when someone doesn't show up, the operator can push "skip" and advance to the next one,
	 * in which case the Q time is set as a qualifier on future queries...
	 */
	public static long lastSkipTime = 0;
	
	public String tokenId = null;
	public int giveTime = 0;
	public int duration = 0;
	public String comments = null;
	
	public SimpleQ() {
		// TODO Auto-generated constructor stub
	}
	
	public static SimpleQ findToken(String tokenId) {
		SimpleQ retSQ = null;
		
		// should use the public statics I know...
		String selection = "select give_time, duration, comments from SimpleQ where token_id = ?" ;
		String selectionArgs[] = {tokenId};
		
				
		Cursor c = MainActivity.myDB.rawQuery(
				selection,
				selectionArgs);
		
		if (c.getCount() > 0) {
			retSQ = new SimpleQ();
			retSQ.tokenId = tokenId;
			retSQ.giveTime = c.getInt(1);
			retSQ.duration = c.getInt(2);
			retSQ.comments = c.getString(3);
		}
		
		return (retSQ);
	}
	
	public static String getCreateStatement() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("create table if not exists " + TABLE_NAME);
		sb.append("(_ID INTEGER PRIMARY KEY,\n " + COLUMN_TOKENID + " TEXT\n,");
		sb.append(COLUMN_GIVE_TIME + " INTEGER\n,");
		sb.append(COLUMN_DURATION + " Integer\n,");
		sb.append(COLUMN_COMMENTS + " TEXT);\n");

		
		sb.append("create index simpleq_token_idx on simpleq(token_id);\n");
		sb.append("create index simpleq_time_idx on simpleq(give_time);");
		sb.append("create index simpleq_time_idx on simpleq(duration);");

		return(sb.toString());
	}

}
