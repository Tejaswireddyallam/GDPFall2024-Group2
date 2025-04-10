package com.example.telemedicine;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.telemedicine.adapter.MedicalRecordsAdapter;
import com.example.telemedicine.model.MedicalRecord;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MedicalRecordsActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private MedicalRecordsAdapter adapter;
    private List<MedicalRecord> medicalRecordsList = new ArrayList<>();
    private String patientId, doctorId;
    private Boolean isDoctor;

    private static final int PICK_FILE_REQUEST = 1;
    private Uri fileUri;
    private StorageReference storageReference;
    private ProgressDialog progressDialog;
    Button uploadMedicalRecordButton;
    String base_url = BuildConfig.API_BASE_URL;
    ImageView backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medical_records);

        uploadMedicalRecordButton = findViewById(R.id.uploadMedicalRecordButton);

        recyclerView = findViewById(R.id.medicalRecordsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new MedicalRecordsAdapter(medicalRecordsList, this);
        recyclerView.setAdapter(adapter);

        isDoctor = getIntent().getBooleanExtra("isDoctor", false);
        if (isDoctor) {
            uploadMedicalRecordButton.setVisibility(View.GONE);
        } else {
            uploadMedicalRecordButton.setVisibility(View.VISIBLE);
        }
        patientId = getIntent().getStringExtra("patientId");
        fetchMedicalRecords();

        storageReference = FirebaseStorage.getInstance().getReference("medicalRecords");
        uploadMedicalRecordButton.setOnClickListener(v -> openFileChooser());

        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> onBackPressed());
    }

    private void fetchMedicalRecords() {
        String url = base_url + "/patient/medicalRecords/" + patientId;
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        boolean success = response.getBoolean("success");
                        if (!success) {
                            Toast.makeText(this, "Empty medical records...", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        JSONArray recordsArray = response.getJSONArray("medicalRecords");
                        Log.d("MedicalRecords", "Response: " + recordsArray.toString());
                        medicalRecordsList.clear();

                        for (int i = 0; i < recordsArray.length(); i++) {

                            JSONObject recordObject = recordsArray.getJSONObject(i);
                            String recordId = recordObject.getString("RecordID");
                            String patientId = recordObject.getString("PatientID");
                            String fileName = recordObject.getString("MedicalRecordName");
                            String fileUrl = recordObject.getString("FileUrl");
                            String date = recordObject.getString("UploadDate");

                            medicalRecordsList.add(new MedicalRecord(recordId, patientId, fileName, fileUrl, date));
                        }
                        Log.d("MedicalRecords", "Medical Records List: " + medicalRecordsList.toString());
                        adapter.updateList(medicalRecordsList, isDoctor);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }, error -> Log.e("MedicalRecords", "Error: " + error.getMessage()));

        queue.add(request);
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        String[] mimeTypes = {"image/*", "application/pdf", "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"};
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
            progressDialog = ProgressDialog.show(this, "Uploading", "Please wait...", true, false);
            String fileExtension = MimeTypeMap.getSingleton().getExtensionFromMimeType(getContentResolver().getType(fileUri));
            StorageReference fileRef = storageReference.child(System.currentTimeMillis() + "." + fileExtension);

            fileRef.putFile(fileUri).addOnSuccessListener(taskSnapshot ->
                    fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Uploading to Database", Toast.LENGTH_SHORT).show();
                        saveMedicalRecordToDatabase(uri.toString());
                    })).addOnFailureListener(e -> {
                progressDialog.dismiss();
                Toast.makeText(this, "Upload Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void saveMedicalRecordToDatabase(String fileUrl) {
        String apiUrl = BuildConfig.API_BASE_URL + "/patient/uploadMedicalRecord";
        RequestQueue queue = Volley.newRequestQueue(this);

        Map<String, String> params = new HashMap<>();
        params.put("patientId", patientId);
        params.put("timestamp", String.valueOf(System.currentTimeMillis()));
        params.put("medicalRecordName", fileUri.getLastPathSegment());
        params.put("fileUrl", fileUrl);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, apiUrl,
                new JSONObject(params), response -> {
            try {
                boolean success = response.getBoolean("success");
                if (success) {
                    Toast.makeText(this, "Medical record uploaded!", Toast.LENGTH_SHORT).show();
                    fetchMedicalRecords();
                    finish();
                } else {
                    Toast.makeText(this, "Failed to save record", Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }, error -> {
            Toast.makeText(this, "Network Error", Toast.LENGTH_SHORT).show();
            Log.e("MedicalRecordUpload", "Error: " + error.getMessage());
        });

        queue.add(jsonObjectRequest);
    }
}
