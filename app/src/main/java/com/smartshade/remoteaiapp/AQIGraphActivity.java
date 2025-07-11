package com.smartshade.remoteaiapp;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.components.Description;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class AQIGraphActivity extends AppCompatActivity {

    LineChart chart;
    final String HISTORICAL_URL = "http://43.205.233.48:8000/historical?device_id=HBSGDHA123";

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.aqi_line_chart);  // ✅ Correct the layout name here

        chart = findViewById(R.id.AQILineChart);
        fetchTemperatureData();
    }

    private void fetchTemperatureData() {
        new Thread(() -> {
            try {
                URL url = new URL(HISTORICAL_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                int code = conn.getResponseCode();
                if (code == HttpURLConnection.HTTP_OK) {
                    String result = new java.util.Scanner(conn.getInputStream()).useDelimiter("\\A").next();
                    JSONObject json = new JSONObject(result);
                    JSONArray aqis = json.getJSONArray("aqi");

                    List<Entry> entries = new ArrayList<>();
                    for (int i = 0; i < aqis.length(); i++) {
                        entries.add(new Entry(i, (float) aqis.getDouble(i)));
                    }

                    runOnUiThread(() -> drawChart(entries, "AQI", "#4CAF50"));
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "❌ Failed to load air quality index data", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void drawChart(List<Entry> entries, String label, String color) {
        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(android.graphics.Color.parseColor(color));
        dataSet.setValueTextSize(10f);
        dataSet.setCircleColor(android.graphics.Color.DKGRAY);
        dataSet.setLineWidth(2f);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);

        Description desc = new Description();
        desc.setText("📈 Last 30 AQI Readings");
        chart.setDescription(desc);

        chart.animateX(1000);
        chart.invalidate();
    }
}
