package com.example.adaminfiesto.droppit.UserProfile;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.adaminfiesto.droppit.DataModels.Photo;
import com.example.adaminfiesto.droppit.DataModels.UserAccountSettings;
import com.example.adaminfiesto.droppit.DataModels.UserSettings;
import com.example.adaminfiesto.droppit.Detail.DetailActivity;
import com.example.adaminfiesto.droppit.R;
import com.example.adaminfiesto.droppit.Utils.BottomNavigationViewHelper;
import com.example.adaminfiesto.droppit.Utils.FirebaseMethods;
import com.example.adaminfiesto.droppit.Utils.RecyclerImagerAdapter;
import com.example.adaminfiesto.droppit.Utils.UniversalImageLoader;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment implements RecyclerImagerAdapter.RecyclerViewClickListener
{
    private String TAG;
    private static final int ACTIVITY_NUM = 2;
    //firebase
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference myRef;
    private FirebaseMethods mFirebaseMethods;

    //widgets
    private TextView mDisplayName, mUsername, mDescription;
    private ProgressBar mProgressBar;
    private CircleImageView mProfilePhoto;
    private Button editProfile;
    private RecyclerView recyclerView;
    private Toolbar toolbar;
    private ImageView profileMenu;
    private BottomNavigationViewEx bottomNavigationView;
    private Context mContext;
    private ArrayList<Photo> mPhotos;
    ArrayList<String> imgUrls;
    private Photo mPhoto;
    //TODO:get username, post, descriptions from firebase
    //todo: population the recycler view

    public ProfileFragment()
    {

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_profile, null);
        mContext = getActivity();
        bottomNavigationView = view.findViewById(R.id.bottomNavView);
        mProfilePhoto = view.findViewById(R.id.profile_photo);
        mUsername =  view.findViewById(R.id.profile_username);
        mDescription = view.findViewById(R.id.display_bio);
        mProgressBar = view.findViewById(R.id.profileProgressBar);
        toolbar = view.findViewById(R.id.profileToolBar);
        profileMenu = view.findViewById(R.id.profileMenu);
        editProfile = view.findViewById(R.id.editButton);
        recyclerView = view.findViewById(R.id.recyclerview);
        mFirebaseMethods = new FirebaseMethods(getActivity());
        mPhotos = new ArrayList<>();
        imgUrls = new ArrayList<>();
        setupToolbar();
        setupFirebaseAuth();
        setupBottomNavigationView();
        setupGridView();

        editProfile.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Log.d(TAG, "onClick: navigating to " + mContext.getString(R.string.edit_profile_fragment));

                Intent intent = new Intent(getActivity(), AccountSettingActivity.class);

                //keyvalue pair we put the calling activity which is the the FLAG, while profile_activity is the activities we're looking for.
                intent.putExtra(getString(R.string.calling_activity), getString(R.string.profile_activity));
                startActivity(intent);
                //
                getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            }

        });

        return view;
    }


    @Override
    public void recyclerViewListClicked(View v, int position)
    {
        //send this uuid.
        String photoID = mPhotos.get(position).getPhoto_id();
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("photoID", Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = sharedPreferences.edit();
        ed.putString("photo", photoID);
        ed.putInt("checker",1);
        ed.apply();

        Intent detailActivity = new Intent(getActivity(), DetailActivity.class);
        detailActivity.putExtra(String.valueOf(R.string.to_detail), "detail");
        startActivity(detailActivity);
    }

    private void setProfileWidgets(UserSettings userSettings)
    {

        Log.d(TAG, "setProfileWidgets: is the user nil "+userSettings.getSettings().getUsername());

        UserAccountSettings settings = userSettings.getSettings();

        UniversalImageLoader.setImage(settings.getProfile_photo(), mProfilePhoto, mProgressBar, "");
        mUsername.setText(settings.getUsername());
        mDescription.setText(settings.getDescription());
        mProgressBar.setVisibility(View.GONE);

    }

    private void setupToolbar()
    {

        ((ProfileActivity)getActivity()).setSupportActionBar(toolbar);

        profileMenu.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Log.d(TAG, "onClick: navigating to account settings.");
                Intent intent = new Intent(mContext, AccountSettingActivity.class);
                startActivity(intent);
                getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            }
        });
    }

    private void setupGridView()
    {
        Log.d(TAG, "setupGridView: Setting up image grid.");

//        final ArrayList<Photo> photos = new ArrayList<>();

        final DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference.child(getString(R.string.dbname_user_photos)).child(FirebaseAuth.getInstance().getCurrentUser().getUid());

        query.addListenerForSingleValueEvent(new ValueEventListener()
        {
            //adds the photo to the actual grid
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                for ( DataSnapshot singleSnapshot :  dataSnapshot.getChildren())
                {
                    Photo photo = new Photo();
                    Map<String, Object> objectMap = (HashMap<String, Object>) singleSnapshot.getValue();

                    try
                    {

                        photo.setCaption(objectMap.get(getString(R.string.field_caption)).toString());
                        photo.setTags(objectMap.get(getString(R.string.field_tags)).toString());
                        photo.setPhoto_id(objectMap.get(getString(R.string.field_photo_id)).toString());
                        photo.setUser_id(objectMap.get(getString(R.string.field_user_id)).toString());
                        photo.setDate_created(objectMap.get(getString(R.string.field_date_created)).toString());
                        photo.setImage_path(objectMap.get(getString(R.string.field_image_path)).toString());

                        mPhotos.add(photo);
                    }
                    catch(NullPointerException e)
                    {
                        Log.e(TAG, "onDataChange: NullPointerException: " + e.getMessage() );
                    }

                }

                imgUrls = new ArrayList<String>();

                for(int i = 0; i < mPhotos.size(); i++)
                {
                    imgUrls.add(mPhotos.get(i).getImage_path());
                   // mPhoto = mPhoto.g
                }

                setupRView();
            }

            @Override
            public void onCancelled(DatabaseError databaseError)
            {
                Log.d(TAG, "onCancelled: query cancelled.");
            }
        });


    }


    public void setupRView()
    {
        if(imgUrls.size() > 0)
        {
            RecyclerImagerAdapter adapter = new RecyclerImagerAdapter(getActivity(), imgUrls,this);
            GridLayoutManager manager = new GridLayoutManager(getActivity(),1);
            manager.setOrientation(LinearLayoutManager.HORIZONTAL);
            recyclerView.setLayoutManager(manager);
            recyclerView.setHasFixedSize(true);
            recyclerView.setAdapter(adapter);
        }
        else
        {
            return;
        }
    }

    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);
    }

    private void setupBottomNavigationView()
    {
        Log.d(TAG, "setupBottomNavigationView: setting up BottomNavigationView");

        BottomNavigationViewHelper.setupBottomNavigationView(bottomNavigationView);
        //remember to get the mContext 's actual context from the getActivity() when the fragment loads
        BottomNavigationViewHelper.enableNavigation(mContext, getActivity(), bottomNavigationView);
        Menu menu = bottomNavigationView.getMenu();
        MenuItem menuItem = menu.getItem(ACTIVITY_NUM);
        menuItem.setChecked(true);
    }

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
