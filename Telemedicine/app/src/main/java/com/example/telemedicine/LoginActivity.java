package com.example.telemedicine;

import static java.lang.Integer.parseInt;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    EditText emailInput, passwordInput;
    RadioGroup roleRadioGroup;
    Button loginButton;
    TextView signupRedirect, forgotPassword;
    AlertDialog loadingDialog;
    String base_url = BuildConfig.API_BASE_URL;
    String url;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        roleRadioGroup = findViewById(R.id.roleRadioGroup);
        loginButton = findViewById(R.id.loginButton);
        signupRedirect = findViewById(R.id.signupRedirect);
        forgotPassword = findViewById(R.id.forgotPassword);

        setupLoadingDialog();

        loginButton.setOnClickListener(v -> loginUser());

        signupRedirect.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });

        forgotPassword.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            int selectedRoleId = roleRadioGroup.getCheckedRadioButtonId();
            RadioButton selectedRoleButton = findViewById(selectedRoleId);
            String role = selectedRoleButton != null ? selectedRoleButton.getText().toString() : "";

            sendForgotPasswordRequest(email, role);
        });
    }

    private void sendForgotPasswordRequest(String email, String role) {
        loadingDialog.show();

        String url = base_url + "/" + role + "/forgot-password";

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("email", email);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error creating request", Toast.LENGTH_SHORT).show();
            loadingDialog.dismiss();
            return;
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, jsonBody,
                response -> {
                    loadingDialog.dismiss();
                    try {
                        boolean success = response.getBoolean("success");
                        String message = response.getString("message");

                        if (success) {
                            Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(LoginActivity.this, "Failed: " + message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(LoginActivity.this, "Error parsing response", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    loadingDialog.dismiss();
                    Toast.makeText(LoginActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(jsonObjectRequest);
    }


    private void setupLoadingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.loading, null); // Custom layout for loading
        builder.setView(dialogView);
        builder.setCancelable(false); // Prevent user interaction during loading
        loadingDialog = builder.create();
    }

    public void loginUser() {
        Log.d(TAG, "Login button clicked");

        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedRoleId = roleRadioGroup.getCheckedRadioButtonId();
        RadioButton selectedRoleButton = findViewById(selectedRoleId);
        String role = selectedRoleButton != null ? selectedRoleButton.getText().toString() : "";

        loginButton.setEnabled(false); // Disable button to prevent multiple clicks
        loadingDialog.show(); // Show loading dialog

        String url;
        if (role.equalsIgnoreCase("Doctor")) {
            url = base_url + "/doctor/login";
        } else {
            url = base_url + "/patient/login";
        }

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("email", email);
            jsonBody.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error creating JSON body", Toast.LENGTH_SHORT).show();
            loadingDialog.dismiss(); // Hide loading dialog
            loginButton.setEnabled(true); // Re-enable button
            return;
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, jsonBody,
                response -> {
                    loadingDialog.dismiss(); // Hide loading dialog
                    loginButton.setEnabled(true); // Re-enable button

                    try {
                        boolean success = response.getBoolean("success");
                        String message = response.getString("message");

                        if (success) {
                            Toast.makeText(LoginActivity.this, "Login Successful: " + message, Toast.LENGTH_SHORT).show();

                            // Extract user details from the response
                            JSONObject userObject = response.getJSONObject("data").getJSONObject("user");

                            String userID = userObject.getString("id");
                            String userName = userObject.getString("name");
                            String userEmail = userObject.getString("email");
                            String userRole = userObject.getString("role");
                            String userToken = userObject.getString("Token");
                            int userContactInfo = userObject.getInt("contact");
                            int isVerified = userObject.getInt("isVerified"); // Extract as integer

                            // Save user details in the database
                            new Thread(() -> {
                                User existingUser = MyApp.getAppDatabase().userDao().getLoggedInUser();

                                if (existingUser == null) {
                                    User loggedUser = new User();
                                    loggedUser.userID = parseInt(userID);
                                    loggedUser.fullName = userName;
                                    loggedUser.email = userEmail;
                                    loggedUser.role = userRole;
                                    loggedUser.token = userToken;
                                    loggedUser.contactInfo = userContactInfo;
                                    loggedUser.isVerified = (isVerified == 1); // Convert to boolean

                                    MyApp.getAppDatabase().userDao().insertUser(loggedUser);
                                } else {
                                    existingUser.fullName = userName;
                                    existingUser.email = userEmail;
                                    existingUser.role = userRole;
                                    existingUser.token = userToken;
                                    existingUser.contactInfo = userContactInfo;
                                    existingUser.isVerified = (isVerified == 1); // Convert to boolean

                                    MyApp.getAppDatabase().userDao().updateUser(existingUser);
                                }
                            }).start();

                            // Navigate to the dashboard
                            if(userRole.equalsIgnoreCase("Doctor")) {
                                Intent intent = new Intent(LoginActivity.this, DoctorDashboardActivity.class);
                                intent.putExtra("doctorId", userID);
                                intent.putExtra("userName", userName);
                                intent.putExtra("userEmail", userEmail);
                                startActivity(intent);
                                finish();
                            } else{
                                Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
                                intent.putExtra("patientId", userID);
                                intent.putExtra("userName", userName);
                                intent.putExtra("userEmail", userEmail);
                                startActivity(intent);
                                finish();
                            }

                        } else {
                            Toast.makeText(LoginActivity.this, "Login Failed: " + message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(LoginActivity.this, "Error parsing response", Toast.LENGTH_SHORT).show();
                    }

                },
                error -> {
                    loadingDialog.dismiss();
                    loginButton.setEnabled(true);
                    Log.e(TAG, "Error : " + error);
                    Toast.makeText(LoginActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
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
    }

}
