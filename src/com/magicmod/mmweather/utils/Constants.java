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

package com.magicmod.mmweather.utils;

public class Constants {
    public static final boolean DEBUG = true;

    public static final String PREF_NAME = "MagicWeather";
    
    public static final long OUTDATED_LOCATION_THRESHOLD_MILLIS = 10L * 60L * 1000L; // 10 minutes
    
    public static final String WEATHER_ICONS = "weather_icons";
    public static final String MONOCHROME = "mono";
    public static final String USE_METRIC = "metric";
    public static final String CITY_ID = "city_id";
    public static final String CITY_NAME = "city_name";
    public static final String COUNTRY_NAME = "country_name";
    public static final String WEATHER_REFRESH_INTERVAL = "weather_refresh_interval";
    public static final String WEATHER_REFRESH_TIMESTAMP = "weather_refresh_timestamp";
    public static final String CALENDAR_24H_FORMATE = "calendar_24h_formate";
}
