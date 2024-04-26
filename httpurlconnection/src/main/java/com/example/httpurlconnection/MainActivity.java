package com.example.httpurlconnection;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.httpurlconnection.databinding.ActivityMainBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    public void onClick(View view) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkinfo = null;
        if (connectivityManager != null) {
            networkinfo = connectivityManager.getActiveNetworkInfo();
        }
        if (networkinfo != null && networkinfo.isConnected()) {
            new IpInfo().execute(); // запуск нового потока
        } else {
            Toast.makeText(this, "Нет интернета", Toast.LENGTH_SHORT).show();
        }
    }
    private class IpInfo extends  DownloadPageTask{
        public IpInfo() {
            super("https://ipinfo.io/json", "GET");
        }
    }

    private class DownloadPageTask extends AsyncTask<String, Void, String> {
        private String path;
        private String request_method;
        public DownloadPageTask(String path, String request_method) {
            this.path = path;
            this.request_method = request_method;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Toast.makeText(MainActivity.this, "Идет загрузка...", Toast.LENGTH_SHORT).show();
        }
        @Override
        protected String doInBackground(String... urls) {
            try {
                return downloadIpInfo();
            } catch (IOException e) {
                e.printStackTrace();
                return "error";
            }
        }
        @Override
        protected void onPostExecute(String result) {
//            binding.weatherView.setText(result);
            Log.d(MainActivity.class.getSimpleName(), result);
            try {
                JSONObject responseJson = new JSONObject(result);
                binding.countryView.setText("Страна: " + responseJson.getString("country"));
                binding.regionView.setText("Регион: " + responseJson.getString("region"));
                binding.cityView.setText("Город: " + responseJson.getString("city"));
                binding.timezoneView.setText("Временная зона: " + responseJson.getString("timezone"));

                String[] coordinates = responseJson.getString("loc").split(",");

                new RequestWeatherInfo(Float.parseFloat(coordinates[0]), Float.parseFloat(coordinates[1])).execute();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            super.onPostExecute(result);
        }
        private class RequestWeatherInfo extends DownloadPageTask {
            public RequestWeatherInfo(float latitude, float longitude) {
                super(String.format("https://api.open-meteo.com/v1/forecast?latitude=%s&longitude=%s&current_weather=true",
                        latitude, longitude), "GET");
            }
            protected void onPostExecute(String result) {
                super.onPostExecute(result);
                try {
                    JSONObject responseJson = new JSONObject(result);
                    JSONObject weatherJson = new JSONObject(responseJson.getString("current_weather"));
                    binding.weatherView.setText("Текущая погода: \n" +
                            "температура - " + weatherJson.getString("temperature") + "\n" +
                            "скорость ветра - " + weatherJson.getString("windspeed"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        private String downloadIpInfo() throws IOException {
            InputStream inputStream = null;
            String data = "";
            try {
                URL url = new URL(path);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setReadTimeout(100000);
                connection.setConnectTimeout(100000);
                connection.setRequestMethod("GET");
                connection.setInstanceFollowRedirects(true);
                connection.setUseCaches(false);
                connection.setDoInput(true);
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) { // 200 OK
                    inputStream = connection.getInputStream();
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    int read = 0;
                    while ((read = inputStream.read()) != -1) {
                        bos.write(read);
                    }
                    bos.close();
                    data = bos.toString();
                } else {
                    data = connection.getResponseMessage() + ". Error Code: " + responseCode;
                }
                connection.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
            }
            return data;
        }
    }
}
