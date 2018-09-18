package org.gheskio.queue;


import android.content.Context;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import com.google.android.gms.analytics.Tracker;


public class MainActivity extends BaseActivity {
    public static EditText mEditText;
    public EditText mCommentText;
    public static SharedPreferences sharedPref = null;

    public static final String DBNAME = "Q_DB";
    public static final String DBVERSION = "v0.1;";

    public static final String DBINITKEY = "IS_DBINITIALIZED";

    public static SimpleQdbHelper mySQRDBH;
    public static SQLiteDatabase myDB = null;

    public static SharedPreferences.Editor editor = null;
    public static String qrCode = "";

    public static boolean enableEdit = false;

    // for startIntentWithResult calls
    public static int QRCODEINTENT = 0;
    public static int EDITRECORDINTENT = 1;
    public static int PREFSINTENT = 2;

    Boolean isInitialized = new Boolean(false);

    private Tracker mTracker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AnalyticsApplication application = (AnalyticsApplication) getApplication();
        mTracker = application.getDefaultTracker();

        sharedPref = getSharedPreferences("gheskioprefs", Context.MODE_PRIVATE);
        editor = sharedPref.edit();
        isInitialized = sharedPref.getBoolean(DBINITKEY, false);

        mySQRDBH = new SimpleQdbHelper(getCurrentFocus().getContext());
        myDB = mySQRDBH.getWritableDatabase();


        checkInit();
        updateQlength();
        updateProfile();

