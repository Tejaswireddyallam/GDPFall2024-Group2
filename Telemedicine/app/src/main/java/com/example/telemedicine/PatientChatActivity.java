package com.example.telemedicine;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.example.telemedicine.adapter.ChatAdapter;
import com.example.telemedicine.model.ChatMessage;
import com.example.telemedicine.model.Doctor;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import android.webkit.MimeTypeMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PatientChatActivity extends AppCompatActivity {
    private EditText messageInput;
    private Button sendButton;
    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatList;
    private DatabaseReference chatRef;
    String patientId;
    String doctorId;

    private static final String TAG = "PatientChatActivity";
    private Button uploadMedicalRecordButton;
    ImageView backButton;
    TextView chatPersonName;
    private static final int PICK_FILE_REQUEST = 1;
    private Uri fileUri;
    private StorageReference storageReference;
    String base_url = BuildConfig.API_BASE_URL;
    String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_chat);

        Intent getIntent = getIntent();
        doctorId = getIntent.getStringExtra("doctorId");
        patientId = getIntent.getStringExtra("patientId");

        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        backButton = findViewById(R.id.backButton);
        chatPersonName = findViewById(R.id.chatPersonName);

        chatList = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatList, patientId);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        if (!isRunningTest()) {
            String chatId = doctorId + "_" + patientId;
            chatRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId);
            storageReference = FirebaseStorage.getInstance().getReference("medicalRecords");
            loadChatHistory();
        }

        sendButton.setOnClickListener(v -> sendMessage());

        uploadMedicalRecordButton = findViewById(R.id.uploadMedicalRecordButton);
        uploadMedicalRecordButton.setOnClickListener(v -> openFileChooser());


        backButton.setOnClickListener(v -> onBackPressed());
        fetchDoctors();
    }

    private boolean isRunningTest() {
        return Build.FINGERPRINT != null && Build.FINGERPRINT.contains("robolectric");
    }

    private void sendMessage() {
        String messageText = messageInput.getText().toString().trim();
        if (!messageText.isEmpty()) {
            String messageId = chatRef.push().getKey();
            ChatMessage message = new ChatMessage(patientId, doctorId, messageText, System.currentTimeMillis());

            assert messageId != null;
            chatRef.child(messageId).setValue(message);
            messageInput.setText("");
        }
    }

    private void loadChatHistory() {
        chatRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    ChatMessage message = data.getValue(ChatMessage.class);
                    chatList.add(message);
                }
                chatAdapter.notifyDataSetChanged();
                chatRecyclerView.scrollToPosition(chatList.size() - 1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        String[] mimeTypes = {"image/*", "application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityForResult(intent, PICK_FILE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            fileUri = data.getData();
            uploadFile();
        }
    }

    private void uploadFile() {
        if (fileUri != null) {
            String fileExtension = MimeTypeMap.getSingleton().getExtensionFromMimeType(getContentResolver().getType(fileUri));
            StorageReference fileRef = storageReference.child(System.currentTimeMillis() + "." + fileExtension);

            fileRef.putFile(fileUri)
                    .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        saveMedicalRecordToDatabase(uri.toString());
                    }))
                    .addOnFailureListener(e -> Toast.makeText(this, "Upload Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void saveMedicalRecordToDatabase(String fileUrl) {
        String messageId = chatRef.push().getKey();
        ChatMessage message = new ChatMessage(patientId, doctorId, "Medical Record", System.currentTimeMillis(), fileUrl);

        chatRef.child(messageId).setValue(message);
    }

    private void fetchDoctors() {
        String url = base_url + "/patient/getDoctor/"+doctorId;
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
                                JSONArray doctorsArray = response.getJSONArray("data");

                                if (doctorsArray.length() > 0) {
                                    JSONObject firstDoctor = doctorsArray.getJSONObject(0);
                                    String doctorName = "Dr. "+firstDoctor.getString("Name");

                                    chatPersonName.setText(doctorName);
                                } else {
                                    Toast.makeText(PatientChatActivity.this, "No doctors available", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(PatientChatActivity.this, "Failed to fetch doctors", Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(PatientChatActivity.this, "Error parsing response", Toast.LENGTH_SHORT).show();
                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Error fetching doctors: " + error.getMessage());
                        Toast.makeText(PatientChatActivity.this, "Network error. Please try again.", Toast.LENGTH_SHORT).show();
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
