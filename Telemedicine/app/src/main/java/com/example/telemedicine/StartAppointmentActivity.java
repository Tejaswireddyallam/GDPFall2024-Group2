package com.example.telemedicine;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class StartAppointmentActivity extends AppCompatActivity {

    public String selectedDate = null;
    public String selectedTimeSlot = null;
    private String doctorId;
    private String patientId, appointmentId, status, availability;
    private Button btnConfirmAppointment;
    AlertDialog loadingDialog;
    String base_url = BuildConfig.API_BASE_URL;
    private int startHour, endHour;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_appointment);

        SharedPreferences sharedPreferences = getSharedPreferences("user", MODE_PRIVATE);
        patientId = sharedPreferences.getString("patientId", "1");

        Intent intent = getIntent();
        if (intent.hasExtra("doctor_id")) {
            doctorId = intent.getStringExtra("doctor_id");
            status = "pending";
        } else {
            doctorId = null;
        }
        if(intent.hasExtra("availability")){
            availability = intent.getStringExtra("availability");
        } else {
            availability = null;
        }

        if (intent.hasExtra("appointment_id")) {
            appointmentId = intent.getStringExtra("appointment_id");
        } else {
            appointmentId = null;
        }

        if(intent.hasExtra("role")){
            if(intent.getStringExtra("role").equals("doctor")) {
                status = "confirmed";
            } else {
                status = "pending";
            }
        }

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> onBackPressed());

        GridLayout gridLayout = findViewById(R.id.gridTimeSlots);
        if (availability != null) {
            try {
                String[] times = availability.split(" - ");
                if (times.length == 2) {
                    String startTimeStr = times[0];
                    String endTimeStr = times[1];

                    SimpleDateFormat inputFormat = new SimpleDateFormat("hh:mm a", Locale.US);
                    SimpleDateFormat hourFormat = new SimpleDateFormat("H", Locale.US);

                    Date startDate = inputFormat.parse(startTimeStr);
                    Date endDate = inputFormat.parse(endTimeStr);

                    startHour = Integer.parseInt(hourFormat.format(startDate));
                    endHour = Integer.parseInt(hourFormat.format(endDate));
                }
            } catch (Exception e) {
                e.printStackTrace();
                startHour = 8;
                endHour = 21;
            }
        } else {
            startHour = 9;
            endHour = 21;
        }

        showHourSlots(gridLayout);

        CalendarView calendarView = findViewById(R.id.calendarView);

        Calendar calendar = Calendar.getInstance();
        calendarView.setMinDate(calendar.getTimeInMillis());
        calendar.add(Calendar.MONTH, 1);
        calendarView.setMaxDate(calendar.getTimeInMillis());

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedDate = dayOfMonth + "/" + (month + 1) + "/" + year;
            Toast.makeText(this, "Selected Date: " + selectedDate, Toast.LENGTH_SHORT).show();
            showHourSlots(gridLayout);
        });

        btnConfirmAppointment = findViewById(R.id.btnConfirmAppointment);
        btnConfirmAppointment.setOnClickListener(v -> {
            disableButton(btnConfirmAppointment);
            if (appointmentId == null) {
                confirmAppointment();
            } else if (doctorId == null) {
                rescheduleAppointment();
            }
        });

        setupLoadingDialog();
    }

    public void showHourSlots(GridLayout gridLayout) {
        gridLayout.removeAllViews();

        for (int hour = startHour; hour <= endHour; hour++) {
            Button hourButton = new Button(this);

            String label;
            if (hour < 12) {
                label = hour + " : 00 AM";
            } else if (hour == 12) {
                label = "12 : 00 PM";
            } else {
                label = (hour - 12) + " : 00 PM";
            }

            hourButton.setText(label);

            int finalHour = hour;
            hourButton.setOnClickListener(v -> showQuarterHourSlots(gridLayout, finalHour));

            gridLayout.addView(hourButton);
        }
    }


    public void showQuarterHourSlots(GridLayout gridLayout, int selectedHour) {
        gridLayout.removeAllViews();

        for (int i = 0; i < 4; i++) {
            int minute = i * 15;
            Button minuteButton = new Button(this);

            String amPm = selectedHour < 12 ? "AM" : "PM";
            int displayHour = (selectedHour % 12 == 0) ? 12 : selectedHour % 12;

            String minuteLabel = String.format(Locale.US, "%02d : %02d %s", displayHour, minute, amPm);
            minuteButton.setText(minuteLabel);

            minuteButton.setOnClickListener(v -> {
                for (int j = 0; j < gridLayout.getChildCount(); j++) {
                    Button child = (Button) gridLayout.getChildAt(j);
                    child.setBackgroundColor(getResources().getColor(android.R.color.white));
                }
                minuteButton.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
                selectedTimeSlot = minuteButton.getText().toString();
                Toast.makeText(this, "Selected Time: " + selectedTimeSlot, Toast.LENGTH_SHORT).show();
            });

            gridLayout.addView(minuteButton);
        }
    }


    private void confirmAppointment() {
        Log.d("Confirm", "Confirming appointment ");
        if (selectedDate == null || selectedTimeSlot == null) {
            Toast.makeText(this, "Please select a date and time slot!", Toast.LENGTH_SHORT).show();
            enableButton(btnConfirmAppointment);
            return;
        }

        try {
            JSONObject jsonParams = new JSONObject();
            jsonParams.put("doctorId", doctorId);
            jsonParams.put("patientId", patientId);
            jsonParams.put("date", selectedDate);
            jsonParams.put("time", selectedTimeSlot);
            jsonParams.put("adminId", "123");
            jsonParams.put("status", "Pending");

            String url = base_url + "/patient/schedule";
            loadingDialog.show();

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    url,
                    jsonParams,
                    response -> {
                        try {
                            boolean success = response.getBoolean("success");
                            loadingDialog.dismiss();
                            enableButton(btnConfirmAppointment);
                            if (success) {
                                Toast.makeText(this, "Appointment Request sent!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Error: " + response.getString("message"), Toast.LENGTH_SHORT).show();
                            }
                            findViewById(R.id.btnBack).performClick();
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Response Parsing Error!", Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> {
                        error.printStackTrace();
                        loadingDialog.dismiss();
                        enableButton(btnConfirmAppointment);
                        Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
            );


            request.setRetryPolicy(new DefaultRetryPolicy(
                    5000,
                    0,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));

            RequestQueue queue = Volley.newRequestQueue(this);
            queue.add(request);

        } catch (JSONException e) {
            e.printStackTrace();
            enableButton(btnConfirmAppointment);
            Toast.makeText(this, "Error creating JSON data!", Toast.LENGTH_SHORT).show();
        }
    }


    private void rescheduleAppointment() {
        Log.d("Reschedule", "Rescheduling appointment ");
        if (selectedDate == null || selectedTimeSlot == null) {
            Toast.makeText(this, "Please select a date and time slot!", Toast.LENGTH_SHORT).show();
            enableButton(btnConfirmAppointment);
            return;
        }

        try {
            JSONObject jsonParams = new JSONObject();
            jsonParams.put("SlotDate", selectedDate);
            jsonParams.put("SlotTime", selectedTimeSlot);
            jsonParams.put("appointmentID", appointmentId);
            jsonParams.put("status", status);

            String url = base_url + "/patient/appointment/reschedule";
            loadingDialog.show();

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.PUT,
                    url,
                    jsonParams,
                    response -> {
                        try {
                            Log.d("Response", response.toString());
                            boolean success = response.getBoolean("success");
                            loadingDialog.dismiss();
                            enableButton(btnConfirmAppointment);
                            if (success) {
                                Toast.makeText(this, "Appointment Reschedule request sent!", Toast.LENGTH_SHORT).show();

                            } else {
                                Toast.makeText(this, "Error: " + response.getString("message"), Toast.LENGTH_SHORT).show();
                            }
                            findViewById(R.id.btnBack).performClick();
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Response Parsing Error!", Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> {
                        error.printStackTrace();
                        loadingDialog.dismiss();
                        enableButton(btnConfirmAppointment);
                        Log.d("Error", error.toString());
                        Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
            );


            request.setRetryPolicy(new DefaultRetryPolicy(
                    5000,
                    0,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));

            RequestQueue queue = Volley.newRequestQueue(this);
            queue.add(request);

        } catch (JSONException e) {
            e.printStackTrace();
            enableButton(btnConfirmAppointment);
            Toast.makeText(this, "Error creating JSON data!", Toast.LENGTH_SHORT).show();
        }
    }



    private void disableButton(Button button) {
        if (button != null) {
            button.setEnabled(false);
        }
    }

    private void enableButton(Button button) {
        if (button != null) {
            button.setEnabled(true);
        }
    }


    private void setupLoadingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.loading, null);
        builder.setView(dialogView);
        builder.setCancelable(false);
        loadingDialog = builder.create();
    }
}
