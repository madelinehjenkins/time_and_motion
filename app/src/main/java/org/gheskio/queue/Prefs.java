package org.gheskio.queue;


import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class Prefs extends BaseActivity implements AdapterView.OnItemSelectedListener {
    private boolean isFirstSpinnerCall = true;
    public static SharedPreferences sharedPref = null;
    public SharedPreferences.Editor editor = null;

    private EditText workerET = null;
    public static String workerVal = null;

    private EditText stationET = null;
    public static String stationVal = null;

    private EditText facilityET = null;
    public static String facilityVal = null;

    private Spinner langSpinner = null;
    public static String screenLang = null;

    private Map<String, Integer> languageToPositionMap = new HashMap<String, Integer>() {{
        put("en", 0);
        put("fr", 1);
        put("ht", 2);
    }};

    private Map<String, Locale> fromSelectionToLocale = new HashMap<String, Locale>() {{
        put("English", new Locale("en"));
        put("Français", new Locale("fr"));
        put("Kreyòl", new Locale("ht"));
    }};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prefs);
        setSpinnerFromLocale();
        refreshPrefs();
        Spinner spinner = (Spinner) findViewById(R.id.spinner1);
        spinner.setOnItemSelectedListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.prefs, menu);
        return true;
    }


    private void setSpinnerFromLocale() {
        Locale currentLocale = getResources().getConfiguration().locale;
        Integer spinnerPosition = languageToPositionMap.get(currentLocale.toString().substring(0,2));
        Spinner langSpinner = (Spinner) findViewById(R.id.spinner1);
        langSpinner.setSelection(spinnerPosition);
    }

    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        String selectedLanguage = parent.getItemAtPosition(pos).toString();
        Locale selectedLocale = fromSelectionToLocale.get(selectedLanguage);

        Locale currentLocale = getResources().getConfiguration().locale;

        if (!isFirstSpinnerCall) {
            if (!currentLocale.equals(selectedLocale)) {
                SharedPreferences.Editor e = PreferenceManager.getDefaultSharedPreferences(this).edit();
                e.putString("locale_override", selectedLocale.toString());
                e.commit();

                recreate();
            }
        } else {
            isFirstSpinnerCall = false;
        }
    }

    public void onNothingSelected(AdapterView<?> parent) { }


    public void doServerPrefs(View view) {
        Intent intent = new Intent(this, Server.class);
        startActivity(intent);
    }

    private void refreshPrefs() {
        sharedPref = getSharedPreferences("gheskioprefs", Context.MODE_PRIVATE);

        workerET = (EditText) findViewById(R.id.numGivesText);
        stationET = (EditText) findViewById(R.id.editText3);
        facilityET = (EditText) findViewById(R.id.editText20);
        workerET.setText(workerVal);
        stationET.setText(stationVal);
        facilityET.setText(facilityVal);


        workerVal = sharedPref.getString("WORKER_ID", "");
        stationVal = sharedPref.getString("STATION_ID", "");
        facilityVal = sharedPref.getString("FACILITY_ID", "");
    }

    public void savePrefs(View view) {
        // save stuff back into SharedPrefs...
        boolean haveSufficientInfo = true;
        workerVal = workerET.getText().toString();
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_LONG;
        if (workerVal.length() == 0) {
            String msg = getResources().getString(R.string.please_add_worker);
            Toast toast = Toast.makeText(context, msg, duration);
            toast.show();
            haveSufficientInfo = false;
        }
        stationVal = stationET.getText().toString();
        if (stationVal.length() == 0) {
            String msg = getResources().getString(R.string.please_add_station);
            Toast toast = Toast.makeText(context, msg, duration);
            toast.show();
            haveSufficientInfo = false;
        }
        facilityVal = facilityET.getText().toString();
        if (facilityVal.length() == 0) {
            String msg = getResources().getString(R.string.please_add_facility);
            Toast toast = Toast.makeText(context, msg, duration);
            toast.show();
            haveSufficientInfo = false;
        }

        if (haveSufficientInfo) {
            editor = sharedPref.edit();
            editor.putString("WORKER_ID", workerVal);
            editor.putString("STATION_ID", stationVal);
            editor.putString("FACILITY_ID", facilityVal);

            editor.apply();

            String msg = getResources().getString(R.string.prefs_saved);
            Toast toast = Toast.makeText(context, msg, duration);
            toast.show();
            finish();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();  // Always call the superclass method first
        refreshPrefs();
    }
}
