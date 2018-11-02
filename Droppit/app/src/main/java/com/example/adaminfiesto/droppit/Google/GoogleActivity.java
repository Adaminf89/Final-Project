package com.example.adaminfiesto.droppit.Google;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.example.adaminfiesto.droppit.DataModels.Photo;
import com.example.adaminfiesto.droppit.Login.LoginActivity;
import com.example.adaminfiesto.droppit.Main.FragmentMap;
import com.example.adaminfiesto.droppit.Main.HomeActivity;
import com.example.adaminfiesto.droppit.Main.NextActivity;
import com.example.adaminfiesto.droppit.R;
import com.example.adaminfiesto.droppit.Utils.BottomNavigationViewHelper;
import com.example.adaminfiesto.droppit.Utils.Permissions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class GoogleActivity extends AppCompatActivity
{
    public Context mContext = GoogleActivity.this;
    private static final int ACTIVITY_NUM = 0;
    private static final int VERIFY_PERMISSIONS_REQUEST = 2;
    private static final int  CAMERA_REQUEST_CODE = 4;
    //firebase
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    //widgets for this view and viewpager
    FloatingActionButton fab;
    private String TAG;
    private LatLng dalocation;
    private ArrayList<String> mUsers;
    private ArrayList<Photo> mPhotos;
    private ArrayList<Photo> cPhotos;
    ProgressBar progressBar;
    private LatLng thePlaceToShow;
    FusedLocationProviderClient fusedLocationProviderClient;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        fab = findViewById(R.id.floatingActionButton);
        progressBar = findViewById(R.id.progressBarMap);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(mContext);
        mUsers = new ArrayList<>();
        mPhotos = new ArrayList<>();
        cPhotos = new ArrayList<>();
        setupBottomNavigationView();
        setupFirebaseAuth();
        getUserPhoto();

        Log.i(TAG, "....: map should be here ");
//
//        if(checkPermissionsArray(Permissions.PERMISSIONS))
//        {
//            getUserPhoto();
//        }
//        else
//        {
//            verifyPermissions(Permissions.PERMISSIONS);
//        }

        fab.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if(GoogleActivity.this.checkPermissions(Permissions.CAMERA_PERMISSION[0]))
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
                ed.putFloat("lat", (float) thePlaceToShow.latitude);
                ed.putFloat("long", (float) thePlaceToShow.longitude);
                ed.apply();

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
        CountDownLatch done = new CountDownLatch(1);

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
                        photo.setPhoto_id(objectMap.get(getString(R.string.field_photo_id)).toString());
                        photo.setLocation(objectMap.get(getString(R.string.field_location)).toString());
                        photo.setLocationlong(objectMap.get(getString(R.string.field_locationlong)).toString());

                        mPhotos.add(photo);
                    }

                    checker(mPhotos);
                    setupViewPager();
                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError)
                {

                }
            });

        }

    }

    private void setupViewPager()
    {
        GoogleFragment frag = GoogleFragment.newInstance(cPhotos);
        getFragmentManager().beginTransaction().replace(R.id.frameFrag, frag, GoogleFragment.TAG).commitAllowingStateLoss();
    }

    public ArrayList<Photo> checker (ArrayList<Photo> P)
    {
        if (ActivityCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
        {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return null;
        }

        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>()
        {
            public void onSuccess(Location location)
            {

                for (Photo i : P)
                {
                    thePlaceToShow = new LatLng(location.getLatitude(), location.getLongitude());
                    LatLng latM = new LatLng(Double.valueOf(i.getLocation()), Double.valueOf(i.getLocationlong()));
                    double dis = CalculationByDistance(latM, thePlaceToShow);

                    if (dis < 10.0f)
                    {
                        cPhotos.add(i);
                    }
                }

            }
        });

        return cPhotos;
    }

    public double CalculationByDistance(LatLng StartP, LatLng EndP)
    {
        int Radius = 6371;// radius of earth in Km
        double lat1 = StartP.latitude;
        double lat2 = EndP.latitude;
        double lon1 = StartP.longitude;
        double lon2 = EndP.longitude;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);

        double c = 2 * Math.asin(Math.sqrt(a));
        double valueResult = Radius * c;
        double km = valueResult / 1;
        DecimalFormat newFormat = new DecimalFormat("####");
        int kmInDec = Integer.valueOf(newFormat.format(km));
        double meter = valueResult % 1000;
        int meterInDec = Integer.valueOf(newFormat.format(meter));
        Log.i("Radius Value", "" + valueResult + "   KM  " + kmInDec
                + " Meter   " + meterInDec);

        return Radius * c;
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

        ActivityCompat.requestPermissions(GoogleActivity.this, permissions, VERIFY_PERMISSIONS_REQUEST);
    }

    public boolean checkPermissions(String permission)
    {
        Log.d(TAG, "checkPermissions: checking permission: " + permission);

        int permissionRequest = ActivityCompat.checkSelfPermission(GoogleActivity.this, permission);
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
        //mPhotos.clear();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        //getUserPhoto();
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
        if (mAuthListener != null)
        {
            mAuth.removeAuthStateListener(mAuthListener);
//            progressBar.setVisibility(View.GONE);
        }
    }
}
