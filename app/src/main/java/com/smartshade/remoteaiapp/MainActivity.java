package com.smartshade.remoteaiapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.speech.tts.TextToSpeech;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.speech.tts.Voice;
import android.view.View;
import android.widget.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.os.Handler;
import okhttp3.*;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.*;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.utils.ColorTemplate;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


import com.google.gson.Gson;



public class MainActivity extends AppCompatActivity {

    EditText deviceIdInput;
    TextView temperatureText, humidityText, aqiText, predictionText, statusText;
    Button fetchButton;

    LineChart aqiRawChart;
    Handler handler = new Handler();
    String deviceId = "";
    final String API_URL = "http://52.66.249.123:8000/check-access";  // Update if needed

    TextToSpeech tts; // ✅ Correctly placed as a member variable

    @SuppressLint({"SetTextI18n", "MissingInflatedId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        deviceIdInput = findViewById(R.id.deviceIdInput);
        temperatureText = findViewById(R.id.temperatureText);
        humidityText = findViewById(R.id.humidityText);
        aqiText = findViewById(R.id.aqiText);
        predictionText = findViewById(R.id.predictionText);
        statusText = findViewById(R.id.statusText);
        fetchButton = findViewById(R.id.fetchButton);

        aqiRawChart = findViewById(R.id.aqiRawChart);
        Button btnTemp = findViewById(R.id.btnShowTempGraph);
        Button btnHum = findViewById(R.id.btnShowHumGraph);
        Button btnAqi = findViewById(R.id.btnShowAqiGraph);

        btnTemp.setOnClickListener(v -> startActivity(new Intent(this, TemperatureGraphActivity.class)));
        btnHum.setOnClickListener(v -> startActivity(new Intent(this, HumGraphActivity.class)));
        btnAqi.setOnClickListener(v -> startActivity(new Intent(this, AQIGraphActivity.class)));

        tts = new TextToSpeech(getApplicationContext(), status -> {
            if (status != TextToSpeech.ERROR) {
                tts.setLanguage(Locale.ENGLISH);

                // (Optional) Set male voice if available
                for (Voice voice : tts.getVoices()) {
                    String name = voice.getName().toLowerCase();
                    if (voice.getLocale().equals(Locale.ENGLISH) && name.contains("male")) {
                        tts.setVoice(voice);
                        break;
                    }
                }


            }
        });

        // Gemini setup
//        var model = new GenerativeModel("gemini-pro", "AIzaSyBN9zcBDDPSTOYA7Or8Hk9vM7MR7xt8Pg8");
        final String GEMINI_API_KEY = "AIzaSyBN9zcBDDPSTOYA7Or8Hk9vM7MR7xt8Pg8";

// Get latest values from text fields (or store after last fetch)
        String latestTemp = temperatureText.getText().toString();
        String latestHum = humidityText.getText().toString();
        String latestAqi = aqiText.getText().toString();


        EditText chatInput = findViewById(R.id.chatInput);
        TextView chatResponse = findViewById(R.id.chatResponse);


        findViewById(R.id.chatSend).setOnClickListener(v -> {
            String msg = chatInput.getText().toString().trim();
            if (msg.isEmpty()) return;

            String prompt = "Temperature = " + latestTemp + ", Humidity = " + latestHum + ", AQI = " + latestAqi + ". User asked: " + msg;

            JsonObject message = new JsonObject();
            message.addProperty("role", "user");

            JsonObject part = new JsonObject();
            part.addProperty("text", prompt);

            message.add("parts", new Gson().toJsonTree(new JsonObject[]{part}));

            JsonObject body = new JsonObject();
            body.add("contents", new Gson().toJsonTree(new JsonObject[]{message}));

            JsonObject safety = new JsonObject();
            safety.addProperty("category", "HARM_CATEGORY_DANGEROUS_CONTENT");
            safety.addProperty("threshold", "BLOCK_NONE");
            body.add("safetySettings", new Gson().toJsonTree(new JsonObject[]{safety}));

            JsonObject config = new JsonObject();
            config.addProperty("temperature", 0.7);
            body.add("generationConfig", config);

            GeminiApiService service = RetrofitClient.getClient().create(GeminiApiService.class);
            String url = "v1beta/models/gemini-pro:generateContent?key=" + GEMINI_API_KEY;

            service.generateContent(url, body.getAsJsonArray()).enqueue(new Callback<JsonObject>() {
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            JsonObject replyContent = response.body()
                                    .getAsJsonArray("candidates")
                                    .get(0).getAsJsonObject()
                                    .getAsJsonObject("content")
                                    .getAsJsonArray("parts")
                                    .get(0).getAsJsonObject();


                            String reply = replyContent.get("text").getAsString();
                            runOnUiThread(() -> chatResponse.setText("Bot: " + reply));
                        } catch (Exception e) {
                            runOnUiThread(() -> chatResponse.setText("❌ Parsing error"));
                        }
                    } else {
                        runOnUiThread(() -> chatResponse.setText("❌ Gemini API error: " + response.code()));
                    }
                }

                public void onFailure(Call<JsonObject> call, Throwable t) {
                    runOnUiThread(() -> chatResponse.setText("⚠️ Gemini failed: " + t.getMessage()));
                }
            });
        });




        fetchButton.setOnClickListener(v -> {
            deviceId = deviceIdInput.getText().toString().trim();

            if (!deviceId.isEmpty()) {
                statusText.setText("🔄 Fetching data...");
                fetchSensorData();
            } else {
                statusText.setText("❗ Please enter a valid device ID.");
            }
        });

    }

    private void fetchSensorData() {
        new Thread(() -> {
            try {
                URL url = new URL(API_URL + "?device_id=" + deviceId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String result = new java.util.Scanner(conn.getInputStream()).useDelimiter("\\A").next();
                    JSONObject json = new JSONObject(result);
                    try {
                        if (json.has("analyzing") && json.getBoolean("analyzing")) {
                            runOnUiThread(() -> {
                                statusText.setText("🕒 Still analyzing data...");
                                tts.speak("The device is still analyzing. Please try again shortly.",
                                        TextToSpeech.QUEUE_FLUSH, null, null);
                            });
                            return; // Exit early
                        }
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }

                    String temp = json.getString("temperature");
                    String hum = json.getString("humidity");
                    String aqi = json.getString("aqi");
                    String pred = json.getString("prediction");

                    runOnUiThread(() -> {

                        temperatureText.setText("🌡 Temp: " + temp + "°C");
                        humidityText.setText("💧 Humidity: " + hum + "%");
                        aqiText.setText("🌫 AQI: " + aqi);
                        predictionText.setText("🧠 Prediction: " + pred);
                        statusText.setText("✅ Data Updated!");

                        String suggestion = generateSuggestion(Double.parseDouble(temp),
                                Double.parseDouble(hum),
                                Double.parseDouble(aqi));

//                        String voiceText = "Environmental conditions: " + temp + " degree Celsius. Humidity is " + hum + " percent. Air quality index is " + aqi + ". Prediction: " + pred + "         "  + suggestion;
//                        tts.speak(voiceText, TextToSpeech.QUEUE_FLUSH, null, null);

                        String firstPart = "Environmental conditions: " + temp + " degree Celsius. Humidity is " + hum + " percent. Air quality index is " + aqi + ". Prediction: " + pred + ".";
                        String secondPart = suggestion;

                        tts.speak(firstPart, TextToSpeech.QUEUE_FLUSH, null, "FIRST_PART");

                        new android.os.Handler().postDelayed(() -> {
                            tts.speak(secondPart, TextToSpeech.QUEUE_ADD, null, "SECOND_PART");
                        }, 2000);


                        TextView suggestionText = findViewById(R.id.suggestionText);
                        suggestionText.setText("Suggestion: " + suggestion);
                        fetchAQIValues();


                        ((TextView) findViewById(R.id.suggestionText)).setText("🌿 Stay hydrated. Consider using ventilation.");
                    });

                } else {
                    runOnUiThread(() -> statusText.setText("❌ Error fetching data."));
                }
            } catch (Exception e) {
                runOnUiThread(() -> statusText.setText("⚠️ " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    private void fetchAQIValues() {
        new Thread(() -> {
            try {
                URL url = new URL("http://43.205.233.48:8000/aqi-data"); // your backend endpoint
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String result = new java.util.Scanner(conn.getInputStream()).useDelimiter("\\A").next();
                    JSONObject json = new JSONObject(result);
                    JSONArray aqiArray = json.getJSONArray("aqi");

                    List<Float> aqiList = new ArrayList<>();
                    for (int i = 0; i < aqiArray.length(); i++) {
                        aqiList.add((float) aqiArray.getDouble(i));
                    }

                    runOnUiThread(() -> renderRawAQIChart(aqiList));
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Failed to load AQI chart", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void renderRawAQIChart(List<Float> aqiValues) {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < aqiValues.size(); i++) {
            entries.add(new Entry(i, aqiValues.get(i)));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Raw AQI");
        dataSet.setColor(Color.BLUE);
        dataSet.setValueTextSize(10f);
        dataSet.setCircleColor(Color.RED);

        LineData lineData = new LineData(dataSet);
        aqiRawChart.setData(lineData);

        Description desc = new Description();
        desc.setText("Latest AQI Values");
        aqiRawChart.setDescription(desc);

        aqiRawChart.invalidate(); // refresh chart
    }

    private String generateSuggestion(double temp, double hum, double aqi) {
        if (temp > 35 && aqi > 100) {
            return "High heat and pollution. Stay indoors and hydrated.";
        } else if (temp < 20 && hum > 60) {
            return "Risk of dampness. Ensure ventilation.";
        } else if (aqi > 150) {
            return "Poor air quality. Use air purifier or mask.";
        } else {
            return "Conditions are good. Enjoy your day!";
        }
    }
}

