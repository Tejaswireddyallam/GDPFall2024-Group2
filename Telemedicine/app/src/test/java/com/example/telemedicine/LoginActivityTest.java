package com.example.telemedicine;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.os.Build;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.NoCache;
import com.example.telemedicine.database.MyApp;
import com.example.telemedicine.database.User;


import com.android.volley.RequestQueue;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowToast;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = Build.VERSION_CODES.R)
public class LoginActivityTest {

    private LoginActivity loginActivity;
    private EditText emailInput, passwordInput;
    private RadioGroup roleRadioGroup;
    private Button loginButton;

    @Before
    public void setUp() {
        try (ActivityScenario<LoginActivity> scenario = ActivityScenario.launch(LoginActivity.class)) {
            scenario.onActivity(activity -> {
                loginActivity = activity;
                emailInput = activity.findViewById(R.id.emailInput);
                passwordInput = activity.findViewById(R.id.passwordInput);
                roleRadioGroup = activity.findViewById(R.id.roleRadioGroup);
                loginButton = activity.findViewById(R.id.loginButton);
            });
        }
    }

    @Test
    public void testLoginWithValidCredentials() {

        String mockResponseString = "{ \"success\": true, \"message\": \"Welcome\", \"data\": { \"user\": { \"id\": \"123\", \"name\": \"John Doe\", \"email\": \"test@example.com\", \"role\": \"patient\", \"Token\": \"abc123\", \"contact\": 5551234, \"isVerified\": 1 } } }";
        JSONObject mockResponse;
        try {
            mockResponse = new JSONObject(mockResponseString);
        } catch (JSONException e) {
            throw new RuntimeException("Invalid JSON mock response");
        }

        MockJsonObjectRequest mockRequest = new MockJsonObjectRequest(
                Request.Method.POST,
                "https://example.com/api/login",
                null,
                response -> {
                    try {
                        boolean success = response.getBoolean("success");
                        String message = response.getString("message");
                        assertTrue("Login should be successful", success);
                        assertEquals("Welcome", message);
                    } catch (JSONException e) {
                        fail("JSON parsing error: " + e.getMessage());
                    }
                },
                error -> fail("Error listener triggered: " + error.getMessage()),
                mockResponse
        );

        RequestQueue mockQueue = new RequestQueue(new NoCache(), new MockNetwork()) {
            @Override
            public <T> Request<T> add(Request<T> request) {
                if (request instanceof MockJsonObjectRequest) {
                    ((MockJsonObjectRequest) request).deliverResponse(mockResponse);
                }
                return request;
            }
        };

        mockQueue.start();
        mockQueue.add(mockRequest);

        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }


    @Test
    public void testRedirectToSignup() {
        View signupRedirect = loginActivity.findViewById(R.id.signupRedirect);
        signupRedirect.performClick();

        Intent expectedIntent = new Intent(loginActivity, SignupActivity.class);
        assertEquals(expectedIntent.getComponent(), shadowOf(loginActivity).getNextStartedActivity().getComponent());
    }

    @Test
    public void testLoginWithInvalidCredentials() {

        String mockResponseString = "{ \"success\": false, \"message\": \"Invalid credentials\" }";
        JSONObject mockResponse;
        try {
            mockResponse = new JSONObject(mockResponseString);
        } catch (JSONException e) {
            throw new RuntimeException("Invalid JSON mock response");
        }

        MockJsonObjectRequest mockRequest = new MockJsonObjectRequest(
                Request.Method.POST,
                "https://example.com/api/login",
                null,
                response -> fail("Success listener should not be triggered for invalid credentials"),
                error -> {

                    String toastText = ShadowToast.getTextOfLatestToast();
                    assertThat("Toast not shown", toastText, containsString("Login Failed: Invalid credentials"));
                },
                mockResponse
        );

        RequestQueue mockQueue = new RequestQueue(new NoCache(), new MockNetwork()) {
            @Override
            public <T> Request<T> add(Request<T> request) {
                if (request instanceof MockJsonObjectRequest) {
                    ((MockJsonObjectRequest) request).deliverResponse(mockResponse);
                }
                return request;
            }
        };

        MyApp.setRequestQueue(mockQueue);

        try (ActivityScenario<LoginActivity> scenario = ActivityScenario.launch(LoginActivity.class)) {
            scenario.onActivity(activity -> {

                EditText emailInput = activity.findViewById(R.id.emailInput);
                EditText passwordInput = activity.findViewById(R.id.passwordInput);
                RadioGroup roleRadioGroup = activity.findViewById(R.id.roleRadioGroup);
                Button loginButton = activity.findViewById(R.id.loginButton);

                emailInput.setText("invalid@example.com");
                passwordInput.setText("wrongpassword");
                roleRadioGroup.check(R.id.radio_patient);

                loginButton.performClick();

                Shadows.shadowOf(Looper.getMainLooper()).idle();
            });
        }
    }

}
