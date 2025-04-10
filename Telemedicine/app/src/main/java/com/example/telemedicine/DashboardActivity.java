package com.example.telemedicine;

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

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.telemedicine.adapter.AppointmentsAdapter;
import com.example.telemedicine.database.MyApp;
import com.example.telemedicine.database.User;
import com.example.telemedicine.model.Appointments;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "DashboardActivity";
    private TextView userNameTextView;
    public String patientId;

    private RecyclerView recyclerView;
    private AppointmentsAdapter adapter;
    private List<Appointments> appointmentsList = new ArrayList<>();
    String base_url = BuildConfig.API_BASE_URL;
    public Executor executor = Executors.newSingleThreadExecutor();
    BottomNavigationView bottomNavigationView;
    private Button logoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        Intent getIntent = getIntent();
        patientId = getIntent.getStringExtra("patientId");

        SharedPreferences sharedPreferences = getSharedPreferences("user", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("patientId", patientId);
        editor.apply();

        userNameTextView = findViewById(R.id.userNameTextView);

        fetchAndLogUserInfo();

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        bottomNavigationView.setOnNavigationItemSelectedListener(this::onNavigationItemSelected);


        recyclerView = findViewById(R.id.appointmentsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AppointmentsAdapter(appointmentsList, this);
        recyclerView.setAdapter(adapter);

        fetchAppointments();

        LinearLayout serviceDoctor = findViewById(R.id.service_doctor);
        LinearLayout servicePharmacy = findViewById(R.id.service_pharmacy);
        LinearLayout serviceAppointments = findViewById(R.id.service_appointments);
        Button findDoctorBtn = findViewById(R.id.findDoctorBtn);
        LinearLayout medicalRecords = findViewById(R.id.service_medical_records);

        View.OnClickListener navigateToSearchDoctor = v -> {
            Intent intent = new Intent(DashboardActivity.this, SearchDoctorActivity.class);
            intent.putExtra("patientId", patientId);
            startActivity(intent);
        };

        View.OnClickListener navigateToPatientAppointment = v -> {
            Intent intent = new Intent(DashboardActivity.this, PatientAppointmentActivity.class);
            intent.putExtra("patientId", patientId);
            startActivity(intent);
        };

        View.OnClickListener navigateToPharmacy = v -> {
            Intent intent = new Intent(DashboardActivity.this, PharmacyListActivity.class);
            intent.putExtra("patientId", patientId);
            startActivity(intent);
        };

        View.OnClickListener navigateToMedicalRecords = v -> {
            Intent intent = new Intent(DashboardActivity.this, MedicalRecordsActivity.class);
            intent.putExtra("patientId", patientId);
            intent.putExtra("isDoctor", false);
            startActivity(intent);
        };

        serviceDoctor.setOnClickListener(navigateToSearchDoctor);
        findDoctorBtn.setOnClickListener(navigateToSearchDoctor);
        servicePharmacy.setOnClickListener(navigateToPharmacy);
        serviceAppointments.setOnClickListener(navigateToPatientAppointment);
        medicalRecords.setOnClickListener(navigateToMedicalRecords);

        ImageButton btnRefresh = findViewById(R.id.btnRefresh);
        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchAppointments();
            }
        });


        //logoutButton = findViewById(R.id.logoutButton);

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
                    // Toast.makeText(DashboardActivity.this, "Profile Clicked", Toast.LENGTH_SHORT).show();
                    Intent profileIntent = new Intent(DashboardActivity.this, PatientProfileActivity.class);
                    profileIntent.putExtra("patientId", patientId);
                    startActivity(profileIntent);
                    return true;
                } else if (id == R.id.nav_logout) {
                    //Toast.makeText(DashboardActivity.this, "Logout Clicked", Toast.LENGTH_SHORT).show();

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

    private void showFeedbackPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(DashboardActivity.this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.feedback_popup, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        EditText feedbackInput = dialogView.findViewById(R.id.feedbackInput);
        Button submitButton = dialogView.findViewById(R.id.submitFeedbackBtn);

        submitButton.setOnClickListener(v -> {
            String feedback = feedbackInput.getText().toString().trim();
            if (!feedback.isEmpty()) {
                Toast.makeText(this, "Thank you for your feedback!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else {
                feedbackInput.setError("Please enter some feedback.");
            }
        });

        dialog.show();
    }


    @Override
    protected void onResume() {
        super.onResume();
       // fetchAppointments();
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
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
            Intent chatIntent = new Intent(DashboardActivity.this, ChatListActivity.class);
            chatIntent.putExtra("userId", patientId);
            chatIntent.putExtra("isDoctor", false);
            startActivity(chatIntent);
            return true;
        } else if (item.getItemId() == R.id.nav_settings) {
            showSettingsPopup(bottomNavigationView);
            return true;
        }
        return false;
    }


    private void fetchAndLogUserInfo() {
        executor.execute(() -> {
            User loggedInUser = MyApp.getAppDatabase().userDao().getLoggedInUser();

            if (loggedInUser != null) {
                Log.d(TAG, "User Info:");
                Log.d(TAG, "Name: " + loggedInUser.fullName);
                Log.d(TAG, "Email: " + loggedInUser.email);
                Log.d(TAG, "Role: " + loggedInUser.role);
                Log.d(TAG, "Token: " + loggedInUser.token);
                Log.d(TAG, "Contact Info: " + loggedInUser.contactInfo);
                Log.d(TAG, "Is Verified: " + loggedInUser.isVerified);

                runOnUiThread(() -> userNameTextView.setText(loggedInUser.fullName));
            } else {
                runOnUiThread(() -> {
                    Toast.makeText(this, "No logged-in user found!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                });
            }
        });
    }

    public void fetchAppointments() {
        String url = base_url +"/patient/appointment/" + patientId;
        Log.d("Appointment URL", url);
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
                                // Log.d(TAG, "Appointments: " + appointmentsArray.toString());
                                appointmentsList.clear();

                                for (int i = 0; i < appointmentsArray.length(); i++) {
                                    JSONObject appointmentObject = appointmentsArray.getJSONObject(i);

                                    String slotDate = appointmentObject.getString("SlotDate");
                                    String slotTime = appointmentObject.getString("SlotTime");
                                    String doctorName = appointmentObject.getString("DoctorName");
                                    String status = appointmentObject.getString("Status");
                                    String id = appointmentObject.getString("AppointmentID");
                                    String doctorId = appointmentObject.getString("DoctorID");

                                    Appointments appointment = new Appointments(slotDate, slotTime, doctorName, id, status, patientId, doctorId);
                                    appointmentsList.add(appointment);
                                }

                                adapter.updateList(appointmentsList);
                            }
                            else{
                                Toast.makeText(DashboardActivity.this, "Empty appointments...", Toast.LENGTH_SHORT).show();
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(DashboardActivity.this, "Error parsing response", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Error fetching appointments: " + error.getMessage());
                        Toast.makeText(DashboardActivity.this, "Failed to fetch appointments. Please try again.", Toast.LENGTH_SHORT).show();
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

    private void performLogout() {

        SharedPreferences sharedPreferences = getSharedPreferences("user", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        new Thread(() -> {
            MyApp.getAppDatabase().userDao().clearUserData();
        }).start();

        Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
