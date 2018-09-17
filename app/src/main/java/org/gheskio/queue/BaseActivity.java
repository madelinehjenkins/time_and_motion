package org.gheskio.queue;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;

import java.util.Locale;

public class BaseActivity extends Activity{
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(updateBaseContextLocale(base));
    }

    private Context updateBaseContextLocale(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String language = preferences.getString("locale_override", Locale.getDefault().toString());
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        return updateResourcesLocale(context, locale);
    }

    private Context updateResourcesLocale(Context context, Locale locale) {
        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(locale);
        return context.createConfigurationContext(configuration);
    }

    private Boolean didLanguageChange(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String sharedLanguage = preferences.getString("locale_override", Locale.getDefault().toString());
        String currentLanguage = context.getResources().getConfiguration().locale.toString();

        return !sharedLanguage.equals(currentLanguage);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (didLanguageChange(this)) {
            recreate();
        }
    }

}
