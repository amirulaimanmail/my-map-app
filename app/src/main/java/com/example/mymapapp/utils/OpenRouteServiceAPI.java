package com.example.mymapapp.utils;

import com.example.mymapapp.model.DirectionsResponse;
import com.example.mymapapp.model.GeocodingResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;
import retrofit2.http.Url;

public interface OpenRouteServiceAPI {

    @Headers({
            "Authorization: 5b3ce3597851110001cf62489cdbe9fc2aae49ad8621b47ec5a197be",
            "Content-Type: application/json"
    })

    @GET
    Call<DirectionsResponse> getRouteWithQueryParams(@Url String url);

    @GET("geocode/search")
    Call<GeocodingResponse> searchLocation(
            @Query("api_key") String apiKey,
            @Query("text") String query,
            @Query("sources") String sources,
            @Query("layers") String layers,
            @Query("size") int size,
            @Query("boundary.country") String country
    );
}

