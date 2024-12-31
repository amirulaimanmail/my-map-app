package com.example.mymapapp.network;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.mymapapp.model.DirectionsResponse;
import com.example.mymapapp.model.GeocodingResponse;
import com.example.mymapapp.model.GeocodingResult;
import com.example.mymapapp.utils.OpenRouteServiceAPI;

import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Api_map_service {

    private static final String OPEN_ROUTE_SERVICE_BASE_URL = "https://api.openrouteservice.org/";

    public interface searchLocationOnCompleteListener{
        void onCompleteListener(List<GeocodingResult> results);
    }

    public interface createRouteOnCompleteListener{
        void onCompleteListener(List<List<Double>> coordinates, Double duration, Double distance);
    }

    public static void searchLocation(Context context, String query, searchLocationOnCompleteListener listener) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(OPEN_ROUTE_SERVICE_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        OpenRouteServiceAPI geocodingService = retrofit.create(OpenRouteServiceAPI.class);

        Call<GeocodingResponse> call = geocodingService.searchLocation(
                "5b3ce3597851110001cf62489cdbe9fc2aae49ad8621b47ec5a197be",
                query,
                "openstreetmap",
                "venue",
                5,
                "MY"
        );

        call.enqueue(new Callback<GeocodingResponse>() {
            @Override
            public void onResponse(@NonNull Call<GeocodingResponse> call, @NonNull Response<GeocodingResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    GeocodingResponse geocodingResponse = response.body();
                    List<GeocodingResponse.Feature> features = geocodingResponse.getFeatures();

                    if (features != null && !features.isEmpty()) {
                        List<GeocodingResult> results = new ArrayList<>();
                        for (GeocodingResponse.Feature feature : features) {
                            List<Double> coordinates = feature.getGeometry().getCoordinates();
                            String label = feature.getProperties().getLabel();

                            if (coordinates != null && coordinates.size() >= 2) {
                                GeocodingResult result = new GeocodingResult(
                                        String.valueOf(coordinates.get(1)), // Latitude
                                        String.valueOf(coordinates.get(0)), // Longitude
                                        label
                                );
                                results.add(result);
                            }
                        }
                        listener.onCompleteListener(results);
                    } else {
                        Toast.makeText(context, "No results found", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(context, "Error fetching results", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<GeocodingResponse> call, @NonNull Throwable t) {
                Toast.makeText(context, "Failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    //ROUTE FUNCTIONS
    public static void createRoute(Context context, Marker startMarker, Marker endMarker, int travelMode, createRouteOnCompleteListener listener) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(OPEN_ROUTE_SERVICE_BASE_URL)  // Base URL without the specific endpoint
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        OpenRouteServiceAPI api = retrofit.create(OpenRouteServiceAPI.class);

        // Declare start and end coordinates
        double startLongitude = startMarker.getPosition().getLongitude();
        double startLatitude = startMarker.getPosition().getLatitude();

        double endLongitude = endMarker.getPosition().getLongitude();
        double endLatitude = endMarker.getPosition().getLatitude();

        Log.d("Route Coordinates", "Start: " + startLatitude + ", " + startLongitude);
        Log.d("Route Coordinates", "End: " + endLatitude + ", " + endLongitude);

        String walkingMode = "driving-car";

        if(travelMode == 1){
            walkingMode = "foot-walking";
        }

        // Build the URL with query parameters for start and end coordinates
        String url = "v2/directions/" + walkingMode + "?start=" + startLongitude + "," + startLatitude +
                "&end=" + endLongitude + "," + endLatitude;
        Log.d("Route Coordinates", "URL: " + url);

        // Make the request with the correct URL format
        Call<DirectionsResponse> call = api.getRouteWithQueryParams(url);

        call.enqueue(new Callback<DirectionsResponse>() {
            @Override
            public void onResponse(@NonNull Call<DirectionsResponse> call, @NonNull Response<DirectionsResponse> response) {
                if (response.isSuccessful()) {
                    Log.d("Route Coordinates", "Success");

                    DirectionsResponse directionsResponse = response.body();

                    //Log.d("Route Coordinates", String.valueOf(directionsResponse.getRoutes().get(0).getProperties().getSegments().get(0).getDistance()));

                    assert directionsResponse != null;
                    List<DirectionsResponse.Route> routes = directionsResponse.getRoutes();
                    if (routes != null && !routes.isEmpty()) {
                        // Access the route data
                        List<List<Double>> coordinates = routes.get(0).getGeometry().getCoordinates();
                        listener.onCompleteListener(coordinates, directionsResponse.getRoutes().get(0).getProperties().getSegments().get(0).getDuration() ,directionsResponse.getRoutes().get(0).getProperties().getSegments().get(0).getDistance());
                    } else {
                        Toast.makeText(context, "No routes found", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(context, "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                    Log.d("Route Coordinates", response.toString());
                }
            }

            @Override
            public void onFailure(@NonNull Call<DirectionsResponse> call, @NonNull Throwable t) {
                Toast.makeText(context, "Failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
