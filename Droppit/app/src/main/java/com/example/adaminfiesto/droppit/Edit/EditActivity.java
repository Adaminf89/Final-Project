package com.example.adaminfiesto.droppit.Edit;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.adaminfiesto.droppit.DataModels.UserAccountSettings;
import com.example.adaminfiesto.droppit.DataModels.UserSettings;
import com.example.adaminfiesto.droppit.Detail.DetailActivity;
import com.example.adaminfiesto.droppit.R;
import com.example.adaminfiesto.droppit.UserProfile.ProfileActivity;
import com.example.adaminfiesto.droppit.Utils.FirebaseMethods;
import com.example.adaminfiesto.droppit.Utils.UniversalImageLoader;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import de.hdodenhof.circleimageview.CircleImageView;

public class EditActivity extends AppCompatActivity
{

    //firebase
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference myRef;
    private FirebaseMethods mFirebaseMethods;


    private Context mContext = EditActivity.this;
    private CircleImageView imageView;
    private EditText editCaption;
    private Button saveBtn;
    private TextView mChangeProfilePhoto;
    private String TAG;
    private String imageUrl;
    private String picID;
    private Bitmap clasBitmap;
    private static final int CAMERA_REQUEST_CODE = 6;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        imageView = findViewById(R.id.edit_profile_photo);
        mChangeProfilePhoto = findViewById(R.id.changeProfilePhoto);
        editCaption = findViewById(R.id.edit_caption);
        saveBtn = findViewById(R.id.save_btn);
        setupFirebaseAuth();
        setProfileWidgets();

    }

    private void setProfileWidgets()
    {

        SharedPreferences sharedPreferences = getSharedPreferences("edit", Context.MODE_PRIVATE);
        imageUrl = sharedPreferences.getString("photo","");
        picID = sharedPreferences.getString("photoID", "");

        UniversalImageLoader.setImage(imageUrl, imageView, null, "");
        editCaption.setText(sharedPreferences.getString("caption",""));

        //TODO: Give user ability to change photo
        mChangeProfilePhoto.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Log.d(TAG, "onClick: changing profile photo");
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
            }
        });

        saveBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {

                if(clasBitmap == null)
                {
                    return;
                }
                else
                    {

                    FirebaseMethods firebaseMethods = new FirebaseMethods(mContext);
                    firebaseMethods.uploadNewPhoto("detail", null, null,
                            0, null, clasBitmap,
                            null, null, picID);

                    Intent backProfile = new Intent(mContext, DetailActivity.class);
                    backProfile.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(backProfile);
                }
            }

        });

    }

    private void setupFirebaseAuth()
    {
        Log.d(TAG, "setupFirebaseAuth: setting up firebase auth.");

        mAuth = FirebaseAuth.getInstance();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        myRef = mFirebaseDatabase.getReference();
        //userID = mAuth.getCurrentUser().getUid();

        mAuthListener = new FirebaseAuth.AuthStateListener()
        {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth)
            {

                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null)
                {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                }
                else
                {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }

            }
        };

        myRef.addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {

                //retrieve user information from the database
                //setProfileWidgets(mFirebaseMethods.getUserSettings(dataSnapshot));

                //retrieve images for the user in question
            }

            @Override
            public void onCancelled(DatabaseError databaseError)
            {

            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == CAMERA_REQUEST_CODE)
        {

            Log.d(TAG, "onActivityResult: done taking a photo.");
            Log.d(TAG, "onActivityResult: attempting to navigate to final share screen.");

            try
            {
                Bitmap bitmap;
                bitmap = (Bitmap) data.getExtras().get("data");

                if(bitmap != null)
                {
                    //set the new profile picture
                    clasBitmap = bitmap;

                }

            }
            catch (NullPointerException e)
            {
                Log.d(TAG, "onActivityResult: NullPointerException: " + e.getMessage());
            }
        }
        else
        {
            return;
        }
    }

    @Override
    public void onStart()
    {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop()
    {
        super.onStop();
        if (mAuthListener != null)
        {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

    }

}
