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
