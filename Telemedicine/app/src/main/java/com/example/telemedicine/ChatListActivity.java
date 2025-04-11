package com.example.telemedicine;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import com.example.telemedicine.adapter.ChatListAdapter;
import com.example.telemedicine.database.MyApp;
import com.example.telemedicine.model.Doctor;
import com.example.telemedicine.model.Chat;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ChatListActivity extends AppCompatActivity {
    private static final String TAG = "ChatListActivity";
    private RecyclerView chatRecyclerView;
    private AlertDialog loadingDialog;
    private ChatListAdapter adapter;
    private List<Chat> chatList = new ArrayList<>();
    private String currentUserId;
    private boolean isDoctor;
    private DatabaseReference chatsRef;
    String base_url = BuildConfig.API_BASE_URL;
    BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_view);

        currentUserId = getIntent().getStringExtra("userId");
        if (currentUserId == null) {
            Toast.makeText(this, "Error: User ID not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        isDoctor = getIntent().getBooleanExtra("isDoctor", false);

        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatListAdapter(this, chatList);
        chatRecyclerView.setAdapter(adapter);

        chatsRef = FirebaseDatabase.getInstance().getReference("chats");
        setupLoadingDialog();
        fetchChats();

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.nav_chats);
        bottomNavigationView.setOnItemSelectedListener(this::onNavigationItemSelected);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNavigationView.setSelectedItemId(R.id.nav_chats);
       // setupLoadingDialog();
       // fetchChats();
    }

    private void fetchChats() {
        chatsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                chatList.clear();
                for (DataSnapshot chatSnapshot : dataSnapshot.getChildren()) {
                    String chatId = chatSnapshot.getKey();
                    if (chatId != null && chatId.contains(currentUserId)) {
                        String[] ids = chatId.split("_");
                        String otherId = ids[0].equals(currentUserId) ? ids[1] : ids[0];
                        Log.d(TAG, "Chat ID: " + chatId + ", Other ID: " + otherId);
                        fetchUserDetails(otherId, currentUserId);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error fetching chats: " + databaseError.getMessage());
            }
        });
    }

    private void fetchUserDetails(String userId, String currentUserId) {
        Log.d(TAG, "Fetching user details for user ID: " + userId);
        String url = isDoctor ? base_url + "/doctor/getPatient/" + userId : base_url + "/patient/getDoctor/" + userId;
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            JSONArray dataArray = response.getJSONArray("data");
                            if (dataArray.length() > 0) {
                                JSONObject userData = dataArray.getJSONObject(0);
                                String name = userData.getString("Name");
                                Chat chat = new Chat(currentUserId, name, userId, isDoctor);
                                chatList.add(chat);
                                adapter.notifyDataSetChanged();
                            }
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing user data: " + e.getMessage());
                    }
                },
                error -> Log.e(TAG, "Error fetching user details: " + error.getMessage())
        );
        Volley.newRequestQueue(this).add(jsonObjectRequest);
    }

    private boolean onNavigationItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.nav_home) {
            Intent intent = new Intent(ChatListActivity.this, isDoctor ? DoctorDashboardActivity.class : DashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.nav_chats) {
            return true;
        } else if (item.getItemId() == R.id.nav_settings) {
            showSettingsPopup(bottomNavigationView);
            return true;
        }
        return false;
    }

    public void showSettingsPopup(View anchorView) {
        View settingsItemView = findViewById(R.id.nav_settings);
        if (settingsItemView == null) return;

        PopupMenu popup = new PopupMenu(this, settingsItemView, android.view.Gravity.END);
        popup.getMenuInflater().inflate(R.menu.settings_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.nav_profile) {
                    // Toast.makeText(DashboardActivity.this, "Profile Clicked", Toast.LENGTH_SHORT).show();
                    if(isDoctor){
                        Intent profileIntent = new Intent(ChatListActivity.this, DoctorProfileActivity.class);
                        profileIntent.putExtra("doctorId", currentUserId);
                        startActivity(profileIntent);
                        return true;
                    } else {
                        Intent profileIntent = new Intent(ChatListActivity.this, PatientProfileActivity.class);
                        profileIntent.putExtra("patientId", currentUserId);
                        startActivity(profileIntent);
                    }

                    return true;
                } else if (id == R.id.nav_logout) {
                    //Toast.makeText(DashboardActivity.this, "Logout Clicked", Toast.LENGTH_SHORT).show();

                    showLogoutConfirmationDialog();
                    return true;
                } else if (id == R.id.nav_feedback) {
                    //Toast.makeText(DashboardActivity.this, "Feedback Clicked", Toast.LENGTH_SHORT).show();
                    showFeedbackPopup();
                    return true;
                }
                return false;
            }
        });

        popup.show();
    }

    private void showFeedbackPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(ChatListActivity.this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.feedback_popup, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        EditText feedbackInput = dialogView.findViewById(R.id.feedbackInput);
        Button submitButton = dialogView.findViewById(R.id.submitFeedbackBtn);

        submitButton.setOnClickListener(v -> {
            String feedback = feedbackInput.getText().toString().trim();
            if (!feedback.isEmpty()) {
                String role = isDoctor ? "doctor" : "patient";
                ActivityLogHelper.submitLog(ChatListActivity.this, Integer.parseInt(currentUserId), feedback, role);

                Toast.makeText(this, "Thank you for your feedback!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else {
                feedbackInput.setError("Please enter some feedback.");
            }
        });

        dialog.show();
    }

    public void showLogoutConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Logout Confirmation")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        performLogout();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void performLogout() {

        SharedPreferences sharedPreferences = getSharedPreferences("user", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        new Thread(() -> {
            MyApp.getAppDatabase().userDao().clearUserData();
        }).start();

        Intent intent = new Intent(ChatListActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupLoadingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.loading, null);
        builder.setView(dialogView);
        builder.setCancelable(false);
        loadingDialog = builder.create();
    }

}




