package com.example.telemedicine;

import android.content.Context;
import android.widget.Toast;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ActivityLogHelper {

    static String base_url = BuildConfig.API_BASE_URL;

    public static void submitLog(Context context, int userId, String description, String role) {
        String logDatetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        String url = base_url + "/public/addActivityLog";

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("userId", userId);
            jsonBody.put("logDatetime", logDatetime);
            jsonBody.put("description", description);
            jsonBody.put("role", role);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(context, "Failed to build log request", Toast.LENGTH_SHORT).show();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, jsonBody,
                response -> {
                    try {
                        String message = response.getString("message");
                        Toast.makeText(context, "Log submitted: " + message, Toast.LENGTH_SHORT).show();
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(context, "Error in response", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    error.printStackTrace();
                    Toast.makeText(context, "Failed to submit log", Toast.LENGTH_SHORT).show();
                }) {

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                5000,
                0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        RequestQueue queue = Volley.newRequestQueue(context);
        queue.add(request);
    }
}

