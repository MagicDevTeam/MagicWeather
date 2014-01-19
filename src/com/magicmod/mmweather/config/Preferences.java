package com.magicmod.mmweather.config;

import android.content.Context;
import android.content.SharedPreferences;

import com.magicmod.mmweather.utils.Constants;

public class Preferences {
    public static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
    }
    
    public static boolean isMetric(Context context) {
        return getPrefs(context).getBoolean(Constants.USE_METRIC, true);
    }
    
    public static boolean setMetric(Context context, boolean b) {
        return getPrefs(context).edit().putBoolean(Constants.USE_METRIC, b).commit();
    }
    
    public static String getCityID(Context context) { //default is shanghai
        return getPrefs(context).getString(Constants.CITY_ID, "2151849");
    }
    
    public static boolean setCityID(Context context, String id) {
        return getPrefs(context).edit().putString(Constants.CITY_ID, id).commit();
    }
    
    public static String getCountryName(Context context) {
        return getPrefs(context).getString(Constants.COUNTRY_NAME, "ShangHai");
    }
    
    public static boolean setCountryName(Context context, String countyNameString) {
        return getPrefs(context).edit().putString(Constants.COUNTRY_NAME, countyNameString).commit();
    }
}

