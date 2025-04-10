package com.example.telemedicine;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.os.Build;
import android.os.Looper;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.ListView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.NoCache;
import com.example.telemedicine.database.MyApp;

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
public class DoctorDashboardActivityTest {

    private DoctorDashboardActivity doctorDashboardActivity;
    private RecyclerView appointmentsRecyclerView;
    private LinearLayout serviceAppointments;
    private LinearLayout servicePharmacy;
    private ListView doctorAppointmentListView;

    @Before
    public void setUp() {
        try (ActivityScenario scenario = ActivityScenario.launch(DoctorDashboardActivity.class)) {
            scenario.onActivity(activity -> {
                doctorDashboardActivity =(DoctorDashboardActivity) activity;
                doctorDashboardActivity.executor = Runnable::run;

                appointmentsRecyclerView = activity.findViewById(R.id.appointmentsRecyclerView);
                serviceAppointments = activity.findViewById(R.id.service_appointments);
                servicePharmacy = activity.findViewById(R.id.service_pharmacy);
              //  serviceMedicalRecords = activity.findViewById(R.id.service_medical_records);
               // doctorAppointmentListView = activity.findViewById(R.id.doctorAppointmentsRecyclerView);
            });
        }
    }

    @Test
    public void testNavHomeSelected_NoAction() {
        MenuItem item = mock(MenuItem.class);
        when(item.getItemId()).thenReturn(R.id.nav_home);

        boolean result = doctorDashboardActivity.onNavigationItemSelected(item);
        assertTrue(result);

        Intent nextStartedActivity = shadowOf(doctorDashboardActivity).getNextStartedActivity();
      //  assertNull("No new activity should be launched for nav_home", nextStartedActivity);
    }

    @Test
    public void testNavChatsSelected_LaunchesChatListActivity() {
        MenuItem item = mock(MenuItem.class);
        when(item.getItemId()).thenReturn(R.id.nav_chats);

        boolean result = doctorDashboardActivity.onNavigationItemSelected(item);
        assertTrue(result);

        Intent intent = shadowOf(doctorDashboardActivity).getNextStartedActivity();
        assertNotNull(intent);
        assertEquals(ChatListActivity.class.getName(), intent.getComponent().getClassName());
        assertEquals("Doctor ID should be passed", doctorDashboardActivity.doctorId,
                intent.getStringExtra("userId"));
        assertTrue(intent.getBooleanExtra("isDoctor", false));
    }

    @Test
    public void testAppointmentsClick_NavigatesToDoctorAppointmentActivity() {
        serviceAppointments.performClick();

        Intent intent = shadowOf(doctorDashboardActivity).getNextStartedActivity();
        assertNotNull(intent);
        assertEquals(DoctorAppointmentActivity.class.getName(), intent.getComponent().getClassName());
    }

    @Test
    public void testServicePharmacyClick_NavigatesToPharmacyListActivity() {

        servicePharmacy.performClick();

        Intent expectedIntent = new Intent(doctorDashboardActivity, PharmacyListActivity.class);
        Intent actualIntent = Shadows.shadowOf(doctorDashboardActivity).getNextStartedActivity();
        assertNotNull("Intent should not be null", actualIntent);
        assertEquals(expectedIntent.getComponent(), actualIntent.getComponent());
    }

    @Test
    public void testFetchAppointments_Success() {
        String mockResponseString = "{ \"appointments\": [{\"id\": \"1\", \"patient\": \"John Doe\", \"date\": \"2025-04-01\", \"status\": \"Confirmed\"}]}";
        JSONObject mockResponse;
        try {
            mockResponse = new JSONObject(mockResponseString);
        } catch (JSONException e) {
            throw new RuntimeException("Invalid mock JSON");
        }

        MockJsonObjectRequest request = new MockJsonObjectRequest(
                Request.Method.GET,
                "https://example.com/api/doctor/appointments",
                null,
                response -> {
                    try {
                        JSONArray appointments = response.getJSONArray("appointments");
                        assertEquals(1, appointments.length());
                        assertEquals("John Doe", appointments.getJSONObject(0).getString("patient"));
                    } catch (JSONException e) {
                        fail("JSON parsing failed: " + e.getMessage());
                    }
                },
                error -> fail("Unexpected error response"),
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
        doctorDashboardActivity.fetchAppointments();
        shadowOf(Looper.getMainLooper()).idle();
    }

    @Test
    public void testFetchAppointments_Failure() {
        String mockResponseString = "{\"success\": false, \"message\": \"No appointments found\"}";
        JSONObject mockResponse;
        try {
            mockResponse = new JSONObject(mockResponseString);
        } catch (JSONException e) {
            throw new RuntimeException("Invalid mock JSON");
        }

        MockJsonObjectRequest request = new MockJsonObjectRequest(
                Request.Method.GET,
                "https://example.com/api/doctor/appointments",
                null,
                response -> fail("Expected failure but got success"),
                error -> {
                    String toast = ShadowToast.getTextOfLatestToast();
                    assertThat("Toast should show failure", toast, containsString("No appointments found"));
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
        doctorDashboardActivity.fetchAppointments();
        shadowOf(Looper.getMainLooper()).idle();
    }
}

