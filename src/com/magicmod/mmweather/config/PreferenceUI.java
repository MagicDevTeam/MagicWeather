/*************************************************************************

Copyright 2014 MagicMod Project

This file is part of MagicMod Weather.

MagicMod Weather is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

MagicMod Weather is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with MagicMod Weather. If not, see <http://www.gnu.org/licenses/>.

*************************************************************************/

package com.magicmod.mmweather.config;

import android.R.integer;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;

import com.magicmod.mmweather.R;
import com.magicmod.mmweather.WeatherUpdateService;
import com.magicmod.mmweather.utils.Constants;

public class PreferenceUI extends Activity {
    private static final String TAG = "PreferenceUI";
    private static boolean DBG = Constants.DEBUG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        this.getFragmentManager().beginTransaction().replace(android.R.id.content, new Fragment()).commit();
    }

    private class Fragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
        private static final String TAG = "fragment";

        private static final String WEATHER_USE_METRIC = "weather_use_metric";
        private static final String WEATHER_USE_24H = "weather_use_24h";
        private static final String WEATHER_REFRESH_INTERFAL = "weather_refresh_interval";
        private static final String APP_VER = "application_version";
        
        private CheckBoxPreference mMetric;
        private CheckBoxPreference mUse24H;
        private ListPreference mRefreshInterval;
        private Preference mVersion;
        private Context mContext;
        
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.configs);
            
            mContext = this.getActivity().getApplicationContext();
            
            mMetric = (CheckBoxPreference) findPreference(WEATHER_USE_METRIC);
            mMetric.setChecked(Preferences.isMetric(mContext));

            mUse24H = (CheckBoxPreference) findPreference(WEATHER_USE_24H);
            mUse24H.setChecked(Preferences.getCalendar24HFormate(mContext));

            mRefreshInterval = (ListPreference) findPreference(WEATHER_REFRESH_INTERFAL);
            //int i = (int)(Preferences.getWeatherRefreshIntervalInMs(mContext)/60/1000);
            //mRefreshInterval.setSummary(mRefreshInterval.getEntries()[i]);
            
            mVersion = (Preference) findPreference(APP_VER);
            String v;
            try {
                v = this.getActivity().getPackageManager()
                        .getPackageInfo(this.getActivity().getPackageName(), 0).versionName;
                mVersion.setTitle(String.format("MagicMod Weather V%s", v));
                mVersion.setSummary("Copyright \u00A9 2014 MagicMod Project");
            } catch (NameNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                this.getPreferenceScreen().removePreference(mVersion);
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (DBG) Log.d(TAG, String.format("key is = > %s", key));
            Preference pref = this.findPreference(key);
            if (pref instanceof ListPreference) {
                ListPreference lp = (ListPreference) pref;
                pref.setSummary(lp.getEntry());
                String value = lp.getValue();
                if (lp.getKey().equals(WEATHER_REFRESH_INTERFAL)) {
                    Preferences.setWeatherRefreshInterval(mContext, Integer.valueOf(value));
                }
            }
            if (key.equals(WEATHER_USE_METRIC)) {
                Preferences.setMetric(mContext, mMetric.isChecked());
             } else if (key.equals(WEATHER_USE_24H)) {
                Preferences.setCalendar24HFormate(mContext, mUse24H.isChecked());
            }
            Intent i = new Intent(WeatherUpdateService.ACTION_FORCE_UPDATE);
            sendBroadcast(i);
        }
    }
}
