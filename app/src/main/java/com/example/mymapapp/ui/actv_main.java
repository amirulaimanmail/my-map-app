package com.example.mymapapp.ui;

import android.content.pm.PackageManager;
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
import com.example.mymapapp.model.GeocodingResult;
import com.example.mymapapp.network.Api_map_service;
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

public class actv_main extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST = 1;

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
            createRoute(currentLocationMarker, setLocationMarker);
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
        Api_map_service.searchLocation(this, query, (results -> {
            locations.clear();
            locations.addAll(results);
            // Iterate over the results and place markers on the map
            for (GeocodingResult result : results) {
                Log.d("Locations", result.getDisplay_name() + " " + result.getLat() + " " + result.getLon());
            }
            location_search_adapter.notifyDataSetChanged();
        }));
    }

    //ROUTE FUNCTIONS
    private void createRoute(Marker startMarker, Marker endMarker) {
        Api_map_service.createRoute(this, startMarker, endMarker, (results) -> {
            plotRoute(results);
            binding.actvMainLocationMenuLayout.setVisibility(View.GONE);
        });
    }

    //Plotting route on the map
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

        // Initialize the bounding box values
        double minLat = Double.MAX_VALUE, maxLat = Double.MIN_VALUE;
        double minLon = Double.MAX_VALUE, maxLon = Double.MIN_VALUE;

        // Calculate the extreme values of latitude and longitude
        for (GeoPoint point : markerPoints) {
            minLat = Math.min(minLat, point.getLatitude());
            maxLat = Math.max(maxLat, point.getLatitude());
            minLon = Math.min(minLon, point.getLongitude());
            maxLon = Math.max(maxLon, point.getLongitude());
        }

        // Calculate the latitude and longitude span (distance between min and max)
        double latSpan = maxLat - minLat;
        double lonSpan = maxLon - minLon;

        // Define a relative padding factor
        double paddingFactorLon = 0.5;
        double paddingFactorLat = 0.1;

        // Calculate dynamic padding
        double latPadding = latSpan * paddingFactorLon;
        double lonPadding = lonSpan * paddingFactorLat;

        // Create a padded bounding box
        BoundingBox paddedBoundingBox = new BoundingBox(
                maxLat + latPadding, maxLon + lonPadding,
                minLat - latPadding, minLon - lonPadding
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
