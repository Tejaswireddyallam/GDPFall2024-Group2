package com.example.telemedicine;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.telemedicine.adapter.DoctorAppointmentsAdapter;
import com.example.telemedicine.database.MyApp;
import com.example.telemedicine.database.User;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.telemedicine.model.Appointments;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DoctorDashboardActivity extends AppCompatActivity {

    private static final String TAG = "DoctorDashboardActivity";
    private static final int REQUEST_CODE = 200;
    private TextView userNameTextView;
    private RecyclerView recyclerView;
    private DoctorAppointmentsAdapter adapter;
    private List<Appointments> appointmentsList = new ArrayList<>();
    public String doctorId;
    String base_url = BuildConfig.API_BASE_URL;
    public Executor executor = Executors.newSingleThreadExecutor();
    private static final int STORAGE_PERMISSION_CODE = 100;
    BottomNavigationView bottomNavigationView;
    private Button logoutButton;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_dashboard);
        userNameTextView = findViewById(R.id.userNameTextView);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "READ_EXTERNAL_STORAGE permission not granted");

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE);
        } else {
            Log.d(TAG, "READ_EXTERNAL_STORAGE permission already granted");
        }


        Intent getIntent = getIntent();
        doctorId = getIntent.getStringExtra("doctorId");

        SharedPreferences sharedPreferences = getSharedPreferences("user", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("doctorId", doctorId);
        editor.apply();

        LinearLayout serviceAppointments = findViewById(R.id.service_appointments);
        LinearLayout servicePharmacy = findViewById(R.id.service_pharmacy);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        bottomNavigationView.setOnNavigationItemSelectedListener(this::onNavigationItemSelected);

        recyclerView = findViewById(R.id.doctorAppointmentsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DoctorAppointmentsAdapter(appointmentsList, this);
        recyclerView.setAdapter(adapter);

        View.OnClickListener navigateToPatientAppointment = v -> {
            Log.d(TAG, "Patient Appointment button clicked!");
            Intent intent = new Intent(DoctorDashboardActivity.this, DoctorAppointmentActivity.class);
            intent.putExtra("doctorId", doctorId);
            startActivity(intent);
        };

        View.OnClickListener navigateToPharmacy = v -> {
            Intent intent = new Intent(DoctorDashboardActivity.this, PharmacyListActivity.class);
            intent.putExtra("doctorId", doctorId);
            intent.putExtra("isDoctor", true);
            startActivity(intent);
        };

        fetchAppointments();
        fetchAndLogUserInfo();

        serviceAppointments.setOnClickListener(navigateToPatientAppointment);
        servicePharmacy.setOnClickListener(navigateToPharmacy);

        ImageButton btnRefresh = findViewById(R.id.btnRefresh);
        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchAppointments();
            }
        });

    }

    public void showSettingsPopup(View anchorView) {
        View settingsItemView = findViewById(R.id.nav_settings);
        if (settingsItemView == null) return;

        PopupMenu popup = new PopupMenu(this, settingsItemView, android.view.Gravity.END);
        popup.getMenuInflater().inflate(R.menu.settings_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.nav_profile) {
                   // Toast.makeText(DoctorDashboardActivity.this, "Profile Clicked", Toast.LENGTH_SHORT).show();
                    Intent profileIntent = new Intent(DoctorDashboardActivity.this, DoctorProfileActivity.class);
                    profileIntent.putExtra("doctorId", doctorId);
                    startActivity(profileIntent);
                    return true;
                } else if (id == R.id.nav_logout) {
                    //Toast.makeText(DoctorDashboardActivity.this, "Logout Clicked", Toast.LENGTH_SHORT).show();

                    showLogoutConfirmationDialog();
                    return true;
                } else if (id == R.id.nav_feedback) {
                    //Toast.makeText(DashboardActivity.this, "Feedback Clicked", Toast.LENGTH_SHORT).show();
                    showFeedbackPopup();
                    return true;
                }
                return false;
            }
        });

        popup.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
       // fetchAndLogUserInfo();
       // fetchAppointments();
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
    }

    private void showFeedbackPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(DoctorDashboardActivity.this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.feedback_popup, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        EditText feedbackInput = dialogView.findViewById(R.id.feedbackInput);
        Button submitButton = dialogView.findViewById(R.id.submitFeedbackBtn);

        submitButton.setOnClickListener(v -> {
            String feedback = feedbackInput.getText().toString().trim();
            if (!feedback.isEmpty()) {

                ActivityLogHelper.submitLog(DoctorDashboardActivity.this, Integer.parseInt(doctorId), feedback, "doctor");

                Toast.makeText(this, "Thank you for your feedback!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else {
                feedbackInput.setError("Please enter some feedback.");
            }
        });

        dialog.show();
    }


    @Override
    protected void onRestart() {
        super.onRestart();
        fetchAppointments();
        Log.d("Appt Refetched", "onRestart called, appointments refetched");
    }

    public boolean onNavigationItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.nav_home) {
            return true;
        } else if (item.getItemId() == R.id.nav_chats) {
            Intent chatIntent = new Intent(DoctorDashboardActivity.this, ChatListActivity.class);
            chatIntent.putExtra("userId", doctorId);
            chatIntent.putExtra("isDoctor", true);
            startActivity(chatIntent);
            return true;
        } else if (item.getItemId() == R.id.nav_settings) {
            showSettingsPopup(bottomNavigationView);
            return true;
        }
        return false;
    }

    public void fetchAndLogUserInfo() {
        new Thread(() -> {

            User loggedInUser = MyApp.getAppDatabase().userDao().getLoggedInUser();

            if (loggedInUser != null) {
                Log.d(TAG, "User Info:");
                Log.d(TAG, "Name: " + loggedInUser.fullName);
                Log.d(TAG, "Email: " + loggedInUser.email);
                Log.d(TAG, "Role: " + loggedInUser.role);
                Log.d(TAG, "Token: " + loggedInUser.token);
                Log.d(TAG, "Contact Info: " + loggedInUser.contactInfo);
                Log.d(TAG, "Is Verified: " + loggedInUser.isVerified);

                runOnUiThread(() -> {
                    userNameTextView.setText(loggedInUser.fullName);
                });
            } else {
                Log.e(TAG, "No logged-in user found in the database!");
            }
        }).start();
    }

    public void fetchAppointments() {
        String url = base_url +"/doctor/appointment/" + doctorId;

        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if (response.getBoolean("success")) {
                                JSONArray appointmentsArray = response.getJSONArray("appointments");
                                Log.d(TAG, "Appointments: " + appointmentsArray.toString());
                                appointmentsList.clear();

                                for (int i = 0; i < appointmentsArray.length(); i++) {
                                    JSONObject appointmentObject = appointmentsArray.getJSONObject(i);

                                    String slotDate = appointmentObject.getString("SlotDate");
                                    String slotTime = appointmentObject.getString("SlotTime");
                                    String patientName = appointmentObject.getString("PatientName");
                                    String status = appointmentObject.getString("Status");
                                    String id = appointmentObject.getString("AppointmentID");
                                    String patientId = appointmentObject.getString("PatientID");

                                    Appointments appointment = new Appointments(slotDate, slotTime, patientName, id, status, patientId, doctorId);
                                    appointmentsList.add(appointment);
                                }

                                adapter.updateList(appointmentsList);
                            }
                            else {
                                Toast.makeText(DoctorDashboardActivity.this, "Empty appointments...", Toast.LENGTH_SHORT).show();
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(DoctorDashboardActivity.this, "Error parsing response", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Error fetching appointments: " + error.getMessage());
                        Toast.makeText(DoctorDashboardActivity.this, "Failed to fetch appointments. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                5000,
                0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        queue.add(jsonObjectRequest);
    }

    public void showLogoutConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Logout Confirmation")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        performLogout();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public void performLogout() {

        SharedPreferences sharedPreferences = getSharedPreferences("user", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();


        new Thread(() -> {
            MyApp.getAppDatabase().userDao().clearUserData();
        }).start();


        Intent intent = new Intent(DoctorDashboardActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

}
