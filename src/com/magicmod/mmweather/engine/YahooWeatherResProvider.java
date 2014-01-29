/*
 * Copyright (C) 2014 The MagicMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.magicmod.mmweather.engine;

import android.R.dimen;
import android.R.integer;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.content.Context;
import android.content.Loader.ForceLoadContentObserver;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.renderscript.Element;
import android.util.DisplayMetrics;
import android.util.Log;

import com.magicmod.mmweather.R;
import com.magicmod.mmweather.engine.WeatherInfo.DayForecast;
import com.magicmod.mmweather.utils.Constants;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class YahooWeatherResProvider implements WeatherResProvider{
    private static final String TAG = "YahooWeatherResProvider";
    private static final boolean DEBUG = Constants.DEBUG;

    @Override
    public int getWeatherIconResId(Context context, String conditionCode, String iconSet) {
        if (iconSet == null) {
            iconSet = "_";
        } else {
            iconSet = "_" + iconSet + "_";
        }
        final String iconName = "weather" + iconSet + conditionCode; 
        final Resources res = context.getResources();
        final int resId = res.getIdentifier(iconName, "drawable", context.getPackageName());
        
        if (resId != 0) {
            return resId;
        }
        return R.drawable.weather_na;
    }

    @Override
    public Bitmap getWeatherIconBitmap(Context context, String conditionCode, String iconSet) {
        if (iconSet == null) {
            iconSet = "_";
        } else {
            iconSet = "_" + iconSet + "_";
        }
        final String iconName = "weather" + iconSet + conditionCode;
        final Resources res = context.getResources();
        final int resId = res.getIdentifier(iconName, "drawable", context.getPackageName());
        
        Drawable d = res.getDrawable(resId);
        if (d instanceof BitmapDrawable) {
            BitmapDrawable bd = (BitmapDrawable) d;
            return bd.getBitmap();
        }
        
        Bitmap b = Bitmap.createBitmap(d.getIntrinsicWidth(), d.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        d.setBounds(0, 0, b.getWidth(), b.getHeight());
        d.draw(c);
        c.setBitmap(null);
        return b;
    }

    @Override
    public DayForecast getPreFixedWeatherInfo(Context context, final DayForecast forecast) {
        if (forecast == null) {
            return null;
        }
        DayForecast day = new DayForecast();
        day.setHumidity(forecast.getHumidity() + "%");
        day.setTemperature(forecast.getTemperature() + "\u00b0" + forecast.getTempUnit());
        day.setTempHigh(forecast.getTempHigh() + "\u00b0" + forecast.getTempUnit());
        day.setTempLow(forecast.getTempLow() + "\u00b0" + forecast.getTempUnit());
        day.setWindSpeed(forecast.getWindSpeed() + forecast.getWindSpeedUnit());
        day.setAQIData(forecast.getAQIData());
        day.setCity(forecast.getCity());
        //day.setCondition(forecast.getCondition());
        day.setCondition(getCondition(context, forecast.getConditionCode(), forecast.getCondition()));
        day.setConditionCode(forecast.getConditionCode());
        day.setDate(forecast.getDate());
        day.setPM2Dot5Data(forecast.getPM2Dot5Data());
        day.setSunRaise(forecast.getSunRise());
        day.setSunSet(forecast.getSunSet());
        day.setSynctimestamp(forecast.getSynctimestamp());
        day.setTempUnit(forecast.getTempUnit());
        day.setWindSpeedUnit(forecast.getWindSpeedUnit());
        
        String windDirec = forecast.getWindDirection();
        if (!windDirec.equals(WeatherInfo.DATA_NULL)) {
            int resId;
            
            int windDirection = Integer.parseInt(forecast.getWindDirection());
            if (windDirection < 23) resId = R.string.weather_N;
            else if (windDirection < 68) resId = R.string.weather_NE;
            else if (windDirection < 113) resId = R.string.weather_E;
            else if (windDirection < 158) resId = R.string.weather_SE;
            else if (windDirection < 203) resId = R.string.weather_S;
            else if (windDirection < 248) resId = R.string.weather_SW;
            else if (windDirection < 293) resId = R.string.weather_W;
            else if (windDirection < 338) resId = R.string.weather_NW;
            else resId = R.string.weather_N;
            windDirec = context.getString(resId);
            day.setWindDirection(windDirec);
            
        }
        return day;
    }

    @Override
    public DayForecast getPreFixedWeatherInfo(final DayForecast forecast) {
        // TODO Auto-generated method stub
        return forecast;
    }

    @Override
    public String getYear(final DayForecast forecast) {
        String s[] = forecast.getDate().split(" ");
        return s[2];
    }

    @Override
    public String getMonth(final DayForecast forecast) {
        String s[] = forecast.getDate().split(" ");
        return s[1];
    }

    @Override
    public String getDay(final DayForecast forecast) {
        String s[] = forecast.getDate().split(" ");
        return s[0];
    }

    @Override
    public String getWeek(final DayForecast forecast, Context context) {
        Calendar c = null;
        c = Calendar.getInstance(Locale.CHINESE);
        String s[] = forecast.getDate().split(" ");
        String date = String.format("%s/%s/%s", s[0], monthToNum(s[1]), s[2]);
        int dayForWeek = 0;
        try {
            c.setTime(new SimpleDateFormat("dd/MM/yyyy").parse(date));
            dayForWeek = c.get(Calendar.DAY_OF_WEEK) - 1;
        } catch (ParseException e) {
            Log.e(TAG, "formate data string error, will return the origin data string");
            e.printStackTrace();
            return forecast.getDate();
        }
        Resources res = context.getResources();
        return res.getStringArray(R.array.week_entries)[dayForWeek];
    }
    
    private String monthToNum(String month) {
        String m[] = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        String n[] = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"};
        for (int i=0; i<m.length; i++) {
            if (month.equals(m[i])) {
                return n[i];
            }
        }
        return month;
    }
    
    private static String getCondition(Context context, String conditionCode, String condition) {
        final Resources res = context.getResources();
        final int resId = res.getIdentifier("weather_" + conditionCode, "string", context.getPackageName());
        if (resId != 0) {
            return res.getString(resId);
        }
        return condition;
    }
}
