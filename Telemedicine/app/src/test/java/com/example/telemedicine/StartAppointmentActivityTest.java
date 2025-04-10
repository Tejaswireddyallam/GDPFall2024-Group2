package com.example.telemedicine;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.widget.Button;
import android.widget.GridLayout;

import androidx.test.core.app.ApplicationProvider;

import com.android.volley.RequestQueue;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = Build.VERSION_CODES.R)
public class StartAppointmentActivityTest {

    private StartAppointmentActivity activity;

    @Before
    public void setUp() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), StartAppointmentActivity.class);
        intent.putExtra("doctor_id", "D001");
        intent.putExtra("availability", "10:00 AM - 2:00 PM");
        intent.putExtra("appointment_id", "A001");
        intent.putExtra("role", "patient");

        ActivityController<StartAppointmentActivity> controller =
                Robolectric.buildActivity(StartAppointmentActivity.class, intent).create().start().resume();
        activity = controller.get();
    }

    @Test
    public void testComponentsNotNull() {
        assertNotNull(activity.findViewById(R.id.btnBack));
        assertNotNull(activity.findViewById(R.id.gridTimeSlots));
        assertNotNull(activity.findViewById(R.id.calendarView));
        assertNotNull(activity.findViewById(R.id.btnConfirmAppointment));
    }

    @Test
    public void testHourSlotsAreDisplayed() {
        GridLayout gridLayout = activity.findViewById(R.id.gridTimeSlots);
        assertTrue(gridLayout.getChildCount() > 0);
    }

    @Test
    public void testTimeSlotSelectionChangesUI() {
        GridLayout gridLayout = activity.findViewById(R.id.gridTimeSlots);
        Button firstButton = (Button) gridLayout.getChildAt(0);
        assertNotNull(firstButton);

        firstButton.performClick();
        Button quarterButton = (Button) gridLayout.getChildAt(0);
        quarterButton.performClick();

        assertNotNull(activity.selectedTimeSlot);
        assertEquals(quarterButton.getText().toString(), activity.selectedTimeSlot);
    }

    @Test
    public void testConfirmAppointmentWithSelectionTriggersRequest() {
        activity.selectedDate = "10/4/2025";
        activity.selectedTimeSlot = "10 : 00 AM";

        Button btnConfirm = activity.findViewById(R.id.btnConfirmAppointment);
        btnConfirm.performClick();

        assertFalse(btnConfirm.isEnabled() || activity.loadingDialog == null);
    }

    @Test
    public void testRescheduleAppointmentLogicPath() {

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), StartAppointmentActivity.class);
        intent.putExtra("appointment_id", "A001");
        intent.putExtra("role", "doctor");

        ActivityController<StartAppointmentActivity> controller =
                Robolectric.buildActivity(StartAppointmentActivity.class, intent).create().start().resume();
        StartAppointmentActivity rescheduleActivity = controller.get();

        rescheduleActivity.selectedDate = "10/4/2025";
        rescheduleActivity.selectedTimeSlot = "10 : 50 AM";

        Button btnConfirm = rescheduleActivity.findViewById(R.id.btnConfirmAppointment);
        btnConfirm.performClick();

        assertFalse(btnConfirm.isEnabled() || rescheduleActivity.loadingDialog == null);
    }
}

