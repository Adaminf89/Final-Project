package com.example.adaminfiesto.droppit.Main;

import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.adaminfiesto.droppit.R;
import com.example.adaminfiesto.droppit.Utils.FirebaseMethods;
import com.example.adaminfiesto.droppit.Utils.UniversalImageLoader;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class NextActivity extends AppCompatActivity
{
    private static final String TAG = "NextActivity";

    //firebase
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference myRef;
    private FirebaseMethods mFirebaseMethods;
    private LatLng location;
    double longitude;
    double latitude;

    //widgets
    private EditText mCaption;
    private CheckBox mCheckBox;
    //vars
    private String mAppend = "file:/";
    private int imageCount = 0;
    private String imgUrl;
    String isPrivate;
    private Bitmap bitmap;
    private Intent intent;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_next);
        mFirebaseMethods = new FirebaseMethods(NextActivity.this);
        mCaption = (EditText) findViewById(R.id.caption);
        mCheckBox = (CheckBox)findViewById(R.id.privatebox);
        isPrivate = "false";
        mCheckBox.setText("This is Private");

//      LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter(String.valueOf(R.string.intent_location)));

        mCheckBox.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
               if(mCheckBox.isChecked())
               {
                   isPrivate = "true";
                   mCheckBox.setText("This is Public");
               }
               else
                   {
                       mCheckBox.setText("This is Private");
                       isPrivate = "false";
                   }
            }
        });

        getSharedData();
        setupFirebaseAuth();
        final TextView share = (TextView) findViewById(R.id.tvShare);

        share.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Log.d(TAG, "onClick: navigating to the final share screen.");

                //upload the image to firebase
                Toast.makeText(NextActivity.this, "Attempting to upload new photo", Toast.LENGTH_SHORT).show();
                //pop the backstack so the map will be refeshed for when it has to load again
                //NextActivity.this.getSupportFragmentManager().popBackStack("FragmentMap", FragmentManager.POP_BACK_STACK_INCLUSIVE);
                String caption = mCaption.getText().toString();
                Log.d(TAG, "onIsPrivate: "+isPrivate);

                if(location == null)
                {
                    Toast.makeText(NextActivity.this,"Something went wrong with your location check your service and try again", Toast.LENGTH_LONG).show();
                    return;
                }
                else
                    {
                        //uploading a userphoto or brandnew photo
                        if(intent.hasExtra(getString(R.string.selected_image)))
                        {
                            share.setClickable(false);
                            imgUrl = intent.getStringExtra(getString(R.string.selected_image));
                            mFirebaseMethods.uploadNewPhoto(getString(R.string.new_photo), caption, isPrivate, imageCount, imgUrl,null, Double.toString(latitude), Double.toString(longitude));
                        }
                        else if(intent.hasExtra(getString(R.string.selected_bitmap)))
                        {
                            share.setClickable(false);
                            bitmap = (Bitmap) intent.getParcelableExtra(getString(R.string.selected_bitmap));
                            mFirebaseMethods.uploadNewPhoto(getString(R.string.new_photo), caption, isPrivate, imageCount,null, bitmap, Double.toString(latitude), Double.toString(longitude));
                        }
                    }
            }
        });

        setImage();
        share.setClickable(true);
    }

    private void getSharedData()
    {
        String[] latlong;

        SharedPreferences sharedPreferences = getSharedPreferences("Test", Context.MODE_PRIVATE);

        latitude = (double) sharedPreferences.getFloat("lat",0);
        longitude = (double) sharedPreferences.getFloat("long",0);

        location = new LatLng(latitude,longitude);

    }


     // gets the image url from the incoming intent and displays the chosen image
    private void setImage()
    {
        intent = getIntent();

        ImageView image = (ImageView) findViewById(R.id.imageShare);

        if(intent.hasExtra(getString(R.string.selected_image)))
        {
            imgUrl = intent.getStringExtra(getString(R.string.selected_image));
            Log.d(TAG, "setImage: got new image url: " + imgUrl);
            UniversalImageLoader.setImage(imgUrl, image, null, mAppend);
        }
        else if(intent.hasExtra(getString(R.string.selected_bitmap)))
        {
            bitmap = (Bitmap) intent.getParcelableExtra(getString(R.string.selected_bitmap));
            Log.d(TAG, "setImage: got new bitmap");
            image.setImageBitmap(bitmap);
        }
    }

     /*
     ------------------------------------ Firebase ---------------------------------------------
     */

    private void setupFirebaseAuth()
    {
        Log.d(TAG, "setupFirebaseAuth: setting up firebase auth.");
        mAuth = FirebaseAuth.getInstance();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        myRef = mFirebaseDatabase.getReference();
        Log.d(TAG, "onDataChange: image count: " + imageCount);

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();


                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
                // ...
            }
        };

        myRef.addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {

                imageCount = mFirebaseMethods.getImageCount(dataSnapshot);
                Log.d(TAG, "onDataChange: image count: " + imageCount);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
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

}
