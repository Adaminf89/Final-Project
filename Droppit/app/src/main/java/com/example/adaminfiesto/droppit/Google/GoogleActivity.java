package com.example.adaminfiesto.droppit.Google;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.SyncStateContract;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.adaminfiesto.droppit.DataModels.Photo;
import com.example.adaminfiesto.droppit.Login.LoginActivity;

import com.example.adaminfiesto.droppit.Main.NextActivity;
import com.example.adaminfiesto.droppit.R;
import com.example.adaminfiesto.droppit.Utils.BottomNavigationViewHelper;
import com.example.adaminfiesto.droppit.Utils.FirebaseMethods;
import com.example.adaminfiesto.droppit.Utils.Permissions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class GoogleActivity extends AppCompatActivity implements OnCompleteListener<Void> {
    public Context mContext = GoogleActivity.this;
    private static final int ACTIVITY_NUM = 0;
    private static final int VERIFY_PERMISSIONS_REQUEST = 2;
    private static final int CAMERA_REQUEST_CODE = 6;
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

    /**
     * Tracks whether the user requested to add or remove geofences, or to do neither.
     */
    private enum PendingGeofenceTask {
        ADD, REMOVE, NONE
    }

    private List<Geofence> mGeofenceList;
    private GeofencingClient mGeofencingClient;
    private PendingIntent mGeofencePendingIntent;
    private PendingGeofenceTask mPendingGeofenceTask = PendingGeofenceTask.NONE;

    private PendingIntent getGeofencePendingIntent()
    {

        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }

        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);

        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        mGeofencePendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);

        return mGeofencePendingIntent;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        //geos
        // Initially set the PendingIntent used in addGeofences() and removeGeofences() to null.
        mGeofencePendingIntent = null;
        mGeofencingClient = LocationServices.getGeofencingClient(this);
        mGeofenceList = new ArrayList<>();
        //ui widgets
        fab = findViewById(R.id.floatingActionButton);
        progressBar = findViewById(R.id.progressBarMap);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(mContext);
        mUsers = new ArrayList<>();
        mPhotos = new ArrayList<>();
        cPhotos = new ArrayList<>();

        //setup to load data and bottom nav
        setupBottomNavigationView();
        setupFirebaseAuth();


        Log.i(TAG, "....: map should be here ");

        if (checkPermissionsArray(Permissions.PERMISSIONS))
        {
            Handler h = new Handler();
            h.post(new Runnable()
            {
                @Override
                public void run() {
                    getUserPhoto();
                }
            });
        }
        else
            {
            verifyPermissions(Permissions.PERMISSIONS);
        }

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (GoogleActivity.this.checkPermissions(Permissions.CAMERA_PERMISSION[0])) {
                    Log.d(TAG, "onClick: starting camera");
                    //send the location alone via broadcast
                    Log.d(TAG, "onClick: location " + dalocation);
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
                } else {
                    return;
                }
            }
        });
    }


    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
        // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
        // is already inside that geofence.
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);

        // Add the geofences to be monitored by geofencing service.
        builder.addGeofences(mGeofenceList);

        // Return a GeofencingRequest.
        return builder.build();
    }

    private void populateGeofenceList(ArrayList<Photo> points) {

        for (Photo p : points) {
            Double lat = Double.valueOf(p.getLocation());
            Double longit = Double.valueOf(p.getLocationlong());

            mGeofenceList.add(new Geofence.Builder()
                    // Set the request ID of the geofence. This is a string to identify this
                    // geofence.
                    .setRequestId(p.getPhoto_id())
                    // Set the circular region of this geofence.
                    .setCircularRegion(
                            lat,
                            longit,
                            Constants.GEOFENCE_RADIUS_IN_METERS
                    )

                    // Set the expiration duration of the geofence. This geofence gets automatically
                    // removed after this period of time.
                    .setExpirationDuration(Constants.GEOFENCE_EXPIRATION_IN_MILLISECONDS)

                    // Set the transition types of interest. Alerts are only generated for these
                    // transition. We track entry and exit transitions in this sample.
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                    //Create the geofence.
                    .build());
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_REQUEST_CODE) {

            Log.d(TAG, "onActivityResult: done taking a photo.");
            Log.d(TAG, "onActivityResult: attempting to navigate to final share screen.");


            try {

                Bitmap bitmap;
                bitmap = (Bitmap) data.getExtras().get("data");

                Log.d(TAG, "onActivityResult: received new bitmap from camera: " + bitmap);
                Intent intent = new Intent(mContext, NextActivity.class);
                intent.putExtra(getString(R.string.selected_bitmap), bitmap);

                if (thePlaceToShow == null)
                {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    fusedLocationProviderClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                        public void onSuccess(Location location) {
                            thePlaceToShow = new LatLng(location.getLatitude(), location.getLongitude());
                            SharedPreferences sharedPreferences = getSharedPreferences("Test", Context.MODE_PRIVATE);
                            SharedPreferences.Editor ed = sharedPreferences.edit();
                            ed.putFloat("lat", (float) thePlaceToShow.latitude);
                            ed.putFloat("long", (float) thePlaceToShow.longitude);
                            ed.apply();
                        }
                    });

                    startActivity(intent);
                }
                else
                    {
                        SharedPreferences sharedPreferences = getSharedPreferences("Test", Context.MODE_PRIVATE);
                        SharedPreferences.Editor ed = sharedPreferences.edit();
                        ed.putFloat("lat", (float) thePlaceToShow.latitude);
                        ed.putFloat("long", (float) thePlaceToShow.longitude);
                        ed.apply();
                        startActivity(intent);
                    }







            } catch (NullPointerException e)
            {
                Log.d(TAG, "onActivityResult: NullPointerException: " + e.getMessage());
            }
        } else {
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
                Log.d(TAG, "database of userphotos" + dataSnapshot.toString());

                for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
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

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

        if(mUsers.size() == 0)
        {
            setupViewPager();
        }

        for (int i = 0; i < mUsers.size(); i++)
        {
            Query query = reference.child(getString(R.string.dbname_user_photos))
                    .child(mUsers.get(i)).orderByChild(getString(R.string.field_user_id)).equalTo(mUsers.get(i));

            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    //soo we are within the user_photo nods as such we need to get the values
                    //of the nods and put them to the phote/droppit class
                    for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
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
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

        }

    }

    private void setupViewPager()
    {

        GoogleFragment frag = GoogleFragment.newInstance(cPhotos);
        getFragmentManager().beginTransaction().replace(R.id.frameFrag, frag, GoogleFragment.TAG).commitAllowingStateLoss();

    }

    public ArrayList<Photo> checker(ArrayList<Photo> P)
    {
        if (ActivityCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_COARSE_LOCATION)
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

                if(cPhotos.size() > 0)
                {
                    populateGeofenceList(cPhotos);
                    addGeofences();
                }

            }
        });

        return cPhotos;
    }

    public double CalculationByDistance(LatLng StartP, LatLng EndP) {
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

        BottomNavigationViewHelper.enableNavigation(mContext, this, bottomNavigationViewEx);

        Menu menu = bottomNavigationViewEx.getMenu();

        MenuItem menuItem = menu.getItem(ACTIVITY_NUM);

        menuItem.setChecked(true);
    }

    public void verifyPermissions(String[] permissions) {
        Log.d(TAG, "verifyPermissions: verifying permissions.");

        ActivityCompat.requestPermissions(GoogleActivity.this, permissions, VERIFY_PERMISSIONS_REQUEST);
    }

    private void addGeofences()
    {
        if (!checkPermissionGeo())
        {
            showSnackbar("Something went wrong with your permissions");
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mGeofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent()).addOnCompleteListener(this);
    }

    //todo: just do one calls for checks... in refactor
    private boolean checkPermissionGeo()
    {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
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

    private void showSnackbar(final String text)
    {
        View container = findViewById(android.R.id.content);

        if (container != null)
        {
            Snackbar.make(container, text, Snackbar.LENGTH_LONG).show();
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

    private void updateGeofencesAdded(boolean added)
    {
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putBoolean(Constants.GEOFENCES_ADDED_KEY, added)
                .apply();
    }

    private boolean getGeofencesAdded()
    {
        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
                Constants.GEOFENCES_ADDED_KEY, false);
    }

    private void removeGeofences()
    {
        if (!checkPermissionGeo())
        {
            showSnackbar("something went wrong");
            return;
        }
        mGeofencingClient.removeGeofences(getGeofencePendingIntent()).addOnCompleteListener(this);
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
        GeofenceTransitionsIntentService.shouldContinue = false;
        mPhotos.clear();
        cPhotos.clear();
    }

    @Override
    protected void onResume()
    {
        super.onResume();

    }

    @Override
    protected void onStart()
    {
        super.onStart();
        GeofenceTransitionsIntentService.shouldContinue = true;
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop()
    {
        super.onStop();
        mPhotos.clear();
        cPhotos.clear();
        removeGeofences();
        GeofenceTransitionsIntentService.shouldContinue = false;
        if (mAuthListener != null)
        {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    @Override
    public void onComplete(@NonNull Task<Void> task)
    {
        mPendingGeofenceTask = PendingGeofenceTask.NONE;
        if (task.isSuccessful())
        {
            updateGeofencesAdded(!getGeofencesAdded());
            System.out.print("Worked listadded geo");
        }
        else {
            // Get the status code for the error and log it using a user-friendly message.
            System.out.print("failed no go on complete");
        }

    }
}
