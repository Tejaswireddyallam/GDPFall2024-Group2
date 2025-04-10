package com.example.telemedicine.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.telemedicine.BuildConfig;
import com.example.telemedicine.R;
import com.example.telemedicine.model.Pharmacy;

import java.util.List;

public class PharmacyAdapter extends RecyclerView.Adapter<PharmacyAdapter.PharmacyViewHolder> {

    private List<Pharmacy> pharmacyList;
    private Context context;
    String base_url = BuildConfig.API_BASE_URL;

    // Constructor
    public PharmacyAdapter(List<Pharmacy> pharmacyList, Context context) {
        this.pharmacyList = pharmacyList;
        this.context = context;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateList(List<Pharmacy> newPharmacyList) {
        pharmacyList = newPharmacyList;
        notifyDataSetChanged();
    }

    // ViewHolder Class
    public static class PharmacyViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvLocation, tvContact;

        public PharmacyViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_pharmacy_name);
            tvLocation = itemView.findViewById(R.id.tv_pharmacy_location);
            tvContact = itemView.findViewById(R.id.tv_pharmacy_contact);
        }
    }

    @NonNull
    @Override
    public PharmacyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.pharmacy_item, parent, false);
        return new PharmacyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PharmacyViewHolder holder, int position) {
        // Get current pharmacy
        Pharmacy pharmacy = pharmacyList.get(position);

        holder.tvName.setText(pharmacy.getName() != null ? pharmacy.getName() : "N/A");
        holder.tvLocation.setText(pharmacy.getLocation() != null ? pharmacy.getLocation() : "N/A");
        holder.tvContact.setText(pharmacy.getContactInfo() != null ? pharmacy.getContactInfo() : "N/A");
    }

    @Override
    public int getItemCount() {
        return pharmacyList != null ? pharmacyList.size() : 0;
    }
}
