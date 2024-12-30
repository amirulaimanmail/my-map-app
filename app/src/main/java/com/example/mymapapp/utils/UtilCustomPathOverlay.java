package com.example.mymapapp.utils;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;

import java.util.List;

public class UtilCustomPathOverlay extends Overlay {
    private final List<GeoPoint> geoPoints;
    private final Paint paint;

    public UtilCustomPathOverlay(List<GeoPoint> geoPoints, Paint paint) {
        this.geoPoints = geoPoints;
        this.paint = paint;
        this.paint.setStyle(Paint.Style.STROKE); // Ensure we are only stroking the line, not filling
    }

    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {
        if (shadow || geoPoints.size() < 2) {
            return;
        }

        Projection projection = mapView.getProjection();
        Path path = new Path();
        Point point = new Point();

        // Move to the first point
        projection.toPixels(geoPoints.get(0), point);
        path.moveTo(point.x, point.y);

        // Draw the rest of the points
        for (int i = 1; i < geoPoints.size(); i++) {
            projection.toPixels(geoPoints.get(i), point);
            path.lineTo(point.x, point.y);
        }

        canvas.drawPath(path, paint);
    }
}


