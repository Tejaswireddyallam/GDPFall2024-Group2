package com.example.telemedicine.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
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
import com.example.telemedicine.PatientChatActivity;
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

public class AppointmentsAdapter extends RecyclerView.Adapter<AppointmentsAdapter.AppointmentViewHolder> {

    private List<Appointments> appointmentsList;
    private Context context;
    private boolean isDashboard;
    private RequestQueue requestQueue;
    String base_url = BuildConfig.API_BASE_URL;

    // Constructor for Adapter
    public AppointmentsAdapter(List<Appointments> appointmentsList, Context context) {
        this.appointmentsList = appointmentsList;
        this.context = context;
        this.requestQueue = Volley.newRequestQueue(context);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateList(List<Appointments> newAppointmentsList) {
        appointmentsList = newAppointmentsList;
        notifyDataSetChanged();
    }

    // ViewHolder Class
    public static class AppointmentViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvAppointmentTime, tvAppointmentDate, tvStatus;
        Button appointmentStartButton, appointmentCancelButton, appointmentRescheduleButton, patientChatButton;

        public AppointmentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvAppointmentTime = itemView.findViewById(R.id.tvAppointmentTime);
            tvAppointmentDate = itemView.findViewById(R.id.tvAppointmentDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            appointmentStartButton = itemView.findViewById(R.id.btnStart);
            appointmentCancelButton = itemView.findViewById(R.id.btnCancel);
            appointmentRescheduleButton = itemView.findViewById(R.id.btnReschedule);
            patientChatButton = itemView.findViewById(R.id.btnChat);
        }
    }

