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
    
    public static boolean setCityName(Context context, String name) {
        return getPrefs(context).edit().putString(Constants.CITY_NAME, name).commit();
    }
    
    public static String getCityName(Context context) {
        return getPrefs(context).getString(Constants.CITY_NAME, "ShangHai");
    }
    
    public static String getCountryName(Context context) {
        return getPrefs(context).getString(Constants.COUNTRY_NAME, "ShangHai");
    }
    
    public static boolean setCountryName(Context context, String countyNameString) {
        return getPrefs(context).edit().putString(Constants.COUNTRY_NAME, countyNameString).commit();
    }
    
    /**
     * 2014年1月27日
     * @param context
     * @param interval 分钟数
     * @return
     */
    public static boolean setWeatherRefreshInterval(Context context, int interval) {
        return getPrefs(context).edit().putInt(Constants.WEATHER_REFRESH_INTERVAL, interval).commit();
    }
    
    /**
     * 2014年1月27日
     * @param context
     * @return 以毫秒为单位返回时间
     */
    public static long getWeatherRefreshIntervalInMs(Context context) {
        String value = getPrefs(context).getString(Constants.WEATHER_REFRESH_INTERVAL, "30");
        return Long.parseLong(value) * 60 * 1000;
    }
    
    /**
     * 2014年1月27日
     * @param context
     * @param time 当前的毫秒数(System.currentTimeMillis())
     * @return
     */
    public static boolean setWeatherRefreshTimestamp(Context context, long time) {
        return getPrefs(context).edit().putLong(Constants.WEATHER_REFRESH_TIMESTAMP, time).commit();
    }
    
    /**
     * 2014年1月27日
     * @param context
     * @return 返回存储的毫秒数
     */
    public static long getWeatherRefreshTimestamp(Context context) {
        return getPrefs(context).getLong(Constants.WEATHER_REFRESH_TIMESTAMP, System.currentTimeMillis());
    }
    
    public static boolean setCalendar24HFormate(Context context, boolean b) {
        return getPrefs(context).edit().putBoolean(Constants.CALENDAR_24H_FORMATE, b).commit();
    }
    
    public static boolean getCalendar24HFormate(Context context) {
        return getPrefs(context).getBoolean(Constants.CALENDAR_24H_FORMATE, true);
    }
}

