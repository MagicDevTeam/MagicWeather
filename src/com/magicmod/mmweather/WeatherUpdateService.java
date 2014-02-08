package com.magicmod.mmweather;

import android.R.integer;
import android.app.KeyguardManager;
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
    
    /*private TextView mWeatherSourceView, mHourView, mCityView, mTodayTempView, mDataView;
    private TextView mTempLowHightView, mWindView;
    private ImageView mWeatherIcon;*/
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
                /*if (FirstFlag) {
                    updateWeatherView();
                    FirstFlag = !FirstFlag;
                } else */if (action.equals(ACTION_FORCE_UPDATE)) {
                    updateWeatherView();
                } else if (action.equals(ACTION_UPDATE_FINISHED)
                        && !intent.getBooleanExtra(EXTRA_UPDATE_CANCELLED, false)) {
                    updateWeatherView();
                } else {
                    long now = System.currentTimeMillis();
                    long start = mLastRefreshTimestamp
                            + Preferences.getWeatherRefreshIntervalInMs(WeatherUpdateService.this);

                    if (DBG)
                        Log.d(TAG, String.format(
                                "now time is %d,  refresh target time should be %d", now, start));

                    if (now >= start) {
                        if (DBG)
                            Log.d(TAG, "Refresh weathr info due to at refresh time");

                        mLastRefreshTimestamp = now;
                        Preferences.setWeatherRefreshTimestamp(WeatherUpdateService.this,
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

        mLastRefreshTimestamp = Preferences.getWeatherRefreshTimestamp(this);

        registerReceiver(); // 注册相关的广播
    }
    
    protected void updateDateView() {
        if (DBG) Log.d(TAG, "update date view");
        //SimpleDateFormat sdf = new new SimpleDateFormat("HHmm");
        Calendar ca = Calendar.getInstance(this.getResources().getSystem().getConfiguration().locale);
        StringBuilder builder = new StringBuilder();        
        boolean is24h = Preferences.getCalendar24HFormate(getApplicationContext());
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
        if (!is24h) 
            builder.append(ca.get(Calendar.AM_PM)==Calendar.AM ? "am" : "pm");

        if (DBG) Log.d(TAG, "hour is " + builder.toString());
        
        mWidgetViews.setTextViewText(R.id.hour_view, builder.toString());
        
        final WeatherApplication application = (WeatherApplication) this.getApplication();
        ComponentName componentName = new ComponentName(application,
                WeatherWidget.class);
        AppWidgetManager.getInstance(application).updateAppWidget(componentName, mWidgetViews);
    }

    protected void updateWeatherView() {
        if (DBG) Log.d(TAG, "update weather view");
        //mWidgetViews.setImageViewResource(R.id.weather_icon, R.drawable.weather_na);
        WeatherApplication app = (WeatherApplication)this.getApplication();
        WeatherEngine engine = app.getWeatherEngine();
        WeatherInfo info = engine.getCache();
        WeatherProvider provider = engine.getWeatherProvider();
        WeatherResProvider res = engine.getWeatherProvider().getWeatherResProvider();
        ArrayList<DayForecast> days = info.getDayForecast();//provider.getWeatherInfo().getDayForecast();
        if (days.isEmpty()) {
            if (DBG) Log.d(TAG, "Can't get weather info, not refresh view");
            return;
        }
        DayForecast today = res.getPreFixedWeatherInfo(this.getApplicationContext(), days.get(0));
        
        mWidgetViews.setTextViewText(R.id.weather_source_view, getString(provider.getNameResourceId()));
        mWidgetViews.setTextViewText(R.id.city_view, Preferences.getCityName(this));
        mWidgetViews.setTextViewText(R.id.today_temp, today.getTemperature());
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

        boolean active = mTask != null && mTask.getStatus() != AsyncTask.Status.FINISHED;
        
        if (active) {
            Log.d(TAG, "Weather update is still active, not starting new update");
            return START_REDELIVER_INTENT;
        }
        
        /*boolean force = ACTION_FORCE_UPDATE.equals(intent.getAction());
        if (!shouldUpdate(force)) {
            Log.d(TAG, "Service started, but shouldn't update ... stopping");
            //stopSelf();
            sendCancelledBroadcast();
            return START_NOT_STICKY;
        }*/

        mTask = new WeatherUpdateTask();
        mTask.execute();

        //return START_REDELIVER_INTENT;
        return super.onStartCommand(intent, flags, startId);
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
        long interval = Preferences.getWeatherRefreshIntervalInMs(this);
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
            this.mContext = WeatherUpdateService.this;
        }
        
        @Override
        protected void onPreExecute() {
            if (DBG) Log.d(TAG, "ACQUIRING WAKELOCK");
            this.mWakeLock.acquire();
        }

        @Override
        protected WeatherInfo doInBackground(Void... params) {
            //WeatherProvider provider = mWeatherEngine.getWeatherProvider();
            WeatherApplication app = (WeatherApplication)getApplication();
            WeatherEngine engine = app.getWeatherEngine();
            WeatherProvider provider = engine.getWeatherProvider();
            WeatherInfo info = engine.getCache();//provider.getWeatherInfo();
            if (info != null) { // ensure we opened the app before add widget
                //provider.refreshData();
                LocationResult result = new LocationResult();
                result.id = Preferences.getCityID(mContext);
                result.city = Preferences.getCityName(mContext);//mTitleCityName.getText().toString();
                result.country = Preferences.getCountryName(mContext);
                info = provider.getWeatherInfo(result.id, result.city, Preferences.isMetric(mContext));//getWeatherInfo();
                if (info != null) { //refresh data succeed
                    engine.setToCache(info);
                    return info;
                }
            } 
            return null;


            //Else we get weather info from saved city infomation
            /*info = provider.getWeatherInfo(Preferences.getCityID(mContext),
                    Preferences.getCityName(mContext), Preferences.isMetric(mContext));

            if (info != null) {
                //mWeatherEngine.setToCache(info);
                engine.setToCache(info);
                return info;
            }*/
            //return cached info
            //return null;//mWeatherEngine.getCache();
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
            
            //Do not stop the services
            //stopSelf();
        }
    }
}
