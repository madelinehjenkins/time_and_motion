package org.gheskio.queue;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class Gedit extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gedit);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.edit_record, menu);
		return true;
	}
	
	public void doUpdate(View view) {
		
		EditText commentText = (EditText)findViewById(R.id.editText1);
		String newComments = commentText.getText().toString();
		String tokenId = this.getIntent().getStringExtra("TOKEN_ID"); 	
		
		// ContentValues values = new ContentValues();
		// values.put(SimpleQRecord.COLUMN_COMMENTS, newComments);
		
		// int numRows = MainActivity.myDB.update(SimpleQRecord.TABLE_NAME, values,
				// SimpleQRecord.COLUMN_TOKEN_ID + "=" + tokenId , null );
		
		// for some reason, update method not working :-/
		String updateString = "update simpleq set comments = '" + newComments + "' where token_id = '" + tokenId + "';";
		MainActivity.myDB.execSQL(updateString);
		
		// put an entry in the log
		SimpleQRecord editRecord = new SimpleQRecord(tokenId, newComments, "edit_token");
			
		// XXX - add a new log record to denote this has been modified ; 
		// perhaps with duraction -1 ??
			
		Context context = getApplicationContext();
		String msg = getResources().getString(R.string.record_updated);
		int duration = Toast.LENGTH_SHORT;

		Toast toast = Toast.makeText(context, msg, duration);
		toast.show();	
			
	    Intent i = getIntent();
	    i.putExtra("tokenId", tokenId);
	    i.putExtra("comments", newComments);
	    setResult(Activity.RESULT_OK, i);

	    finish();
	}
	
	public void doDelete(View view) {
		String tokenId = this.getIntent().getStringExtra("TOKEN_ID"); 		
		String selection = "delete from simpleq where token_id = " + 
				tokenId + " and duration = 0";
			
		// String selectionArgs[] = {};
		// MainActivity.myDB.delete(SimpleQRecord.TABLE_NAME, SimpleQRecord.COLUMN_TOKEN_ID + "=?", new String[] { tokenId });
		// for some reason, delete not working... :-/
		
		String deleteString = "delete from simpleq where token_id = '" + tokenId + "';";
		MainActivity.myDB.execSQL(deleteString);
		// XXX - add a new log record to denote this has been modified ; 
		// perhaps with duraction -1 ??
			
		String deleteString2 = "delete from simpleqrecord where token_id = '" + tokenId + "';";
		MainActivity.myDB.execSQL(deleteString2);
		
		SimpleQRecord editRecord = new SimpleQRecord(tokenId, " ", "delete_token");

			
		TextView mTextView = (TextView)findViewById(R.id.textView1);
		mTextView.setText("");	
			
		EditText commentText = (EditText)findViewById(R.id.editText1);
		commentText.setText("");
		commentText.setEnabled(false);
			
		TextView startTimeText = (TextView)findViewById(R.id.textView5);
		startTimeText.setText("");
			
		// XXX - add a new log record to denote this has been deleted by operator ; 
		// perhaps with duraction -1 ??
			
		Context context = getApplicationContext();
		String msg = getResources().getString(R.string.record_deleted);
		int duration = Toast.LENGTH_SHORT;

		Toast toast = Toast.makeText(context, msg, duration);
		toast.show();
			
	    Intent i = getIntent();
	    i.putExtra("tokenId", "");
	    i.putExtra("comments", "");
	    setResult(RESULT_OK, i);
	    finish();
	        
	}
	
    @Override
    protected void onStart() {
        super.onStart();
        // The activity is about to become visible.
        String tokenId = this.getIntent().getStringExtra("TOKEN_ID"); 
		TextView mTextView = (TextView)findViewById(R.id.textView1);
		mTextView.setText(tokenId);		
		
        String comments = this.getIntent().getStringExtra("COMMENTS"); 
		EditText commentText = (EditText)findViewById(R.id.editText1);
		commentText.setText(comments);
		
		String startTime = this.getIntent().getStringExtra("START_TIME"); 
		TextView startTimeText = (TextView)findViewById(R.id.textView5);
		startTimeText.setText(startTime); 		
    }
    
}
