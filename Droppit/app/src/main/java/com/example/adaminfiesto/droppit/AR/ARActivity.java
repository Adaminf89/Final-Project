package com.example.adaminfiesto.droppit.AR;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.adaminfiesto.droppit.DataModels.Photo;
import com.example.adaminfiesto.droppit.R;
import com.example.adaminfiesto.droppit.Utils.BottomNavigationViewHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableException;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import uk.co.appoly.arcorelocation.LocationMarker;
import uk.co.appoly.arcorelocation.LocationScene;


public class ARActivity extends AppCompatActivity
{
    private static final String TAG = ARActivity.class.getSimpleName();
    public Context mContext = ARActivity.this;
    private static final int ACTIVITY_NUM = 3;
    private ArrayList<Photo> passPhotos;
    private ArrayList<String> mUsers;
    private ArrayList<Photo> mPhotos;
    private LatLng thePlaceToShow;
    private ArFragment arFragment;
    //ar
    private Snackbar loadingMessageSnackbar = null;
    private boolean installRequested;
    private ModelRenderable andyRenderable;
    private LocationScene locationScene;

    private ArSceneView arSceneView;
    FusedLocationProviderClient fusedLocationProviderClient;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar);
        passPhotos = new ArrayList<>();
        mUsers = new ArrayList<>();
        mPhotos = new ArrayList<>();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        arSceneView = findViewById(R.id.arFrameLayout);
        getUserPhoto();
        getDeviceLocation();

        CompletableFuture<ModelRenderable> andy = ModelRenderable.builder()
                .setSource(this, R.raw.andy)
                .build();

        CompletableFuture.allOf(andy)
                .handle(
                        (notUsed, throwable) ->
                        {
                            if (throwable != null)
                            {
                                DemoUtils.displayError(this, "Unable to load renderables", throwable);
                                return null;
                            }
                            try
                            {
                                andyRenderable = andy.get();
                            }
                            catch (InterruptedException | ExecutionException ex)
                            {
                                DemoUtils.displayError(this, "Unable to load renderables", ex);
                            }
                            return null;
                        });


        arSceneView
                .getScene()
                .setOnUpdateListener(frameTime -> {

            Frame frame = arSceneView.getArFrame();

            if (frame == null)
            {
                return;
            }

            if (frame.getCamera().getTrackingState() != TrackingState.TRACKING)
            {
                return;
            }

            if (locationScene == null)
            {
                locationScene = new LocationScene(this, this, arSceneView);

                locationScene.mLocationMarkers.add(new LocationMarker(-81.3290023803711,28.808021545410156, getAndy("Drop")));

            }

            if (locationScene != null)
            {
                //hideLoadingMessage();
                locationScene.processFrame(arSceneView.getArFrame());
            }

        });

        setupBottomNavigationView();
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
                        photo.setPhoto_id(objectMap.get(getString(R.string.field_photo_id)).toString());
                        photo.setLocation(objectMap.get(getString(R.string.field_location)).toString());
                        photo.setLocationlong(objectMap.get(getString(R.string.field_locationlong)).toString());

                        mPhotos.add(photo);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }

    }

    private void getDeviceLocation()
    {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>()
        {
            public void onSuccess(Location location)
            {

                thePlaceToShow = new LatLng(location.getLatitude(), location.getLongitude());
                //call the distance of the radius of from user
                //grab the data for AR
                for (Photo i : mPhotos)
                {
                    LatLng latM = new LatLng(Double.valueOf(i.getLocation()), Double.valueOf(i.getLocationlong()));
                    double dis = CalculationByDistance(latM, thePlaceToShow);

                    if (dis > 15.0f)
                    {
                        passPhotos.add(i);
                    }
                }

            }
        });
    }



    private Node getAndy(String name)
    {
        Node base = new Node();
        base.setParent(arSceneView.getScene());
        base.setRenderable(andyRenderable);
        Context c = this;

        base.setOnTapListener((v, event) -> Toast.makeText(
                c, "Location: "+name, Toast.LENGTH_LONG)
                .show());

        return base;
    }



    @Override
    protected void onResume()
    {
        super.onResume();

        if (arSceneView == null)
        {
            return;
        }
        if (arSceneView.getSession() == null)
        {
            // If the session wasn't created yet, don't resume rendering.
            // This can happen if ARCore needs to be updated or permissions are not granted yet.
            try {
                Session session = DemoUtils.createArSession(this, installRequested);
                if (session == null)
                {
                    installRequested = DemoUtils.hasCameraPermission(this);
                    return;
                }
                else
                    {
                    arSceneView.setupSession(session);
                }
            }
            catch (UnavailableException e)
            {
                DemoUtils.handleSessionException(this, e);
            }
        }

        if(locationScene!=null)
        {
            locationScene.resume();
        }

        try
        {
            arSceneView.resume();

        }
        catch (CameraNotAvailableException ex)
        {
            finish();
            return;
        }
    }


    @Override
    public void onPause()
    {
        super.onPause();
        if(locationScene!=null)
        {
            locationScene.pause();
        }
        arSceneView.pause();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        if(locationScene!=null)
        {
            locationScene.pause();
            locationScene = null;
        }
        arSceneView.destroy();
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


    private void setupBottomNavigationView()
    {
        BottomNavigationViewEx bottomNavigationViewEx = findViewById(R.id.bottomNavView);

        BottomNavigationViewHelper.setupBottomNavigationView(bottomNavigationViewEx);

        BottomNavigationViewHelper.enableNavigation(mContext,this, bottomNavigationViewEx);

        Menu menu = bottomNavigationViewEx.getMenu();

        MenuItem menuItem = menu.getItem(ACTIVITY_NUM);

        menuItem.setChecked(true);
    }

}
