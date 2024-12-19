package com.example.mymapapp.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class DirectionsResponse {

    @SerializedName("features")
    private List<Route> routes;

    public static class Route {
        @SerializedName("geometry")
        private Geometry geometry;

        public Geometry getGeometry() {
            return geometry;
        }
    }

    public static class Geometry {
        @SerializedName("coordinates")
        private List<List<Double>> coordinates;

        public List<List<Double>> getCoordinates() {
            return coordinates;
        }
    }

    public List<Route> getRoutes() {
        return routes;
    }

    public void setRoutes(List<Route> routes) {
        this.routes = routes;
    }
}
