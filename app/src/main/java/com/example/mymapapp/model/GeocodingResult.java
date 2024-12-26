package com.example.mymapapp.model;

/** @noinspection unused*/
public class GeocodingResult {
    private String lat;
    private String lon;
    private String display_name;

    public GeocodingResult(String lat, String lon, String display_name) {
        this.lat = lat;
        this.lon = lon;
        this.display_name = display_name;
    }

    public String getLat() {
        return lat;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public String getLon() {
        return lon;
    }

    public void setLon(String lon) {
        this.lon = lon;
    }

    public String getDisplay_name() {
        return display_name;
    }

    public void setDisplay_name(String display_name) {
        this.display_name = display_name;
    }
}

