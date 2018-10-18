package com.example.adaminfiesto.droppit.Main;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.adaminfiesto.droppit.DataModels.Photo;
import com.example.adaminfiesto.droppit.Detail.DetailActivity;
import com.example.adaminfiesto.droppit.R;
import com.example.adaminfiesto.droppit.Utils.UniversalImageLoader;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static android.content.Context.LOCATION_SERVICE;
import static android.support.constraint.Constraints.TAG;

public class FragmentMap extends Fragment implements
        OnMapReadyCallback, GoogleMap.InfoWindowAdapter,
        GoogleMap.OnInfoWindowClickListener,
        LocationListener,
        GoogleMap.OnMapLongClickListener
{

    MapView gMapView;
    private GoogleMap mMap = null;
    private static final int REQUEST_LOCATION = 0x0101;
    private double locationlat;
    private double locationlong;
    private ArrayList<String> mUsers;
    private ArrayList<Photo> mPhotos;
    private dataPass datapasser;
    private LocationManager mLC;
    private static View view;
    LatLng thePlaceToShow;
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
        } else {
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

        CameraUpdate camMovement = CameraUpdateFactory.newLatLngZoom(thePlaceToShow, 16);

        mMap.animateCamera(camMovement);

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

                locationlat = location.getLatitude();
                locationlong = location.getLongitude();

                zoomInCamara(location);

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
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener()
        {
            @Override
            public void onInfoWindowClick(Marker marker)
            {

                String index = marker.getId();
                index = index.replace("m","");

                if(Integer.valueOf(index) >= mPhotos.size())
                {
                    getUserPhoto();
                }
                else
                    {
                    String photoID = mPhotos.get(Integer.valueOf(index)).getPhoto_id();


                    //send this uuid.
                    SharedPreferences sharedPreferences = getActivity().getSharedPreferences("photoID", Context.MODE_PRIVATE);
                    SharedPreferences.Editor ed = sharedPreferences.edit();

                    ed.putString("photo", photoID);
                    ed.apply();

                    Log.d(TAG, "onInfoWindowClick:is marker and photo the same " + marker.getTitle());
                    Intent detailActivity = new Intent(getActivity(), DetailActivity.class);
                    detailActivity.putExtra(String.valueOf(R.string.to_detail), "detail");
                    startActivity(detailActivity);
                }
            }
        });

        getUserPhoto();
        getDeviceLocation();

    }

    private void makeMarker()
    {
        mMap.clear();
        //have to load and for loop each marker
        for (int i = 0; i < mPhotos.size(); i++)
        {

            MarkerOptions options = new MarkerOptions();

            options.title(mPhotos.get(i).getCaption());
            //options.snippet(mPhotos.get(i).getTags() + "~" + arrayLocation.get(i).getpImage());
            options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
//            UniversalImageLoader.setImage(mPhotos.get(i).getImage_path(),,null,"");
            LatLng ToShow = new LatLng(Double.valueOf(mPhotos.get(i).getLocation()), Double.valueOf(mPhotos.get(i).getLocationlong()));

            options.position(ToShow);

            mMap.addMarker(options);
        }
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
            gMapView.onStart();
    }

    @Override
    public void onStop()
    {
        super.onStop();
        if (gMapView != null)
            gMapView.onStop();

    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (gMapView != null)
            gMapView.onResume();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (gMapView != null)
            gMapView.onDestroy();
    }

    private void getUserPhoto()
    {

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference reference = database.getReference(getString(R.string.dbname_user_photos));
        Query query = reference;

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
                        photo.setTags(objectMap.get(getString(R.string.field_tags)).toString());
                        photo.setPhoto_id(objectMap.get(getString(R.string.field_photo_id)).toString());
                        photo.setUser_id(objectMap.get(getString(R.string.field_user_id)).toString());
                        photo.setLocation(objectMap.get(getString(R.string.field_location)).toString());
                        photo.setLocationlong(objectMap.get(getString(R.string.field_locationlong)).toString());
                        photo.setmPrivate(objectMap.get(getString(R.string.field_date_private)).toString());
                        photo.setDate_created(objectMap.get(getString(R.string.field_date_created)).toString());
                        photo.setImage_path(objectMap.get(getString(R.string.field_image_path)).toString());

                        mPhotos.add(photo);

                    }
                         makeMarker();

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }

    }

}
