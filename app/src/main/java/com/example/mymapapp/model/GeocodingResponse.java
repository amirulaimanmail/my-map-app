package com.example.mymapapp.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** @noinspection unused*/
public class GeocodingResponse {

    @SerializedName("features")
    private List<Feature> features;

    public List<Feature> getFeatures() {
        return features;
    }

    public static class Feature {
        @SerializedName("geometry")
        private Geometry geometry;
        @SerializedName("properties")
        private Properties properties;

        public Geometry getGeometry() {
            return geometry;
        }

        public Properties getProperties() {
            return properties;
        }
    }

    public static class Geometry {
        @SerializedName("coordinates")
        private List<Double> coordinates;

        public List<Double> getCoordinates() {
            return coordinates;
        }
    }

    public static class Properties {
        @SerializedName("label")
        private String label;

        public String getLabel() {
            return label;
        }
    }
}
