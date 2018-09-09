package org.gheskio.queue;

import java.util.*;
import java.sql.*;
import java.io.*;
import java.text.*;
import javax.servlet.*;
import javax.servlet.http.*;

/** This program goes through records collected by the Gheskio mobile client,
 * and matches 'begin_wait' with 'end_wait' records to generate sessions over
 * a given period of time. Resultant matched records generate a "session", that can either
 * be placed into a database table, or written to a file.  Those records that have no match
 * are written to a "dangling_records" file. 
 *
 * For now, We restrict the sessions to be at the same facility.
 */

public class MakeSessions extends HttpServlet {

        static String jdbcString = null;
        static String table2insert = null;
        static String userName = null;
        static String passWord = null;

        /** figure out what we proxy to, etc */
        public void init() {
                jdbcString = getInitParameter("jdbcString") ;
                table2insert = getInitParameter("table2insert") ;
                userName = getInitParameter("userName");
                passWord = getInitParameter("passWord");
        }

	private static void usage() {
		System.out.println("java org.gheskio.queue [-i inTextFile -o outSessionFile -d outDanglersFile]");
		System.exit(-1);
	}

	/** test utility */
	private static Connection getConnFromFile(String propsheetName) {
		Properties myProps = new Properties();
		Connection conn = null;
		try {
                        FileInputStream propsIS = new FileInputStream(propsheetName);
                        myProps.load(propsIS);
			jdbcString = myProps.getProperty("jdbcString");
			table2insert = myProps.getProperty("table2insert");
			conn = DriverManager.getConnection(jdbcString, myProps);
			return(conn);
                } catch (Exception fnfe) {
                        System.err.println("cannot file Properties file!");
                        usage();
                }
		return(conn);
	}

	protected static synchronized Object[] getRecordsFromDB(Connection conn) {
		Object retSQRrows[] = null;
		try {
			StringBuilder sb = new StringBuilder();
			sb.append("select token_id, convert(varchar(19),event_time), event_type, comments, worker_id, station_id, facility_id from " + table2insert + " where len(token_id) > 0 and app_name = 'waiting_time_app' order by token_id, event_time, facility_id;");
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sb.toString());
			ArrayList retRows = new ArrayList();
			while (rs.next()) {
				SerialQRecord nextSQR = new SerialQRecord();
				nextSQR.token_id = rs.getString(1);
				nextSQR.dateInString = rs.getString(2);
				nextSQR.event_type = rs.getString(3);
				nextSQR.comments = rs.getString(4);
				nextSQR.worker_id = rs.getString(5);
				nextSQR.station_id = rs.getString(6);
				nextSQR.facility_id = rs.getString(7);
			
				retRows.add(nextSQR);	
			}
			rs.close();
			conn.close();
			retSQRrows = retRows.toArray();
			return(retSQRrows);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return(retSQRrows);
	}

