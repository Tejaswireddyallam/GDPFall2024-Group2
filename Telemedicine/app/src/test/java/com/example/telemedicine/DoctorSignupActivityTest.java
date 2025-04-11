package com.example.telemedicine;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.os.Build;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.NoCache;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = Build.VERSION_CODES.R)
public class DoctorSignupActivityTest {

    private ActivityScenario<SignupActivity> scenario;
    private EditText nameInput, emailInput, passwordInput, confirmPasswordInput, addressInput, contactInput;
    private RadioGroup roleRadioGroup;
    private Button signupButton;
    private TextView loginRedirect;

    @Before
    public void setUp() {
        scenario = ActivityScenario.launch(SignupActivity.class);
        scenario.onActivity(activity -> {
            nameInput = activity.findViewById(R.id.nameInput);
            emailInput = activity.findViewById(R.id.emailInput);
            passwordInput = activity.findViewById(R.id.passwordInput);
            confirmPasswordInput = activity.findViewById(R.id.confirmPasswordInput);
            roleRadioGroup = activity.findViewById(R.id.roleRadioGroup);
            signupButton = activity.findViewById(R.id.signupButton);
            loginRedirect = activity.findViewById(R.id.loginRedirect);
            addressInput = activity.findViewById(R.id.addressInput);
            contactInput = activity.findViewById(R.id.contactInput);
        });
    }

    @Test
    public void testEmptyFields() {
        scenario.onActivity(activity -> {
            roleRadioGroup.check(R.id.radio_patient);
            addressInput.setText("123 Main St");
            signupButton.performClick();
            String toastText = ShadowToast.getTextOfLatestToast();
            assertThat("Toast not shown", toastText, containsString("Please fill all fields"));
        });
    }

    @Test
    public void testInvalidEmail() {
        scenario.onActivity(activity -> {
            nameInput.setText("John Doe");
            emailInput.setText("invalidEmail");
            passwordInput.setText("password123");
            confirmPasswordInput.setText("password123");
            roleRadioGroup.check(R.id.radio_patient);
            addressInput.setText("123 Main St");
            contactInput.setText("1234567890");
            signupButton.performClick();
            String toastText = ShadowToast.getTextOfLatestToast();
            assertThat("Toast not shown", toastText, containsString("Invalid email format"));
        });
    }

    @Test
    public void testPasswordMismatch() {
        scenario.onActivity(activity -> {
            nameInput.setText("John Doe");
            emailInput.setText("johndoe@example.com");
            passwordInput.setText("password123");
            confirmPasswordInput.setText("password321");
            roleRadioGroup.check(R.id.radio_patient);
            addressInput.setText("123 Main St");
            contactInput.setText("1234567890");
            signupButton.performClick();
            String toastText = ShadowToast.getTextOfLatestToast();
            assertThat("Toast not shown", toastText, containsString("Passwords do not match"));
        });
    }


    @Test
    public void testSuccessfulDoctorSignup() {
        String mockResponseString = "{ \"success\": true, \"message\": \"Signup successful\" }";
        JSONObject mockResponse;
        try {
            mockResponse = new JSONObject(mockResponseString);
        } catch (JSONException e) {
            throw new RuntimeException("Invalid JSON mock response");
        }

        MockJsonObjectRequest mockRequest = new MockJsonObjectRequest(
                Request.Method.POST,
                "https://example.com/api/signup",
                null,
                response -> {
                    try {
                        boolean success = response.getBoolean("success");
                        String message = response.getString("message");
                        assertTrue("Signup should be successful", success);
                        assertEquals("Signup successful", message);
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

        scenario.onActivity(activity -> {
            nameInput.setText("Dr. Jane Doe");
            emailInput.setText("drjanedoe@example.com");
            passwordInput.setText("password123");
            confirmPasswordInput.setText("password123");
            contactInput.setText("9876543210");
            roleRadioGroup.check(R.id.radio_doctor);

            EditText specializationInput = activity.findViewById(R.id.specializationInput);
            EditText qualificationInput = activity.findViewById(R.id.qualificationInput);
            TextView startTimeText = activity.findViewById(R.id.startTimeText);
            TextView endTimeText = activity.findViewById(R.id.endTimeText);

            specializationInput.setText("Dermatology");
            qualificationInput.setText("MBBS, MD");

            activity.startTime = "10:00 AM";
            activity.endTime = "06:00 PM";
            startTimeText.setText("10:00 AM");
            endTimeText.setText("06:00 PM");

            signupButton.performClick();

            Shadows.shadowOf(Looper.getMainLooper()).idle();
        });
    }
}

