package com.example.mymapapp.utils;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ReplacementSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class UtilStringTag {

    // Method to apply color to the entire string or parts of it
    public static SpannableString applyTagsToString(String message, int color) {
        // Create a SpannableString from the message
        SpannableString spannableString = new SpannableString(message);

        // Apply the color span to the whole string
        spannableString.setSpan(
                new UneditableColorSpan(color),
                0, // Start from the beginning of the string
                message.length(), // End at the end of the string
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        return spannableString;
    }

    // Custom ReplacementSpan to apply color and uneditable properties
    public static class UneditableColorSpan extends ReplacementSpan {
        private final int textColor;

        public UneditableColorSpan(int color) {
            textColor = color;
        }

        @Override
        public int getSize(@NonNull Paint paint, CharSequence text, int start, int end, @Nullable Paint.FontMetricsInt fm) {
            return Math.round(paint.measureText(text, start, end));
        }

        @Override
        public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, @NonNull Paint paint) {
            // Save the original paint color
            int originalColor = paint.getColor();
            // Set the paint color to the desired text color
            paint.setColor(textColor);
            // Draw the text
            canvas.drawText(text, start, end, x, y, paint);
            // Restore the original paint color
            paint.setColor(originalColor);
        }
    }
}
