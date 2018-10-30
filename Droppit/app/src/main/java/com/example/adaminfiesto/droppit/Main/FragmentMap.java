package com.example.adaminfiesto.droppit.Main;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Parcelable;
import android.support.v4.app.ActivityCompat;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.adaminfiesto.droppit.DataModels.Photo;
import com.example.adaminfiesto.droppit.Detail.DetailActivity;
import com.example.adaminfiesto.droppit.R;
import com.example.adaminfiesto.droppit.Utils.FirebaseMethods;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.content.Context.LOCATION_SERVICE;
import static android.support.constraint.Constraints.TAG;

public class FragmentMap extends Fragment implements
        OnMapReadyCallback, GoogleMap.InfoWindowAdapter,
        GoogleMap.OnInfoWindowClickListener,
        LocationListener,
        GoogleMap.OnMapLongClickListener
{
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference myRef;
    private FirebaseMethods mFirebaseMethods;

    MapView gMapView;
    private GoogleMap mMap = null;
    private static final int REQUEST_LOCATION = 0x0101;
    private double locationlat;
    private double locationlong;
    private ArrayList<String> mUsers;
    private ArrayList<Photo> mPhotos;
    private ArrayList<Photo> passPhotos;
    private ArrayList<Marker> mMarker;
    private dataPass datapasser;
    private LocationManager mLC;
    private static View view;
    LatLng thePlaceToShow;
    LatLng userloaction;
    FusedLocationProviderClient fusedLocationProviderClient;


    public interface dataPass
    {
        void location(LatLng lat);

    }

    //attach context for the interface
    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);
        datapasser = (dataPass) context;
    }

    public static FragmentMap newInstance(ArrayList<Photo> photos)
    {
        Bundle args = new Bundle();
        FragmentMap fragment = new FragmentMap();
        args.putParcelableArrayList("pArray", photos);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {

        if (view != null)
        {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (parent != null)
                parent.removeView(view);
        }

        try
        {
            view = inflater.inflate(R.layout.fragment_map, container, false);
            gMapView = view.findViewById(R.id.mapView);
            gMapView.getMapAsync(this);
            mUsers = new ArrayList<>();
            mPhotos = new ArrayList<>();
            passPhotos = new ArrayList<>();
            mMarker = new ArrayList<>();

            //get the data passed from activity to fragment
            if(getArguments() != null)
            {
                mPhotos.clear();
                mPhotos = getArguments().getParcelableArrayList("pArray");
            }

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }


        gMapView.onCreate(getArguments());

        return view;
    }

    @Override
    public void onCreate(Bundle bundle)
    {
        super.onCreate(bundle);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity());
        //request location when activity start
        //get location data from context
        mLC = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

        //check if we have permissions
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            //get our last known location
            Location lastKnown = mLC.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if (lastKnown != null)
            {
                locationlat = lastKnown.getLatitude();
                locationlong = lastKnown.getLongitude();
            }
            else if (lastKnown == null)
            {
                mLC.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, this);
            }
        }
        else
            {
            //request permission if we dont have them.
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        }

        setHasOptionsMenu(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        Log.d(TAG, "onRequestPermissionsResult: TEST TES TES TES TES TES TES TE ES TES TES TSE");
        //request location when activity start
        //get location data from context
        mLC = (LocationManager) getActivity().getSystemService(LOCATION_SERVICE);
        //check if we have permissions
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)
        {
            //get our last known location
            Location lastKnown = mLC.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if (lastKnown != null)
            {
                locationlat = lastKnown.getLatitude();
                locationlong = lastKnown.getLongitude();
            }
            else if (lastKnown == null)
            {
                mLC.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
            }

        }
        else
            {
            //request permission if we dont have them.
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        }

        gMapView.getMapAsync(this);
    }

    private void zoomInCamara(Location location)
    {
        if (mMap == null)
        {
            return;
        }

        thePlaceToShow = new LatLng(location.getLatitude(), location.getLongitude());

        Log.d(TAG, "zoomInCamara: " + location.toString());

        datapasser.location(thePlaceToShow);

        CameraUpdate camMovement = CameraUpdateFactory.newLatLngZoom(thePlaceToShow, 19.0f);

        mMap.moveCamera(camMovement);
        //mMap.animateCamera(camMovement);
    }

    private void getDeviceLocation()
    {
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
//                locationlat = location.getLatitude();
//                locationlong = location.getLongitude();
                zoomInCamara(location);

                //call the distance of the radius of from user
                //grab the data for AR
                for (Photo i : mPhotos)
                {
                    LatLng latM = new LatLng(Double.valueOf(i.getLocation()), Double.valueOf(i.getLocationlong()));
                    double dis = CalculationByDistance(latM,thePlaceToShow);

                    if (dis > 15.0f)
                    {
                        passPhotos.add(i);
                    }
                }

            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        mMap.setMyLocationEnabled(true);
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setZoomGesturesEnabled(false);

        //if marker info is cliked
        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {

                // get the marker position then the id then passdata
                LatLng p = marker.getPosition();

                for (Photo i : mPhotos)
                {
                    if (i.getLocation().equals(String.valueOf(p.latitude)) && i.getLocationlong().equals(String.valueOf(p.longitude)))
                    {
                        String photoID = i.getPhoto_id();
                        //send this uuid.
                        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("photoID", Context.MODE_PRIVATE);
                        SharedPreferences.Editor ed = sharedPreferences.edit();
                        ed.putInt("checker", 0);
                        ed.putString("photo", photoID);
                        ed.apply();
                    }
                }

                Log.d(TAG, "onInfoWindowClick:is marker and photo the same " + marker.getTitle());
                Intent detailActivity = new Intent(getActivity(), DetailActivity.class);
                getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                detailActivity.putExtra(String.valueOf(R.string.to_detail), "detail");
                startActivity(detailActivity);

            }
        });

        makeMarker();
        getDeviceLocation();
        mMap.getUiSettings().setScrollGesturesEnabled(false);

    }

    private void makeMarker()
    {
        mMap.clear();
        //have to load and for loop each marker
        for (int i = 0; i < mPhotos.size(); i++)
        {
            MarkerOptions options = new MarkerOptions();
            options.title(mPhotos.get(i).getCaption());
            options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
            LatLng ToShow = new LatLng(Double.valueOf(mPhotos.get(i).getLocation()), Double.valueOf(mPhotos.get(i).getLocationlong()));
            options.position(ToShow);
            mMap.addMarker(options);
        }

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

    @Override
    public void onLocationChanged(Location location)
    {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras)
    {

    }

    @Override
    public void onProviderEnabled(String provider)
    {

    }

    @Override
    public void onProviderDisabled(String provider)
    {

    }

    @Override
    public View getInfoWindow(Marker marker)
    {
        return null;
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }

    @Override
    public void onInfoWindowClick(Marker marker)
    {

    }

    @Override
    public void onMapLongClick(LatLng latLng)
    {

    }

    @Override
    public void onStart()
    {
        super.onStart();
        if (gMapView != null)
        {
            gMapView.onStart();
        }
    }

    @Override
    public void onStop()
    {
        super.onStop();

        if (gMapView != null)
        {
            gMapView.onStop();
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (gMapView != null)
        {
            gMapView.onResume();
        }

    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (gMapView != null)
            //clear frag data
            mPhotos.clear();
            mUsers.clear();
            passPhotos.clear();
            mMarker.clear();
            gMapView.onDestroy();
    }

}
