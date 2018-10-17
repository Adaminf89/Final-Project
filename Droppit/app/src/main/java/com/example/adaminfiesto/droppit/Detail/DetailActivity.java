package com.example.adaminfiesto.droppit.Detail;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;

import com.example.adaminfiesto.droppit.DataModels.Photo;
import com.example.adaminfiesto.droppit.Main.NextActivity;
import com.example.adaminfiesto.droppit.R;
import com.example.adaminfiesto.droppit.UserProfile.ProfileActivity;
import com.example.adaminfiesto.droppit.UserProfile.ProfileFragment;
import com.example.adaminfiesto.droppit.Utils.FirebaseMethods;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static android.support.constraint.Constraints.TAG;
import static com.example.adaminfiesto.droppit.R.layout.detail_activity;

public class DetailActivity extends AppCompatActivity
{
    private static final String TAG = "DetailActivity";
    //firebase
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference myRef;
    private FirebaseMethods mFirebaseMethods;
    private String photoID;
    Photo pdata;
    private int imageCount = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detail_activity);
        mFirebaseMethods = new FirebaseMethods(DetailActivity.this);

        getSharedData();
        setupFirebaseAuth();
        getPhotos();

    }

    private void getData()
    {
        Intent getIntent = getIntent();
        String checker = getIntent.getStringExtra(String.valueOf(R.string.to_detail));

        if(checker.equals("detail"))
        {

            DetailFragmentPrivate fragment = new DetailFragmentPrivate();
            FragmentTransaction transaction = DetailActivity.this.getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.container, fragment.newInstance(pdata));
            //transaction.addToBackStack(getString(R.string.profile_fragment));
            transaction.commit();
        }
    }

    private void getSharedData()
    {
        SharedPreferences sharedPreferences = getSharedPreferences("photoID", Context.MODE_PRIVATE);
        photoID = sharedPreferences.getString("photo","");
    }

    private void getPhotos()
    {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference.child(getString(R.string.dbname_photos)).child(photoID);
        System.out.println(photoID);

        query.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                if(dataSnapshot.exists())
                {

                    pdata = dataSnapshot.getValue(Photo.class);

                    System.out.println(pdata);

                    Log.d(TAG, "onDataChange: " + pdata.getCaption().toString());

                    getData();

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

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
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }
}
