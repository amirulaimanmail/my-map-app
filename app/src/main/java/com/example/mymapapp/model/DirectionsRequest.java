package com.example.mymapapp.model;

public class DirectionsRequest {
    private String[] coordinates;

    public DirectionsRequest(double startLongitude, double startLatitude, double endLongitude, double endLatitude) {
        coordinates = new String[]{
                startLongitude + "," + startLatitude,
                endLongitude + "," + endLatitude
        };
    }

    // Getters and setters
    public String[] getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(String[] coordinates) {
        this.coordinates = coordinates;
    }
}
