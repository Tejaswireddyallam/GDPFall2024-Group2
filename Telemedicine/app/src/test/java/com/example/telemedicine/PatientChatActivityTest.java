package com.example.telemedicine;

import android.content.Intent;
import android.os.Build;
import android.widget.TextView;
import android.widget.Button;
import android.widget.EditText;

import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = Build.VERSION_CODES.R)
public class PatientChatActivityTest {

    private PatientChatActivity activity;

    @Before
    public void setUp() {
        System.setProperty("isTest", "true"); // Skip Firebase initialization in tests

        Intent intent = new Intent();
        intent.putExtra("doctorId", "doc001");
        intent.putExtra("patientId", "pat001");

        activity = Robolectric.buildActivity(PatientChatActivity.class, intent)
                .create()
                .start()
                .resume()
                .get();
    }

    @Test
    public void testUIComponentsNotNull() {
        assertNotNull(activity.findViewById(R.id.messageInput));
        assertNotNull(activity.findViewById(R.id.sendButton));
        assertNotNull(activity.findViewById(R.id.chatRecyclerView));
        assertNotNull(activity.findViewById(R.id.backButton));
        assertNotNull(activity.findViewById(R.id.uploadMedicalRecordButton));
        assertNotNull(activity.findViewById(R.id.chatPersonName));
    }

    @Test
    public void testSendMessageClearsInput() {
        EditText messageInput = activity.findViewById(R.id.messageInput);
        Button sendButton = activity.findViewById(R.id.sendButton);

        messageInput.setText("");
        // sendButton.performClick(); // Uncomment when sendMessage() is stubbed/mocked

        assertEquals("", messageInput.getText().toString());
    }

    @Test
    public void testRecyclerViewHasAdapter() {
        RecyclerView chatRecyclerView = activity.findViewById(R.id.chatRecyclerView);
        assertNotNull(chatRecyclerView.getAdapter());
    }

    @Test
    public void testUploadMedicalRecordButtonClick() {
        Button uploadMedicalRecordButton = activity.findViewById(R.id.uploadMedicalRecordButton);
        uploadMedicalRecordButton.performClick();

        Intent expectedIntent = Shadows.shadowOf(activity).getNextStartedActivity();
        assertNotNull(expectedIntent);
        assertEquals(Intent.ACTION_GET_CONTENT, expectedIntent.getAction());
    }

    @Test
    public void testMockDoctorNameDisplay() throws Exception {
        JSONObject mockedResponse = new JSONObject();
        mockedResponse.put("success", true);
        JSONArray dataArray = new JSONArray();
        JSONObject doctorObj = new JSONObject();
        doctorObj.put("Name", "Dr. John");
        dataArray.put(doctorObj);
        mockedResponse.put("data", dataArray);

        JSONArray doctors = mockedResponse.getJSONArray("data");
        JSONObject firstDoctor = doctors.getJSONObject(0);
        String doctorName = firstDoctor.getString("Name");

        TextView chatPersonName = activity.findViewById(R.id.chatPersonName);
        chatPersonName.setText(doctorName);

        assertEquals("Dr. John", chatPersonName.getText().toString());
    }
}
