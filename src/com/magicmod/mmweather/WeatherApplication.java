package com.magicmod.mmweather;

import android.app.Application;
import android.content.Context;

import com.magicmod.mmweather.engine.WeatherEngine;
import com.magicmod.mmweather.utils.Constants;

public class WeatherApplication extends Application{
    private static final String TAG = "weather_application";
    private static final boolean DBG = Constants.DEBUG;
    
    private WeatherApplication mApplication;
    private Context mContext;
    
    private WeatherEngine mWeatherEngine;
    
    @Override
    public void onCreate() {
        mContext = this.getApplicationContext();
        super.onCreate();
    }
    
    public WeatherEngine getWeatherEngine() {
        if (mWeatherEngine == null) {
            mWeatherEngine = WeatherEngine.getinstance(mContext);
        }
        return mWeatherEngine;
    }
    
    public void setWeatherEngine(WeatherEngine engine) {
        mWeatherEngine = engine;
    }
}
