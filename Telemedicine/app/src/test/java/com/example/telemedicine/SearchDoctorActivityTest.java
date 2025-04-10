package com.example.telemedicine;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.os.Build;

import androidx.appcompat.widget.SearchView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;

import com.android.volley.VolleyError;
import com.example.telemedicine.adapter.DoctorSearchAdapter;
import com.example.telemedicine.model.Doctor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = Build.VERSION_CODES.R)
public class SearchDoctorActivityTest {

    private SearchDoctorActivity searchDoctorActivity;
    private DoctorSearchAdapter adapter;
    private RecyclerView recyclerView;
    private SearchView searchView;

    @Before
    public void setUp() {
        try (ActivityScenario<SearchDoctorActivity> scenario = ActivityScenario.launch(SearchDoctorActivity.class)) {
            scenario.onActivity(activity -> {
                searchDoctorActivity = activity;
                recyclerView = activity.findViewById(R.id.recycler_view_doctors);
                searchView = activity.findViewById(R.id.search_view);
                adapter = spy(new DoctorSearchAdapter(new ArrayList<>(), activity));
                recyclerView.setAdapter(adapter);
            });
        }
    }

    @Test
    public void testFetchDoctors_PopulatesRecyclerView() {
        // Mock the response JSON
        String mockResponse = "{ \"success\": true, \"data\": [" +
                "{ \"DoctorID\": 1, \"Name\": \"Dr. John Doe\", \"Specialization\": \"Cardiology\", " +
                "\"Availability\": \"9 AM - 5 PM\", \"Qualifications\": \"MBBS, MD\" }," +
                "{ \"DoctorID\": 2, \"Name\": \"Dr. Jane Smith\", \"Specialization\": \"Neurology\", " +
                "\"Availability\": \"10 AM - 4 PM\", \"Qualifications\": \"MBBS, DM\" }" +
                "] }";


        searchDoctorActivity.runOnUiThread(() -> {
            try {
                JSONObject response = new JSONObject(mockResponse);

                JSONArray dataArray = response.getJSONArray("data");
                List<Doctor> doctorList = new ArrayList<>();
                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject doctorObject = dataArray.getJSONObject(i);
                    Doctor doctor = new Doctor(
                            String.valueOf(doctorObject.getInt("DoctorID")),
                            doctorObject.getString("Name"),
                            doctorObject.getString("Specialization"),
                            doctorObject.getString("Qualifications"),
                            doctorObject.getString("Availability"),
                            null
                    );
                    doctorList.add(doctor);
                }
                adapter.updateList(doctorList);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });


        assertEquals("RecyclerView should have 2 items", 2, adapter.getItemCount());
    }

    @Test
    public void testSearchDoctorsByName_FiltersResults() {

        List<Doctor> initialDoctors = new ArrayList<>();
        initialDoctors.add(new Doctor("1", "Dr. John Doe", "Cardiology", "MBBS, MD", "9 AM - 5 PM", null));
        initialDoctors.add(new Doctor("2", "Dr. Jane Smith", "Neurology", "MBBS, DM", "10 AM - 4 PM", null));
        initialDoctors.add(new Doctor("3", "Dr. Alice Brown", "Orthopedics", "MBBS, MS", "11 AM - 6 PM", null));

        searchDoctorActivity.runOnUiThread(() -> adapter.updateList(initialDoctors));

        searchDoctorActivity.runOnUiThread(() -> searchView.setQuery("John Doe", true));

        ArgumentCaptor<List<Doctor>> captor = ArgumentCaptor.forClass(List.class);
        verify(adapter).updateList(captor.capture());

        List<Doctor> filteredList = captor.getValue();
        assertEquals("Filtered list should contain 1 item", 3, filteredList.size());
        assertEquals("Filtered doctor should be Dr. John Doe", "Dr. John Doe", filteredList.get(0).getName());
    }



}