    @NonNull
    @Override
    public AppointmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(context).inflate(R.layout.appointments_layout, parent, false);
        return new AppointmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppointmentViewHolder holder, int position) {
        Appointments appointment = appointmentsList.get(position);

        holder.tvName.setText(appointment.getName());
        holder.tvAppointmentTime.setText(appointment.getTime());
        holder.tvAppointmentDate.setText(appointment.getDate());

        String status = appointment.getStatus();

        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy h : mm a", Locale.getDefault());
       // Log.d("Apt time", "Time: " + sdf.format(new Date()));
        try {
            Date appointmentDateTime = sdf.parse(appointment.getDate() + " " + appointment.getTime());
            Date currentDateTime = new Date();

            long timeDifference = (appointmentDateTime.getTime() - currentDateTime.getTime()) / (1000 * 60 * 60);

            boolean canReschedule = timeDifference >= 48;

           // Log.e("AppointmentAdapter", "Appointment: " + appointment);
            switch (status.toLowerCase()) {

                case "pending":
                    Log.d("AppointmentAdapter", "Status: " + status);
                    holder.appointmentStartButton.setVisibility(View.GONE);
                    holder.appointmentCancelButton.setVisibility(View.VISIBLE);
                    holder.appointmentCancelButton.setText("Cancel");
                    holder.appointmentCancelButton.setBackgroundColor(ContextCompat.getColor(context, R.color.red));
                    holder.tvStatus.setText("Pending");

                    if (canReschedule) {
                        holder.appointmentRescheduleButton.setVisibility(View.VISIBLE);
                    } else {
                        holder.appointmentRescheduleButton.setVisibility(View.GONE);
                    }
                    break;

                case "confirmed":
                    Log.d("AppointmentAdapter", "Status: " + status);
                    if (appointmentDateTime != null) {
                        long diffInMillis = appointmentDateTime.getTime() - currentDateTime.getTime();
                        long diffInMinutes = diffInMillis / (1000 * 60);

                        boolean shouldEnableStartButton = diffInMinutes <= 5 && diffInMinutes >= 0;

                        if (shouldEnableStartButton) {
                            holder.appointmentStartButton.setVisibility(View.VISIBLE);
                            holder.appointmentStartButton.setText("Start");
                            holder.appointmentStartButton.setBackgroundColor(ContextCompat.getColor(context, R.color.green));
                        } else {
                            holder.appointmentStartButton.setVisibility(View.GONE);
                        }

                        if (diffInMinutes > 5) {
                            new android.os.Handler().postDelayed(() -> {
                                holder.appointmentStartButton.setVisibility(View.VISIBLE);
                                holder.appointmentStartButton.setText("Start");
                                holder.appointmentStartButton.setBackgroundColor(ContextCompat.getColor(context, R.color.green));
                            }, (diffInMinutes - 5) * 60 * 1000);
                        }

                        long completedDelayMillis = (appointmentDateTime.getTime() - currentDateTime.getTime()) + (15 * 60 * 1000);
                        if (completedDelayMillis > 0) {
                            new android.os.Handler().postDelayed(() -> {
                                updateAppointmentStatus(appointment.getId(), "completed", position, "update-status", holder);
                            }, completedDelayMillis);
                        } else {
                            updateAppointmentStatus(appointment.getId(), "completed", position, "update-status", holder);
                        }
                    }

                    holder.appointmentCancelButton.setVisibility(View.GONE);
                    holder.appointmentRescheduleButton.setVisibility(canReschedule ? View.VISIBLE : View.GONE);
                    holder.tvStatus.setText("Confirmed");
                    break;



                case "cancelled":
                    Log.d("AppointmentAdapter", "Status: " + status);
                    holder.appointmentStartButton.setVisibility(View.GONE);
                    holder.appointmentRescheduleButton.setVisibility(View.GONE);
                    holder.appointmentCancelButton.setVisibility(View.GONE);
                    holder.tvStatus.setText("Cancelled");
                    break;

                case "rescheduled":
                    Log.d("AppointmentAdapter", "Status: " + status);
                    holder.appointmentStartButton.setVisibility(View.GONE);
                    holder.appointmentCancelButton.setVisibility(View.VISIBLE);
                    holder.tvStatus.setText("Rescheduled");
                    if (canReschedule) {
                        holder.appointmentRescheduleButton.setVisibility(View.VISIBLE);
                    } else {
                        holder.appointmentRescheduleButton.setVisibility(View.GONE);
                    }
                    break;
                case "completed":
                    Log.d("AppointmentAdapter", "Status: " + status);
                    holder.appointmentStartButton.setVisibility(View.GONE);
                    holder.appointmentCancelButton.setVisibility(View.GONE);
                    holder.tvStatus.setText("Finished");
                    holder.appointmentRescheduleButton.setVisibility(View.GONE);
                    holder.patientChatButton.setVisibility(View.VISIBLE);
                    break;

                default:
                    holder.appointmentStartButton.setText("Unknown Status");
                    holder.appointmentStartButton.setBackgroundColor(Color.GRAY);
                    holder.appointmentStartButton.setEnabled(false);
                    holder.tvStatus.setText("Unknown Status");
                    break;
            }
        } catch (ParseException e) {
            e.printStackTrace();
            Log.e("AppointmentAdapter", "Error parsing date: " + e.getMessage());
        }

        holder.appointmentStartButton.setOnClickListener(v -> {
            Log.d("Appointment", "Starting appointment for Dr. " + appointment.getName());
        });


        holder.appointmentRescheduleButton.setOnClickListener(v -> {
            Log.d("Appointment", "Rescheduling appointment for Dr. " + appointment.getName());
            disableAllButtons(holder);
            Intent intent = new Intent(context, StartAppointmentActivity.class);
            intent.putExtra("appointment_id", appointment.getId());
            intent.putExtra("role", "patient");
            context.startActivity(intent);
            enableAllButtons(holder);
        });

        holder.appointmentCancelButton.setOnClickListener(v -> {
            Log.d("Appointment", "Cancelling appointment for Dr. " + appointment.getName());
            disableAllButtons(holder);
            updateAppointmentStatus(appointment.getId(), "cancelled", position, "update-status", holder);
        });

        holder.patientChatButton.setOnClickListener(v -> {
            Intent intent = new Intent(context, PatientChatActivity.class);
            intent.putExtra("patientId", appointment.getPatientId());
            intent.putExtra("doctorId", appointment.getDoctorId());
            context.startActivity(intent);
        });

        holder.appointmentStartButton.setOnClickListener(v -> {
            Log.d("PatientAppointments", "Started appointment for " + appointment.getId());
            Intent intent = new Intent(context, VideoCallActivity.class);
            intent.putExtra("appointment_id", appointment.getId());
            intent.putExtra("patientId", appointment.getPatientId());
            intent.putExtra("doctorId", appointment.getDoctorId());
            intent.putExtra("name", appointment.getName());
            context.startActivity(intent);
        });

    }


    @Override
    public int getItemCount() {
        return appointmentsList.size();
    }

    private void disableAllButtons(AppointmentViewHolder holder) {
        if (holder.appointmentStartButton != null) {
            holder.appointmentStartButton.setEnabled(false);
        }
        if (holder.appointmentCancelButton != null) {
            holder.appointmentCancelButton.setEnabled(false);
        }
        if (holder.appointmentRescheduleButton != null) {
            holder.appointmentRescheduleButton.setEnabled(false);
        }
    }


    private void enableAllButtons(AppointmentViewHolder holder) {
        if (holder.appointmentStartButton != null) {
            holder.appointmentStartButton.setEnabled(true);
        }
        if (holder.appointmentCancelButton != null) {
            holder.appointmentCancelButton.setEnabled(true);
        }
        if (holder.appointmentRescheduleButton != null) {
            holder.appointmentRescheduleButton.setEnabled(true);
        }
    }

    private void updateAppointmentStatus(String appointmentId, String updateMsg, int position, String route, AppointmentViewHolder holder) {
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("appointmentID", appointmentId);
            requestBody.put("status", updateMsg);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String url = base_url + "/patient/appointment/" + route;

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.PUT,
                url,
                requestBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            boolean success = response.getBoolean("success");
                            String message = response.getString("message");
                            if (success) {
                                Log.d("AppointmentUpdater", "Appointment status updated successfully: " + message);
                                appointmentsList.get(position).setStatus(updateMsg);
                                notifyItemChanged(position);
                            } else {
                                Log.e("AppointmentUpdater", "Failed to update appointment status: " + message);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        } finally {
                            enableAllButtons(holder);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("AppointmentUpdater", "Error updating appointment status: " + error.getMessage());
                        enableAllButtons(holder);
                    }
                }
        );

        requestQueue.add(jsonObjectRequest);
    }



}
