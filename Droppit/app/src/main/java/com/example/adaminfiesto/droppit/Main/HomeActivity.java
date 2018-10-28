package com.example.adaminfiesto.droppit.Main;

import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.example.adaminfiesto.droppit.AR.ARActivity;
import com.example.adaminfiesto.droppit.DataModels.Comment;
import com.example.adaminfiesto.droppit.DataModels.Photo;
import com.example.adaminfiesto.droppit.Login.LoginActivity;
import com.example.adaminfiesto.droppit.R;
import com.example.adaminfiesto.droppit.Search.SearchActivity;
import com.example.adaminfiesto.droppit.UserProfile.ProfileActivity;
import com.example.adaminfiesto.droppit.UserProfile.ProfileFragment;
import com.example.adaminfiesto.droppit.Utils.BottomNavigationViewHelper;
import com.example.adaminfiesto.droppit.Utils.Permissions;
import com.example.adaminfiesto.droppit.Utils.SectionsPagerAdapter;
import com.example.adaminfiesto.droppit.Utils.UniversalImageLoader;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static android.support.constraint.Constraints.TAG;


public class HomeActivity extends AppCompatActivity implements FragmentMap.dataPass
{

    public Context mContext = HomeActivity.this;
    private static final int ACTIVITY_NUM = 0;
    private static final int HOME_FRAGMENT = 1;
    private static final int VERIFY_PERMISSIONS_REQUEST = 1;
    private static final int  CAMERA_REQUEST_CODE = 5;
    //firebase
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    //widgets for this view and viewpager
    private ViewPager mViewPager;
    private FrameLayout mFrameLayout;
    private RelativeLayout mRelativeLayout;
    FloatingActionButton fab;
    private String TAG;
    private LatLng dalocation;
    private ArrayList<String> mUsers;
    private ArrayList<Photo> mPhotos;
    ProgressBar progressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        mFrameLayout = findViewById(R.id.container);
        mViewPager = findViewById(R.id.viewpager_container);
        mRelativeLayout = findViewById(R.id.relLayoutParent);
        fab = findViewById(R.id.floatingActionButton);
        progressBar = findViewById(R.id.progressBarMap);
        mUsers = new ArrayList<>();
        mPhotos = new ArrayList<>();
        setupBottomNavigationView();
        setupFirebaseAuth();
        initImageLoader();

        getUserPhoto();



        Log.i(TAG, "....: map should be here ");

        if(checkPermissionsArray(Permissions.PERMISSIONS))
        {
           getUserPhoto();
        }
        else
        {
            verifyPermissions(Permissions.PERMISSIONS);
        }

        fab.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if(HomeActivity.this.checkPermissions(Permissions.CAMERA_PERMISSION[0]))
                {
                    Log.d(TAG, "onClick: starting camera");
                    //send the location alone via broadcast
                    Log.d(TAG, "onClick: location "+ dalocation);
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
                }
                else
                {
                    return;
                }
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
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

                Log.d(TAG, "onActivityResult: received new bitmap from camera: " + bitmap);
                Intent intent = new Intent(this, NextActivity.class);
                intent.putExtra(getString(R.string.selected_bitmap), bitmap);


                SharedPreferences sharedPreferences = getSharedPreferences("Test", Context.MODE_PRIVATE);
                SharedPreferences.Editor ed = sharedPreferences.edit();

                ed.putFloat("lat", (float) dalocation.latitude);
                ed.putFloat("long", (float) dalocation.longitude);
                ed.commit();

                startActivity(intent);


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

    //this is the first screen that will A. Load for the user and B utilize the imageloader for maps
    private void initImageLoader()
    {
        UniversalImageLoader universalImageLoader = new UniversalImageLoader(mContext);
        ImageLoader.getInstance().init(universalImageLoader.getConfig());
    }

