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
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.telemedicine.database.MyApp;
import com.example.telemedicine.database.User;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class DoctorProfileActivity extends AppCompatActivity {

    private static final String TAG = "DoctorProfileActivity";
    private boolean isEditing = false;
    private AlertDialog loadingDialog;
    private Button btnEditProfile;
    private TextView tvUserName;
    private String doctorId;
    private EditText etUsername, etGender, etDob, etMobile, etEmail, etQualification, etSpecialization, etAvailability;
    private String base_url = BuildConfig.API_BASE_URL;
    BottomNavigationView bottomNavigationView;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_profile);

        Intent getIntent = getIntent();
        doctorId = getIntent.getStringExtra("doctorId");

        btnEditProfile = findViewById(R.id.btnEditProfile);
        etUsername = findViewById(R.id.etUsername);
        tvUserName = findViewById(R.id.tvUserName);
        etGender = findViewById(R.id.etGender);
        etDob = findViewById(R.id.etDob);
        etMobile = findViewById(R.id.etMobile);
        etEmail = findViewById(R.id.etEmail);
        etAvailability = findViewById(R.id.etAvailability);
        etQualification = findViewById(R.id.etQualification);
        etSpecialization = findViewById(R.id.etSpecialization);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> onBackPressed());

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.nav_settings);
        bottomNavigationView.setOnNavigationItemSelectedListener(this::onNavigationItemSelected);

        setupLoadingDialog();
        fetchUser();

        btnEditProfile.setOnClickListener(view -> handleEditSave());
    }


    private boolean onNavigationItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.nav_home) {
            Intent homeIntent = new Intent(DoctorProfileActivity.this, DoctorDashboardActivity.class);
            homeIntent.putExtra("doctorId", doctorId);
            startActivity(homeIntent);
            return true;
        } else if (item.getItemId() == R.id.nav_chats) {
            Intent chatIntent = new Intent(DoctorProfileActivity.this, ChatListActivity.class);
            chatIntent.putExtra("userId", doctorId);
            chatIntent.putExtra("isDoctor", true);
            startActivity(chatIntent);
            return true;
        }  else if (item.getItemId() == R.id.nav_settings) {
            showSettingsPopup(bottomNavigationView);
            return true;
        }
        return false;
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
        AlertDialog.Builder builder = new AlertDialog.Builder(DoctorProfileActivity.this);
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

        Intent intent = new Intent(DoctorProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void handleEditSave() {
        if (!isEditing) {
            // Enable editing
            isEditing = true;
            btnEditProfile.setText("Save Profile");
            enableEditing(true, etUsername, etGender, etDob, etMobile, etAvailability, etQualification, etSpecialization);
        } else {
            // Save profile
            isEditing = false;
            btnEditProfile.setText("Edit Profile");
            enableEditing(false, etUsername, etGender, etDob, etMobile, etAvailability, etQualification, etSpecialization);
            updateProfile();
        }
    }

    private void updateProfile() {
        btnEditProfile.setEnabled(false);
        loadingDialog.show();

        String url = base_url + "/doctor/updateProfile";
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("fullname", etUsername.getText().toString());
            jsonBody.put("email", etEmail.getText().toString());
            jsonBody.put("contact", etMobile.getText().toString());
            jsonBody.put("availability", etAvailability.getText().toString());
            jsonBody.put("qualification", etQualification.getText().toString());
            jsonBody.put("specialization", etSpecialization.getText().toString());
            jsonBody.put("gender", etGender.getText().toString());
            jsonBody.put("dob", etDob.getText().toString());
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error creating JSON body", Toast.LENGTH_SHORT).show();
            loadingDialog.dismiss();
            btnEditProfile.setEnabled(true);
            return;
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.PUT, url, jsonBody,
                response -> {
                    loadingDialog.dismiss();
                    btnEditProfile.setEnabled(true);

                    try {
                        if (response.getBoolean("success")) {
                            Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                            tvUserName.setText(etUsername.getText());
                        } else {
                            Toast.makeText(this, "Update failed: " + response.getString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing response", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    loadingDialog.dismiss();
                    btnEditProfile.setEnabled(true);
                    Log.e(TAG, "Error : " + error);
                    Toast.makeText(this, "Error updating profile", Toast.LENGTH_SHORT).show();
                }) {

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(jsonObjectRequest);
    }

    private void fetchUser() {
        loadingDialog.show();

        new Thread(() -> {
            User loggedInUser = MyApp.getAppDatabase().userDao().getLoggedInUser();
            if (loggedInUser != null) {
                String email = loggedInUser.email;

                // API call to fetch profile
                runOnUiThread(() -> {
                    String url = base_url + "/doctor/fetchProfile";
                    JSONObject jsonBody = new JSONObject();
                    try {
                        jsonBody.put("email", email);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error creating JSON body", Toast.LENGTH_SHORT).show();
                        loadingDialog.dismiss();
                        return;
                    }

                    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, jsonBody,
                            response -> {
                                loadingDialog.dismiss();

                                try {
                                    if (response.getBoolean("success")) {
                                        Toast.makeText(this, "Profile fetched successfully!", Toast.LENGTH_SHORT).show();

                                        // Extract user details from the response
                                        JSONObject userObject = response.getJSONObject("data").getJSONObject("user");

                                        String userName = userObject.optString("name", "");
                                        String gender = userObject.optString("gender", "");
                                        String dob = userObject.optString("dob", "");
                                        String contact = userObject.optString("contact", "");
                                        String availability = userObject.optString("availability", "");
                                        String qualification = userObject.optString("qualification", "");
                                        String specialization = userObject.optString("specialization", "");
                                        // Update the UI
                                        etUsername.setText(userName);
                                        tvUserName.setText(userName);
                                        if (!gender.equals("0")) {
                                            etGender.setText(gender);
                                        }
                                        if (!dob.equals("0")) {
                                            etDob.setText(dob);
                                        }
                                        etMobile.setText(contact);
                                        etAvailability.setText(availability);
                                        etQualification.setText(qualification);
                                        etSpecialization.setText(specialization);
                                        etEmail.setText(email);
                                    } else {
                                        Toast.makeText(this, "Failed to fetch profile: " + response.getString("message"), Toast.LENGTH_SHORT).show();
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    Toast.makeText(this, "Error parsing response", Toast.LENGTH_SHORT).show();
                                }
                            },
                            error -> {
                                loadingDialog.dismiss();
                                Log.e(TAG, "Error fetching profile: " + error);
                                Toast.makeText(this, "Error fetching profile", Toast.LENGTH_SHORT).show();
                            }) {
                        @Override
                        public Map<String, String> getHeaders() {
                            Map<String, String> headers = new HashMap<>();
                            headers.put("Content-Type", "application/json");
                            return headers;
                        }
                    };

                    jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                            5000,
                            0,
                            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                    ));

                    RequestQueue queue = Volley.newRequestQueue(this);
                    queue.add(jsonObjectRequest);
                });
            } else {
                runOnUiThread(() -> {
                    loadingDialog.dismiss();
                    Toast.makeText(this, "No logged-in user found!", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }


    private void enableEditing(boolean enable, EditText... fields) {
        for (EditText field : fields) {
            field.setEnabled(enable);
        }
    }

    private void setupLoadingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.loading, null);
        builder.setView(dialogView);
        builder.setCancelable(false);
        loadingDialog = builder.create();
    }

}
