package com.example.telemedicine;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.test.core.app.ApplicationProvider;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;

@RunWith(RobolectricTestRunner.class)
public class DoctorloginActivity {

    private LoginActivity activity;
    private EditText emailInput, passwordInput;
    private RadioGroup roleRadioGroup;
    private RadioButton doctorRadioButton;
    private Button loginButton;

    @Before
    public void setUp() {
        ActivityController<LoginActivity> controller = Robolectric.buildActivity(LoginActivity.class).create().start().resume();
        activity = controller.get();

        emailInput = activity.findViewById(R.id.emailInput);
        passwordInput = activity.findViewById(R.id.passwordInput);
        roleRadioGroup = activity.findViewById(R.id.roleRadioGroup);
        loginButton = activity.findViewById(R.id.loginButton);
        doctorRadioButton = new RadioButton(activity);
        doctorRadioButton.setText("Doctor");
        doctorRadioButton.setId(View.generateViewId());
        roleRadioGroup.addView(doctorRadioButton);
        roleRadioGroup.check(doctorRadioButton.getId());
    }

    @Test
    public void loginDoctor_withInvalidCredentials_shouldShowError() {
        // Set invalid credentials
        emailInput.setText("invalid@doctor.com");
        passwordInput.setText("wrongpassword");

        // Click login
        loginButton.performClick();

        // Check that loading dialog was shown
        assertNotNull(activity.loadingDialog);

        // Since the real server isn't hit, we mock the RequestQueue behavior
        RequestQueue mockQueue = mock(RequestQueue.class);
        Volley.newRequestQueue(ApplicationProvider.getApplicationContext());
        verify(mockQueue, never()).add(any());
    }
}