    private void setupViewPager()
    {
        FragmentMap fragment = new FragmentMap();
        FragmentTransaction transaction = HomeActivity.this.getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.container2, FragmentMap.newInstance(mPhotos));
        transaction.addToBackStack("FragmentMap");
        transaction.commitAllowingStateLoss();
    }

    private void getUserPhoto()
    {

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference reference = database.getReference(getString(R.string.dbname_user_photos));
        Query query = reference;

        mPhotos.clear();
        mUsers.clear();

        query.addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                Log.d(TAG,"database of userphotos" +dataSnapshot.toString());

                for(DataSnapshot singleSnapshot : dataSnapshot.getChildren())
                {
                    //Log.d(TAG, "onDataChange: found user: " + singleSnapshot.child(getString(R.string.field_user_id)).getValue());

                    mUsers.add(singleSnapshot.getKey().toString());
                }

                getPhotos();
            }

            public void onCancelled(DatabaseError databaseError)
            {

            }
        });

    }

    private void getPhotos()
    {
        mPhotos.clear();

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

        for(int i = 0; i < mUsers.size(); i++)
        {
            Query query = reference.child(getString(R.string.dbname_user_photos))
                    .child(mUsers.get(i)).orderByChild(getString(R.string.field_user_id)).equalTo(mUsers.get(i));

            query.addListenerForSingleValueEvent(new ValueEventListener()
            {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                {
                    //soo we are within the user_photo nods as such we need to get the values
                    //of the nods and put them to the phote/droppit class
                    for(DataSnapshot singleSnapshot : dataSnapshot.getChildren())
                    {
                        Photo photo = new Photo();
                        Map<String, Object> objectMap = (HashMap<String, Object>) singleSnapshot.getValue();

                        photo.setCaption(objectMap.get(getString(R.string.field_caption)).toString());
//                        photo.setTags(objectMap.get(getString(R.string.field_tags)).toString());
                        photo.setPhoto_id(objectMap.get(getString(R.string.field_photo_id)).toString());
//                        photo.setUser_id(objectMap.get(getString(R.string.field_user_id)).toString());
                        photo.setLocation(objectMap.get(getString(R.string.field_location)).toString());
                        photo.setLocationlong(objectMap.get(getString(R.string.field_locationlong)).toString());

                        mPhotos.add(photo);

                    }

                    setupViewPager();

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }

    }

    //since every layout has the xml bottem navView we can use this method to nav back and forth
    private void setupBottomNavigationView()
    {
        BottomNavigationViewEx bottomNavigationViewEx = findViewById(R.id.bottomNavView);

        BottomNavigationViewHelper.setupBottomNavigationView(bottomNavigationViewEx);

        BottomNavigationViewHelper.enableNavigation(mContext,this, bottomNavigationViewEx);

        Menu menu = bottomNavigationViewEx.getMenu();

        MenuItem menuItem = menu.getItem(ACTIVITY_NUM);

        menuItem.setChecked(true);
    }

    public void verifyPermissions(String[] permissions)
    {
        Log.d(TAG, "verifyPermissions: verifying permissions.");

        ActivityCompat.requestPermissions(HomeActivity.this, permissions, VERIFY_PERMISSIONS_REQUEST);
    }

    public boolean checkPermissions(String permission)
    {
        Log.d(TAG, "checkPermissions: checking permission: " + permission);

        int permissionRequest = ActivityCompat.checkSelfPermission(HomeActivity.this, permission);
        //check go or no
        if(permissionRequest != PackageManager.PERMISSION_GRANTED)
        {
            Log.d(TAG, "checkPermissions: \n Permission was not granted for: " + permission);
            return false;
        }
        else
        {
            Log.d(TAG, "checkPermissions: \n Permission was granted for: " + permission);
            return true;
        }
    }

    private void checkCurrentUser(FirebaseUser user)
    {
        Log.d(TAG, "checkCurrentUser: checking if user is logged in.");

        if(user == null)
        {
            Intent intent = new Intent(mContext, LoginActivity.class);
            startActivity(intent);
        }
    }

    //utilize array from permission class
    public boolean checkPermissionsArray(String[] permissions)
    {

        for(int i = 0; i< permissions.length; i++)
        {
            String check = permissions[i];
            if(!checkPermissions(check))
            {
                return false;
            }
        }
        return true;
    }

    private void setupFirebaseAuth()
    {
        Log.d(TAG, "setupFirebaseAuth: setting up firebase auth.");

        mAuth = FirebaseAuth.getInstance();

        mAuthListener = new FirebaseAuth.AuthStateListener()
        {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth)
            {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                //check if the user is logged in
                checkCurrentUser(user);

                if (user != null)
                {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }

            }
        };
    }

    @Override
    protected void onPause()
    {
        super.onPause();
//        mPhotos.clear();
//        mUsers.clear();
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);

    }

    @Override
    public void onStop()
    {
        super.onStop();
        mPhotos.clear();
        mUsers.clear();
        if (mAuthListener != null)
        {
            mAuth.removeAuthStateListener(mAuthListener);
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }

    //TODO:Reload the fragment
    @Override
    protected void onResume()
    {
        super.onResume();
        mPhotos.clear();
        getUserPhoto();
        Log.d(TAG, "onResume: " + dalocation);
    }

    @Override
    public void location(LatLng lat) {
        dalocation = lat;
    }

}
