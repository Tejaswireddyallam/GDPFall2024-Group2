package com.example.telemedicine;

import com.android.volley.Network;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;

public class MockNetwork implements Network {
    @Override
    public NetworkResponse performRequest(Request<?> request) {
        return null; // Not needed for this mock
    }
}

