package com.example.mymapapp.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** @noinspection unused*/
public class DirectionsResponse {

    @SerializedName("features")
    private List<Route> routes;

    public static class Route {
        @SerializedName("geometry")
        private Geometry geometry;
        @SerializedName("properties")
        private Properties properties;

        public Geometry getGeometry() {
            return geometry;
        }

        public Properties getProperties(){
            return properties;
        }
    }

    public static class Geometry {
        @SerializedName("coordinates")
        private List<List<Double>> coordinates;

        public List<List<Double>> getCoordinates() {
            return coordinates;
        }
    }

    public static class Properties {
        @SerializedName("segments")
        private List<Segment> segments;

        public List<Segment> getSegments() {
            return segments;
        }
    }

    public static class Segment {
        @SerializedName("distance")
        private double distance;  // Use double if it's a number, otherwise keep it as String

        @SerializedName("duration")
        private double duration;  // Use double if it's a number, otherwise keep it as String

        public double getDistance(){
            return distance;
        }

        public double getDuration(){
            return duration;
        }
    }

    public List<Route> getRoutes() {
        return routes;
    }

    public void setRoutes(List<Route> routes) {
        this.routes = routes;
    }
}
