package org.gheskio.queue;

import java.net.Socket;


import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;

import java.net.URL;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.Toast;

import android.os.AsyncTask;


import android.app.Activity;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class Qstats extends BaseActivity {

    // textView2 - gives
    // textView4 - takes
    // textView8 - startTime
    // textView10 - endTime
    // textView6 - avg wait
    // textView12 - Q size
    // textView14 - avg Q size

    public static ProgressDialog mProgress = null;
    protected boolean justDidUpload = false;

    // not sure why we can't just reuse public statics from
    // MainActivity, but whatever...

    public static SimpleQdbHelper qstatSQRDBH = null;
    public static SQLiteDatabase myDB = null;
    public static SharedPreferences.Editor editor = null;
    public static SharedPreferences sharedPref = null;

    public static boolean uploadProblem = true;

    private void checkInit() {
        // open our basic KV store and see if we have initialized the
        // SQLlite db yet, or if this is the first time through...
        sharedPref = getPreferences(Context.MODE_PRIVATE);
        editor = sharedPref.edit();
        Application myApp = this.getApplication();

        Context currentContext = myApp.getApplicationContext();
        qstatSQRDBH = new SimpleQdbHelper(currentContext);
        myDB = qstatSQRDBH.getWritableDatabase();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qstats);
        checkInit();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        // The activity is about to become visible.
        refreshStats();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // The activity is about to become visible.
        // refreshStats();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The activity has become visible (it is now "resumed").
        refreshStats();
    }

    public void doRefresh(View view) {
        refreshStats();
    }

    public void doClear(View view) {
        AlertDialog.Builder confirmDeleteBuilder = new AlertDialog.Builder(this);
        confirmDeleteBuilder.setMessage(getString(R.string.confirm_delete_stats_message));
        confirmDeleteBuilder.setCancelable(true);

        confirmDeleteBuilder.setPositiveButton(
                getString(R.string.delete),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String whereClause = "delete from simpleq ";
                        myDB.execSQL(whereClause);

                        whereClause = "delete from simpleqrecord ";
                        myDB.execSQL(whereClause);

                        refreshStats();
                    }
                });

        confirmDeleteBuilder.setNegativeButton(
                getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        AlertDialog confirmDelete = confirmDeleteBuilder.create();
        confirmDelete.show();
    }

    public void doRefreshStats(View view) {
        refreshStats();
    }

    public void refreshStats() {


        TextView avgTimeTV = (TextView) findViewById(R.id.textView12);

        TextView numBeginWaitsTV = (TextView) findViewById(R.id.textView8);
        TextView numEndWaitsTV = (TextView) findViewById(R.id.textView10);

        TextView qSizeTV = (TextView) findViewById(R.id.textView14);


        String selectionArgs[] = {};
        String selection = null;

        // horribly inefficient, but effective...

        Cursor c;
        selection = "select count(*) from simpleqrecord where event_type = 'start_wait'";
        c = myDB.rawQuery(selection, selectionArgs);
        c.moveToFirst();
        int numBeginWaits = c.getInt(0);
        c.close();
        numBeginWaitsTV.setText(Integer.toString(numBeginWaits));

        selection = "select count(*) from simpleqrecord where event_type = 'end_wait'";
        c = myDB.rawQuery(selection, selectionArgs);
        c.moveToFirst();
        int numEndWaits = c.getInt(0);
        c.close();
        numEndWaitsTV.setText(Integer.toString(numEndWaits));

        // update the Q length
        selection = "select count(*) from simpleq where duration = 0";
        c = myDB.rawQuery(selection, selectionArgs);
        c.moveToFirst();
        int qSize = c.getInt(0);
        c.close();
        qSizeTV.setText(Integer.toString(qSize));

        // try to get average of completed waits, if we have any
        selection = "select count(*) from simpleq where duration > 0";
        c = myDB.rawQuery(selection, selectionArgs);
        c.moveToFirst();
        int numSQRows = c.getInt(0);
        c.close();

        if (numSQRows > 0) {

            selection = "select avg(duration) from simpleq where duration > 0";
            c = myDB.rawQuery(selection, selectionArgs);
            c.moveToFirst();

            int avgTime = c.getInt(0);
            c.close();

            // ok - try to format this into hrs+minutes
            int numSeconds = avgTime / 1000;
            float mins = ((float) numSeconds) / 60;
            avgTimeTV.setText(mins + " mins");

        } else {

            // check to be sure it isn't already in the Q
            Context context = getApplicationContext();
            String msg = getResources().getString(R.string.not_enough_info_for_avgs);
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, msg, duration);
            toast.show();

            avgTimeTV.setText("-");
        }

    }


    /**
     * upload the SimpleQRecord history file to the server
     *
     * @param view
     */

    public void doUpload(View view) {
        QstatUpload uploadOp = new QstatUpload();
        uploadOp.execute("");
    }

    public void doUpdate(View view) {
        refreshStats();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.qstats, menu);
        return true;
    }


    private class QstatUpload extends AsyncTask<String, Integer, Long> {

        private ProgressDialog mProgress = null;

        protected void onProgressUpdate(Integer... progress) {

        }

        protected Long doInBackground(String... strings) {

            Long returnVal = new Long(0);

            try {
//				String uploadURL = MainActivity.sharedPref.getString("URL", "http://192.168.10.9:8080/gheskio/upload/?foo=goo");
                String uploadURL = MainActivity.sharedPref.getString("URL", "http://ec2-52-15-156-17.us-east-2.compute.amazonaws.com:8080/gheskio/upload");
                Uri uploadUri = Uri.parse(uploadURL);
                String uploadHost = uploadUri.getHost();
                // check if we can get to the host...
                int uploadPort = uploadUri.getPort();
                if (uploadPort == -1) {
                    uploadPort = 80;
                }
                // just for fun, try to open a socket there...
                System.out.println("trying: " + uploadHost + ":" + uploadPort);
                Socket testSocket = new Socket();
                testSocket.connect(new InetSocketAddress(uploadHost, uploadPort), 1000);

                java.io.InputStream testIS = testSocket.getInputStream();
                // if no exception, hunky dory and continue...
                testSocket.close();

                // get the count of number of rows to update the progress bar
                String selection = "select count(*) from simpleqrecord";
                String selectionArgs[] = {};

                // now see if it's already in the SimpleQ...
                Cursor c = MainActivity.myDB.rawQuery(selection, selectionArgs);
                c.moveToFirst();
                int numRows = c.getInt(0);
                c.close();

                if (numRows > 0) {

                    boolean isDone = false;
                    StringBuilder sb = new StringBuilder();
                    numRows = 0;

                    // open URL for POSTing
                    Authenticator.setDefault(new SimpleAuth());

                    URL url;
                    HttpURLConnection urlConn;

                    url = new URL(uploadURL);

                    urlConn = (HttpURLConnection) url.openConnection();
                    urlConn.setRequestMethod("POST");
                    urlConn.setDoOutput(true);
                    urlConn.setDoInput(true);
                    urlConn.setUseCaches(false);

                    PrintWriter pw = new PrintWriter(urlConn.getOutputStream());

                    // now actually get and upload the Rows...
                    selection = "Select " +
                            SimpleQRecord.COLUMN_TOKEN_ID + ", " +
                            SimpleQRecord.COLUMN_EVENT_TIME + ", " +
                            SimpleQRecord.COLUMN_COMMENTS + ", " +
                            SimpleQRecord.COLUMN_EVENT_TYPE + ", " +
                            SimpleQRecord.COLUMN_WORKER_ID + ", " +
                            SimpleQRecord.COLUMN_STATION_ID + ", " +
                            SimpleQRecord.COLUMN_FACILITY_ID + " from SimpleQRecord";

                    c = MainActivity.myDB.rawQuery(selection, selectionArgs);
                    c.moveToFirst();

                    // XXX - should really get, add the device id
                    // to the upload string as well...


                    while (!isDone) {
                        sb.setLength(0);
                        sb.append("waiting_time_app" + "|");
                        sb.append(SimpleQRecord.VERSION + "|");
                        String nextToken = c.getString(0);
                        sb.append(nextToken + "|");
                        long eventTime = c.getLong(1);
                        sb.append(Long.toString(eventTime) + "|");
                        String nextComments = c.getString(2);
                        sb.append(nextComments + "|");
                        String nextEventType = c.getString(3);
                        sb.append(nextEventType + "|");
                        String nextWorkerId = c.getString(4);
                        sb.append(nextWorkerId + "|");
                        String nextStationID = c.getString(5);
                        sb.append(nextStationID + "|");
                        String nextFacilityID = c.getString(6);
                        sb.append(nextFacilityID + "|");

                        // String base64String = Base64.encodeToString(sb.toString().getBytes(), Base64.DEFAULT);
                        pw.println(sb.toString());
                        ++numRows;
                        // mProgress.setProgress(numRows);

                        if (!c.moveToNext()) {
                            isDone = true;
                        }
                    }

                    c.close();

                    pw.flush();
                    InputStream is = urlConn.getInputStream();
                    is.close();


                    urlConn.disconnect();

                    //TL2018.09.08: Comment out line 360 - 367 to disable deletion of database after upload
                    // delete the rows...
//					 String deleteString =  "delete from SimpleQRecord";
//					 MainActivity.myDB.execSQL(deleteString);

                    // delete completed SimpleQ items; mark others as fragments
//					 deleteString =  "delete from SimpleQ where duration > 0";
//					 MainActivity.myDB.execSQL(deleteString);

                }

                Qstats.uploadProblem = false;


            } catch (Exception e) {
                Qstats.uploadProblem = true;
                e.printStackTrace();
                returnVal = new Long(-1);
                // check to be sure it isn't already in the Q
                // Context context = getApplicationContext();
                // String msg = getResources().getString(R.string.problem_uploading);
                // int duration = Toast.LENGTH_SHORT;
                // Toast toast = Toast.makeText(context, msg, duration);
                // toast.show();
            }

            justDidUpload = true;
            return (returnVal);
        }

        @Override
        protected void onPostExecute(Long result) {
            Context context = getApplicationContext();
            int retVal = result.intValue();

            if (retVal == 0) {
                String msg = getResources().getString(R.string.upload_success);
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(context, msg, duration);
                toast.show();
                finish();
            } else {
                String msg = getResources().getString(R.string.upload_failure);
                int duration = Toast.LENGTH_LONG;
                Toast toast = Toast.makeText(context, msg, duration);
                toast.show();
            }
        }
    }

}
