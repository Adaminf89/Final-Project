package com.example.adaminfiesto.droppit.UserProfile;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.adaminfiesto.droppit.DataModels.User;
import com.example.adaminfiesto.droppit.DataModels.UserAccountSettings;
import com.example.adaminfiesto.droppit.DataModels.UserSettings;
import com.example.adaminfiesto.droppit.Main.HomeActivity;
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

public class EditProfileFragment extends Fragment
{
    private static final String TAG = "EditProfileFragment";
    //firebase
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference myRef;
    private FirebaseMethods mFirebaseMethods;
    private String userID;
    //EditProfile Fragment widgets
    EditText mUsername, mDisplayname, mDescription;
    private TextView mChangeProfilePhoto;
    private ImageView mProfilePhoto;
    //vars
    private UserSettings mUserSettings;
    private static final int CAMERA_REQUEST_CODE = 6;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_edit_profile, container,false);
        mProfilePhoto = view.findViewById(R.id.edit_profile_photo);
        mUsername = view.findViewById(R.id.edit_username);
        mDisplayname = view.findViewById(R.id.edit_display_name);
        mDescription = view.findViewById(R.id.edit_description);
        mChangeProfilePhoto = view.findViewById(R.id.changeProfilePhoto);
        mFirebaseMethods = new FirebaseMethods(getActivity());


        ImageView checkmark = view.findViewById(R.id.saveChanges);

        checkmark.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Log.d(TAG, "onClick: attempting to save changes.");
                saveProfileSettings();
            }
        });

        setupFirebaseAuth();

        return view;
    }

    //con
    public EditProfileFragment()
    {
    }

    private void checkIfUsernameExists(final String username)
    {
        Log.d(TAG, "checkIfUsernameExists: Checking if  " + username + " already exists.");
        //query is faster to look through the db
        //since we know there should be a user at this point
        //we can query the dbuser then look and the username to compare if it matches the current username
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        //we have the string values to insure that this is not ever misspelled
        Query query = reference
                .child(getString(R.string.dbname_users))
                .orderByChild(getString(R.string.field_username))
                .equalTo(username);
        query.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {

                if(!dataSnapshot.exists())
                {
                    //add the username
                    mFirebaseMethods.updateUsername(username);
                    Toast.makeText(getActivity(), "saved username.", Toast.LENGTH_SHORT).show();

                }
                for(DataSnapshot singleSnapshot: dataSnapshot.getChildren())
                {
                    if (singleSnapshot.exists())
                    {
                        Log.d(TAG, "checkIfUsernameExists: FOUND A MATCH: " + singleSnapshot.getValue(User.class).getUsername());
                        Toast.makeText(getActivity(), "That username already exists.", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError)
            {

            }
        });
    }

    private void saveProfileSettings()
    {
        final String displayName = mDisplayname.getText().toString();
        final String username = mUsername.getText().toString();
        final String description = mDescription.getText().toString();

        //case1: if the user made a change to their username
        if(!mUserSettings.getUser().getUsername().equals(username))
        {

            checkIfUsernameExists(username);
        }
        //change the remaining items
        if(!mUserSettings.getSettings().getDisplay_name().equals(displayName))
        {
            //update displayname
            mFirebaseMethods.updateUserAccountSettings(displayName, null, null, 0);
        }
        if(!mUserSettings.getSettings().getDescription().equals(description)){
            //update description
            mFirebaseMethods.updateUserAccountSettings(null, null, description, 0);
        }

        //TODO:Create INTENT BACK
        Intent backProfile = new Intent(getContext(), ProfileActivity.class);
        backProfile.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        getActivity().startActivity(backProfile);

    }

    //so when the profile loads we will load the users info into the correct fields
    private void setProfileWidgets(UserSettings userSettings)
    {
        Log.d(TAG, "setProfileWidgets: setting widgets with data retrieving from firebase database: " + userSettings.toString());
        //we have much of the data in both the user and useracc
        mUserSettings = userSettings;
        UserAccountSettings settings = userSettings.getSettings();

        UniversalImageLoader.setImage(settings.getProfile_photo(), mProfilePhoto, null, "");
        mDisplayname.setText(settings.getDisplay_name());
        mUsername.setText(settings.getUsername());
        mDescription.setText(settings.getDescription());

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
    }


    private void setupFirebaseAuth()
    {
        Log.d(TAG, "setupFirebaseAuth: setting up firebase auth.");

        mAuth = FirebaseAuth.getInstance();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        myRef = mFirebaseDatabase.getReference();
        userID = mAuth.getCurrentUser().getUid();

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
                    FirebaseMethods firebaseMethods = new FirebaseMethods(getContext());
                    firebaseMethods.uploadNewPhoto(getString(R.string.profile_photo),null, null,
                            0,null, bitmap,
                            null, null, null);
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
