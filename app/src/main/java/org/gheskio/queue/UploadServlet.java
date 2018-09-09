package org.gheskio.queue;

/** A class to put uploaded items into SQL Server
 * The structure of an uploaded record is:
 *
 * tokenId|eventTime|comments|type|worker|station|facility
 */

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class UploadServlet extends HttpServlet {

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

	public static String getDateTime2String(java.util.Date pDate) {
		GregorianCalendar gCal = new GregorianCalendar();
		gCal.setTime(pDate);

		StringBuilder sb = new StringBuilder();
		sb.append("CAST('");
		sb.append(gCal.get(Calendar.YEAR) + "-" + (1 + gCal.get(Calendar.MONTH)) + "-" + gCal.get(Calendar.DAY_OF_MONTH));
		sb.append(" " + gCal.get(Calendar.HOUR_OF_DAY) + ":" + gCal.get(Calendar.MINUTE) + ":" + gCal.get(Calendar.SECOND));
		sb.append("' as datetime2(7))");
//		sb.append("' as datetime)");
		return(sb.toString());

	}

	public synchronized static void doUploadFile(BufferedReader bReader, Statement stmt, String remoteAddr) throws IOException, SQLException {

		String nextLine = bReader.readLine();
		StringBuilder sb = new StringBuilder();
		while (nextLine != null) {

			try {
				try {
					System.out.println("seeing: " + nextLine);
					SerialQRecord sqr = new SerialQRecord(nextLine, remoteAddr);	
					sb.setLength(0);
					sb.append("insert into " + table2insert + " (receive_ip, receive_time, app_name, token_id, event_time, comments, event_type, worker_id, station_id, facility_id) values (");
					sb.append("'" + sqr.receive_ip + "',");
					sb.append(getDateTime2String(sqr.receive_time) + ",");
					sb.append("'" + sqr.app_name + "',");
					sb.append("'" + sqr.token_id + "',");

					java.util.Date origEventDate = new java.util.Date(sqr.event_time);
					sb.append(getDateTime2String(origEventDate) + ",");

					sb.append("'" + sqr.comments + "',");
					sb.append("'" + sqr.event_type + "',");
					sb.append("'" + sqr.worker_id + "',");
					sb.append("'" + sqr.station_id + "',");
					sb.append("'" + sqr.facility_id + "'");
					sb.append(");");

					System.out.println("executing: " + sb.toString());
					System.out.flush();
			

					int numRows = stmt.executeUpdate(sb.toString());

					/* save for prepared statement optimization once we figure out datetime2 casting...
					pstmt.setString(1, sqr.receive_ip);

					pstmt.setString(2, sqr.receive_time.toString());

					pstmt.setString(3, sqr.app_name);
					pstmt.setString(4, sqr.token_id);

					java.sql.Date sqlDate = new java.sql.Date(sqr.event_time);
					pstmt.setString(5, sqlDate.toString());

					pstmt.setString(6, sqr.comments);
					pstmt.setString(7, sqr.event_type);
					pstmt.setString(8, sqr.worker_id);
					pstmt.setString(9, sqr.station_id);
					pstmt.setString(10, sqr.facility_id);

					int numRows = pstmt.executeUpdate();
					*/

					System.out.println("inserted: " + nextLine);

					nextLine = bReader.readLine();
				} catch (Exception e) {
					System.out.println("problem with: " + nextLine);
					e.printStackTrace();
					nextLine = bReader.readLine();
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

		InputStream sIOStream = req.getInputStream();
		InputStreamReader isr = new InputStreamReader(sIOStream);
		BufferedReader br = new BufferedReader(isr);

		String nextLine = null;
		try {

			// Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
			Class.forName("net.sourceforge.jtds.jdbc.Driver");

			Properties props = new Properties();

			props.setProperty("USER", userName);
			props.setProperty("PASSWORD", passWord);
			props.setProperty("SERVERTYPE", "1");
			props.setProperty("TDS", "8.0");

			System.out.println("trying: " + jdbcString + " with username: " + userName + " and pw: " + passWord);
			Connection conn = DriverManager.getConnection(jdbcString, props);

			Statement stmt = conn.createStatement();

			String remoteAddr = req.getRemoteAddr();
			doUploadFile(br, stmt, remoteAddr);

			// XXX - need to figure out CASTing to dates in SQLServer prepared statements
			// PreparedStatement pstmt = conn.prepareStatement("insert into " + table2insert + " (receive_ip, receive_time, app_name, token_id, event_time, comments, event_type, worker_id, station_id, facility_id) values (?, CAST(? as datetime2(7)), ?, ?, CAST(? AS datetime(2)),?,?,?,?,?)");

			// doUploadFile(br, pstmt);

			conn.close();

			OutputStream os = res.getOutputStream();
			PrintStream ps = new PrintStream(os);
			ps.println("OK");
			ps.flush();
			ps.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

	}

	public static void usage() {
		System.out.println("usage: java -classpath CLASSPATH -j jdbcString -u username -p passwd [-f eventFile]");
		System.exit(-1);
	}

	/** simple main for testing...
	 *
	 * if no event file is given, just connect and select "helo"
	 * from the database.  If an event file is given, for now, hard-wire the V0 of the simple record 
	 * of the GHESKIO queue item. 
	 *
	 * XXX - for now, we know this is GHESKIO, using MS Sql server back end, using the
	 * first record format of the SimpleQueue
	 *
	 */

	public static void main(String args[]) {

		String connectionString = null;
		String userName = null;
		String passWord = null;	
		String eventFile = null;
		boolean doCreate = false;
		String sql2execute = null;
		
		try {
			for (int i = 0; i < args.length; i++) {
				if (args[i].equals("-u")) {
					++i;
					userName = args[i];
				} else if (args[i].equals("-p")) {
					++i;
					passWord = args[i];
				} else if (args[i].equals("-j")) {
					++i;
					connectionString = args[i];
				} else if (args[i].equals("-t")) {
					++i;
					table2insert = args[i];
				} else if (args[i].equals("-f")) {
					++i;
					eventFile = args[i];
				} else  {
					System.out.println("unknown option: " + args[i]);
					usage();
				}
			}
			if ((connectionString != null) && (userName != null) && (passWord != null)) {

				// A connection string looks like:
				// jdbc:sqlserver://localhost:1433;databaseName=AdventureWorks;integratedSecurity=true;

				// Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
				Class.forName("net.sourceforge.jtds.jdbc.Driver");

				Properties props = new Properties();

				props.setProperty("USER", userName);
				props.setProperty("PASSWORD", passWord);
				props.setProperty("SERVERTYPE", "1");
				props.setProperty("TDS", "8.0");

				System.out.println("trying: " + connectionString + " with username: " + userName + " and pw: " + passWord);
				Connection conn = DriverManager.getConnection(connectionString, props);

				if (conn != null) {
					if (eventFile != null) {
						FileReader fr = new FileReader(eventFile);
						BufferedReader br = new BufferedReader(fr);

						Statement stmt = conn.createStatement();
						doUploadFile(br, stmt, null);

						// XXX - need to figure out CASTing to dates in SQLServer prepared statements
						// PreparedStatement pstmt = conn.prepareStatement("insert into " + table2insert + " (receive_ip, receive_time, app_name, token_id, event_time, comments, event_type, worker_id, station_id, facility_id) values (?, CAST(? as datetime2(7)), ?, ?, CAST(? AS datetime(2)),?,?,?,?,?)");
						// doUploadFile(br, pstmt);

						conn.close();
					} else {
						Statement stmt = conn.createStatement();
						ResultSet rs = stmt.executeQuery("select 'helo'");
						while (rs.next()) {
							String heloString = rs.getString(1);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