        //
    }

    @Override
    protected void onStart() {
        super.onStart();
        // The activity is about to become visible.
        updateQlength();
        updateProfile();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The activity has become visible (it is now "resumed").
        updateQlength();
        updateProfile();
    }

    private void checkInit() {
        // open our basic KV store and see if we have initialized the
        // SQLlite db yet, or if this is the first time through...


        if (!isInitialized) {
            // XXX - use async task for better UI response...
            String createString = SimpleQRecord.getCreateStatement();
            myDB.execSQL(createString);
            System.out.println("executing: " + createString);
            createString = SimpleQ.getCreateStatement();
            System.out.println("executing: " + createString);
            myDB.execSQL(createString);

            editor.putBoolean(DBINITKEY, true);
            boolean isCommitted = editor.commit();

        }

        // check to see if some identity exists in Prefs...
        String workerVal = sharedPref.getString("WORKER_ID", "");
        String stationVal = sharedPref.getString("STATION_ID", "");
        String facilityVal = sharedPref.getString("FACILITY_ID", "");

        boolean needId = true;

        if ((workerVal.length() > 0) && (stationVal.length() > 0) && (facilityVal.length() > 0)) {
            needId = false;
        }

        if (needId) {
            Intent intent = new Intent(MainActivity.this, Prefs.class);
            startActivity(intent);
        }

    }

    public void doQStats(View view) {
        checkInit();
        Intent intent = new Intent(this, Qstats.class);
        startActivity(intent);
    }

    public void doPrefs(View view) {
        checkInit();
        Intent intent = new Intent(this, Prefs.class);
        startActivityForResult(intent, PREFSINTENT);
    }

    /* TL2018.06.16 */
    public void doQRScan(View view) {
        checkInit();

        Intent intent = new Intent(this, QrCodeScanner.class);
        startActivityForResult(intent, QRCODEINTENT);

        //	Intent intent = new Intent("com.google.zxing.client.android.SCAN"); ORIGINAL
        //	intent.putExtra("SCAN_MODE", "QR_CODE_MODE"); ORIGINAL
        //	startActivityForResult(intent, QRCODEINTENT); ORIGINAL
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        setContentView(R.layout.activity_main);
        EditText tokenText = (EditText) findViewById(R.id.qrCode);
        if (requestCode == QRCODEINTENT) {
            if (resultCode == RESULT_OK) {
                qrCode = intent.getStringExtra("SCAN_RESULT");
                tokenText.setText(qrCode);
            }
        } else if (requestCode == EDITRECORDINTENT) {
            if (resultCode == RESULT_OK) {
                String resultTokenid = intent.getStringExtra("tokenId");
                // if deleted, will be zero length
                tokenText.setText(resultTokenid);

                String resultComments = intent.getStringExtra("comments");
                EditText commentsEditText = (EditText) findViewById(R.id.comments);
                commentsEditText.setText(resultComments);
                Button editButton = (Button) findViewById(R.id.editButton);
                if (resultTokenid.length() > 0) {
                    editButton.setEnabled(true);
                } else {
                    editButton.setEnabled(false);
                }


            } else if (requestCode == PREFSINTENT) {
                // need to redraw the layout...
                onResume();
            }
        }
    }

    private void updateQlength() {

        String selection = "Select count(*) from " + SimpleQ.TABLE_NAME + " where " +
                SimpleQ.COLUMN_DURATION + " = 0";

        String selectionArgs[] = {};

        Cursor c = MainActivity.myDB.rawQuery(selection, selectionArgs);

        if (c.getCount() > 0) {
            c.moveToFirst();
            int tokenCount = c.getInt(0);
            TextView tokenCountTV = (TextView) findViewById(R.id.queueSize);
            tokenCountTV.setText(Integer.toString(tokenCount));
        }
        c.close();

    }


    private void updateProfile() {
        Prefs.workerVal = sharedPref.getString("WORKER_ID", "");
        Prefs.stationVal = sharedPref.getString("STATION_ID", "");
        Prefs.facilityVal = sharedPref.getString("FACILITY_ID", "");

        TextView workerTextView = (TextView) findViewById(R.id.worker);
        workerTextView.setText(Prefs.workerVal);

        TextView stationTextView = (TextView) findViewById(R.id.station);
        stationTextView.setText(Prefs.stationVal);

        TextView facilityTextView = (TextView) findViewById(R.id.facility);
        facilityTextView.setText(Prefs.facilityVal);
    }

    /**
     * give a token
     */
    public void doStartWait(View view) {
        if (checkIdentity() == 1) {
            mEditText = ((EditText) findViewById(R.id.qrCode));
            TextView commentET = (TextView) findViewById(R.id.comments);


            String commentVal = commentET.getText().toString();

            String tokenVal = mEditText.getText().toString().trim();
            if (tokenVal != null) {

                if (tokenVal.length() > 0) {
                    // check to be sure token isn't already given...
                    String queryString = "select give_time from simpleq where token_id = '" +
                            tokenVal + "' and duration = 0";
                    String args[] = {};

                    Cursor c = MainActivity.myDB.rawQuery(queryString, args);
                    if (c.getCount() > 0) {
                        InfoDialog.show(MainActivity.this, getString(R.string.token_already_given));
                    } else {
                        SimpleQRecord sqr = new SimpleQRecord(tokenVal, commentVal, "start_wait");
                        mEditText.setText("");
                        commentET.setText("");

                        Button editButton = (Button) findViewById(R.id.editButton);
                        editButton.setEnabled(false);
                    }
                    c.close();
                } else {
                    InfoDialog.show(MainActivity.this, getString(R.string.token_id_needed));
                }
            } else {
                // check to be sure it isn't already in the Q
                // TODO: Ensure this is correct - likely wrong message, assume this one actually should send you to login
                InfoDialog.show(MainActivity.this, getString(R.string.token_id_needed));
            }
            updateQlength();
        }
    }

    public void doEdit(View view) {

        Intent intent = new Intent(this, Gedit.class);

        mEditText = (EditText) findViewById(R.id.qrCode);
        String tokenVal = mEditText.getText().toString();

        if (tokenVal != null) {

            String queryString = "select comments, give_time from simpleq where token_id = '" +
                    tokenVal + "' and duration = 0";

            String args[] = {};

            Cursor c = MainActivity.myDB.rawQuery(queryString, args);

            if (c.getCount() == 0) {
                // hmmm - where did this token come from??
                c.close();
                InfoDialog.show(MainActivity.this, getString(R.string.token_id_needed));
            } else {

                // ok - we know about this outstanding token...
                c.moveToFirst();
                String commentVal = c.getString(0);
                long giveTime = c.getLong(1);

                c.close();

                intent.putExtra("TOKEN_ID", tokenVal);
                intent.putExtra("COMMENTS", commentVal);
                java.util.Date tokenTime = new java.util.Date(giveTime);
                intent.putExtra("START_TIME", tokenTime.toString());
                startActivityForResult(intent, EDITRECORDINTENT);
            }
        }

    }

    // return val of 1 means we have sufficient identity
    protected int checkIdentity() {

        checkInit();

        Prefs.workerVal = sharedPref.getString("WORKER_ID", "");
        Prefs.stationVal = sharedPref.getString("STATION_ID", "");
        Prefs.facilityVal = sharedPref.getString("FACILITY_ID", "");

        if ((Prefs.workerVal.length() == 0) || (Prefs.stationVal.length() == 0) ||
                (Prefs.facilityVal.length() == 0)) {

            Intent intent = new Intent(MainActivity.this, Prefs.class);
            startActivity(intent);

            return (0);
        } else {
            return (1);
        }
    }

    ;


    /**
     * take a token
     * <p>
     * In doing so, we implicitly are making a boundary on when it was
     * given, as indicated by the original "give" event.
     * <p>
     * However, between  give  and take events, an upload might occur,
     * that cleans out the original give event.
     */
    public void doEndWait(View view) {

        if (checkIdentity() == 1) {

            EditText mEditText = (EditText) findViewById(R.id.qrCode);
            String tokenVal = mEditText.getText().toString();

            mCommentText = (EditText) findViewById(R.id.comments);
            String commentVal = mCommentText.getText().toString();

            if (!TextUtils.isEmpty(tokenVal)) {

                // add an event row ... regardless
                SimpleQRecord sqr = new SimpleQRecord(tokenVal, commentVal, "end_wait");

                // now, check to be sure token is really in the Q..
                // note - we can get into weird situations where
                // the worker has cleared the queue, but there are still
                // outstanding tokens. For now, we err on the side of
                // at least recording the event

                String queryString = "select give_time from simpleq where token_id = '" +
                        tokenVal + "' and duration = 0";

                String args[] = {};

                Cursor c = MainActivity.myDB.rawQuery(queryString, args);

                if (c.getCount() == 0) {
                    // hmmm - where did this token come from??
                    c.close();
                    InfoDialog.show(MainActivity.this, getString(R.string.unknown_token));

                } else {

                    // ok - we know about this outstanding token...
                    c.moveToFirst();
                    long giveTime = c.getLong(0);
                    long nowTime = new java.util.Date().getTime();

                    c.close();
                    long duration = nowTime - giveTime;
                    String updateSQL = "update simpleQ set duration = " + duration + " where token_id = '" +
                            tokenVal + "'";

                    MainActivity.myDB.execSQL(updateSQL);

                    // clear the fields
                    mEditText.setText("");
                    mCommentText.setText("");

                    Button editButton = (Button) findViewById(R.id.editButton);
                    editButton.setEnabled(false);
                    updateQlength();
                }

            } else {
                InfoDialog.show(MainActivity.this, getString(R.string.token_id_needed));
            }
            updateQlength();
        }
    }


    /**
     * look at the next token in the line
     */
    public void doNext(View view) {
        // make sure there is something in the Queue at all
        updateQlength();

        TextView tokenCountTV = (TextView) findViewById(R.id.queueSize);
        String tokenCountText = tokenCountTV.getText().toString();
        int tokenCount = Integer.parseInt(tokenCountText);
        if (tokenCount == 0) {
            InfoDialog.show(MainActivity.this, getString(R.string.no_tokens_found));
        } else {
            checkInit();
            String selection = "select min(give_time) from simpleq where duration = 0";
            String selectionArgs[] = {};

            Cursor c = MainActivity.myDB.rawQuery(
                    selection,
                    selectionArgs);

            c.moveToFirst();
            long minGiveTime = c.getLong(0);
            c.close();

            selection = "select token_id, comments from simpleq where give_time = " + Long.toString(minGiveTime);
            String selectionArgs2[] = {};
            c = MainActivity.myDB.rawQuery(
                    selection,
                    selectionArgs2);

            c.moveToFirst();
            String nextToken = c.getString(0);
            String nextComment = c.getString(1);
            c.close();

            mEditText = (EditText) findViewById(R.id.qrCode);
            mEditText.setText(nextToken);

            mCommentText = (EditText) findViewById(R.id.comments);
            mCommentText.setText(nextComment);

            SimpleQ.lastSkipTime = minGiveTime;


            Button editButton = (Button) findViewById(R.id.editButton);
            editButton.setEnabled(true);
        }
    }

    /**
     * look at the next token in the line
     */
    public void doSkip(View view) {
        // make sure there is something in the Queue at all
        updateQlength();

        mEditText = (EditText) findViewById(R.id.qrCode);
        mCommentText = (EditText) findViewById(R.id.comments);
        TextView tokenCountTV = (TextView) findViewById(R.id.queueSize);

        String selectionArgs[] = {};

        String tokenCountText = tokenCountTV.getText().toString();
        int tokenCount = Integer.parseInt(tokenCountText);
        if (tokenCount == 0) {
            InfoDialog.show(MainActivity.this, getString(R.string.no_tokens_found));
        } else {
            checkInit();
            if (SimpleQ.lastSkipTime == 0) {
                // need to set it to the first item
                String firstGiveTimeQuery = "select min(give_time) from simpleq where duration = 0";
                Cursor c = MainActivity.myDB.rawQuery(firstGiveTimeQuery, selectionArgs);
                if (c.getCount() > 0) {
                    c.moveToFirst();
                    SimpleQ.lastSkipTime = c.getLong(0);
                    System.out.println("setting mintime to: " + SimpleQ.lastSkipTime);
                    System.out.flush();

                }
                c.close();
            }

            String selection = "select give_time from simpleq where give_time > " + SimpleQ.lastSkipTime + " and duration = 0 order by give_time limit 1";
            System.out.println(selection);

            Cursor c = MainActivity.myDB.rawQuery(selection, selectionArgs);

            if (c.getCount() > 0) {
                c.moveToFirst();
                long minGiveTime = c.getLong(0);
                c.close();

                System.out.println(selection);
                System.out.flush();

                String selection2 = "select token_id, comments from simpleq where give_time = " + Long.toString(minGiveTime);
                String selectionArgs2[] = {};
                System.out.println(selection2);
                System.out.flush();

                c = MainActivity.myDB.rawQuery(selection2, selectionArgs2);

                c.moveToFirst();
                String nextToken = c.getString(0);
                String nextComment = c.getString(1);
                c.close();

                mEditText.setText(nextToken);
                mCommentText.setText(nextComment);

                SimpleQ.lastSkipTime = minGiveTime;
                java.util.Date tokenTime = new java.util.Date();
                tokenTime.setTime(minGiveTime);
                Button editButton = (Button) findViewById(R.id.editButton);
                editButton.setEnabled(true);
            } else {
                c.close();
                mEditText.setText("");
                mCommentText.setText("");

                InfoDialog.show(MainActivity.this, getString(R.string.end_of_queue_reached));
            }
        }
    }

    public void doDelete(View view) {
        checkInit();
        mEditText = (EditText) findViewById(R.id.qrCode);
        TextView commentET = (TextView) findViewById(R.id.comments);
        String commentVal = commentET.getText().toString();

        String tokenVal = mEditText.getText().toString();
        if (tokenVal != null) {
            mEditText = (EditText) findViewById(R.id.qrCode);

            String selection = "delete from " + SimpleQ.TABLE_NAME + " where token_id = ? and " +
                    SimpleQ.COLUMN_DURATION + " = 0";

            String selectionArgs[] = {tokenVal};

            Cursor c = MainActivity.myDB.rawQuery(
                    selection,
                    selectionArgs);

            c.close();
            // clear the fields
            mEditText.setText("");
            mCommentText = (EditText) findViewById(R.id.comments);
            commentET.setText("");
            updateQlength();
        } else {
            InfoDialog.show(getApplicationContext(), getString(R.string.token_id_needed));
        }
        updateQlength();
    }
}
