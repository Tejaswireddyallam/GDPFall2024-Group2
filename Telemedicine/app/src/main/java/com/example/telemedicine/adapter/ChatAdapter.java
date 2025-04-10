package com.example.telemedicine.adapter;

import android.animation.ObjectAnimator;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.telemedicine.BuildConfig;
import com.example.telemedicine.R;
import com.example.telemedicine.model.ChatMessage;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import java.text.SimpleDateFormat;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    private List<ChatMessage> chatList;
    private String currentUserId;
    String base_url = BuildConfig.API_BASE_URL;


    public ChatAdapter(List<ChatMessage> chatList, String currentUserId) {
        this.chatList = chatList;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                viewType == 1 ? R.layout.item_chat_sent : R.layout.item_chat_received, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage message = chatList.get(position);

        if (message.getFileUrl() != null && !message.getFileUrl().isEmpty()) {

            holder.messageText.setVisibility(View.GONE);
            holder.fileLayout.setVisibility(View.VISIBLE);

            String fileUrl = message.getFileUrl();
            String fileName = getFileNameFromUrl(fileUrl, message.getTimestamp());
            holder.fileName.setText(fileName);

            holder.fileLayout.setOnClickListener(v -> downloadFile(holder.itemView.getContext(), fileUrl, fileName));
        } else {

            holder.messageText.setVisibility(View.VISIBLE);
            holder.fileLayout.setVisibility(View.GONE);
            holder.messageText.setText(message.getMessage());
        }
    }

    private String getFileNameFromUrl(String fileUrl, long timestamp) {
        try {
            String urlWithoutQuery = fileUrl.split("\\?")[0];
            String decodedUrl = URLDecoder.decode(urlWithoutQuery, StandardCharsets.UTF_8.name());
            String fileName = decodedUrl.substring(decodedUrl.lastIndexOf('/') + 1);

            String fileExtension = "";
            int lastDotIndex = fileName.lastIndexOf('.');
            if (lastDotIndex != -1 && lastDotIndex < fileName.length() - 1) {
                fileExtension = fileName.substring(lastDotIndex + 1);
                fileName = fileName.substring(0, lastDotIndex);
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("HH-mm-MM-dd-yyyy");
            String dateStr = dateFormat.format(new Date(timestamp));

            return dateStr + "_" + fileName + (fileExtension.isEmpty() ? "" : "." + fileExtension);
        } catch (Exception e) {
            Log.e("TAG", "Error in getFileNameFromUrl: " + e.getMessage());
            return "unknown_file";
        }
    }



    @Override
    public int getItemViewType(int position) {
        ChatMessage message = chatList.get(position);
        return message.getSenderId().equals(currentUserId) ? 1 : 0;
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        LinearLayout fileLayout;
        TextView fileName, fileSize;
        ImageView fileIcon;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            fileLayout = itemView.findViewById(R.id.fileLayout);
            fileName = itemView.findViewById(R.id.fileName);
            fileSize = itemView.findViewById(R.id.fileSize);
            fileIcon = itemView.findViewById(R.id.fileIcon);
        }
    }

    private void downloadFile(Context context, String fileUrl, String fileName) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(fileUrl));
        request.setTitle(fileName);
        request.setDescription("Downloading...");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (downloadManager != null) {
            downloadManager.enqueue(request);
            Toast.makeText(context, "Downloading file...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Download Manager not available", Toast.LENGTH_SHORT).show();
        }
    }


}

