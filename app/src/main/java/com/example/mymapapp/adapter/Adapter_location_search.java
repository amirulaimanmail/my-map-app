package com.example.mymapapp.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mymapapp.databinding.RecyclerItemSearchLocationBinding;
import com.example.mymapapp.model.GeocodingResult;
import com.example.mymapapp.utils.UtilStringTag;

import java.util.ArrayList;

/** @noinspection FieldCanBeLocal*/
public class Adapter_location_search extends RecyclerView.Adapter<Adapter_location_search.location_search_list_viewholder>{
    private final ArrayList<GeocodingResult> geocodingResults;
    private final OnItemClickListener listener;
    private final Context context;

    public interface OnItemClickListener {
        void onItemClicked(GeocodingResult geocodingResult);
    }

    public Adapter_location_search(Context context, ArrayList<GeocodingResult> geocodingResults, OnItemClickListener listener) {
        this.geocodingResults = geocodingResults;
        this.listener = listener;
        this.context = context;
    }

    @NonNull
    @Override
    public Adapter_location_search.location_search_list_viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        RecyclerItemSearchLocationBinding binding = RecyclerItemSearchLocationBinding.inflate(inflater, parent, false);
        return new location_search_list_viewholder(binding);
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onBindViewHolder(@NonNull Adapter_location_search.location_search_list_viewholder holder, int position) {
        GeocodingResult geocodingResult = geocodingResults.get(position);

        holder.binding.setLocation(geocodingResult);

        if(geocodingResult.getDisplay_name().equals("\uD83D\uDCCDCurrent Location")){
            holder.binding.recyclerItemSearchTv.setText(UtilStringTag.currentLocationText(context));
        }

        holder.itemView.setOnClickListener(v -> {

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
