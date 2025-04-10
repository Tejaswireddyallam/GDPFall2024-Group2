package com.example.telemedicine.adapter;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.telemedicine.BuildConfig;
import com.example.telemedicine.R;
import com.example.telemedicine.model.MedicalRecord;
import com.example.telemedicine.model.Pharmacy;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MedicalRecordsAdapter extends RecyclerView.Adapter<MedicalRecordsAdapter.ViewHolder> {

    private List<MedicalRecord> medicalRecordsList;
    private Context context;
    private String base_url = BuildConfig.API_BASE_URL;
    private static Boolean isDoctor;

    public MedicalRecordsAdapter(List<MedicalRecord> medicalRecordsList, Context context) {
        this.medicalRecordsList = medicalRecordsList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_medical_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MedicalRecord record = medicalRecordsList.get(position);
        holder.fileNameTextView.setText(record.getFileName()+" Medical Record ");
        holder.timestampTextView.setText(record.getFormattedTimestamp());
        String fileName = getFileNameFromUrl(record.getFileUrl(), record.getFormattedTimestamp(), record.getFileName());
        holder.downloadButton.setOnClickListener(v -> downloadFile(holder.itemView.getContext(), record.getFileUrl(), fileName));

        holder.deleteButton.setOnClickListener(v -> deleteMedicalRecord(record, position));
    }

    @Override
    public int getItemCount() {
        return medicalRecordsList.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateList(List<MedicalRecord> newMedicalRecordList,Boolean isDoctorRole) {
        medicalRecordsList = newMedicalRecordList;
        isDoctor = isDoctorRole;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView fileNameTextView, timestampTextView;
        Button downloadButton, deleteButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            fileNameTextView = itemView.findViewById(R.id.fileNameTextView);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
            downloadButton = itemView.findViewById(R.id.downloadButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            if (isDoctor) {
                deleteButton.setVisibility(View.GONE);
                deleteButton.setEnabled(false);
            }
        }
    }

    private String getFileNameFromUrl(String fileUrl, String timestamp, String filename) {
        try {
            String urlWithoutQuery = fileUrl.split("\\?")[0];
            String decodedUrl = URLDecoder.decode(urlWithoutQuery, StandardCharsets.UTF_8.name());
            String fileName = decodedUrl.substring(decodedUrl.lastIndexOf('/') + 1);

            String fileExtension = "";
            int lastDotIndex = fileName.lastIndexOf('.');
            if (lastDotIndex != -1 && lastDotIndex < fileName.length() - 1) {
                fileExtension = fileName.substring(lastDotIndex + 1);
                fileName = fileName.substring(0, lastDotIndex);
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("HH-mm-MM-dd-yyyy");
            String dateStr = dateFormat.format(new Date(timestamp));
            String finalName = filename + "_Medical_Record_" + dateStr + (fileExtension.isEmpty() ? "" : "." + fileExtension);
            return finalName;
        } catch (Exception e) {
            Log.e("TAG", "Error in getFileNameFromUrl: " + e.getMessage());
            return "unknown_file";
        }
    }

    private void downloadFile(Context context, String fileUrl, String fileName) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(fileUrl));
        request.setTitle(fileName);
        request.setDescription("Downloading...");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (downloadManager != null) {
            downloadManager.enqueue(request);
            Toast.makeText(context, "Downloading file...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Download Manager not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteMedicalRecord(MedicalRecord record, int position) {
        String url = base_url + "/patient/deleteMedicalRecord";

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("recordId", record.getRecordId());
            requestBody.put("patientId", record.getPatientId());
            Log.d("MedicalRecordAdapter", "Deleting medical record with ID: " + record.getRecordId() + " " + record.getPatientId());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestQueue queue = Volley.newRequestQueue(context);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, requestBody,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            medicalRecordsList.remove(position);
                            notifyItemRemoved(position);
                            Toast.makeText(context, "Medical record deleted!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, "Failed to delete record", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }, error -> Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show());

        queue.add(request);
    }
}

