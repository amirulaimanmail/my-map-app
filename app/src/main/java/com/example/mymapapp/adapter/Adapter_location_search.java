package com.example.mymapapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mymapapp.databinding.RecyclerItemSearchLocationBinding;
import com.example.mymapapp.model.GeocodingResult;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;

import java.util.ArrayList;

public class Adapter_location_search extends RecyclerView.Adapter<Adapter_location_search.location_search_list_viewholder>{
    private final ArrayList<GeocodingResult> geocodingResults;
    private MapController mapController;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClicked(GeocodingResult geocodingResult);
    }

    public Adapter_location_search(ArrayList<GeocodingResult> geocodingResults, MapController mapController, OnItemClickListener listener) {
        this.geocodingResults = geocodingResults;
        this.mapController = mapController;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Adapter_location_search.location_search_list_viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        RecyclerItemSearchLocationBinding binding = RecyclerItemSearchLocationBinding.inflate(inflater, parent, false);
        return new location_search_list_viewholder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull Adapter_location_search.location_search_list_viewholder holder, int position) {
        GeocodingResult geocodingResult = geocodingResults.get(position);

        holder.binding.setLocation(geocodingResult);

        GeoPoint location = new GeoPoint(Double.parseDouble(geocodingResult.getLat()), Double.parseDouble(geocodingResult.getLon()));

        holder.itemView.setOnClickListener(v -> {
            double zoomLevel = 18;
            long zoomSpeed = 800;
            mapController.animateTo(location, zoomLevel, zoomSpeed);

            if (listener != null) {
                listener.onItemClicked(geocodingResult);
            }

            geocodingResults.clear();
            notifyDataSetChanged();
        });
    }

    @Override
    public int getItemCount() {
        return geocodingResults.size();
    }

    public static class location_search_list_viewholder extends RecyclerView.ViewHolder{
        private final RecyclerItemSearchLocationBinding binding;

        public location_search_list_viewholder(@NonNull RecyclerItemSearchLocationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
