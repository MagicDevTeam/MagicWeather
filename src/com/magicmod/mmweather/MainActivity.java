
package com.magicmod.mmweather;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.magicmod.mmweather.config.Preferences;
import com.magicmod.mmweather.engine.WeatherEngine;
import com.magicmod.mmweather.engine.WeatherInfo;
import com.magicmod.mmweather.engine.WeatherProvider;
import com.magicmod.mmweather.engine.WeatherResProvider;
import com.magicmod.mmweather.engine.WeatherInfo.DayForecast;
import com.magicmod.mmweather.engine.WeatherProvider.LocationResult;
import com.magicmod.mmweather.utils.Constants;
import com.magicmod.mmweather.utils.widget.RotateImageView;
import com.magicmod.mmweather.utils.widget.CirclePageIndicator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


public class MainActivity extends FragmentActivity implements OnClickListener {

    private static final String TAG = "MainActivity";
    private static final boolean DBG = Constants.DEBUG;
    
    /*private static final int LOACTION_OK = 0;
    private static final int UPDATE_EXISTS_CITY = 2;
    public static final int GET_WEATHER_SCUESS = 3;
    public static final int GET_WEATHER_FAIL = 4;*/

    //Title
    private ImageView mCityManagerBtn, mLocationBtn, mShareBtn;
    private TextView mTitleCityName;
    private RotateImageView mUpdateProgressBar;
    
    //Today's view
    private TextView mCountryTextView;
    private TextView mCityTextView, mSyncTimeTextView, mWeatherSourceTextView, mPm25TextView, mAqiDataTextView;
    private TextView mTodayWeekTextView, mTodayMonthTextView, mTempTextView, mHumidityTextView, mWeatherConditionTextView, mWindTextView;
    private ImageView mWeatherImageView, mAqiImageView;
    private View mAqiRootView;
    
    
    private ViewPager mViewPager;
    private List<Fragment> fragments;
    private NextDaysWeatherPagerAdapter mWeatherPagerAdapter;
    
    WeatherEngine mWeatherEngine;

    private Context mContext;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mContext = getApplicationContext();
        
        setContentView(R.layout.activity_main);
        
        mWeatherEngine = WeatherEngine.getinstance(mContext);
        
