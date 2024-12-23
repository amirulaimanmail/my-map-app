package com.example.mymapapp.ui;

import android.content.pm.PackageManager;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.mymapapp.R;
import com.example.mymapapp.adapter.Adapter_location_search;
import com.example.mymapapp.databinding.ActvMainBinding;
import com.example.mymapapp.model.GeocodingResult;
import com.example.mymapapp.network.Api_map_service;
import com.example.mymapapp.utils.UtilStringTag;
import com.example.mymapapp.utils.UtilUnitConversion;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.tabs.TabLayout;

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

    private String fromLocationName, destinationLocationName;

    private Boolean allowSearch, navMode;

    private Boolean fromEtFocused, toEtFocused;

    private Adapter_location_search location_search_adapter;

    private Marker currentLocationMarker, fromLocationMarker, toLocationMarker;
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
        fromLocationMarker = new Marker(binding.actvMainMapview);
        toLocationMarker = new Marker(binding.actvMainMapview);
        currentRoute = new Polyline();

        binding.actvMainLocationMenuLayout.setVisibility(View.GONE);
        binding.actvMainNavigationMenuLayout.setVisibility(View.GONE);

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        //setup adapter
        LinearLayoutManager searchLocationLayoutManager = new LinearLayoutManager(this);
        location_search_adapter = new Adapter_location_search(locations, this::handleAdapterCallback);
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

        //Handle on back functions
        toEtFocused = false;
        fromEtFocused = false;

        binding.actvMainFromLayout.setVisibility(View.GONE);
        navMode = false;
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if(toEtFocused || fromEtFocused){
                    binding.actvMainToEt.clearFocus();
                    binding.actvMainFromEt.clearFocus();

                    binding.actvMainToEtClearBtn.setVisibility(View.GONE);
                    binding.actvMainFromEtClearBtn.setVisibility(View.GONE);
                }
                else if(navMode){
                    navMode = false;
                    binding.actvMainFromLayout.setVisibility(View.GONE);
                    binding.actvMainNavigationMenuLayout.setVisibility(View.GONE);
                }
                else{
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

        allowSearch = true;

        //HANDLE TO EDITTEXT
        binding.actvMainToEt.addTextChangedListener(new TextWatcher() {
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
                        binding.actvMainToEtClearBtn.setVisibility(View.VISIBLE);
                        delayedTask = () -> searchLocation(s.toString());

                        handler.postDelayed(delayedTask, 500); // 1-second delay
                    }
                    else{
                        binding.actvMainToEtClearBtn.setVisibility(View.GONE);
                    }
                }
            }
        });

        binding.actvMainToEtClearBtn.setVisibility(View.GONE);
        binding.actvMainToEtClearBtn.setOnClickListener(v -> {
            binding.actvMainToEtClearBtn.setVisibility(View.GONE);
            binding.actvMainToEt.setText("");
        });

        binding.actvMainToEt.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                toEtFocused = true;
                if (!binding.actvMainToEt.getText().toString().isEmpty()) {
                    binding.actvMainToEtClearBtn.setVisibility(View.VISIBLE);
                }
                setRecyclerConstraint(binding.actvMainToLayout);
            }
            else{
                toEtFocused = false;

                allowSearch = false;
                binding.actvMainToEt.setText(destinationLocationName);
                allowSearch = true;

                locations.clear();
                location_search_adapter.notifyDataSetChanged();
                binding.actvMainToEtClearBtn.setVisibility(View.GONE);
            }
        });


        //HANDLE FROM EDITTEXT
        binding.actvMainFromEt.addTextChangedListener(new TextWatcher() {
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
                        binding.actvMainFromEtClearBtn.setVisibility(View.VISIBLE);
                        delayedTask = () -> searchLocation(s.toString());

                        handler.postDelayed(delayedTask, 500); // 1-second delay
                    }
                    else{
                        binding.actvMainFromEtClearBtn.setVisibility(View.GONE);
                    }
                }
            }
        });

        binding.actvMainFromEtClearBtn.setVisibility(View.GONE);
        binding.actvMainFromEtClearBtn.setOnClickListener(v -> {
            binding.actvMainFromEtClearBtn.setVisibility(View.GONE);
            binding.actvMainFromEt.setText("");
        });

        binding.actvMainFromEt.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                fromEtFocused = true;
                if (!binding.actvMainFromEt.getText().toString().isEmpty()) {
                    binding.actvMainFromEtClearBtn.setVisibility(View.VISIBLE);
                }
                setRecyclerConstraint(binding.actvMainFromLayout);
            }
            else{
                fromEtFocused = false;

                allowSearch = false;
                binding.actvMainFromEt.setText(fromLocationName);
                allowSearch = true;

                locations.clear();
                location_search_adapter.notifyDataSetChanged();
                binding.actvMainFromEtClearBtn.setVisibility(View.GONE);
            }
        });

        //HANDLE TAB LAYOUT
        binding.actvMainTabLayout.addTab(binding.actvMainTabLayout.newTab().setText("Drive"));
        binding.actvMainTabLayout.addTab(binding.actvMainTabLayout.newTab().setText("Walk"));
        // Set a listener to handle tab selection
        binding.actvMainTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // Handle tab selection
                int selectedTabPosition = tab.getPosition();
                if(selectedTabPosition == 0){
                    createRoute(fromLocationMarker, toLocationMarker, 0);
                } else if (selectedTabPosition == 1) {
                    createRoute(fromLocationMarker, toLocationMarker, 1);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Handle tab unselection
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Handle tab reselection
            }
        });

        binding.actvMainLocationMenuCloseBtn.setOnClickListener(v -> binding.actvMainLocationMenuLayout.setVisibility(View.GONE));
        binding.actvMainLocationMenuDirectionBtn.setOnClickListener(v -> createRoute(fromLocationMarker, toLocationMarker, 0));

        binding.actvMainNavigationMenuCloseBtn.setOnClickListener(v -> binding.actvMainNavigationMenuLayout.setVisibility(View.GONE));
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
                setLocationOnMap(convertLocation, currentLocationMarker, true, false);
                fromLocationMarker.setPosition(convertLocation);
            } else {
                Toast.makeText(this, "Unable to fetch current location", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Error fetching location: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void setLocationOnMap(GeoPoint location, Marker marker, Boolean showLocation, Boolean isDestination) {
        // Show location functions
        if(showLocation){
            double zoomLevel = 18;
            long zoomSpeed = 800;
            mMapController.animateTo(location, zoomLevel, zoomSpeed);
        }

        // Add a marker at the current location
        marker.setPosition(location);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.ic_location_pin);

        if(marker == currentLocationMarker){
            drawable = ContextCompat.getDrawable(this, R.drawable.ic_my_location_pin);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        }
        else if(!isDestination){
            drawable = ContextCompat.getDrawable(this, R.drawable.ic_location_marker);
        }

        marker.setIcon(drawable);

        marker.setOnMarkerClickListener((clickedMarker, mapView) -> {
            double zoomLevel = 18;
            long zoomSpeed = 800;

            mapView.getController().animateTo(clickedMarker.getPosition(), zoomLevel, zoomSpeed);
            return true;
        });

        binding.actvMainMapview.getOverlays().add(marker);
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
    private void createRoute(Marker startMarker, Marker endMarker, int travelMode) {
        Api_map_service.createRoute(this, startMarker, endMarker, travelMode, (results, duration, distance) -> {
            plotRoute(results, travelMode);
            binding.actvMainLocationMenuLayout.setVisibility(View.GONE);

            if(travelMode == 0){
                binding.actvMainNavigationMenuTransportModeTv.setText(R.string.drive);
            }
            else if(travelMode == 1){
                binding.actvMainNavigationMenuTransportModeTv.setText(R.string.walking);

            }

            binding.actvMainNavigationMenuLayout.setVisibility(View.VISIBLE);
            binding.actvMainNavigationMenuDistanceTv.setText(UtilUnitConversion.formatMetersToKilometers(distance));
            binding.actvMainNavigationMenuDurationTv.setText(UtilUnitConversion.formatSecondsToTime(duration));

            binding.actvMainFromLayout.setVisibility(View.VISIBLE);
            navMode = true;
        });
    }

    //Plotting route on the map
    public void plotRoute(List<List<Double>> coordinates, int travelMode) {
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
        currentRoute.setGeodesic(true);
        currentRoute.setPoints(geoPoints);

        // Create the paint object for styling the polyline
        Paint paint = new Paint();
        paint.setColor(getResources().getColor(R.color.routepathcolor));  // Set polyline color to blue
        paint.setStrokeWidth(10);     // Set polyline width to 8 pixels
        paint.setAntiAlias(true);    // Smooth out edges
        paint.setStrokeCap(Paint.Cap.ROUND);

        if(travelMode == 1){
            float[] dashPattern = {5f, 50f};  // Dash length and space length
            DashPathEffect dashPathEffect = new DashPathEffect(dashPattern, 0);
            paint.setPathEffect(dashPathEffect);
        }

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

        // Apply a minimum bounding box size to avoid zooming in too much
        double minBoxSize = 0.01; // Minimum allowable span for latitude and longitude
        if (latSpan < minBoxSize) {
            latPadding = minBoxSize / 2; // Increase the padding to reach the minimum size
        }
        if (lonSpan < minBoxSize) {
            lonPadding = minBoxSize / 2; // Increase the padding to reach the minimum size
        }

        // Update the bounding box with the minimum size constraints
        BoundingBox paddedBoundingBox = new BoundingBox(
                maxLat + latPadding, maxLon + lonPadding,
                minLat - latPadding, minLon - lonPadding
        );

        // Adjust the map view to fit the bounding box
        binding.actvMainMapview.zoomToBoundingBox(paddedBoundingBox, true);

    }

    private void handleAdapterCallback(GeocodingResult geocodingResult){
        hideKeyboard();
        GeoPoint location = new GeoPoint(Double.parseDouble(geocodingResult.getLat()), Double.parseDouble(geocodingResult.getLon()));

        if(!navMode){
            destinationLocationName = geocodingResult.getDisplay_name();
            binding.actvMainLocationMenuTv.setText(destinationLocationName);
            allowSearch = false;
            binding.actvMainToEt.setText(destinationLocationName);

            int color = ContextCompat.getColor(this, R.color.currentlocationcolor);
            SpannableString currentLocationText = UtilStringTag.applyTagsToString("\uD83D\uDCCDCurrent Location", color);
            binding.actvMainFromEt.setText(currentLocationText);

            allowSearch = true;

            setLocationOnMap(location, toLocationMarker, true, true);
            binding.actvMainLocationMenuLayout.setVisibility(View.VISIBLE);
        }
        else{
            if(fromEtFocused){
                fromLocationName = geocodingResult.getDisplay_name();
                allowSearch = false;
                binding.actvMainFromEt.setText(fromLocationName);
                allowSearch = true;
                setLocationOnMap(location, fromLocationMarker, false, false);
                createRoute(fromLocationMarker, toLocationMarker, 0);
            }
            else if(toEtFocused){
                destinationLocationName = geocodingResult.getDisplay_name();
                allowSearch = false;
                binding.actvMainToEt.setText(destinationLocationName);
                allowSearch = true;
                setLocationOnMap(location, toLocationMarker, false, true);
                createRoute(fromLocationMarker, toLocationMarker, 0);
            }
        }

        binding.actvMainToEt.clearFocus();
        binding.actvMainFromEt.clearFocus();
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

    private void setRecyclerConstraint(ConstraintLayout layout) {
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(binding.actvMainRootLayout);
        constraintSet.connect(binding.actvMainSearchRv.getId(), ConstraintSet.TOP, layout.getId(), ConstraintSet.BOTTOM, 0);
        constraintSet.applyTo(binding.actvMainRootLayout);
    }

}
