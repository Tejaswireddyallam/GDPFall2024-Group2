package com.example.telemedicine.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.telemedicine.BuildConfig;
import com.example.telemedicine.DoctorChatActivity;
import com.example.telemedicine.MedicalRecordsActivity;
import com.example.telemedicine.R;
import com.example.telemedicine.StartAppointmentActivity;
import com.example.telemedicine.VideoCallActivity;
import com.example.telemedicine.model.Appointments;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DoctorAppointmentsAdapter extends RecyclerView.Adapter<DoctorAppointmentsAdapter.DoctorAppointmentViewHolder> {

    private List<Appointments> appointmentsList;
    private Context context;
    private RequestQueue requestQueue;
    String base_url = BuildConfig.API_BASE_URL;
    private final SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy h : mm a", Locale.getDefault());
    private static String patientId;

    public DoctorAppointmentsAdapter(List<Appointments> appointmentsList, Context context) {
        this.appointmentsList = appointmentsList;
        this.context = context;
        this.requestQueue = Volley.newRequestQueue(context.getApplicationContext());
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateList(List<Appointments> newAppointmentsList) {
        appointmentsList = newAppointmentsList;
        notifyDataSetChanged();
    }

    public static class DoctorAppointmentViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvAppointmentTime, tvAppointmentDate, tvStatus;
        Button btnAccept, btnCancel, btnReschedule, btnStart, doctorChatButton, btnGetMedicalRecord;

        public DoctorAppointmentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvPatientName);
            tvAppointmentTime = itemView.findViewById(R.id.tvAppointmentTime);
            tvAppointmentDate = itemView.findViewById(R.id.tvAppointmentDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnStart = itemView.findViewById(R.id.btnStart);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnCancel = itemView.findViewById(R.id.btnCancel);
            btnReschedule = itemView.findViewById(R.id.btnReschedule);
            doctorChatButton = itemView.findViewById(R.id.btnChat);
            btnGetMedicalRecord = itemView.findViewById(R.id.btnGetMedicalRecord);
        }
    }

    @NonNull
    @Override
    public DoctorAppointmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.doctor_appointments_layout, parent, false);
        return new DoctorAppointmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DoctorAppointmentViewHolder holder, int position) {
        Appointments appointment = appointmentsList.get(position);
        holder.tvName.setText(appointment.getName());
        holder.tvAppointmentTime.setText(appointment.getTime());
        holder.tvAppointmentDate.setText(appointment.getDate());
        holder.tvStatus.setText(appointment.getStatus());
        try {
            Date appointmentDateTime = sdf.parse(appointment.getDate() + " " + appointment.getTime());
            long timeDifference = getTimeDifference(appointment.getDate(), appointment.getTime());
            boolean canReschedule = timeDifference >= 48;

            Date currentDateTime = new Date();

            switch (appointment.getStatus().toLowerCase()) {
                case "pending":
                    holder.tvStatus.setText("Pending");
                    holder.btnAccept.setVisibility(View.VISIBLE);
                    holder.btnCancel.setVisibility(View.VISIBLE);
                    holder.btnReschedule.setVisibility(canReschedule ? View.VISIBLE : View.GONE);
                    break;

                case "confirmed":
                    if (appointmentDateTime != null) {
                        long diffInMillis = appointmentDateTime.getTime() - currentDateTime.getTime();
                        long diffInMinutes = diffInMillis / (1000 * 60);

                        boolean shouldEnableStartButton = diffInMinutes <= 5 && diffInMinutes >= 0;

                        if (shouldEnableStartButton) {
                            holder.btnStart.setVisibility(View.VISIBLE);
                            holder.btnStart.setText("Start");
                            holder.btnStart.setBackgroundColor(ContextCompat.getColor(context, R.color.green));
                            holder.btnCancel.setVisibility(View.GONE);
                            holder.btnGetMedicalRecord.setVisibility(View.VISIBLE);
                        } else {
                            holder.btnStart.setVisibility(View.GONE);
                            holder.btnCancel.setVisibility(View.VISIBLE);
                            holder.btnGetMedicalRecord.setVisibility(View.VISIBLE);
                        }

                        if (diffInMinutes > 5) {
                            new android.os.Handler().postDelayed(() -> {
                                holder.btnStart.setVisibility(View.VISIBLE);
                                holder.btnStart.setText("Start");
                                holder.btnStart.setBackgroundColor(ContextCompat.getColor(context, R.color.green));
                                holder.btnCancel.setVisibility(View.GONE);
                                holder.btnGetMedicalRecord.setVisibility(View.VISIBLE);
                            }, (diffInMinutes - 5) * 60 * 1000);
                        }

                        long completeDelay = (appointmentDateTime.getTime() + (15 * 60 * 1000)) - currentDateTime.getTime();
                        if (completeDelay > 0) {
                            new android.os.Handler().postDelayed(() -> {
                                updateAppointmentStatus(appointment.getId(), "completed", position, holder);
                            }, completeDelay);
                        } else {

                            updateAppointmentStatus(appointment.getId(), "completed", position, holder);
                        }
                    }

                    holder.btnAccept.setVisibility(View.GONE);
                    holder.btnCancel.setVisibility(View.VISIBLE);
                    holder.btnReschedule.setVisibility(canReschedule ? View.VISIBLE : View.GONE);
                    holder.tvStatus.setText("Confirmed");
                    holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.green));
                    break;



                case "cancelled":
                    holder.tvStatus.setText("Cancelled");
                    holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.red));
                    holder.btnAccept.setVisibility(View.GONE);
                    holder.btnStart.setVisibility(View.GONE);
                    holder.btnCancel.setVisibility(View.GONE);
                    holder.btnReschedule.setVisibility(View.GONE);
                    break;

                case "rescheduled":
                    holder.btnStart.setVisibility(View.GONE);
                    holder.btnAccept.setVisibility(View.GONE);
                    holder.btnCancel.setVisibility(View.VISIBLE);
                    holder.tvStatus.setText("Rescheduled");
                    if (canReschedule) {
                        holder.btnReschedule.setVisibility(View.VISIBLE);
                    } else {
                        holder.btnReschedule.setVisibility(View.GONE);
                    }
                    break;

                case "completed":
                    holder.btnStart.setVisibility(View.GONE);
                    holder.btnAccept.setVisibility(View.GONE);
                    holder.btnCancel.setVisibility(View.GONE);
                    holder.tvStatus.setText("Finished");
                    holder.btnReschedule.setVisibility(View.GONE);
                    holder.doctorChatButton.setVisibility(View.VISIBLE);
                    break;

                default:
                    holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.gray));
                    break;
            }

        } catch (ParseException e) {
            e.printStackTrace();
            Log.e("AppointmentAdapter", "Error parsing date: " + e.getMessage());
        }

        holder.btnReschedule.setOnClickListener(v -> {
            Intent intent = new Intent(context, StartAppointmentActivity.class);
            intent.putExtra("appointment_id", appointment.getId());
            intent.putExtra("role", "doctor");
            context.startActivity(intent);
        });

        holder.btnCancel.setOnClickListener(v -> {
            Log.d("DoctorAppointments", "Cancelled appointment for " + appointment.getId());
            disableAllButtons(holder);
            updateAppointmentStatus(appointment.getId(), "cancelled", position, holder);
        });
        holder.btnAccept.setOnClickListener(v -> {
            Log.d("DoctorAppointments", "Accepted appointment for " + appointment.getId());
            disableAllButtons(holder);
            updateAppointmentStatus(appointment.getId(),"confirmed",position,holder);
        });
        holder.doctorChatButton.setOnClickListener(v -> {
            Intent intent = new Intent(context, DoctorChatActivity.class);
            intent.putExtra("patientId", appointment.getPatientId());
            intent.putExtra("doctorId", appointment.getDoctorId());
            context.startActivity(intent);
        });
        holder.btnStart.setOnClickListener(v -> {
            Log.d("DoctorAppointments", "Started appointment for " + appointment.getId());
            Intent intent = new Intent(context, VideoCallActivity.class);
            intent.putExtra("appointment_id", appointment.getId());
            intent.putExtra("patientId", appointment.getPatientId());
            intent.putExtra("doctorId", appointment.getDoctorId());
            intent.putExtra("name", appointment.getName());
            context.startActivity(intent);
        });
        holder.btnGetMedicalRecord.setOnClickListener(v -> {
            Intent intent = new Intent(context, MedicalRecordsActivity.class);
            intent.putExtra("patientId", appointment.getPatientId());
            intent.putExtra("isDoctor", true);
            context.startActivity(intent);

        });
    }

    @Override
    public int getItemCount() {
        return appointmentsList.size();
    }

    private long getTimeDifference(String date, String time) {
        try {
            Date appointmentDateTime = sdf.parse(date + " " + time);
            Date currentDateTime = new Date();
            return (appointmentDateTime.getTime() - currentDateTime.getTime()) / (1000 * 60 * 60);
        } catch (ParseException e) {
            e.printStackTrace();
            Log.e("AppointmentAdapter", "Error parsing date: " + e.getMessage());
            return -1;
        }
    }

    private void disableAllButtons(DoctorAppointmentViewHolder holder) {
        holder.btnAccept.setEnabled(false);
        holder.btnCancel.setEnabled(false);
        holder.btnReschedule.setEnabled(false);
    }

    private void enableAllButtons(DoctorAppointmentViewHolder holder) {
        holder.btnAccept.setEnabled(true);
        holder.btnCancel.setEnabled(true);
        holder.btnReschedule.setEnabled(true);
    }

    private void updateAppointmentStatus(String appointmentId, String updateMsg, int position, DoctorAppointmentViewHolder holder) {
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("appointmentID", appointmentId);
            requestBody.put("status", updateMsg);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String url = base_url + "/doctor/appointment/update-status";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.PUT, url, requestBody,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            Log.d("AppointmentUpdater", "Appointment status updated successfully");
                            appointmentsList.get(position).setStatus(updateMsg);
                            notifyItemChanged(position);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } finally {
                        enableAllButtons(holder);
                    }
                },
                error -> {
                    Log.e("AppointmentUpdater", "Error updating status: " + error.getMessage());
                    enableAllButtons(holder);
                }
        );
        requestQueue.add(jsonObjectRequest);
    }
}
