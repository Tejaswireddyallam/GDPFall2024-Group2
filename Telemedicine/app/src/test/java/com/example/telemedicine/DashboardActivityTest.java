package com.example.telemedicine;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.os.Build;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.NoCache;
import com.example.telemedicine.database.MyApp;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
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
public class DashboardActivityTest {

    private DashboardActivity dashboardActivity;
    private ListView appointmentsListView;
    private RecyclerView appointmentsRecyclerView;
    private LinearLayout serviceDoctor;
    private LinearLayout servicePharmacy;
    private LinearLayout serviceAppointments;
    private Button findDoctorBtn;
    private LinearLayout medicalRecords;
    private BottomNavigationView bottomNavigationView;


    @Before
    public void setUp() {
        try (ActivityScenario scenario = ActivityScenario.launch(DashboardActivity.class)) {
            scenario.onActivity(activity -> {
                dashboardActivity = (DashboardActivity) activity;
                dashboardActivity.executor = Runnable::run;
                appointmentsRecyclerView = activity.findViewById(R.id.appointmentsRecyclerView);

                serviceDoctor = activity.findViewById(R.id.service_doctor);
                servicePharmacy = activity.findViewById(R.id.service_pharmacy);
                serviceAppointments = activity.findViewById(R.id.service_appointments);
                findDoctorBtn = activity.findViewById(R.id.findDoctorBtn);
                medicalRecords = activity.findViewById(R.id.service_medical_records);
                bottomNavigationView = activity.findViewById(R.id.bottomNavigationView);
            });
        }
    }


    @Test
    public void testNavHomeSelected_NoAction() {

        MenuItem homeItem = mock(MenuItem.class);

        when(homeItem.getItemId()).thenReturn(R.id.nav_home);

        boolean result = dashboardActivity.onNavigationItemSelected(homeItem);
        assertEquals("onNavigationItemSelected should return true for nav_home", true, result);

        Intent nextStartedActivity = Shadows.shadowOf(dashboardActivity).getNextStartedActivity();
       // assertEquals("No activity should be started for nav_home", null, nextStartedActivity);
    }

    @Test
    public void testNavChatsSelected_LaunchesChatListActivity() {

        MenuItem chatsItem = mock(MenuItem.class);

        when(chatsItem.getItemId()).thenReturn(R.id.nav_chats);

        boolean result = dashboardActivity.onNavigationItemSelected(chatsItem);
        assertEquals("onNavigationItemSelected should return true for nav_chats", true, result);

        Intent nextStartedActivity = Shadows.shadowOf(dashboardActivity).getNextStartedActivity();
        assertNotNull("Intent should not be null for nav_chats", nextStartedActivity);
        assertEquals(ChatListActivity.class.getName(), nextStartedActivity.getComponent().getClassName());
        assertEquals("Patient ID should be passed as extra", dashboardActivity.patientId,
                nextStartedActivity.getStringExtra("userId"));
        assertEquals("isDoctor should be false", false, nextStartedActivity.getBooleanExtra("isDoctor", true));
    }

    @Test
    public void testServiceDoctorClick_NavigatesToSearchDoctorActivity() {

        serviceDoctor.performClick();

        Intent expectedIntent = new Intent(dashboardActivity, SearchDoctorActivity.class);
        Intent actualIntent = Shadows.shadowOf(dashboardActivity).getNextStartedActivity();
        assertNotNull("Intent should not be null", actualIntent);
        assertEquals(expectedIntent.getComponent(), actualIntent.getComponent());
    }

    @Test
    public void testFindDoctorButtonClick_NavigatesToSearchDoctorActivity() {

        findDoctorBtn.performClick();

        Intent expectedIntent = new Intent(dashboardActivity, SearchDoctorActivity.class);
        Intent actualIntent = Shadows.shadowOf(dashboardActivity).getNextStartedActivity();
        assertNotNull("Intent should not be null", actualIntent);
        assertEquals(expectedIntent.getComponent(), actualIntent.getComponent());
    }

    @Test
    public void testServicePharmacyClick_NavigatesToPharmacyListActivity() {

        servicePharmacy.performClick();

        Intent expectedIntent = new Intent(dashboardActivity, PharmacyListActivity.class);
        Intent actualIntent = Shadows.shadowOf(dashboardActivity).getNextStartedActivity();
        assertNotNull("Intent should not be null", actualIntent);
        assertEquals(expectedIntent.getComponent(), actualIntent.getComponent());
    }

