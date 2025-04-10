package com.example.telemedicine;

import static io.agora.rtc2.RtcEngine.*;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import io.agora.media.RtcTokenBuilder2;
import io.agora.media.RtcTokenBuilder2.Role;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.video.VideoCanvas;

public class VideoCallActivity extends AppCompatActivity {

    private static final String AGORA_APP_ID = "a0a1b37c9e4e433d85141df7b5d9c5b6";
    private static final String AGORA_APP_CERTIFICATE = "ba20091863c241618b6de6dad2aee5ed";
    private static String CHANNEL_NAME ;
    private static String TOKEN ;
    private static final int PERMISSION_REQ_ID = 22;
    String base_url = BuildConfig.API_BASE_URL;
    String appointmentId;

    private RtcEngine agoraEngine;
    private FrameLayout localVideoContainer, remoteVideoContainer;
    private Button endCallButton, muteButton, videoButton, switchCameraButton;
    private TextView callTimerTextView, personNameTextView;
    private ImageView profileIconImageView;

    private boolean isMuted = false;
    private boolean isVideoEnabled = true;
    private boolean isFrontCamera = true;
    private int callDuration = 0;
    private Handler timerHandler = new Handler();

    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onUserJoined(int uid, int elapsed) {
            runOnUiThread(() ->{
                Toast.makeText(VideoCallActivity.this, "User joined the call", Toast.LENGTH_SHORT).show();
                setupRemoteVideo(uid);
                startCallTimer();
            });
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            runOnUiThread(() ->{
                Toast.makeText(VideoCallActivity.this, "User left the call", Toast.LENGTH_SHORT).show();
                removeRemoteVideo();
            });
        }

        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            runOnUiThread(() -> Toast.makeText(VideoCallActivity.this, "Connected to call", Toast.LENGTH_SHORT).show());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_call);

        String name = getIntent().getStringExtra("name");
        String patientId = getIntent().getStringExtra("patientId");
        String doctorId = getIntent().getStringExtra("doctorId");
        appointmentId = getIntent().getStringExtra("appointment_id");
        CHANNEL_NAME = patientId + doctorId;
        Log.d("VideoCallActivity", "Channel name: " + CHANNEL_NAME);
        TOKEN = generateToken(CHANNEL_NAME);
        personNameTextView = findViewById(R.id.person_name);
        personNameTextView.setText(name);

        initUI();
        checkPermissions();
    }

    private void initUI() {
        localVideoContainer = findViewById(R.id.local_video_view_container);
        remoteVideoContainer = findViewById(R.id.remote_video_view_container);
        endCallButton = findViewById(R.id.endCallButton);
        callTimerTextView = findViewById(R.id.call_timer);
        personNameTextView = findViewById(R.id.person_name);
        profileIconImageView = findViewById(R.id.profile_icon);

        muteButton = findViewById(R.id.muteButton);
        videoButton = findViewById(R.id.videoButton);
        switchCameraButton = findViewById(R.id.switchCameraButton);

        endCallButton.setOnClickListener(v -> endCall());
        muteButton.setOnClickListener(v -> toggleMute());
        videoButton.setOnClickListener(v -> toggleVideo());
        switchCameraButton.setOnClickListener(v -> switchCamera());

      //  startCallTimer();
    }

    private String generateToken(String channelName) {
        RtcTokenBuilder2 tokenBuilder = new RtcTokenBuilder2();
        return tokenBuilder.buildTokenWithUid(AGORA_APP_ID, AGORA_APP_CERTIFICATE, channelName, 0,
                Role.ROLE_PUBLISHER, 3600, 3600);
    }

    private void checkPermissions() {
        String[] PERMISSIONS = {
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA
        };

        if (!hasPermissions(PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQ_ID);
        } else {
            initializeAndJoinChannel();
        }
    }

    private boolean hasPermissions(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_ID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeAndJoinChannel();
            } else {
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void initializeAndJoinChannel() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = getBaseContext();
            config.mAppId = AGORA_APP_ID;
            config.mEventHandler = mRtcEventHandler;
            agoraEngine = create(config);

            agoraEngine.enableVideo();
            agoraEngine.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION);

            setupLocalVideo();
            agoraEngine.startPreview();
            agoraEngine.joinChannel(TOKEN, CHANNEL_NAME, "", 0);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupLocalVideo() {
        SurfaceView localView = RtcEngine.CreateRendererView(getBaseContext());
        localVideoContainer.removeAllViews();
        localVideoContainer.addView(localView);
        agoraEngine.setupLocalVideo(new VideoCanvas(localView, VideoCanvas.RENDER_MODE_HIDDEN, 0));
    }

    private void setupRemoteVideo(int uid) {
        SurfaceView remoteView = RtcEngine.CreateRendererView(getBaseContext());
        remoteVideoContainer.removeAllViews();
        remoteVideoContainer.addView(remoteView);
        agoraEngine.setupRemoteVideo(new VideoCanvas(remoteView, VideoCanvas.RENDER_MODE_HIDDEN, uid));
    }

    private void removeRemoteVideo() {
        remoteVideoContainer.removeAllViews();
    }

    private void toggleMute() {
        isMuted = !isMuted;
        agoraEngine.muteLocalAudioStream(isMuted);
        muteButton.setBackgroundResource(isMuted ? R.drawable.ic_mic_off : R.drawable.ic_mic_on);
    }

    private void toggleVideo() {
        isVideoEnabled = !isVideoEnabled;
        agoraEngine.muteLocalVideoStream(!isVideoEnabled);
        videoButton.setBackgroundResource(isVideoEnabled ? R.drawable.ic_videocam_on : R.drawable.ic_videocam_off);
        localVideoContainer.setVisibility(isVideoEnabled ? View.VISIBLE : View.INVISIBLE);
    }

    private void switchCamera() {
        agoraEngine.switchCamera();
        isFrontCamera = !isFrontCamera;
        switchCameraButton.setBackgroundResource(isFrontCamera ? R.drawable.ic_camera_switch : R.drawable.ic_camera_switch);
    }

    private void endCall() {
        updateAppointment();
        if (agoraEngine != null) {
            agoraEngine.leaveChannel();
        }
        destroy();
        finish();
    }

    private void startCallTimer() {
        timerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                callDuration++;
                updateCallTimerText();
                timerHandler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    private void updateCallTimerText() {
        int minutes = callDuration / 60;
        int seconds = callDuration % 60;
        String timeString = String.format("%02d:%02d", minutes, seconds);
        callTimerTextView.setText(timeString);
    }

    private void updateAppointment() {
        Log.d("Appointment Update", "Updating appointment completed status");
        String status = "completed";
        try {
            JSONObject jsonParams = new JSONObject();
            jsonParams.put("appointmentID", appointmentId);
            jsonParams.put("status", status);

            String url = base_url + "/patient/appointment/update-status";

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.PUT,
                    url,
                    jsonParams,
                    response -> {
                        try {
                            Log.d("Response", response.toString());
                            boolean success = response.getBoolean("success");
                            if (success) {
                                Toast.makeText(this, "Appointment status updated!", Toast.LENGTH_SHORT).show();

                            } else {
                                Toast.makeText(this, "Error: " + response.getString("message"), Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Response Parsing Error!", Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> {
                        error.printStackTrace();
                        Log.d("Error", error.toString());
                        Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
            );


            request.setRetryPolicy(new DefaultRetryPolicy(
                    5000,
                    0,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));

            RequestQueue queue = Volley.newRequestQueue(this);
            queue.add(request);

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error creating JSON data!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
       // updateAppointment();
        super.onDestroy();
        timerHandler.removeCallbacksAndMessages(null);
        if (agoraEngine != null) {
            agoraEngine.leaveChannel();
        }
        destroy();
    }
}
