package com.dev.surya.texter;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;


/**
 * A simple {@link Fragment} subclass.
 */
public class RequestFragment extends Fragment {

    private View requestFragmentView;
    private RecyclerView myRequestList;

    private DatabaseReference chatRequestRef, usersRef, contactsRef;
    private FirebaseAuth mAuth;

    private String currentUserID;

    public RequestFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        requestFragmentView =  inflater.inflate(R.layout.fragment_request, container, false);

        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();

        chatRequestRef = FirebaseDatabase.getInstance().getReference().child("Chat Requests");
        usersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        contactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts");

        myRequestList = requestFragmentView.findViewById(R.id.chat_requests_list);
        myRequestList.setLayoutManager(new LinearLayoutManager(getContext()));

        return requestFragmentView;
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseRecyclerOptions<Contacts> options = new FirebaseRecyclerOptions.Builder<Contacts>()
                .setQuery(chatRequestRef.child(currentUserID), Contacts.class)
                .build();

        FirebaseRecyclerAdapter<Contacts, RequestViewHolder> adapter = new FirebaseRecyclerAdapter<Contacts, RequestViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final RequestViewHolder holder, int position, @NonNull Contacts model) {
                holder.itemView.findViewById(R.id.request_accept_btn).setVisibility(View.VISIBLE);
                holder.itemView.findViewById(R.id.request_decline_btn).setVisibility(View.VISIBLE);

               final String listUserID = getRef(position).getKey();
               DatabaseReference getTypeRef = getRef(position).child("request_type").getRef();

               getTypeRef.addValueEventListener(new ValueEventListener() {
                   @Override
                   public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                       if(dataSnapshot.exists()){
                           String type = dataSnapshot.getValue().toString();

                           if(type.equals("received")){
                               usersRef.child(listUserID).addValueEventListener(new ValueEventListener() {
                                   @Override
                                   public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                       if(dataSnapshot.hasChild("image")){

                                           final String requestProfileImage = dataSnapshot.child("image").getValue().toString();
                                           Picasso.get().load(requestProfileImage)
                                                   .placeholder(R.drawable.profile_image).into(holder.profileImage);

                                       }
                                           final String requestUserName = dataSnapshot.child("name").getValue().toString();

                                           holder.userName.setText(requestUserName);
                                           holder.userStatus.setText("Wants to connect with you");

                                           holder.acceptButton.setOnClickListener(new View.OnClickListener() {
                                               @Override
                                               public void onClick(View v) {
                                                   contactsRef.child(currentUserID).child(listUserID).child("Contact")
                                                           .setValue("Saved").addOnCompleteListener(new OnCompleteListener<Void>() {
                                                       @Override
                                                       public void onComplete(@NonNull Task<Void> task) {
                                                           if(task.isSuccessful()){
                                                               contactsRef.child(listUserID).child(currentUserID).child("Contact")
                                                                       .setValue("Saved").addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                   @Override
                                                                   public void onComplete(@NonNull Task<Void> task) {
                                                                       if(task.isSuccessful()){
                                                                           chatRequestRef.child(currentUserID).child(listUserID)
                                                                                   .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {

                                                                               @Override
                                                                               public void onComplete(@NonNull Task<Void> task) {
                                                                                   if(task.isSuccessful()) {
                                                                                       chatRequestRef.child(listUserID).child(currentUserID)
                                                                                               .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                           @Override
                                                                                           public void onComplete(@NonNull Task<Void> task) {
                                                                                               Toast.makeText(getContext(),"Contact Saved", Toast.LENGTH_SHORT).show();
                                                                                           }
                                                                                       });
                                                                                   }
                                                                               }
                                                                           });
                                                                       }
                                                                   }
                                                               });
                                                           }
                                                       }
                                                   });
                                               }
                                           });

                                           holder.declineButton.setOnClickListener(new View.OnClickListener() {
                                               @Override
                                               public void onClick(View v) {
                                                   chatRequestRef.child(currentUserID).child(listUserID)
                                                           .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {

                                                       @Override
                                                       public void onComplete(@NonNull Task<Void> task) {
                                                           if(task.isSuccessful()) {
                                                               chatRequestRef.child(listUserID).child(currentUserID)
                                                                       .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                   @Override
                                                                   public void onComplete(@NonNull Task<Void> task) {
                                                                       Toast.makeText(getContext(),"Request Declined", Toast.LENGTH_SHORT).show();
                                                                   }
                                                               });
                                                           }
                                                       }
                                                   });
                                               }
                                           });

                                   }

                                   @Override
                                   public void onCancelled(@NonNull DatabaseError databaseError) {

                                   }
                               });
                           }
                           if(type.equals("sent")){

                               usersRef.child(listUserID).addValueEventListener(new ValueEventListener() {
                                   @Override
                                   public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                       if(dataSnapshot.hasChild("image")){

                                           final String requestProfileImage = dataSnapshot.child("image").getValue().toString();
                                           Picasso.get().load(requestProfileImage)
                                                   .placeholder(R.drawable.profile_image).into(holder.profileImage);

                                       }
                                       final String requestUserName = dataSnapshot.child("name").getValue().toString();

                                       holder.userName.setText(requestUserName);
                                       holder.userStatus.setText("Request Pending");
                                       holder.acceptButton.setVisibility(View.INVISIBLE);
                                       holder.declineButton.setVisibility(View.INVISIBLE);

                                   }

                                   @Override
                                   public void onCancelled(@NonNull DatabaseError databaseError) {

                                   }
                               });
                           }

                       }
                   }

                   @Override
                   public void onCancelled(@NonNull DatabaseError databaseError) {

                   }
               });

            }

            @NonNull
            @Override
            public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

                View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.users_display_layout, viewGroup, false);
                RequestViewHolder holder = new RequestViewHolder(view);
                return holder;
            }
        };

        myRequestList.setAdapter(adapter);
        adapter.startListening();

    }

    public static class RequestViewHolder extends RecyclerView.ViewHolder {

        TextView userName, userStatus;
        CircleImageView profileImage;
        Button acceptButton, declineButton;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);

            userName = itemView.findViewById(R.id.user_profile_name);
            userStatus = itemView.findViewById(R.id.user_status);
            profileImage = itemView.findViewById(R.id.users_profile_image);
            acceptButton = itemView.findViewById(R.id.request_accept_btn);
            declineButton = itemView.findViewById(R.id.request_decline_btn);
        }
    }
}
