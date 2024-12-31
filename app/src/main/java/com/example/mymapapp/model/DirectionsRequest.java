package com.example.mymapapp.model;

/** @noinspection unused*/
public class DirectionsRequest {

    public DirectionsRequest(double startLongitude, double startLatitude, double endLongitude, double endLatitude) {
        String[] coordinates = new String[]{
                startLongitude + "," + startLatitude,
                endLongitude + "," + endLatitude
        };
    }
}
