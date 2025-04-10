package com.example.telemedicine;

import static org.junit.Assert.*;

import android.content.Intent;
import android.os.Build;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = Build.VERSION_CODES.R)
public class PatientProfileActivityTest {

    private PatientProfileActivity activity;

    @Before
    public void setUp() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), PatientProfileActivity.class);
        ActivityController<PatientProfileActivity> controller = Robolectric.buildActivity(PatientProfileActivity.class, intent).create().start().resume();
        activity = controller.get();
    }

    @Test
    public void testInitialUIState() {
        Button btnEdit = activity.findViewById(R.id.btnEditProfile);
        assertNotNull(btnEdit);
        assertEquals("Edit Profile", btnEdit.getText().toString());

        EditText etName = activity.findViewById(R.id.etUsername);
        assertNotNull(etName);
        assertFalse(etName.isEnabled());
    }

    @Test
    public void testEnableEditMode() {
        Button btnEdit = activity.findViewById(R.id.btnEditProfile);
        btnEdit.performClick();

        assertEquals("Save Profile", btnEdit.getText().toString());

        EditText etName = activity.findViewById(R.id.etUsername);
        assertTrue(etName.isEnabled());
    }

    @Test
    public void testDisableEditMode() {
        Button btnEdit = activity.findViewById(R.id.btnEditProfile);
        btnEdit.performClick(); // Enable
        btnEdit.performClick(); // Save

        assertEquals("Edit Profile", btnEdit.getText().toString());

        EditText etName = activity.findViewById(R.id.etUsername);
        assertFalse(etName.isEnabled());
    }

    @Test
    public void testBackButtonExists() {
        ImageButton btnBack = activity.findViewById(R.id.btnBack);
        assertNotNull(btnBack);
    }
}
