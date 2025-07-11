package com.smartshade.remoteaiapp;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.components.Description;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class TemperatureGraphActivity extends AppCompatActivity {
    LineChart chart;
    final String HISTORICAL_URL = "http://43.205.233.48:8000/historical?device_id=HBSGDHA123";

    @SuppressLint({"WrongViewCast", "MissingInflatedId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.temp_line_chart);

        chart = findViewById(R.id.tempLineChart);
        fetchTemperatureData();
    }

    private void fetchTemperatureData() {
        new Thread(() -> {
            try {
                URL url = new URL(HISTORICAL_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                int code = conn.getResponseCode();
                if (code == HttpURLConnection.HTTP_OK) {
                    String result = new java.util.Scanner(conn.getInputStream()).useDelimiter("\\A").next();
                    JSONObject json = new JSONObject(result);
                    JSONArray temps = json.getJSONArray("temperature");

                    List<Entry> entries = new ArrayList<>();
                    for (int i = 0; i < temps.length(); i++) {
                        entries.add(new Entry(i, (float) temps.getDouble(i)));
                    }

                    runOnUiThread(() -> drawChart(entries, "Temperature (°C)", "#FF5722"));
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Failed to load temperature data", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void drawChart(List<Entry> entries, String label, String color) {
        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(android.graphics.Color.parseColor(color));
        dataSet.setValueTextSize(10f);
        dataSet.setCircleColor(android.graphics.Color.DKGRAY);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);

        Description desc = new Description();
        desc.setText("Last 30 readings");
        chart.setDescription(desc);

        chart.invalidate();
    }
}
