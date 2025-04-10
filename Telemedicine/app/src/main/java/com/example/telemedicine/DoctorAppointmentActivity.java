package com.example.telemedicine;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.telemedicine.adapter.DoctorAppointmentsAdapter;
import com.example.telemedicine.model.Appointments;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DoctorAppointmentActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private DoctorAppointmentsAdapter adapter;
    private List<Appointments> appointmentsList = new ArrayList<>();
    String base_url = BuildConfig.API_BASE_URL;
    String doctorId;
    private static final String TAG = "DoctorAppointmentActivity";
    private static String patientId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_appointment);

        Intent getIntent = getIntent();
        doctorId = getIntent.getStringExtra("doctorId");

        recyclerView = findViewById(R.id.newAppointmentsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new DoctorAppointmentsAdapter(appointmentsList, this);
        recyclerView.setAdapter(adapter);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> onBackPressed());

        ImageButton btnRefresh = findViewById(R.id.btnRefresh);
        btnRefresh.setOnClickListener(v -> fetchAppointments());

        fetchAppointments();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        fetchAppointments();
        Log.d("Appt Refetched", "onRestart called, appointments refetched");
    }

    private void fetchAppointments() {
        String url = base_url +"/doctor/appointment/" + doctorId;

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
                                JSONArray appointmentsArray = response.getJSONArray("appointments");
                                Log.d(TAG, "Appointments: " + appointmentsArray.toString());
                                appointmentsList.clear();

                                for (int i = 0; i < appointmentsArray.length(); i++) {
                                    JSONObject appointmentObject = appointmentsArray.getJSONObject(i);

                                    String slotDate = appointmentObject.getString("SlotDate");
                                    String slotTime = appointmentObject.getString("SlotTime");
                                    String patientName = appointmentObject.getString("PatientName");
                                    String status = appointmentObject.getString("Status");
                                    String id = appointmentObject.getString("AppointmentID");
                                    //String doctorId = doctorId;
                                    String patientId = appointmentObject.getString("PatientID");

                                    Appointments appointment = new Appointments(slotDate, slotTime, patientName, id, status, patientId, doctorId);
                                    appointmentsList.add(appointment);
                                }

                                adapter.updateList(appointmentsList);

                            } else {
                                Toast.makeText(DoctorAppointmentActivity.this, "Empty appointments...", Toast.LENGTH_SHORT).show();
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(DoctorAppointmentActivity.this, "Error parsing response", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Error fetching appointments: " + error.getMessage());
                        Toast.makeText(DoctorAppointmentActivity.this, "Failed to fetch appointments. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                5000,
                0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        queue.add(jsonObjectRequest);
    }

}
