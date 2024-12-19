package com.example.mymapapp.ui;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.mymapapp.R;
import com.example.mymapapp.adapter.Adapter_location_search;
import com.example.mymapapp.databinding.ActvMainBinding;
import com.example.mymapapp.model.DirectionsResponse;
import com.example.mymapapp.model.GeocodingResult;
import com.example.mymapapp.utils.GeocodingService;
import com.example.mymapapp.utils.OpenRouteServiceAPI;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class actv_main extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST = 1;

    private static final String OPEN_ROUTE_SERVICE_BASE_URL = "https://api.openrouteservice.org/";
    private static final String OPEN_STREET_MAP_BASE_URL = "https://nominatim.openstreetmap.org/";

    private ActvMainBinding binding;

    private MapController mMapController;
    private FusedLocationProviderClient fusedLocationClient;

    private ArrayList<GeocodingResult> locations;

    private String destinationLocationName;

    private Boolean allowSearch;

    private Adapter_location_search location_search_adapter;

    private Marker currentLocationMarker, setLocationMarker;
    private Polyline currentRoute;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActvMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        locations = new ArrayList<>();

        org.osmdroid.config.Configuration.getInstance().setUserAgentValue(getPackageName());

        //Initialize map view
        binding.actvMainMapview.setTileSource(TileSourceFactory.MAPNIK);
        binding.actvMainMapview.setMultiTouchControls(true);
        mMapController = (MapController) binding.actvMainMapview.getController();
        mMapController.setZoom(18);
        binding.actvMainMapview.getZoomController().setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER);

        currentLocationMarker = new Marker(binding.actvMainMapview);
        setLocationMarker = new Marker(binding.actvMainMapview);
        currentRoute = new Polyline();

        binding.actvMainLocationMenuLayout.setVisibility(View.GONE);

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        //setup adapter
        LinearLayoutManager searchLocationLayoutManager = new LinearLayoutManager(this);
        location_search_adapter = new Adapter_location_search(locations, mMapController, (geocodingResult) -> {
            destinationLocationName = geocodingResult.getDisplay_name();
            binding.actvMainLocationMenuTv.setText(destinationLocationName);
            allowSearch = false;
            binding.actvMainEt.setText(destinationLocationName);
            allowSearch = true;

            GeoPoint location = new GeoPoint(Double.parseDouble(geocodingResult.getLat()), Double.parseDouble(geocodingResult.getLon()));
            showLocationOnMap(location, setLocationMarker);
            binding.actvMainLocationMenuLayout.setVisibility(View.VISIBLE);
            hideKeyboard();
        });
        binding.actvMainSearchRv.setLayoutManager(searchLocationLayoutManager);
        binding.actvMainSearchRv.setAdapter(location_search_adapter);

        // Request location permissions
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
        } else {
            getCurrentLocation();
        }

        allowSearch = true;

        binding.actvMainEt.addTextChangedListener(new TextWatcher() {
            private final Handler handler = new Handler();
            private Runnable delayedTask;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed before text changes
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Cancel the previous task if the user types again
                locations.clear();
                location_search_adapter.notifyDataSetChanged();
                if (delayedTask != null) {
                    handler.removeCallbacks(delayedTask);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Schedule the new task to run after 1 second
                if(allowSearch){
                    if(!s.toString().isEmpty()){
                        binding.actvMainEtClearBtn.setVisibility(View.VISIBLE);
                        delayedTask = () -> searchLocation(s.toString());
                        handler.postDelayed(delayedTask, 500); // 1-second delay
                    }
                    else{
                        binding.actvMainEtClearBtn.setVisibility(View.GONE);
                    }
                }
            }
        });

        binding.actvMainEtClearBtn.setVisibility(View.GONE);
        binding.actvMainEtClearBtn.setOnClickListener(v -> {
            binding.actvMainEtClearBtn.setVisibility(View.GONE);
            binding.actvMainEt.setText("");
        });

        binding.actvMainLocationMenuCloseBtn.setOnClickListener(v -> binding.actvMainLocationMenuLayout.setVisibility(View.GONE));

        binding.actvMainLocationMenuDirectionBtn.setOnClickListener(v -> {
            createRoute();
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Location permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return; // Permission check
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                GeoPoint convertLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                showLocationOnMap(convertLocation, currentLocationMarker);
            } else {
                Toast.makeText(this, "Unable to fetch current location", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error fetching location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void showLocationOnMap(GeoPoint location, Marker marker) {
        // Set map center
        mMapController.setCenter(location);

        // Add a marker at the current location
        marker.setPosition(location);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.ic_location_pin);

        if(marker == currentLocationMarker){
            drawable = ContextCompat.getDrawable(this, R.drawable.ic_my_location_pin);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        }

        marker.setIcon(drawable);

        marker.setOnMarkerClickListener((clickedMarker, mapView) -> {
            double zoomLevel = 18;
            long zoomSpeed = 800;

            mapView.getController().animateTo(clickedMarker.getPosition(), zoomLevel, zoomSpeed);
            return true;
        });

        binding.actvMainMapview.getOverlays().add(marker);

        //Toast.makeText(this, "Location set to: " + location.getLatitude() + ", " + location.getLongitude(), Toast.LENGTH_SHORT).show();
    }

    private void searchLocation(String query) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(OPEN_STREET_MAP_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        GeocodingService geocodingService = retrofit.create(GeocodingService.class);

        Call<List<GeocodingResult>> call = geocodingService.searchLocation(query, "json");
        call.enqueue(new Callback<List<GeocodingResult>>() {

            @Override
            public void onResponse(Call<List<GeocodingResult>> call, Response<List<GeocodingResult>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<GeocodingResult> results = response.body();

                    if (!results.isEmpty()) {
                        locations.clear();
                        locations.addAll(results);
                        // Iterate over the results and place markers on the map
                        for (GeocodingResult result : results) {
                            Log.d("Locations", result.getDisplay_name() + " " + result.getLat() + " " + result.getLon());
                        }
                        location_search_adapter.notifyDataSetChanged();

                        //Toast.makeText(actv_main.this, results.size() + " locations found", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(actv_main.this, "No results found", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(actv_main.this, "Error fetching results", Toast.LENGTH_SHORT).show();
                }
            }


            @Override
            public void onFailure(Call<List<GeocodingResult>> call, Throwable t) {
                Toast.makeText(actv_main.this, "Error occurred", Toast.LENGTH_SHORT).show();
            }
        });
    }

    //ROUTE FUNCTIONS
    private void createRoute() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(OPEN_ROUTE_SERVICE_BASE_URL)  // Base URL without the specific endpoint
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        OpenRouteServiceAPI api = retrofit.create(OpenRouteServiceAPI.class);

        // Declare start and end coordinates
        double startLongitude = currentLocationMarker.getPosition().getLongitude();
        double startLatitude = currentLocationMarker.getPosition().getLatitude();

        double endLongitude = setLocationMarker.getPosition().getLongitude();
        double endLatitude = setLocationMarker.getPosition().getLatitude();

        Log.d("Route Coordinates", "Start: " + startLatitude + ", " + startLongitude);
        Log.d("Route Coordinates", "End: " + endLatitude + ", " + endLongitude);

        // Build the URL with query parameters for start and end coordinates
        String url = "v2/directions/driving-car?start=" + startLongitude + "," + startLatitude +
                "&end=" + endLongitude + "," + endLatitude;
        Log.d("Route Coordinates", "URL: " + url);

        // Make the request with the correct URL format
        Call<DirectionsResponse> call = api.getRouteWithQueryParams(url);

        call.enqueue(new Callback<DirectionsResponse>() {
            @Override
            public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                if (response.isSuccessful()) {
                    Log.d("Route Coordinates", "Success");

                    DirectionsResponse directionsResponse = response.body();

                    Log.d("Route Coordinates", directionsResponse.getRoutes().toString());

                    if (directionsResponse != null) {
                        List<DirectionsResponse.Route> routes = directionsResponse.getRoutes();
                        if (routes != null && !routes.isEmpty()) {
                            // Access the route data
                            List<List<Double>> coordinates = routes.get(0).getGeometry().getCoordinates();
                            plotRoute(coordinates);
                        } else {
                            Toast.makeText(actv_main.this, "No routes found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(actv_main.this, "Error: Null response body", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(actv_main.this, "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                    Log.d("Route Coordinates", response.toString());
                }
            }


            @Override
            public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                Toast.makeText(actv_main.this, "Failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    public void plotRoute(List<List<Double>> coordinates) {
        // Remove previous route if it exists
        if (currentRoute != null) {
            binding.actvMainMapview.getOverlays().remove(currentRoute);
        }

        // Create a list of GeoPoints from the coordinates
        List<GeoPoint> geoPoints = new ArrayList<>();
        for (List<Double> point : coordinates) {
            double lat = point.get(1);  // Latitude
            double lon = point.get(0);  // Longitude
            geoPoints.add(new GeoPoint(lat, lon));
        }

        // Create a new polyline and set the points
        currentRoute = new Polyline();
        currentRoute.setPoints(geoPoints);

        // Create the paint object for styling the polyline
        Paint paint = new Paint();
        paint.setColor(getResources().getColor(R.color.routepathcolor));  // Set polyline color to blue
        paint.setStrokeWidth(10);     // Set polyline width to 8 pixels
        paint.setAntiAlias(true);    // Smooth out edges
        paint.setStrokeCap(Paint.Cap.ROUND);

        // Apply the paint style to the polyline
        currentRoute.getOutlinePaint().set(paint);  // Use outline paint for setting stroke color and width

        // Add the styled polyline to the map
        binding.actvMainMapview.getOverlays().add(currentRoute);

        // Refresh the map view
        binding.actvMainMapview.invalidate();

        fitMarkersToMap(geoPoints);
    }

    // Method to fit markers on the map
    public void fitMarkersToMap(List<GeoPoint> markerPoints) {
        if (markerPoints == null || markerPoints.isEmpty()) {
            return;
        }

        double minLat = Double.MAX_VALUE;
        double maxLat = Double.MIN_VALUE;
        double minLon = Double.MAX_VALUE;
        double maxLon = Double.MIN_VALUE;

        // Calculate the bounding box from marker points
        for (GeoPoint point : markerPoints) {
            minLat = Math.min(minLat, point.getLatitude());
            maxLat = Math.max(maxLat, point.getLatitude());
            minLon = Math.min(minLon, point.getLongitude());
            maxLon = Math.max(maxLon, point.getLongitude());
        }

        BoundingBox paddedBoundingBox = new BoundingBox(
                maxLat + 0.01, maxLon + 0.01,
                minLat - 0.01, minLon - 0.01
        );

        // Adjust the map view to fit the bounding box
        binding.actvMainMapview.zoomToBoundingBox(paddedBoundingBox, true); // Animated zoom
    }


    private void hideKeyboard() {
        // Get the InputMethodManager
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        // Get the current focused view
        View view = this.getCurrentFocus();
        if (view != null) {
            // Hide the keyboard
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
