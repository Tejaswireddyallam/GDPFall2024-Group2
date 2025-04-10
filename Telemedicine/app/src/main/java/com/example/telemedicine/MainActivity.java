package com.example.telemedicine;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.telemedicine.database.MyApp;
import com.example.telemedicine.database.User;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.os.Build;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    AlertDialog loadingDialog;
    Button loginButton;
    String base_url = BuildConfig.API_BASE_URL;
    String url;
    private static final int PERMISSION_REQUEST_CODE = 100;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        checkAndRequestPermissions();

        loginButton = findViewById(R.id.loginButton);

        loginButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        setupLoadingDialog();
        fetchAndLogUserInfo();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            Map<String, Integer> permissionResults = new HashMap<>();
            boolean allPermissionsGranted = true;

            for (int i = 0; i < permissions.length; i++) {
                permissionResults.put(permissions[i], grantResults[i]);
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                }
            }

            if (allPermissionsGranted) {
                Toast.makeText(this, "All permissions granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Some permissions were denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }


    public interface UserCallback {
        void onUserFetched(User user);
        void onError(String errorMessage);
    }

    private void getUser(UserCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Fetch logged-in user from the database
                User loggedInUser = MyApp.getAppDatabase().userDao().getLoggedInUser();

                if (loggedInUser != null) {
                    // Log user details (on background thread)
                    Log.d(TAG, "User Info:");
                    Log.d(TAG, "Name: " + loggedInUser.fullName);
                    Log.d(TAG, "Email: " + loggedInUser.email);
                    Log.d(TAG, "Role: " + loggedInUser.role);
                    Log.d(TAG, "Token: " + loggedInUser.token);
                    Log.d(TAG, "Contact Info: " + loggedInUser.contactInfo);
                    Log.d(TAG, "Is Verified: " + loggedInUser.isVerified);

                    // Switch back to the main thread and pass the result via callback
                    runOnUiThread(() -> callback.onUserFetched(loggedInUser));
                } else {
                    runOnUiThread(() -> callback.onError("No logged-in user found in the database!"));
                }
            } catch (Exception e) {
                runOnUiThread(() -> callback.onError("Error fetching user info: " + e.getMessage()));
            }
        });
    }


    private void fetchAndLogUserInfo() {
        getUser(new UserCallback() {
            @Override
            public void onUserFetched(User loggedInUser) {
                // Handle the fetched user object here
                if (loggedInUser != null) {
                    loadingDialog.show(); // Show loading dialog

                    String url;
                    if (loggedInUser.role.equalsIgnoreCase("Doctor")) {
                        url = base_url + "/doctor/validate";
                    } else {
                        url = base_url + "/patient/validate";
                    }

                    JSONObject jsonBody = new JSONObject();
                    try {
                        jsonBody.put("email", loggedInUser.email);
                        jsonBody.put("token", loggedInUser.token);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, "Error creating JSON body", Toast.LENGTH_SHORT).show();
                        loadingDialog.dismiss(); // Hide loading dialog
                        return;
                    }

                    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, jsonBody,
                            response -> {
                                loadingDialog.dismiss(); // Hide loading dialog

                                try {
                                    boolean success = response.getBoolean("success");
                                    String message = response.getString("message");

                                    Log.d(TAG, "Message: "+ message + " json " + response);

                                    if (success) {
                                        Toast.makeText(MainActivity.this, "Login Successful: " + message, Toast.LENGTH_SHORT).show();
                                        // Extract user details from the response
                                        JSONObject userObject = response.getJSONObject("data").getJSONObject("user");
                                        String userID = userObject.getString("id");
                                        String userName = userObject.getString("name");
                                        Log.d(TAG, "Name: " + userName);
                                        // Navigate to the dashboard
                                        if(loggedInUser.role.equalsIgnoreCase("Doctor")){
                                            Intent newintent = new Intent(MainActivity.this, DoctorDashboardActivity.class);
                                            newintent.putExtra("doctorId", userID);
                                            startActivity(newintent);
                                            finish();
                                        } else {
                                            Intent newintent = new Intent(MainActivity.this, DashboardActivity.class);
                                            newintent.putExtra("patientId", userID);
                                            startActivity(newintent);
                                            finish();
                                        }
                                    } else {
                                        Toast.makeText(MainActivity.this, "Validation Failed: " + message, Toast.LENGTH_SHORT).show();
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    Toast.makeText(MainActivity.this, "Error parsing response", Toast.LENGTH_SHORT).show();
                                }
                            },
                            error -> {
                                loadingDialog.dismiss();
                                String errorMessage = error.networkResponse != null
                                        ? new String(error.networkResponse.data)
                                        : error.getMessage();
                                Toast.makeText(MainActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
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

                    RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
                    queue.add(jsonObjectRequest);

                } else {
                    Log.e(TAG, "No logged-in user found in the database!");
                }
            }

            @Override
            public void onError(String errorMessage) {
                // Handle errors here
                Log.e(TAG, errorMessage);
            }
        });
    }

    private void setupLoadingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.loading, null); // Custom layout for loading
        builder.setView(dialogView);
        builder.setCancelable(false); // Prevent user interaction during loading
        loadingDialog = builder.create();
    }

    private void checkAndRequestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+ (SDK 33+)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }


}
