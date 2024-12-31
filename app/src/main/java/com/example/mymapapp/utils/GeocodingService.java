package com.example.mymapapp.utils;

import com.example.mymapapp.model.GeocodingResult;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface GeocodingService {
    // Nominatim API for geocoding addresses
    @GET("geocode/search")
    Call<List<GeocodingResult>> searchLocation(
            @Query("api_key") String apiKey,
            @Query("text") String query,
            @Query("sources") String sources,
            @Query("layers") String layers,
            @Query("size") int size,
            @Query("boundary.country") String country
    );
}

