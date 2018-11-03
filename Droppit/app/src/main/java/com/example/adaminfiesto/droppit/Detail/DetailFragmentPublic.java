package com.example.adaminfiesto.droppit.Detail;

import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.adaminfiesto.droppit.AR.ARActivity;
import com.example.adaminfiesto.droppit.DataModels.Like;
import com.example.adaminfiesto.droppit.DataModels.Photo;
import com.example.adaminfiesto.droppit.DataModels.UserAccountSettings;
import com.example.adaminfiesto.droppit.DataModels.UserSettings;
import com.example.adaminfiesto.droppit.Edit.EditActivity;
import com.example.adaminfiesto.droppit.Google.GoogleActivity;
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
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

public class DetailFragmentPublic extends Fragment
{
    private static final String TAG = "";
    Photo pData;
    TextView tvCaption;
    TextView tvDate;
    TextView tvDistance;
    TextView tvDropTitle;
    String userRating;
    CircleImageView ivDropPhoto;
    CircleImageView ivProfilePhoto;
    ImageView ivNavBtn;
    ImageView ivAR;
    Button deleteBtn;
    Button addBtn;
    Button editBtn;
    Button commentBtn;
    RatingBar rbar;
    Like thisLike;
    Like loadedLike;
    Integer starRating;
    private int checker = 0;
    FirebaseUser currentUser;

    private String Uuid;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference myRef;
    private FirebaseMethods mFirebaseMethods;


    public static DetailFragmentPublic newInstance(Photo pdata, Integer checker)
    {
        Bundle args = new Bundle();
        DetailFragmentPublic fragment = new DetailFragmentPublic();
        fragment.setArguments(args);
        args.putInt("c",checker);
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
        ivDropPhoto = (CircleImageView) view.findViewById(R.id.event_image);
        ivProfilePhoto = (CircleImageView) view.findViewById(R.id.images_public);
        ivAR = view.findViewById(R.id.ArImage);
        deleteBtn = view.findViewById(R.id.delete_btn);
        addBtn = view.findViewById(R.id.add_btn);
        editBtn = view.findViewById(R.id.edit_btn);
        commentBtn = view.findViewById(R.id.comment_btn);
        ivNavBtn = view.findViewById(R.id.navigationBtn);
        rbar = view.findViewById(R.id.ratingBar);
        mFirebaseMethods = new FirebaseMethods(getActivity());
        currentUser = mAuth.getInstance().getCurrentUser();
        Uuid = currentUser.getUid().toString();
        loadedLike = new Like();

        setupFirebaseAuth();


        if(getArguments() != null)
        {
            pData = (Photo) getArguments().getParcelable("Photo");
            checker = getArguments().getInt("c");

        }

        //show the btn if this drop matches the user id
        if(pData.getUser_id().equals(Uuid))
        {
            deleteBtn.setVisibility(View.VISIBLE);
            editBtn.setVisibility(View.VISIBLE);
            commentBtn.setVisibility(View.VISIBLE);

        }
        else
            {
                commentBtn.setVisibility(View.VISIBLE);
                addBtn.setVisibility(View.VISIBLE);
            }

            ivAR.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    SharedPreferences sharedPreferences = getActivity().getSharedPreferences("arID", Context.MODE_PRIVATE);
                    SharedPreferences.Editor ed = sharedPreferences.edit();
                    ed.putString("lat", pData.getLocation());
                    ed.putString("long",pData.getLocationlong());
                    ed.putString("caption", pData.getCaption());
                    ed.apply();

                    Intent intentHome = new Intent(getContext(), ARActivity.class);
                    intentHome.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    getContext().startActivity(intentHome);
                    getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                }
            });

        editBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                SharedPreferences sharedPreferences = getActivity().getSharedPreferences("edit", Context.MODE_PRIVATE);
                SharedPreferences.Editor ed = sharedPreferences.edit();
                ed.putString("UUID", pData.getUser_id());
                ed.putString("photo",pData.getImage_path());
                ed.putString("photoID", pData.getPhoto_id());
                ed.putString("caption", pData.getCaption());
                ed.apply();

                Intent intentEdit = new Intent(getContext(), EditActivity.class);
                intentEdit.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                getContext().startActivity(intentEdit);
                getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            }
        });

        commentBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intentCom = new Intent(getContext(), CommentActivity.class);
                intentCom.putExtra("ComData", pData);
                intentCom.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                getContext().startActivity(intentCom);
                getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            }
        });

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

                Intent intentHome = new Intent(getContext(), GoogleActivity.class);
                intentHome.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                getContext().startActivity(intentHome);
                getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

            }
        });

        addBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
               mFirebaseMethods.collectPhoto(pData);
            }
        });

        ivNavBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Uri gmmIntentUri = Uri.parse("google.navigation:q="+pData.getLocation()+","+pData.getLocationlong());
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                startActivity(mapIntent);
            }
        });

        rbar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser)
            {
                starRating = rbar.getNumStars();
                //set this drops rating buy uuid
                double d = rating;
                thisLike = new Like();
                thisLike.setRating(d);
                thisLike.setUser_id(Uuid);
                mFirebaseMethods.setLikesPhoto(thisLike, pData.getPhoto_id());
            }
        });

        getLikes(pData.getPhoto_id());

        return view;
    }



    public String getTimestampDifference(Photo photo)
    {
        Log.d(TAG, "getTimestampDifference: getting timestamp difference.");

        String difference = "";
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("US/Eastern"));//google 'android list of timezones'
        Date today = c.getTime();
        sdf.format(today);
        Date timestamp;

        final String photoTimestamp = photo.getDate_created();
        try
        {
            timestamp = sdf.parse(photoTimestamp);
            difference = String.valueOf(Math.round(((today.getTime() - timestamp.getTime()) / 1000 / 60 / 60 / 24 )));
        }catch (ParseException e){
            Log.e(TAG, "getTimestampDifference: ParseException: " + e.getMessage() );
            difference = "0";
        }
        return difference +" days ago";
    }

    private void setProfileWidgets(UserSettings userSettings, DataSnapshot dataSnapshot)
    {

        Log.d(TAG, "setProfileWidgets: is the user nil ");
        UserAccountSettings settings = userSettings.getSettings();

        String DropUserID  =  pData.getUser_id();
        //getting the users drop photo
        UserAccountSettings settings2 = mFirebaseMethods.getProfilePhoto(dataSnapshot, DropUserID);
        String image = settings2.getProfile_photo().toString();

        UniversalImageLoader.setImage(image, ivProfilePhoto,null,"");
        UniversalImageLoader.setImage(pData.getImage_path(), ivDropPhoto,null,"");
        tvDropTitle.setText("Drop Details");
        tvCaption.setText(pData.getCaption());
        tvDate.setText(getTimestampDifference(pData));
        tvDistance.setVisibility(View.GONE);

    }

    public void getLikes(String photoID)
    {

        Query q = myRef.child("Likes").child(currentUser.getUid()).child(photoID);

        q.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists())
                {
                    loadedLike = dataSnapshot.getValue(Like.class);

                    if(loadedLike == null)
                    {
                        return;
                    }
                    rbar.setRating(Float.valueOf(loadedLike.getRating().toString()));
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

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
                setProfileWidgets(mFirebaseMethods.getUserSettings(dataSnapshot), dataSnapshot);
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
