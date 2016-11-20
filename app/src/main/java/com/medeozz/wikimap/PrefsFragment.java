package com.medeozz.wikimap;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import java.util.Map;


public class PrefsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.my_prefs_toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        Preference pref = findPreference(key);

        if (pref instanceof ListPreference) {

            ListPreference listPref = (ListPreference) pref;
            pref.setSummary(listPref.getEntry());

            if (key.equals("pref_updateinterval") || key.equals("pref_thumbsize") || key.equals("pref_fontsize")) {
                initRestartApp();
            }
        }

        if(key.equals("pref_marker_color")) {
            initRestartApp();
        }
    }

    private void initRestartApp() {

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                restartApp( getActivity() );
            }
        }, 200);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Set up initial values for all list preferences
        Map<String, ?> sharedPreferencesMap = getPreferenceScreen().getSharedPreferences().getAll();
        Preference pref;
        ListPreference listPref;
        for (Map.Entry<String, ?> entry : sharedPreferencesMap.entrySet()) {
            pref = findPreference(entry.getKey());
            if (pref instanceof ListPreference) {
                listPref = (ListPreference) pref;
                pref.setSummary(listPref.getEntry());
            }
        }

        // Set up a listener whenever a key changes
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {

        // Unregister the listener whenever a key changes
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    public void restartApp(Context context) {

        String tag = "WIKIMAP";

        try {
            //check if the context is given
            if (context != null) {
                //fetch the packagemanager so we can get the default launch activity
                // (you can replace this intent with any other activity if you want
                PackageManager pm = context.getPackageManager();
                //check if we got the PackageManager
                if (pm != null) {
                    //create the intent with the default start activity for your application
                    Intent mStartActivity = pm.getLaunchIntentForPackage(context.getPackageName());
                    if (mStartActivity != null) {
                        mStartActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        //create a pending intent so the application is restarted after System.exit(0) was called.
                        // We use an AlarmManager to call this intent in xxx ms
                        int mPendingIntentId = 223344;
                        PendingIntent mPendingIntent = PendingIntent.getActivity(context, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
                        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                        //kill the application
                        System.exit(0);
                    } else {
                        Log.e(tag, "Was not able to restart application, mStartActivity null");
                    }
                } else {
                    Log.e(tag, "Was not able to restart application, PM null");
                }
            } else {
                Log.e(tag, "Was not able to restart application, Context null");
            }
        } catch (Exception ex) {
            Log.e(tag, "Was not able to restart application");
        }

    }
}