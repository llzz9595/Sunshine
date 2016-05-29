package com.example.android.sunshine.app;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {


    private ArrayAdapter<String>  mForecastAdapter;
    public MainActivityFragment() {
    }

    //设定菜单
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       setHasOptionsMenu(true);
    }

    //设定菜单布局
    public void onCreateOptionsMenu(Menu menu ,MenuInflater inflater)
    {
        inflater.inflate(R.menu.menu_main, menu);
    }

    //事件发生
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if(item.getItemId() == R.id.action_refresh)
        {
            //刷新
            FetchWeatherTask weatherTask = new FetchWeatherTask();
            weatherTask.execute();
            //forecastWeather = weatherTask.forecastJsonStr;
            return true;}
        return false;
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView =  inflater.inflate(R.layout.fragment_main, container, false);

        String [] resultArr;
        String forecastJsonStr = null;
        // 创建数据
       String [] forecastArray ={
                "Today - Sunday - 88/23",
                "Tomorrow - Foggy - 70/40",
                "Weds - Foggy - 70/40",
                "Thurs - Foggy - 70/40",
                "Fri - Foggy - 70/40",
                "Sat - Foggy - 70/40",
                "Sun - Sunday - 75/40"
        };

      //  resultArr = getWeatherDataFromJson(forecastJsonStr, 7);
        //转换为列表
        List<String> weekForecast = new ArrayList<String>(Arrays.asList(forecastArray));

        //创建适配器
        mForecastAdapter = new ArrayAdapter<String>(
                //当前context
                 getActivity(),
                // id of listitem layout
                 R.layout.list_item_forcast,
                    //id of textview
                R.id.list_item_forcast_textview,
                // data id
                weekForecast
        );

        // find refenerence to listView 從rootView 中獲取 listview
        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);

        //setAdapter
        listView.setAdapter(mForecastAdapter);


        return rootView;
    }

    //异步

    public class FetchWeatherTask extends AsyncTask<String ,String[], String[]>{

         String forecastJsonStr;


        protected void onPostExecute(String[] result) {
            //更新适配器
            if(result != null)
            {
                mForecastAdapter.clear();
                for( String dayForecast : result)
                    mForecastAdapter.add(dayForecast);
            }

        }

        /* The date/time conversion code is going to be moved outside the asynctask later,
                     * so for convenience we're breaking it out into its own method now.
                     */
        //设置格式
        private String getReadableDateString(long time){
            // Because the API returns a unix timestamp (measured in seconds),
            // it must be converted to milliseconds in order to be converted to valid date.
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortenedDateFormat.format(time);
        }

        /**
         * Prepare the weather high/lows for presentation. 格式
         */
        private String formatHighLows(double high, double low) {
            // For presentation, assume the user doesn't care about tenths of a degree.
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            String highLowStr = roundedHigh + "/" + roundedLow;
            return highLowStr;
        }

        /**
         * Take the String representing the complete forecast in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         *
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DESCRIPTION = "main";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            // OWM returns daily forecasts based upon the local time of the city that is being
            // asked for, which means that we need to know the GMT offset to translate this data
            // properly.

            // Since this data is also sent in-order and the first day is always the
            // current day, we're going to take advantage of that to get a nice
            // normalized UTC date for all of our weather.

            //获取时间
            Time dayTime = new Time();
            dayTime.setToNow();

            // we start at the day returned by local time. Otherwise this is a mess.
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            // now we work exclusively in UTC
            dayTime = new Time();

            String[] resultStrs = new String[numDays];
            for(int i = 0; i < weatherArray.length(); i++) {
                // For now, using the format "Day, description, hi/low"
                String day;
                String description;
                String highAndLow;

                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                // The date/time is returned as a long.  We need to convert that
                // into something human-readable, since most people won't read "1400356800" as
                // "this saturday".
                long dateTime;
                // Cheating to convert this to UTC time, which is what we want anyhow
                dateTime = dayTime.setJulianDay(julianStartDay+i);
                day = getReadableDateString(dateTime);

                // description is in a child array called "weather", which is 1 element long.
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);

                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);

                highAndLow = formatHighLows(high, low);
                resultStrs[i] = day + " - " + description + " - " + highAndLow;
            }

            for (String s : resultStrs) {
                Log.v("", "Forecast entry: " + s);
            }
            return resultStrs;

        }

        @Override
        protected String[] doInBackground(String... params) {
            // These two need to be declared outside the try/catch
// so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.

            String format = "json";
            String units = "metric";
            int numdays = 7;
            String appid = "398ae2a4c5112f3604756bf1403e58b1";
            try {
                final String FORECAST_BASE_URL ="http://api.openweathermap.org/data/2.5/forecast/daily?";
                final String QUERY_PARAM ="q";
                final String FORMAL_PARAM = "mode";
                final  String UNIT_PARAM = "units";
                final  String DAYS_PARAM = "cnt";
                final  String APPID_PARAM = "appid";
                //UI构造器
                Uri buildUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM ,"94043")
                        .appendQueryParameter(FORMAL_PARAM ,format)
                        .appendQueryParameter(UNIT_PARAM, units)
                        .appendQueryParameter(DAYS_PARAM, Integer.toString(numdays))
                        .appendQueryParameter(APPID_PARAM,appid)
                        .build();

                URL url = new URL(buildUri.toString());
                Log.v("","url----------- "+   url);

                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are available at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
               // URL url = new URL("http://api.openweathermap.org/data/2.5/forecast/daily?q=94043&mode=json&units=metric&cnt=7&appid=398ae2a4c5112f3604756bf1403e58b1");

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
//网络请求不能再主线程
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    forecastJsonStr = null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    forecastJsonStr = null;
                    return null;
                }
                forecastJsonStr = buffer.toString();
               //获取信息json


                Log.v("","Forecast JOSN String "+   forecastJsonStr);
            } catch (Exception e) {
                Log.e("PlaceholderFragment", "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attempting
                // to parse it.
                forecastJsonStr = null;
                return null;
            } finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e("PlaceholderFragment", "Error closing stream", e);
                        return null;
                    }
                }
            }
    try {
        return getWeatherDataFromJson(forecastJsonStr, 7);
    }
    catch(Exception e)
    {
        e.printStackTrace();
        return null;
    }
   }


    }
}
