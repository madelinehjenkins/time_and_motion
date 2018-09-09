package org.gheskio.queue;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.Authenticator;

public class PostTest {

	public static void usage() {
		System.out.println("usage: java org.gheskio.queue.PostTest <srcfile> <destURL>");
	}

	public static void main(String args[]) {

		//open URL for POSTing
		Authenticator.setDefault(new SimpleAuth());
		String          uploadURL = args[1];

		URL             url;
		HttpURLConnection urlConn;

		try {

			System.out.println("sending:" + args[0] + " to: " + args[1]);
			url = new URL(uploadURL);
			urlConn = (HttpURLConnection) url.openConnection();
			urlConn.setDoInput(true);
			urlConn.setDoOutput(true);
			urlConn.setUseCaches(false);
			urlConn.setRequestMethod("POST");

			PrintWriter     pw = new PrintWriter(urlConn.getOutputStream());
			//
			// PrintWriter     pw = new PrintWriter(urlConn.getInputStream());

			FileReader      fis = new FileReader(args[0]);
			BufferedReader  br = new BufferedReader(fis);
			String nextLine = br.readLine();
			while (nextLine != null) {
				System.out.println(nextLine);
				pw.println(nextLine);
				pw.flush();
				nextLine = br.readLine();
			}
			InputStream is = urlConn.getInputStream();

			pw.close();
			urlConn.disconnect();
			fis.close();

		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
