package com.magicmod.mmweather;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.magicmod.mmweather.engine.WeatherInfo;
import com.magicmod.mmweather.engine.WeatherResProvider;
import com.magicmod.mmweather.engine.WeatherInfo.DayForecast;

import java.util.ArrayList;

public class NextDaysFirstWeatherFragment extends Fragment {
    
    private static final String TAG = "NextDaysFirstWeatherFragment";
	
    private TextView weekTv1, weekTv2, weekTv3;
    private ImageView weather_imgIv1, weather_imgIv2, weather_imgIv3;
    private TextView temperatureTv1, temperatureTv2, temperatureTv3;
    private TextView climateTv1, climateTv2, climateTv3;
    private TextView windTv1, windTv2, windTv3;

    private Context mContext;
    private WeatherInfo mWeatherInfo;
    private WeatherResProvider mResProvider;
    public NextDaysFirstWeatherFragment(Context context, WeatherInfo info, WeatherResProvider res) {
        this.mContext = context;
        this.mWeatherInfo = info;
        this.mResProvider = res;
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.main_view_weather_nextdays_item,
                container, false);
        View view1 = view.findViewById(R.id.subitem1);
        View view2 = view.findViewById(R.id.subitem2);
        View view3 = view.findViewById(R.id.subitem3);

        weekTv1 = (TextView) view1.findViewById(R.id.week);
        weekTv2 = (TextView) view2.findViewById(R.id.week);
        weekTv3 = (TextView) view3.findViewById(R.id.week);

        weather_imgIv1 = (ImageView) view1.findViewById(R.id.weather_img);
        weather_imgIv2 = (ImageView) view2.findViewById(R.id.weather_img);
        weather_imgIv3 = (ImageView) view3.findViewById(R.id.weather_img);
        temperatureTv1 = (TextView) view1.findViewById(R.id.temperature);
        temperatureTv2 = (TextView) view2.findViewById(R.id.temperature);
        temperatureTv3 = (TextView) view3.findViewById(R.id.temperature);

        climateTv1 = (TextView) view1.findViewById(R.id.climate);
        climateTv2 = (TextView) view2.findViewById(R.id.climate);
        climateTv3 = (TextView) view3.findViewById(R.id.climate);

        windTv1 = (TextView) view1.findViewById(R.id.wind);
        windTv2 = (TextView) view2.findViewById(R.id.wind);
        windTv3 = (TextView) view3.findViewById(R.id.wind);

        updateWeather(mWeatherInfo, mResProvider, mContext);
        return view;
    }

    //Ugly code,  :(
    public void updateWeather(WeatherInfo info, WeatherResProvider res, Context context) {
        if (info == null || res == null) {
            return;
        }
        ArrayList<DayForecast> days = info.getDayForecast();

        DayForecast day1 = res.getPreFixedWeatherInfo(context, getDayForecast(days, 0));
        DayForecast day2 = res.getPreFixedWeatherInfo(context, getDayForecast(days, 1));
        DayForecast day3 = res.getPreFixedWeatherInfo(context, getDayForecast(days, 2));
        
        if (day1 != null) {
            String week = res.getWeek(day1, context);
            weekTv1.setText(week);
            weather_imgIv1.setImageResource(res.getWeatherIconResId(context, day1.getConditionCode(), null));
            climateTv1.setText(day1.getCondition());
            String s = String.format("%s ~ %s", day1.getTempLow(), day1.getTempHigh());
            temperatureTv1.setText(s);
            windTv1.setText(day1.getWindDirection());
        } else {
            weekTv1.setVisibility(View.INVISIBLE);
            weather_imgIv1.setImageResource(R.drawable.weather_na);
            climateTv1.setVisibility(View.INVISIBLE);
            temperatureTv1.setVisibility(View.INVISIBLE);
            windTv1.setVisibility(View.INVISIBLE);
        }
        
        if (day2 != null) {
            weekTv2.setText(res.getWeek(day2, context));
            weather_imgIv2.setImageResource(res.getWeatherIconResId(context, day2.getConditionCode(), null));
            climateTv2.setText(day2.getCondition());
            String s = String.format("%s ~ %s", day2.getTempLow(), day2.getTempHigh());
            temperatureTv2.setText(s);
            windTv2.setText(day2.getWindDirection());
        } else {
            weekTv2.setVisibility(View.INVISIBLE);
            weather_imgIv2.setImageResource(R.drawable.weather_na);
            climateTv2.setVisibility(View.INVISIBLE);
            temperatureTv2.setVisibility(View.INVISIBLE);
            windTv2.setVisibility(View.INVISIBLE);
        }
        
        if (day3 != null) {
            weekTv3.setText(res.getWeek(day3, context));
            weather_imgIv3.setImageResource(res.getWeatherIconResId(context, day3.getConditionCode(), null));
            climateTv3.setText(day3.getCondition());
            String s = String.format("%s ~ %s", day3.getTempLow(), day3.getTempHigh());
            temperatureTv3.setText(s);
            windTv3.setText(day3.getWindDirection());
        } else {
            weekTv3.setVisibility(View.INVISIBLE);
            weather_imgIv3.setImageResource(R.drawable.weather_na);
            climateTv3.setVisibility(View.INVISIBLE);
            temperatureTv3.setVisibility(View.INVISIBLE);
            windTv3.setVisibility(View.INVISIBLE);
        }
    }
    
    private DayForecast getDayForecast(ArrayList<DayForecast>list, int index) {
        int size = list.size();
        if (size == 0 || list.isEmpty()) {
            return null;
        }
        if (index > size-1) {
            return null;
        }
        return list.get(index);
    }
}
