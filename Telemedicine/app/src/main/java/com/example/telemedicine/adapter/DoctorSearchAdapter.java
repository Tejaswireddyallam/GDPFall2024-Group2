package com.example.telemedicine.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.telemedicine.BuildConfig;
import com.example.telemedicine.R;
import com.example.telemedicine.StartAppointmentActivity;
import com.example.telemedicine.model.Doctor;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DoctorSearchAdapter extends RecyclerView.Adapter<DoctorSearchAdapter.DoctorViewHolder> {

    private List<Doctor> doctorList; // List of doctors
    private Context context;
    private SharedPreferences sharedPreferences;
    String base_url = BuildConfig.API_BASE_URL;

    // Constructor for Adapter
    public DoctorSearchAdapter(List<Doctor> doctorList, Context context) {
        this.doctorList = doctorList;
        this.context = context;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateList(List<Doctor> newDoctorList) {
        doctorList = newDoctorList;
        notifyDataSetChanged();
    }

    // ViewHolder Class
    public static class DoctorViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvSpecialization, tvQualification, tvAvailability;
        ImageView imgProfilePicture;
        Button btnGetAppointment;

        public DoctorViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_doctor_name);
            tvSpecialization = itemView.findViewById(R.id.tv_specialization);
            tvQualification = itemView.findViewById(R.id.tv_qualification);
            tvAvailability = itemView.findViewById(R.id.tv_availability);
            imgProfilePicture = itemView.findViewById(R.id.img_profile_picture);
            btnGetAppointment = itemView.findViewById(R.id.btn_get_appointment);
        }
    }

    @NonNull
    @Override
    public DoctorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate item layout
        View view = LayoutInflater.from(context).inflate(R.layout.doctor_search_items, parent, false);
        return new DoctorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DoctorViewHolder holder, int position) {
        // Get current doctor
        Doctor doctor = doctorList.get(position);

        // Bind data to views with null safety checks
        holder.tvName.setText(doctor.getName() != null ? doctor.getName() : "N/A");
        holder.tvSpecialization.setText(doctor.getSpecialization() != null ? doctor.getSpecialization() : "N/A");
        holder.tvQualification.setText(doctor.getQualification() != null ? doctor.getQualification() : "N/A");
        holder.tvAvailability.setText(doctor.getAvailability() != null ? doctor.getAvailability() : "N/A");


        holder.btnGetAppointment.setOnClickListener(v -> {
            Log.d("DoctorSearchAdapter", "Get Appointment clicked for Dr. " + doctor.getName());

            Intent intent = new Intent(context, StartAppointmentActivity.class);
            intent.putExtra("doctor_id", doctor.getId());
            intent.putExtra("availability", doctor.getAvailability());

            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return doctorList != null ? doctorList.size() : 0;
    }


}
