package com.example.mymapapp.utils;

import com.example.mymapapp.model.GeocodingResult;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface GeocodingService {

    // Nominatim API for geocoding addresses
    @GET("search")
    Call<List<GeocodingResult>> searchLocation(@Query("q") String query, @Query("format") String format);
}

