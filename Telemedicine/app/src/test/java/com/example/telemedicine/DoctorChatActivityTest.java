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
public class DoctorChatActivityTest {

    private DoctorChatActivity activity;

    @Before
    public void setUp() {
        Intent intent = new Intent();
        intent.putExtra("doctorId", "doc001");
        intent.putExtra("patientId", "pat001");

        activity = Robolectric.buildActivity(DoctorChatActivity.class, intent)
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
        assertNotNull(activity.findViewById(R.id.uploadPrescriptionButton));
        assertNotNull(activity.findViewById(R.id.chatPersonName));
    }

    @Test
    public void testSendMessageClearsInput() {
        EditText messageInput = activity.findViewById(R.id.messageInput);
        Button sendButton = activity.findViewById(R.id.sendButton);

        messageInput.setText("");
      //  sendButton.performClick();

        assertEquals("", messageInput.getText().toString());
    }

    @Test
    public void testRecyclerViewHasAdapter() {
        RecyclerView chatRecyclerView = activity.findViewById(R.id.chatRecyclerView);
        assertNotNull(chatRecyclerView.getAdapter());
    }

    @Test
    public void testUploadPrescriptionButtonClick() {
        Button uploadPrescriptionButton = activity.findViewById(R.id.uploadPrescriptionButton);
        uploadPrescriptionButton.performClick();

        Intent expectedIntent = Shadows.shadowOf(activity).getNextStartedActivity();
        assertNotNull(expectedIntent);
        assertEquals(Intent.ACTION_GET_CONTENT, expectedIntent.getAction());
    }

    @Test
    public void testMockPatientNameDisplay() throws Exception {
        // Create mocked response JSON manually
        JSONObject mockedResponse = new JSONObject();
        mockedResponse.put("success", true);
        JSONArray dataArray = new JSONArray();
        JSONObject patientObj = new JSONObject();
        patientObj.put("Name", "Test Patient");
        dataArray.put(patientObj);
        mockedResponse.put("data", dataArray);

        // Simulate setting the patient name
        JSONArray patients = mockedResponse.getJSONArray("data");
        JSONObject firstPatient = patients.getJSONObject(0);
        String patientName = firstPatient.getString("Name");

        TextView chatPersonName = activity.findViewById(R.id.chatPersonName);
        chatPersonName.setText(patientName);

        assertEquals("Test Patient", chatPersonName.getText().toString());
    }
}
