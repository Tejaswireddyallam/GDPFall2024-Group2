package com.example.telemedicine;

import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

public class MockJsonObjectRequest extends JsonObjectRequest {

    private final JSONObject mockResponse;

    public MockJsonObjectRequest(int method, String url, JSONObject jsonRequest,
                                 Response.Listener<JSONObject> listener,
                                 Response.ErrorListener errorListener,
                                 JSONObject mockResponse) {
        super(method, url, jsonRequest, listener, errorListener);
        this.mockResponse = mockResponse;
    }

    @Override
    protected void deliverResponse(JSONObject response) {
        super.deliverResponse(mockResponse); // Use the mocked response
    }
}