	public static void main(String args[]) {

		boolean justWaits = false;	
		String inFileName = null;
		String outSessionFileName = null;
		String outDanglersFileName = null;
		String propsheetName = null;

		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-i")) {
				++i;
				inFileName = args[i];
			}
			if (args[i].equals("-o")) {
				++i;
				outSessionFileName = args[i];
			}
			if (args[i].equals("-p")) {
				++i;
				propsheetName = args[i];
			}
			if (args[i].equals("-w")) {
				justWaits = true;
			}
		}

		Object theSortedEvents[] = null;

		if (inFileName != null) {
			theSortedEvents = getRecordsFromFile(inFileName);
		}

		if (propsheetName != null) {
			Connection conn = getConnFromFile(propsheetName);
			theSortedEvents = getRecordsFromDB(conn);
		}

		printResults(System.out, theSortedEvents, justWaits);

	}

	protected synchronized static void printResults(PrintStream pw, Object theSortedEvents[], boolean justWaits) {

		if (justWaits) {
			pw.println(SimpleSession.getCsvHeader());
		} else {
			pw.println(SerialQRecord.getCsvHeader());
		}

		HashMap theDanglers = new HashMap();
		ArrayList theUnknowns = new ArrayList();
		ArrayList theSessions = new ArrayList();
		ArrayList theCompletedSessions = new ArrayList();

		for (int j = 0; j < theSortedEvents.length; j++) {
			SerialQRecord nextSQR = (SerialQRecord)theSortedEvents[j];

			SimpleDateFormat myDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			if (justWaits) {
				String sKey = nextSQR.token_id + "_" + nextSQR.station_id + "_" + nextSQR.facility_id;
				if ("start_wait".equals(nextSQR.event_type)) {
					SimpleSession nextSS = new SimpleSession();
					nextSS.token_id = nextSQR.token_id;
					nextSS.start_time = nextSQR.dateInString;
					nextSS.duration = 0;
					nextSS.facility_id = nextSQR.facility_id;
					nextSS.station_id = nextSQR.station_id;
					theDanglers.put(sKey, nextSS);
				} else {
					if ("end_wait".equals(nextSQR.event_type)) {
						SimpleSession nextSS = (SimpleSession)theDanglers.get(sKey);
						if (nextSS != null) {
							nextSS.end_time = nextSQR.dateInString;
							theCompletedSessions.add(nextSS);
							theSessions.remove(nextSS);
							theDanglers.remove(sKey);
						} else {
							theUnknowns.add(nextSQR);
						}
					}
				} 
			} else {
				pw.println(nextSQR.toCsvString());
			}
		}
		if (justWaits) {
			for (int i = 0; i < theCompletedSessions.size(); i++) {
				SimpleSession nextSS = (SimpleSession)theCompletedSessions.get(i);
				pw.println(nextSS.toCsvString());
			}
		}
	}

        public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

		InputStream sIOStream = req.getInputStream();
		InputStreamReader isr = new InputStreamReader(sIOStream);
		BufferedReader br = new BufferedReader(isr);

		String nextLine = null;
		try {

			Class.forName("net.sourceforge.jtds.jdbc.Driver");

			Properties props = new Properties();

			props.setProperty("USER", userName);
			props.setProperty("PASSWORD", passWord);
			props.setProperty("SERVERTYPE", "1");
			props.setProperty("TDS", "8.0");

			System.out.println("trying: " + jdbcString + " with username: " + userName + " and pw: " + passWord);
			Connection conn = DriverManager.getConnection(jdbcString, props);

			Object theSortedEvents[] = getRecordsFromDB(conn);

			conn.close();

			String queryInfo = req.getQueryString();
			System.out.println("queryInfo: " + queryInfo);
			System.out.println("seeing: " + queryInfo);
			boolean doSessions = false;
			if (queryInfo.indexOf("=sessions") != -1) {
				doSessions = true;
			}

			res.setContentType("text/comma-separated-values");
			OutputStream os = res.getOutputStream();
			PrintStream ps = new PrintStream(os);
			printResults(ps, theSortedEvents, doSessions);
			ps.flush();
			ps.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
        }

	public static Object[] getRecordsFromFile(String inFileName) {

		ArrayList theSessions = new ArrayList();
		ArrayList<SerialQRecord> theRecords = new ArrayList<SerialQRecord>();
		SerialQRecord[] theSRrecords = null;
		try {
			FileReader fr = new FileReader(inFileName);
			BufferedReader br = new BufferedReader(fr);
			String nextLine = br.readLine();
			while (nextLine != null) {
				SerialQRecord  nextSQR = new SerialQRecord(nextLine, "192.168.0.1");
				theRecords.add(nextSQR);
				nextLine = br.readLine();
			}

			// now sort
			// XXX - not sure why Templating didn't work to get strongly typed arraylist, but..
			
			theSRrecords = new SerialQRecord[theRecords.size()];
			for (int i = 0; i < theRecords.size(); i++) {
				theSRrecords[i] = (SerialQRecord)theRecords.get(i);
			}

			Arrays.sort(theSRrecords);

		} catch (Exception ioe) {
			ioe.printStackTrace();
		}

		return(theSRrecords);
	}
}