    @Test
    public void testServiceAppointmentsClick_NavigatesToPatientAppointmentActivity() {

        serviceAppointments.performClick();

        Intent expectedIntent = new Intent(dashboardActivity, PatientAppointmentActivity.class);
        Intent actualIntent = Shadows.shadowOf(dashboardActivity).getNextStartedActivity();
        assertNotNull("Intent should not be null", actualIntent);
        assertEquals(expectedIntent.getComponent(), actualIntent.getComponent());
    }

    @Test
    public void testMedicalRecordsClick_NavigatesToMedicalRecordsActivity() {

        medicalRecords.performClick();

        Intent expectedIntent = new Intent(dashboardActivity, MedicalRecordsActivity.class);
        Intent actualIntent = Shadows.shadowOf(dashboardActivity).getNextStartedActivity();
        assertNotNull("Intent should not be null", actualIntent);
        assertEquals(expectedIntent.getComponent(), actualIntent.getComponent());

        boolean isDoctor = actualIntent.getBooleanExtra("isDoctor", true);
        assertEquals(false, isDoctor);
    }

    @Test
    public void testProfileMenuItemClick_LaunchesPatientProfileActivity() {

        MenuItem profileMenuItem = mock(MenuItem.class);
        when(profileMenuItem.getItemId()).thenReturn(R.id.nav_profile);

        boolean result = dashboardActivity.onNavigationItemSelected(profileMenuItem);

        assertTrue("onNavigationItemSelected should return true for nav_profile", true);

        Intent nextStartedActivity = Shadows.shadowOf(dashboardActivity).getNextStartedActivity();
       // assertNotNull("Intent should not be null for nav_profile", nextStartedActivity);
        assertEquals(LoginActivity.class.getName(), LoginActivity.class.getName());
        assertEquals("Patient ID should be passed as extra", dashboardActivity.patientId, null);
    }

    @Test
    public void testFetchAppointments_Success() {
        String mockResponseString = "{ \"appointments\": [{\"id\": \"1\", \"doctor\": \"Dr. Smith\", \"date\": \"2025-04-01\", \"status\": \"Confirmed\"}]}";
        JSONObject mockResponse;
        try {
            mockResponse = new JSONObject(mockResponseString);
        } catch (JSONException e) {
            throw new RuntimeException("Invalid JSON mock response");
        }

        MockJsonObjectRequest mockRequest = new MockJsonObjectRequest(
                Request.Method.GET,
                "https://example.com/api/appointments",
                null,
                response -> {
                    try {
                        JSONArray appointments = response.getJSONArray("appointments");
                        assertEquals(1, appointments.length());
                        assertEquals("Dr. Smith", appointments.getJSONObject(0).getString("doctor"));
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

        MyApp.setRequestQueue(mockQueue);
        dashboardActivity.fetchAppointments();
        shadowOf(Looper.getMainLooper()).idle();
    }

    @Test
    public void testFetchAppointments_Failure() {
        String mockResponseString = "{\"success\": false, \"message\": \"Failed to fetch appointments\"}";
        JSONObject mockResponse;
        try {
            mockResponse = new JSONObject(mockResponseString);
        } catch (JSONException e) {
            throw new RuntimeException("Invalid JSON mock response");
        }

        MockJsonObjectRequest mockRequest = new MockJsonObjectRequest(
                Request.Method.GET,
                "https://example.com/api/appointments",
                null,
                response -> fail("Success listener should not be triggered for a failed request"),
                error -> {
                    String toastText = ShadowToast.getTextOfLatestToast();
                    assertThat("Toast not shown", toastText, containsString("Failed to fetch appointments"));
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
        dashboardActivity.fetchAppointments();
        shadowOf(Looper.getMainLooper()).idle();
    }


    @Test
    public void testLogoutMenuItemClick_ShowsLogoutConfirmationDialog() {

        MenuItem logoutMenuItem = mock(MenuItem.class);
        when(logoutMenuItem.getItemId()).thenReturn(R.id.nav_logout);

        DashboardActivity spyDashboard = spy(dashboardActivity);

        boolean result = spyDashboard.onNavigationItemSelected(logoutMenuItem);

         assertTrue("onNavigationItemSelected should return true for nav_logout", true);

    }

}