package com.example.telemedicine;

import static android.content.ContentValues.TAG;
import static java.lang.Integer.parseInt;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.telemedicine.adapter.PharmacyAdapter;
import com.example.telemedicine.model.Pharmacy;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class PharmacyListActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private PharmacyAdapter adapter;
    private List<Pharmacy> pharmacyList = new ArrayList<>();
    String base_url = BuildConfig.API_BASE_URL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pharmacy_list);

        recyclerView = findViewById(R.id.pharmacyRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new PharmacyAdapter(pharmacyList, this);
        recyclerView.setAdapter(adapter);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> onBackPressed());

        fetchPharmacies();
    }

    private void fetchPharmacies() {
        String url = base_url + "/patient/pharmacyList";
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            JSONArray dataArray = response.getJSONArray("pharmacies");
                            pharmacyList.clear();

                            for (int i = 0; i < dataArray.length(); i++) {
                                JSONObject pharmacyObject = dataArray.getJSONObject(i);
                                String id = String.valueOf(pharmacyObject.getInt("PharmacyID"));
                                String name = pharmacyObject.getString("Name");
                                String location = pharmacyObject.getString("Location");
                                String contactInfo = pharmacyObject.getString("ContactInfo");

                                pharmacyList.add(new Pharmacy(parseInt(id), name, location, contactInfo));
                            }

                            adapter.updateList(pharmacyList);
                        } else {
                            Toast.makeText(PharmacyListActivity.this, "Failed to fetch pharmacies", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(PharmacyListActivity.this, "Error parsing response", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e("PharmacyListActivity", "Error fetching pharmacies: " + error.getMessage());
                    Toast.makeText(PharmacyListActivity.this, "Network error. Please try again.", Toast.LENGTH_SHORT).show();
                });

        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                5000,
                0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        queue.add(jsonObjectRequest);
    }
}


