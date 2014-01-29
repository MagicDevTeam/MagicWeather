package com.magicmod.mmweather;

import android.app.KeyguardManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import com.magicmod.mmweather.config.Preferences;
import com.magicmod.mmweather.engine.WeatherEngine;
import com.magicmod.mmweather.engine.WeatherInfo;
import com.magicmod.mmweather.engine.WeatherProvider;
import com.magicmod.mmweather.utils.Constants;
import com.magicmod.mmweather.utils.NetUtil;

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
    private WeatherEngine mWeatherEngine;
    
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
        //final Context mContext = this.getApplicationContext();
        final WeatherApplication application = (WeatherApplication) this.getApplication();
        mWeatherEngine = application.getWeatherEngine();
        
        super.onCreate();
        
        mLastRefreshTimestamp = Preferences.getWeatherRefreshTimestamp(this);
        
        registerReceiver(); //注册相关的广播
    }
    
    protected void updateDateView() {
        if (DBG) Log.d(TAG, "update date view");
        
    }

    protected void updateWeatherView() {
        if (DBG) Log.d(TAG, "update weather view");
        
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
        // TODO Auto-generated method stub
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
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
            mWakeLock.setReferenceCounted(false);
            mContext = WeatherUpdateService.this;
        }
        
        @Override
        protected void onPreExecute() {
            if (DBG) Log.d(TAG, "ACQUIRING WAKELOCK");
            mWakeLock.acquire();
        }

        @Override
        protected WeatherInfo doInBackground(Void... params) {
            WeatherProvider provider = mWeatherEngine.getWeatherProvider();
            WeatherInfo info = provider.getWeatherInfo();
            if (info != null) { // ensure we're using the app now (set basic info in main activity)
                provider.refreshData();
                info = provider.getWeatherInfo();
                if (info != null) { //refresh data succeed
                    return info;
                }
            } else {
                return null;
            }

            info = provider.getWeatherInfo(Preferences.getCityID(mContext),
                    Preferences.getCityName(mContext), Preferences.isMetric(mContext));

            if (info != null) {
                mWeatherEngine.setToCache(info);
                return info;
            }
            //return cached info
            return null;//mWeatherEngine.getCache();
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
            mWakeLock.release();
            
            //Do not stop the services
            //stopSelf();
        }
    }
}
