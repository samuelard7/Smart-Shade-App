package com.smartshade.remoteaiapp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Url;

public interface GeminiApiService {
    @Headers("Content-Type: application/json")
    @POST
    Call<JsonObject> generateContent(
            @Url String url,
            @Body JsonArray requestBody
    );
}
