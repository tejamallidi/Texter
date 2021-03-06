package com.dev.surya.texter;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.support.v7.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {

    private Button updateAccountSettings;
    private EditText userName, userStatus;
    public static CircleImageView userProfileImage;

    private FirebaseAuth mAuth;
    private String currentUserId;
    private DatabaseReference rootRef;

    private Toolbar settingsToolbar;

    private StorageReference userProfileImageRef;

    private ProgressDialog loadingBar;

    private static final int GALLERY_PICK = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();
        rootRef = FirebaseDatabase.getInstance().getReference();
        userProfileImageRef = FirebaseStorage.getInstance().getReference().child("Profile Images");

        initializeFields();

        userName.setVisibility(View.INVISIBLE);


        updateAccountSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateSettings();
            }
        });

        retrieveUserInfo();


        userProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent,GALLERY_PICK);
            }
        });
    }


    private void initializeFields() {
        updateAccountSettings = findViewById(R.id.update_settings_button);
        userName = findViewById(R.id.set_user_name);
        userStatus = findViewById(R.id.set_profile_status);
        userProfileImage = findViewById(R.id.set_profile_image);
        loadingBar = new ProgressDialog(this);
        settingsToolbar = findViewById(R.id.settings_toolbar);
        setSupportActionBar(settingsToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setTitle("Account Settings");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == GALLERY_PICK && resultCode == RESULT_OK && data != null) {
            loadingBar.setTitle("Set Profile Picture");
            loadingBar.setMessage("Please wait while profile image is updating");
            loadingBar.setCanceledOnTouchOutside(false);
            loadingBar.show();

            Uri imageUri = data.getData();
            // start picker to get image for cropping and then use the image in cropping activity
            CropImage.activity()
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setAspectRatio(1, 1)
                    .start(this);
        }
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                final Uri resultUri = result.getUri();
                final StorageReference filePath = userProfileImageRef.child(currentUserId + ".jpg");
                filePath.putFile(resultUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                final String downloadUrl = uri.toString();
                                rootRef.child("Users").child(currentUserId).child("image").setValue(downloadUrl)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if(task.isSuccessful()){
                                                    loadingBar.dismiss();
                                                } else {
                                                    loadingBar.dismiss();
                                                    String e = task.getException().toString();
                                                    Toast.makeText(SettingsActivity.this,"Error: "+e, Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                            }
                        });
                    }
                });
            }
        }
    }

    private void updateSettings() {

        String setUsername = userName.getText().toString();
        String setStatus = userStatus.getText().toString();

        if(TextUtils.isEmpty(setUsername))
            userName.setError("Username cannot be Empty");
        if(TextUtils.isEmpty(setStatus))
            userStatus.setError("Please feel free to update your status");
        else{
            HashMap<String, Object> profileMap = new HashMap<>();
            profileMap.put("uid",currentUserId);
            profileMap.put("name",setUsername);
            profileMap.put("status",setStatus);
            rootRef.child("Users").child(currentUserId).updateChildren(profileMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful()){
                        SendUserToMainActivity();
                        Toast.makeText(SettingsActivity.this,"Profile Updated Successfully", Toast.LENGTH_SHORT).show();
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(SettingsActivity.this, "Error: "+e.getLocalizedMessage(),Toast.LENGTH_SHORT).show();
                }
            });
        }
    }


    private void SendUserToMainActivity() {
        Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
        startActivity(intent);
    }

    private void retrieveUserInfo() {
        rootRef.child("Users").child(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if((dataSnapshot.exists()) && (dataSnapshot.hasChild("name")) && (dataSnapshot.hasChild("image"))){
                    UIUtil.hideKeyboard(SettingsActivity.this);
                    String retreiveUserName = dataSnapshot.child("name").getValue().toString();
                    String retreiveStatus = dataSnapshot.child("status").getValue().toString();
                    String retreiveProfileImage = dataSnapshot.child("image").getValue().toString();

                    userName.setText(retreiveUserName);
                    userStatus.setText(retreiveStatus);
                    Picasso.get().load(retreiveProfileImage).placeholder(R.drawable.profile_image).into(userProfileImage);

                } else if((dataSnapshot.exists()) && (dataSnapshot.hasChild("name"))){
                    UIUtil.hideKeyboard(SettingsActivity.this);
                    String retreiveUserName = dataSnapshot.child("name").getValue().toString();
                    String retreiveStatus = dataSnapshot.child("status").getValue().toString();

                    userName.setText(retreiveUserName);
                    userStatus.setText(retreiveStatus);

                } else {
                    userName.setVisibility(View.VISIBLE);
                    Toast.makeText(SettingsActivity.this, "Please set and update your profile information.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
