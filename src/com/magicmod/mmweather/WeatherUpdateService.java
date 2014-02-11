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

package com.magicmod.mmweather;

import android.R.integer;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.magicmod.mmweather.config.Preferences;
import com.magicmod.mmweather.engine.WeatherEngine;
import com.magicmod.mmweather.engine.WeatherInfo;
import com.magicmod.mmweather.engine.WeatherResProvider;
import com.magicmod.mmweather.engine.WeatherInfo.DayForecast;
import com.magicmod.mmweather.engine.WeatherProvider.LocationResult;
import com.magicmod.mmweather.engine.WeatherProvider;
import com.magicmod.mmweather.utils.Constants;
import com.magicmod.mmweather.utils.ImageUtils;
import com.magicmod.mmweather.utils.NetUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class WeatherUpdateService extends Service {
    private static final String TAG = "WeatherUpdateServices";
    private static boolean DBG = Constants.DEBUG;

    public static final String ACTION_FORCE_UPDATE = "COM.MAGICMOD.MMWEATHER.ACTION.FORCE_WEATHER_UPDATE";
    public static final String ACTION_UPDATE_FINISHED = "COM.MAGICMOD.MMWEATHER.ACTION.WEATHER_UPDATE_FINISHED";
    public static final String EXTRA_UPDATE_CANCELLED = "UPDATE_CANCELLED";
    
    //判断当前屏幕,当暗屏的时候不更新插件以节约电力
    private static boolean mScreenON = true;
    //private static boolean mLockScreenON = false;
    private static long mLastRefreshTimestamp = 0;//System.currentTimeMillis();
    
    private WeatherUpdateTask mTask;
    //private WeatherEngine mWeatherEngine;
    
    private RemoteViews mWidgetViews;
    
    //private boolean FirstFlag = true;
    
    //广播接收者,用以更新widget的时间和天气界面的更新
    private BroadcastReceiver mTimePickerBroadcast = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction().toString();
            //boolean extra = intent.getBooleanExtra(EXTRA_UPDATE_CANCELLED, false);
            if (DBG) Log.d(TAG, String.format("get atcion ==> %S", action));
            if (action.equals(Intent.ACTION_SCREEN_ON)) {
                mScreenON= true;
            }
            if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                mScreenON = false;
            }
            if (!mScreenON) {
                if (DBG) Log.d(TAG, "Screen is off, not update view");
                return;
            }
            
            //Remove KeyguardManager check as the widget could be used on LockScreen
            
            /*KeyguardManager km = (KeyguardManager)getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE);
            if (km.isKeyguardLocked()) {
                if (DBG) Log.d(TAG, "Keyguard is locked ");
                return;
            }*/
            
            PowerManager pm = (PowerManager) getApplicationContext().getSystemService(
                    Context.POWER_SERVICE);
            boolean isScreenON = pm.isScreenOn();
            if (isScreenON) {
                if (action.equals(ACTION_FORCE_UPDATE)) {
                    updateWeatherView();
                } else if (action.equals(ACTION_UPDATE_FINISHED)
                        && !intent.getBooleanExtra(EXTRA_UPDATE_CANCELLED, false)) {
                    updateWeatherView();
                } else {
                    long now = System.currentTimeMillis();
                    long due = Preferences.getWeatherRefreshIntervalInMs(WeatherUpdateService.this
                            .getApplicationContext());
                    long start = mLastRefreshTimestamp + due;

                    if (DBG)
                        Log.d(TAG, String.format(
                                "now time is %d,  refresh target time should be %d", now, start));

                    if (now >= start && due != 0) {
                        if (DBG)
                            Log.d(TAG, "Refresh weathr info due to at refresh time");

                        mLastRefreshTimestamp = now;
                        Preferences.setWeatherRefreshTimestamp(
                                WeatherUpdateService.this.getApplicationContext(),
                                mLastRefreshTimestamp);

                        boolean active = mTask != null
                                && mTask.getStatus() != AsyncTask.Status.FINISHED;
                        if (!active) {
                            mTask = new WeatherUpdateTask();
                            mTask.execute();
                        }
                    }
                }
                updateDateView();
            }
        }
    };

    @Override
    public void onCreate() {
        // final Context mContext = this.getApplicationContext();
        final WeatherApplication application = (WeatherApplication) this.getApplication();
        //mWeatherEngine = application.getWeatherEngine();

        super.onCreate();

        mWidgetViews = new RemoteViews(application.getPackageName(), R.layout.widget_4x2);// 实例化widget
        
        Intent icon = new Intent(application.getApplicationContext(), WeatherWidget.class);
        icon.setAction(WeatherWidget.ACTION_WIDGET_ICON_HOTAREA);
        PendingIntent iconArea = PendingIntent.getBroadcast(this, 1, icon, 1);
        mWidgetViews.setOnClickPendingIntent(R.id.weather_icon, iconArea);

        /*Intent clock = new Intent(application.getApplicationContext(), WeatherWidget.class);
        clock.setAction(WeatherWidget.ACTION_WIDGET_TIME_HOTAREA);
        PendingIntent clockArea = PendingIntent.getBroadcast(this, 1, clock, 1);
        mWidgetViews.setOnClickPendingIntent(R.id.hour_view, clockArea);*/
        final Intent calendarClickIntent = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_APP_CALENDAR);
        final PendingIntent calendarClickPendingIntent = PendingIntent.getActivity(this, 0,
                calendarClickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mWidgetViews.setOnClickPendingIntent(R.id.data_view, calendarClickPendingIntent);

        mLastRefreshTimestamp = Preferences.getWeatherRefreshTimestamp(this.getApplicationContext());

        registerReceiver(); // 注册相关的广播
    }
    
    protected void updateDateView() {
        if (DBG) Log.d(TAG, "update date view");
        //SimpleDateFormat sdf = new new SimpleDateFormat("HHmm");
        Calendar ca = Calendar.getInstance(this.getResources().getSystem().getConfiguration().locale);
        StringBuilder builder = new StringBuilder();        
        boolean is24h = Preferences.getCalendar24HFormate(this.getApplicationContext());
        //Ugly code 
        if (is24h) {
            builder.append(ca.get(Calendar.HOUR_OF_DAY));
        } else {
            int h = ca.get(Calendar.HOUR);
            if (h < 10) builder.append("0");
            builder.append(h);
        }
        builder.append(":");
        int minute = ca.get(Calendar.MINUTE);
        if (minute < 10) builder.append("0");
        builder.append(minute);
        //Remove this, we'll use a stand view later
        /*if (!is24h) 
            builder.append(ca.get(Calendar.AM_PM)==Calendar.AM ? "am" : "pm");*/

        if (DBG) Log.d(TAG, "hour is " + builder.toString());
        
        mWidgetViews.setTextViewText(R.id.hour_view, builder.toString());
        
        final WeatherApplication application = (WeatherApplication) this.getApplication();
        ComponentName componentName = new ComponentName(application,
                WeatherWidget.class);
        AppWidgetManager.getInstance(application).updateAppWidget(componentName, mWidgetViews);
    }

    protected void updateWeatherView() {
        if (DBG) Log.d(TAG, "update weather view");
        WeatherProvider provider;
        WeatherResProvider res;
        ArrayList<DayForecast> days;
        try {
            WeatherApplication app = (WeatherApplication) this.getApplication();
            WeatherEngine engine = app.getWeatherEngine();
            WeatherInfo info = engine.getCache();
            provider = engine.getWeatherProvider();
            res = engine.getWeatherProvider().getWeatherResProvider();
            days = info.getDayForecast();
        } catch (Exception e) {
            e.printStackTrace();
            if (DBG) Log.d(TAG, "Can't get weather info, not refresh view");
            return;            
        }
        /*if (days.isEmpty()) {
            if (DBG) Log.d(TAG, "Can't get weather info, not refresh view");
            return;
        }*/
        DayForecast today = res.getPreFixedWeatherInfo(this.getApplicationContext(), days.get(0));
        
        mWidgetViews.setTextViewText(R.id.weather_source_view, getString(provider.getNameResourceId()));
        Log.d(TAG, "city name is " + today.getCity());
        
        mWidgetViews.setTextViewText(R.id.city_view, today.getCity());
        mWidgetViews.setTextViewText(R.id.today_temp, today.getTemperature());
        //Ugly code
        StringBuilder builder = new StringBuilder();
        builder.append(res.getWeek(today, this));
        builder.append(" ");
        builder.append(res.getDay(today));
        builder.append(" ");
        builder.append(res.getMonth(today));
        mWidgetViews.setTextViewText(R.id.data_view, builder.toString());
        //mWidgetViews.setImageViewResource(R.id.weather_icon, res.getWeatherIconResId(this, today.getConditionCode(), null));
        final Resources resources = this.getResources();
        Drawable d = resources.getDrawable(res.getWeatherIconResId(this, today.getConditionCode(), null));
        Bitmap b = ImageUtils.resizeBitmap(this, d, 120);
        mWidgetViews.setImageViewBitmap(R.id.weather_icon, b);
        
        mWidgetViews.setTextViewText(R.id.temp_low_hight, today.getTempHigh() + " | " + today.getTempLow());
        mWidgetViews.setTextViewText(R.id.wind_view, today.getWindDirection() + " " + today.getWindSpeed());
        
        final WeatherApplication application = (WeatherApplication) this.getApplication();
        ComponentName componentName = new ComponentName(application,
                WeatherWidget.class);
        AppWidgetManager.getInstance(application).updateAppWidget(componentName, mWidgetViews);
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_FORCE_UPDATE);
        filter.addAction(ACTION_UPDATE_FINISHED);
        filter.addAction("android.intent.action.TIME_TICK");
        filter.addAction("android.intent.action.TIME_SET");
        filter.addAction("android.intent.action.DATE_CHANGED");
        filter.addAction("android.intent.action.TIMEZONE_CHANGED");
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mTimePickerBroadcast, filter);
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (DBG) Log.d(TAG, String.format("onBind || Got intent => %s", intent.getAction()));
        return null;
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DBG) 
            Log.d(TAG, String.format("onStartCommand || Got intent => %s", intent.getAction()));

        /*boolean active = mTask != null && mTask.getStatus() != AsyncTask.Status.FINISHED;
        
        if (active) {
            Log.d(TAG, "Weather update is still active, not starting new update");
            return START_REDELIVER_INTENT;
        }*/

        //mTask = new WeatherUpdateTask();
        //mTask.execute();

        updateDateView();
        updateWeatherView();
        //return START_REDELIVER_INTENT;
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        if (mTask != null && mTask.getStatus() != AsyncTask.Status.FINISHED) {
            mTask.cancel(true);
            mTask = null;
        }
        if (mTimePickerBroadcast != null)
            unregisterReceiver(mTimePickerBroadcast);
        super.onDestroy();
    }
 
    private void sendCancelledBroadcast() {
        Intent finishedIntent = new Intent(ACTION_UPDATE_FINISHED);
        finishedIntent.putExtra(EXTRA_UPDATE_CANCELLED, true);
        sendBroadcast(finishedIntent);        
    }

    private boolean shouldUpdate(boolean force) {
        long interval = Preferences.getWeatherRefreshIntervalInMs(this.getApplicationContext());
        if (interval == 0 && !force) {
            if (DBG) Log.v(TAG, "Interval set to manual and update not forced, skip update");
            return false;
        }

        if (!force) {
            return false;
        }
        return NetUtil.isNetworkAvailable(this);
    }

    

    private class WeatherUpdateTask extends AsyncTask<Void, Void, WeatherInfo> {
        private WakeLock mWakeLock;
        private Context mContext;

        public WeatherUpdateTask() {
            if (DBG) Log.d(TAG, "Starting weather update task");
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            this.mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
            this.mWakeLock.setReferenceCounted(false);
            this.mContext = WeatherUpdateService.this.getApplicationContext();
        }
        
        @Override
        protected void onPreExecute() {
            if (DBG) Log.d(TAG, "ACQUIRING WAKELOCK");
            this.mWakeLock.acquire();
        }

        @Override
        protected WeatherInfo doInBackground(Void... params) {
            WeatherEngine engine;
            WeatherProvider provider;
            WeatherInfo info = null;//provider.getWeatherInfo();
            try {
                //WeatherProvider provider = mWeatherEngine.getWeatherProvider();
                WeatherApplication app = (WeatherApplication) getApplication();
                engine = app.getWeatherEngine();
                provider = engine.getWeatherProvider();
                info = engine.getCache();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
            if (info != null) { // ensure we opened the app before add widget
                //provider.refreshData();
                LocationResult result = new LocationResult();
                result.id = Preferences.getCityID(mContext);
                result.city = Preferences.getCityName(mContext);//mTitleCityName.getText().toString();
                result.country = Preferences.getCountryName(mContext);
                if (DBG)
                    Log.d(TAG, String.format("updateWeatherInfo , city id => %s, city name => %s, country => %s", result.id, result.city, result.country));
                info = provider.getWeatherInfo(result.id, result.city, Preferences.isMetric(mContext));//getWeatherInfo();
                if (info != null) { //refresh data succeed
                    engine.setToCache(info);
                    return info;
                }
            } 
            return null;
        }
        
        @Override
        protected void onPostExecute(WeatherInfo result) {
            finish(result);
        }

        @Override
        protected void onCancelled() {
            finish(null);
        }

        private void finish(WeatherInfo result) {
            if (result != null) { //update weather info
                Log.d(TAG, "weather info not null");
                Intent i =  new Intent(ACTION_UPDATE_FINISHED);
                sendBroadcast(i);
                Preferences.setWeatherRefreshTimestamp(mContext, System.currentTimeMillis());
            } else if (isCancelled()){
                if (DBG) Log.d(TAG, "Weather update synctask, cancelled()");
            }
            
            Intent i = new Intent(ACTION_UPDATE_FINISHED);
            i.putExtra(EXTRA_UPDATE_CANCELLED, result == null);
            sendBroadcast(i);
            
            if(DBG) Log.d(TAG, "Release wakelock");
            this.mWakeLock.release();
        }
    }
}
