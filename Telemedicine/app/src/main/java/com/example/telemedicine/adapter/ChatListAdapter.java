package com.example.telemedicine.adapter;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.telemedicine.BuildConfig;
import com.example.telemedicine.ChatListActivity;
import com.example.telemedicine.DoctorChatActivity;
import com.example.telemedicine.PatientChatActivity;
import com.example.telemedicine.R;
import com.example.telemedicine.model.Chat;
import com.example.telemedicine.model.Doctor;

import java.util.List;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ViewHolder> {
    private List<Chat> chatList;
    private Context context;
    String base_url = BuildConfig.API_BASE_URL;

    public ChatListAdapter(Context context, List<Chat> chatList) {
        this.context = context;
        this.chatList = chatList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Chat chat = chatList.get(position);
        holder.nameTextView.setText(chat.getName());
        holder.lastMessageTextView.setText("Tap to view conversation");

        holder.itemView.setOnClickListener(v -> {
            Intent intent;
            if(chat.getIsDoctor()){
                intent = new Intent(context, DoctorChatActivity.class);
                Log.d("ChatListAdapter", "Doctor chat opening " + chat.getChatId() + chat.getUserId());
                intent.putExtra("doctorId", chat.getChatId());
                intent.putExtra("patientId", chat.getUserId());
            } else {
                intent = new Intent(context, PatientChatActivity.class);
                Log.d("ChatListAdapter", "Patient chat opening" + chat.getChatId() + chat.getUserId());
                intent.putExtra("patientId", chat.getChatId());
                intent.putExtra("doctorId", chat.getUserId());
            }
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage;
        TextView nameTextView;
        TextView lastMessageTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profileImage);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            lastMessageTextView = itemView.findViewById(R.id.lastMessageTextView);
        }
    }

    public void updateList(List<Chat> newList) {
        chatList.clear();
        chatList.addAll(newList);
        notifyDataSetChanged();
    }
}


