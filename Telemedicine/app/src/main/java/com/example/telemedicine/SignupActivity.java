package com.example.telemedicine;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.example.telemedicine.database.MyApp;
import com.example.telemedicine.database.User;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


public class SignupActivity extends AppCompatActivity {

    EditText nameInput, emailInput, passwordInput, confirmPasswordInput, contactInput, addressInput, specializationInput, qualificationInput;
    RadioGroup roleRadioGroup;
    Button signupButton;
    LinearLayout availabilityInput;
    TextView loginRedirect, startTimeText, endTimeText;
    AlertDialog loadingDialog;
    String base_url = BuildConfig.API_BASE_URL;
    String url;
    String startTime = "", endTime = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        contactInput = findViewById(R.id.contactInput);
        addressInput = findViewById(R.id.addressInput);
        roleRadioGroup = findViewById(R.id.roleRadioGroup);
        signupButton = findViewById(R.id.signupButton);
        loginRedirect = findViewById(R.id.loginRedirect);
        specializationInput = findViewById(R.id.specializationInput);
        qualificationInput = findViewById(R.id.qualificationInput);
        availabilityInput = findViewById(R.id.availabilityLayout);
        startTimeText = findViewById(R.id.startTimeText);
        endTimeText = findViewById(R.id.endTimeText);

        startTimeText.setOnClickListener(v -> showTimePickerDialog(true));
        endTimeText.setOnClickListener(v -> showTimePickerDialog(false));



        setupLoadingDialog();

        roleRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_doctor) {
                addressInput.setVisibility(View.GONE);
                specializationInput.setVisibility(View.VISIBLE);
                qualificationInput.setVisibility(View.VISIBLE);
                availabilityInput.setVisibility(View.VISIBLE);
            } else if (checkedId == R.id.radio_patient) {
                addressInput.setVisibility(View.VISIBLE);
                specializationInput.setVisibility(View.GONE);
                qualificationInput.setVisibility(View.GONE);
                availabilityInput.setVisibility(View.GONE);
            }
        });

        signupButton.setOnClickListener(v -> signupUser());

        loginRedirect.setOnClickListener(v -> {
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
            startActivity(intent);
        });
    }

    private void setupLoadingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.loading, null);
        builder.setView(dialogView);
        builder.setCancelable(false);
        loadingDialog = builder.create();
    }

    private void showTimePickerDialog(boolean isStartTime) {
        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    String selectedTime = String.format("%02d:%02d %s",
                            (hourOfDay % 12 == 0) ? 12 : hourOfDay % 12,
                            minute,
                            (hourOfDay < 12) ? "AM" : "PM");

                    if (isStartTime) {
                        startTime = selectedTime;
                        startTimeText.setText(startTime);
                    } else {
                        endTime = selectedTime;
                        endTimeText.setText(endTime);
                    }
                }, 9, 0, false);
        timePickerDialog.show();
    }


    private void signupUser() {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();
        String contact = contactInput.getText().toString().trim();

        int selectedRoleId = roleRadioGroup.getCheckedRadioButtonId();
        if (selectedRoleId == -1) {
            Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show();
            return;
        }
        RadioButton selectedRoleButton = findViewById(selectedRoleId);
        String role = selectedRoleButton != null ? selectedRoleButton.getText().toString() : "";


        String address = "";
        if (addressInput.getVisibility() == View.VISIBLE) {
            address = addressInput.getText().toString().trim();
            if (address.isEmpty()) {
                Toast.makeText(this, "Please enter your address", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        String specialization = "";
        String qualification = "";
        String availability = "";

        if (specializationInput.getVisibility() == View.VISIBLE) {
            specialization = specializationInput.getText().toString().trim();
            qualification = qualificationInput.getText().toString().trim();
           // availability = availabilityInput.getText().toString().trim();

            if (specialization.isEmpty() || qualification.isEmpty() || startTime.isEmpty() || endTime.isEmpty()) {
                Toast.makeText(this, "Please fill all doctor details", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()
                || contact.isEmpty() || role.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show();
            return;
        }


        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        signupButton.setEnabled(false);
        loadingDialog.show();


        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("fullname", name);
            jsonBody.put("email", email);
            jsonBody.put("password", password);
            jsonBody.put("contact", contact);
            jsonBody.put("address", address);
            jsonBody.put("role", role);

            if (role.equalsIgnoreCase("Doctor")) {
                availability = startTime + " - " + endTime;
                jsonBody.put("specialization", specialization);
                jsonBody.put("qualification", qualification);
                jsonBody.put("availability", availability);
                url = base_url + "/doctor/signup";
            } else {
                url = base_url + "/patient/signup";
            }

            Log.d("Josnbody", String.valueOf(jsonBody));

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error creating JSON body", Toast.LENGTH_SHORT).show();
            loadingDialog.dismiss();
            signupButton.setEnabled(true);
            return;
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, jsonBody,
                response -> {
                    loadingDialog.dismiss();
                    signupButton.setEnabled(true);

                    if (response.has("message")) {
                        Toast.makeText(SignupActivity.this, "Signup Successful", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(SignupActivity.this, "Signup Failed", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    loadingDialog.dismiss();
                    signupButton.setEnabled(true);

                    Toast.makeText(SignupActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
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