        initViews();
        initWeatherData();
        initFragments();
   }

    @Override
    protected void onResume() {
        super.onResume();
        //initWeatherData();
    }

    private void initViews() {
        mCityManagerBtn = (ImageView) findViewById(R.id.title_city_manager);
        mCityManagerBtn.setOnClickListener(this);
        mLocationBtn = (ImageView) findViewById(R.id.title_location);
        mLocationBtn.setOnClickListener(this);
        mShareBtn = (ImageView) findViewById(R.id.title_share);
        mShareBtn.setOnClickListener(this);
        mUpdateProgressBar = (RotateImageView) findViewById(R.id.title_update_progress);
        mUpdateProgressBar.setOnClickListener(this);
        mTitleCityName = (TextView) findViewById(R.id.title_city_name);
        mTitleCityName.setOnClickListener(this);
        
        mCountryTextView = (TextView) findViewById(R.id.country);
        mCityTextView = (TextView) findViewById(R.id.city);
        mWeatherSourceTextView = (TextView) findViewById(R.id.weather_source);
        mSyncTimeTextView = (TextView) findViewById(R.id.sync_time);
        mPm25TextView = (TextView) findViewById(R.id.pm_data);
        mAqiDataTextView = (TextView) findViewById(R.id.pm2_5_quality);
        
        mTodayWeekTextView = (TextView) findViewById(R.id.week_today);
        mTodayMonthTextView = (TextView) findViewById(R.id.month_today);
        mTempTextView = (TextView) findViewById(R.id.temperature);
        mHumidityTextView = (TextView) findViewById(R.id.humidity);
        mWeatherConditionTextView = (TextView) findViewById(R.id.weather_condition);
        mWindTextView = (TextView) findViewById(R.id.wind);
        mWeatherImageView = (ImageView) findViewById(R.id.weather_img);
        mAqiImageView = (ImageView) findViewById(R.id.pm2_5_img);
        mAqiImageView.setOnClickListener(this);
    }

    /**
     * First data adapter when app opened
     * 2014年1月19日
     */
    private void initWeatherData() {
        if (DBG)
            Log.d(TAG, "init weather data");
        
        WeatherInfo info = mWeatherEngine.getCache(); //get weather info from cache
        if (info == null) { //open a dialog to let user select their city
            if (DBG)
                Log.d(TAG, "get cache fail, start the city input dialog");
            showCityInputDialog(true);
        }
        updateWeatherView(info,false);
    }

    private void initFragments() {
        fragments = new ArrayList<Fragment>();
        fragments.add(new NextDaysFirstWeatherFragment(mContext, mWeatherEngine.getCache(),
                mWeatherEngine.getWeatherProvider().getWeatherResProvider()));
        fragments.add(new NextDaysSecondWeatherFragment(mContext, mWeatherEngine.getCache(),
                mWeatherEngine.getWeatherProvider().getWeatherResProvider()));
        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        mWeatherPagerAdapter = new NextDaysWeatherPagerAdapter(getSupportFragmentManager(),
                fragments);
        mViewPager.setAdapter(mWeatherPagerAdapter);
        ((CirclePageIndicator) findViewById(R.id.indicator)).setViewPager(mViewPager);

    }

    private void updateWeatherView(WeatherInfo weatherInfo, boolean refreshFragment) {
        if (weatherInfo == null) {
            return;
        }

        WeatherProvider provider = mWeatherEngine.getWeatherProvider();
        WeatherResProvider res = provider.getWeatherResProvider();
        
        if (refreshFragment) {
            if (fragments.size() > 0) {
                ((NextDaysFirstWeatherFragment) mWeatherPagerAdapter.getItem(0)).updateWeather(
                        weatherInfo, res, mContext);
                ((NextDaysSecondWeatherFragment) mWeatherPagerAdapter.getItem(1)).updateWeather(
                        weatherInfo, res, mContext);
            }
        }
        
        ArrayList<DayForecast> days = weatherInfo.getDayForecast();
        DayForecast today = res.getPreFixedWeatherInfo(mContext, days.get(0));

        mCountryTextView.setText(Preferences.getCountryName(mContext));
        mTitleCityName.setText(today.getCity());
        mCityTextView.setText(today.getCity());
        mWeatherSourceTextView.setText(provider.getNameResourceId());
        String s = mContext.getResources().getString(R.string.weather_sync_time);
        s = String.format(s, today.getSynctimestamp());
        mSyncTimeTextView.setText(s);
        mPm25TextView.setText(today.getPM2Dot5Data());
        mAqiDataTextView.setText(today.getAQIData());
        s = res.getWeek(today, mContext);
        mTodayWeekTextView.setText(s);
        s = res.getDay(today) + " " + res.getMonth(today);
        mTodayMonthTextView.setText(s);
        mTempTextView.setText(today.getTemperature());
        s = mContext.getResources().getString(R.string.weather_humidity);
        s = String.format(s, today.getHumidity());
        mHumidityTextView.setText(s);
        mWeatherConditionTextView.setText(today.getCondition());
        mWindTextView.setText(today.getWindDirection() + " " + today.getWindSpeed());
        mWeatherImageView.setImageResource(res.getWeatherIconResId(mContext, today.getConditionCode(), null));

    }

    /**
     * 连续按两次返回键就退出
     */
    private long firstTime;

    @Override
    public void onBackPressed() {
        if (System.currentTimeMillis() - firstTime < 3000) {
            finish();
        } else {
            firstTime = System.currentTimeMillis();
            Toast.makeText(mContext, R.string.press_again_exit, Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.title_city_manager:
                if (DBG)
                    Log.d(TAG, "click City manager");
                break;
            case R.id.title_location:
                if (DBG)
                    Log.d(TAG, "click auto location ");
                break;
            case R.id.title_share:
                if (DBG)
                    Log.d(TAG, "click share view");
                showShareMenu();
                break;
            case R.id.title_update_progress:
                if (DBG)
                    Log.d(TAG, "click update view");
                updateWeatherInfo();
                break;
            case R.id.pm2_5_img:
                if (DBG)
                    Log.d(TAG, "click pm2_5_img");
                showPM25Detail();
                break;
            case R.id.title_city_name:
                if (DBG)
                    Log.d(TAG, "City Name display click");
                showCityInputDialog(false);
            default:
                break;
        }

    }
    /**
     * Open a city input view
     * 
     * 2014年1月19日
     * @param exitAppWhenFail whether to exit the app when cancel or get data fail
     * 
     */
    private void showCityInputDialog(final boolean exitAppWhenFail) {
        final EditText editText = new EditText(this);
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setView(editText);
        dialog.setTitle(mContext.getString(R.string.weather_custom_location_dialog_title));
        
        dialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (exitAppWhenFail) {
                    finish();
                }                
            }
        });
        dialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            
            @Override
            public void onClick(DialogInterface dialog, int which) {
                
                String s = editText.getText().toString();
                
                if (s != null && !s.isEmpty()) {
                    showCitySelectDialog(s, exitAppWhenFail);
                } else {
                    if (exitAppWhenFail)
                        finish();
                }
            }
        });
        dialog.show();
    }

    protected void showCitySelectDialog(String cityName, boolean exitAppWhenFail) {
        new WeatherLocationTask(this, cityName, exitAppWhenFail).execute();
    }

    /**
     * Creat a new View to show the PM2.5 detail or weather codition info when user press the PM2.5 image
     * 
     * 2014年1月19日
     */
    private void showPM25Detail() {
        // TODO Auto-generated method stub
        
    }
    private void updateWeatherInfo() {
        LocationResult result = new LocationResult();
        result.id = Preferences.getCityID(mContext);
        result.city = mTitleCityName.getText().toString();
        result.country = Preferences.getCountryName(mContext);
        new WeatherUpdateTask(mContext, result, Preferences.isMetric(mContext)).execute();
    }
    private void showShareMenu() {
        // TODO Auto-generated method stub
        
    }

    public WeatherProvider getWeatherProvider() {
        return mWeatherEngine.getWeatherProvider();
    }
    
    /**
     * AysncTask to creat a city select dialoag and sync to get weather info when user select a city
     *  
     * @author SunRain
     * 
     * 2014年1月19日
     *
     */
    private class WeatherLocationTask extends AsyncTask<Void, Void, List<LocationResult>> {
        private ProgressDialog mProgressDialog;
        private String mLocation;
        private Context mContext;
        private boolean mExitAppWhenFail;
        

        public WeatherLocationTask(Context contexr, String location, boolean exitAppWhenFail) {
            mLocation = location;
            this.mContext = contexr;
            this.mExitAppWhenFail = exitAppWhenFail;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            mProgressDialog = new ProgressDialog(mContext);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setMessage(mContext.getString(R.string.weather_progress_title));
            mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    cancel(true);
                }
            });
            mProgressDialog.show();
        }

        @Override
        protected List<LocationResult> doInBackground(Void... input) {
            return mWeatherEngine.getWeatherProvider().getLocations(mLocation);
        }

        @Override
        protected void onPostExecute(List<LocationResult> results) {
            super.onPostExecute(results);

            if (results == null || results.isEmpty()) {
                Toast.makeText(mContext,
                        mContext.getString(R.string.weather_retrieve_location_dialog_title),
                        Toast.LENGTH_SHORT)
                        .show();
                if (mExitAppWhenFail) {
                    finish();
                }
            } else if (results.size() > 1) {
                handleResultDisambiguation(results);
            } else {
                applyLocation(results.get(0));
            }
            mProgressDialog.dismiss();
        }

        private void handleResultDisambiguation(final List<LocationResult> results) {
            CharSequence[] items = buildItemList(results);
            new AlertDialog.Builder(mContext)
                    .setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            applyLocation(results.get(which));
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (mExitAppWhenFail) {
                                finish();
                            }                            
                        }
                    })
                    .setTitle(R.string.weather_select_location)
                    .show();
        }

        private CharSequence[] buildItemList(List<LocationResult> results) {
            boolean needCountry = false, needPostal = false;
            String countryId = results.get(0).countryId;
            HashSet<String> postalIds = new HashSet<String>();

            for (LocationResult result : results) {
                if (!TextUtils.equals(result.countryId, countryId)) {
                    needCountry = true;
                }
                String postalId = result.countryId + "##" + result.city;
                if (postalIds.contains(postalId)) {
                    needPostal = true;
                }
                postalIds.add(postalId);
                if (needPostal && needCountry) {
                    break;
                }
            }

            int count = results.size();
            CharSequence[] items = new CharSequence[count];
            for (int i = 0; i < count; i++) {
                LocationResult result = results.get(i);
                StringBuilder builder = new StringBuilder();
                if (needPostal && result.postal != null) {
                    builder.append(result.postal).append(" ");
                }
                builder.append(result.city);
                if (needCountry) {
                    String country = result.country != null
                            ? result.country : result.countryId;
                    builder.append(" (").append(country).append(")");
                }
                items[i] = builder.toString();
            }
            return items;
        }

        private void applyLocation(final LocationResult result) {
            new WeatherUpdateTask(mContext, result, Preferences.isMetric(MainActivity.this.mContext)).execute();
        }
    }
    
    /**
     * AsyncTask to refresh the weather info
     * 
     * @author SunRain
     * 
     * 2014年1月19日
     *
     */
    private class WeatherUpdateTask extends AsyncTask<Void, Void, WeatherInfo> {
        private Context mContext;
        private LocationResult mLocationResult;
        private boolean mIsMeric;

        public WeatherUpdateTask(Context context, LocationResult result, boolean isMeric) {
            this.mContext = context;
            this.mLocationResult = result;
            this.mIsMeric = isMeric;
            
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mUpdateProgressBar.startAnim();
        }

        @Override
        protected WeatherInfo doInBackground(Void... params) {
            WeatherInfo info = mWeatherEngine.getWeatherProvider().getWeatherInfo(mLocationResult.id, mLocationResult.city, mIsMeric);
            if (info != null) {
                mWeatherEngine.setToCache(info);
                Preferences.setCityID(MainActivity.this.mContext, mLocationResult.id);
                Preferences.setCountryName(MainActivity.this.mContext, mLocationResult.country);
            }
            return info;
        }
        
        @Override
        protected void onPostExecute(WeatherInfo info) {
            super.onPostExecute(info);
            mUpdateProgressBar.stopAnim();
            updateWeatherView(info,true);
        }
    }

}
