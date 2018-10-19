package com.example.adaminfiesto.droppit.Detail;

import android.app.FragmentManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.example.adaminfiesto.droppit.DataModels.Like;
import com.example.adaminfiesto.droppit.DataModels.Photo;
import com.example.adaminfiesto.droppit.DataModels.UserAccountSettings;
import com.example.adaminfiesto.droppit.DataModels.UserSettings;
import com.example.adaminfiesto.droppit.Main.HomeActivity;
import com.example.adaminfiesto.droppit.Main.NextActivity;
import com.example.adaminfiesto.droppit.R;
import com.example.adaminfiesto.droppit.Utils.FirebaseMethods;
import com.example.adaminfiesto.droppit.Utils.UniversalImageLoader;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;

public class DetailFragmentPublic extends Fragment
{
    private static final String TAG = "";
    Photo pData;
    TextView tvCaption;
    TextView tvDate;
    TextView tvDistance;
    TextView tvDropTitle;
    ImageView ivDropPhoto;
    ImageView ivProfilePhoto;
    ImageView ivNavBtn;
    Button deleteBtn;
    RatingBar rbar;
    Like thisLike;
    Integer starRating;
    FirebaseUser currentUser;

    private String Uuid;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference myRef;
    private FirebaseMethods mFirebaseMethods;


    public static DetailFragmentPublic newInstance(Photo pdata)
    {

        Bundle args = new Bundle();
        DetailFragmentPublic fragment = new DetailFragmentPublic();
        fragment.setArguments(args);
        args.putParcelable("Photo", pdata);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_drop_public, null);
        tvCaption = view.findViewById(R.id.caption);
        tvDate = view.findViewById(R.id.textDate);
        tvDropTitle = view.findViewById(R.id.dropName);
        tvDistance = view.findViewById(R.id.textDistance);
        ivDropPhoto = view.findViewById(R.id.event_image);
        ivProfilePhoto = view.findViewById(R.id.user_image);
        deleteBtn = view.findViewById(R.id.delete_btn);
        ivNavBtn = view.findViewById(R.id.navigationBtn);
        rbar = view.findViewById(R.id.ratingBar);
        mFirebaseMethods = new FirebaseMethods(getActivity());
        currentUser = mAuth.getInstance().getCurrentUser();
        Uuid = currentUser.getUid().toString();

        setupFirebaseAuth();

        if(getArguments() != null)
        {
            pData = (Photo) getArguments().getParcelable("Photo");
        }
        //show the btn if this drop matches the user id
        if(pData.getUser_id().equals(Uuid))
        {
            deleteBtn.setVisibility(View.VISIBLE);
        }

        deleteBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {

                myRef.child(getContext().getString(R.string.dbname_user_photos))
                        .child(Uuid)
                        .child(pData.getPhoto_id())
                        .removeValue();

                myRef.child(getContext().getString(R.string.dbname_photos))
                        .child(pData.getPhoto_id())
                        .removeValue();

                Intent intentHome = new Intent(getContext(), HomeActivity.class);
                intentHome.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                getContext().startActivity(intentHome);
                getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

            }
        });

        ivNavBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String uri = String.format(Locale.ENGLISH, "geo:%f,%f", Double.valueOf(pData.getLocation()),
                        Double.valueOf(pData.getLocationlong()));
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                startActivity(intent);
            }
        });

        rbar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser)
            {
                starRating = rbar.getNumStars();
                thisLike = new Like();
                thisLike.setRating(starRating);
                thisLike.setUser_id(Uuid);
                mFirebaseMethods.setLikesPhoto(thisLike, pData.getPhoto_id());
            }
        });


        return view;
    }

    private void setProfileWidgets(UserSettings userSettings)
    {

        Log.d(TAG, "setProfileWidgets: is the user nil ");
        UserAccountSettings settings = userSettings.getSettings();

        UniversalImageLoader.setImage(settings.getProfile_photo(), ivProfilePhoto,null,"");
        UniversalImageLoader.setImage(pData.getImage_path(), ivDropPhoto,null,"");
        tvDropTitle.setText("Drop Details");
        tvCaption.setText(pData.getCaption());
        tvDate.setText(pData.getDate_created());
        tvDistance.setText("loading...");

    }

    //since we need a specific user data rather than pushing it throughout the app we will just make a call to firebase to get that data
    //from the user profile node
    private void setupFirebaseAuth()
    {
        Log.d(TAG, "setupFirebaseAuth: setting up firebase auth.");
        mAuth = FirebaseAuth.getInstance();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        myRef = mFirebaseDatabase.getReference();

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
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
                // ...
            }
        };


        myRef.addValueEventListener(new ValueEventListener()
        {
            //GET the snapshot allowing us to READ OR WRITE TO THE DATABASE
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                //retrieve user information from the database
                setProfileWidgets(mFirebaseMethods.getUserSettings(dataSnapshot));
                //retrieve images for the user in question

            }
            @Override
            public void onCancelled(DatabaseError databaseError)
            {

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
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }


}
