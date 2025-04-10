package com.example.telemedicine;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.telemedicine.adapter.DoctorSearchAdapter;
import com.example.telemedicine.model.Doctor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SearchDoctorActivity extends AppCompatActivity {

    private static final String TAG = "SearchDoctorActivity";
    private DoctorSearchAdapter adapter;
    private List<Doctor> doctorList = new ArrayList<>();
    private String patientId;
    String base_url = BuildConfig.API_BASE_URL;
    String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_doctor);

        // Get patientId from intent
        Intent getIntent = getIntent();
        patientId = getIntent.getStringExtra("patientId");

        // Back button functionality
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> onBackPressed());

        // Set up RecyclerView
        RecyclerView recyclerView = findViewById(R.id.recycler_view_doctors);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DoctorSearchAdapter(doctorList, this);
        recyclerView.setAdapter(adapter);

        // Set up SearchView
        SearchView searchView = findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterDoctors(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterDoctors(newText);
                return true;
            }

            private void filterDoctors(String query) {
                List<Doctor> filteredList = new ArrayList<>();
                for (Doctor doctor : doctorList) {
                    if (doctor.getName().toLowerCase().contains(query.toLowerCase()) ||
                            doctor.getSpecialization().toLowerCase().contains(query.toLowerCase())) {
                        filteredList.add(doctor);
                    }
                }
                adapter.updateList(filteredList);
            }
        });

        fetchDoctors();
    }

    private void fetchDoctors() {
        String url = base_url + "/patient/getDoctors";
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if (response.getBoolean("success")) {
                                JSONArray dataArray = response.getJSONArray("data");
                                doctorList.clear();

                                for (int i = 0; i < dataArray.length(); i++) {
                                    JSONObject doctorObject = dataArray.getJSONObject(i);

                                    // Parse each doctor's details
                                    String id = String.valueOf(doctorObject.getInt("DoctorID"));
                                    String name = doctorObject.getString("Name");
                                    String specialization = doctorObject.getString("Specialization");
                                    String availability = doctorObject.getString("Availability");
                                    String qualification = doctorObject.getString("Qualifications");

                                    Doctor doctor = new Doctor(id, name, specialization, qualification, availability, null);
                                    doctorList.add(doctor);
                                }

                                adapter.updateList(doctorList);

                            } else {
                                Toast.makeText(SearchDoctorActivity.this, "Failed to fetch doctors", Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(SearchDoctorActivity.this, "Error parsing response", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Error fetching doctors: " + error.getMessage());
                        Toast.makeText(SearchDoctorActivity.this, "Network error. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });

        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                5000,
                0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        queue.add(jsonObjectRequest);
    }
}
