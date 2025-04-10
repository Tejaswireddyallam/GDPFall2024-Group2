package com.example.telemedicine;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.ImageButton;

import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.example.telemedicine.adapter.PharmacyAdapter;
import com.example.telemedicine.model.Pharmacy;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;


import java.util.ArrayList;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = Build.VERSION_CODES.R)
public class PharmacyListActivityTest {

    private PharmacyListActivity activity;
    private RecyclerView recyclerView;

    @Before
    public void setUp() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        ActivityController<PharmacyListActivity> controller = Robolectric.buildActivity(PharmacyListActivity.class, intent).create().start().resume();
        activity = controller.get();
        recyclerView = activity.findViewById(R.id.pharmacyRecyclerView);
    }

    @Test
    public void testActivityNotNull() {
        assertNotNull(activity);
    }

    @Test
    public void testRecyclerViewExists() {
        assertNotNull(recyclerView);
    }

    @Test
    public void testBackButtonExists() {
        ImageButton backButton = activity.findViewById(R.id.btnBack);
        assertNotNull(backButton);
    }

    @Test
    public void testFetchPharmacyList() throws JSONException {
        // Mock JSON response
        JSONArray mockResponse = new JSONArray();

        JSONObject pharmacy1 = new JSONObject();
        pharmacy1.put("PharmacyID", "1");
        pharmacy1.put("Name", "Pharmacy One");
        pharmacy1.put("Location", "Downtown");
        pharmacy1.put("ContactInfo", "1234567890");

        JSONObject pharmacy2 = new JSONObject();
        pharmacy2.put("PharmacyID", "2");
        pharmacy2.put("Name", "Pharmacy Two");
        pharmacy2.put("Location", "Uptown");
        pharmacy2.put("ContactInfo", "9876543210");

        mockResponse.put(pharmacy1);
        mockResponse.put(pharmacy2);

        ArrayList<Pharmacy> mockPharmacyList = new ArrayList<>();
        for (int i = 0; i < mockResponse.length(); i++) {
            JSONObject obj = mockResponse.getJSONObject(i);
            Pharmacy pharmacy = new Pharmacy();
            pharmacy.setPharmacyID(Integer.parseInt(obj.getString("PharmacyID")));
            pharmacy.setName(obj.getString("Name"));
            pharmacy.setLocation(obj.getString("Location"));
            pharmacy.setContactInfo(obj.getString("ContactInfo"));
            mockPharmacyList.add(pharmacy);
        }

        Context context = activity;
        activity.runOnUiThread(() -> {
            PharmacyAdapter adapter = new PharmacyAdapter(mockPharmacyList, context);
            recyclerView.setAdapter(adapter);
        });

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertNotNull(recyclerView.getAdapter());
        assertEquals(2, recyclerView.getAdapter().getItemCount());
        assertTrue(recyclerView.getAdapter() instanceof PharmacyAdapter);
    }
}
