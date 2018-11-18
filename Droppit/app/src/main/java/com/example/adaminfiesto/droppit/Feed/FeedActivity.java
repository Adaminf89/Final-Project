package com.example.adaminfiesto.droppit.Feed;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.example.adaminfiesto.droppit.AR.ARActivity;
import com.example.adaminfiesto.droppit.DataModels.Comment;
import com.example.adaminfiesto.droppit.DataModels.Like;
import com.example.adaminfiesto.droppit.DataModels.Photo;
import com.example.adaminfiesto.droppit.Detail.DetailActivity;
import com.example.adaminfiesto.droppit.Detail.DetailFragmentPrivate;
import com.example.adaminfiesto.droppit.Detail.DetailFragmentPublic;
import com.example.adaminfiesto.droppit.Google.GeofenceTransitionsIntentService;
import com.example.adaminfiesto.droppit.Login.LoginActivity;
import com.example.adaminfiesto.droppit.R;
import com.example.adaminfiesto.droppit.UserProfile.ProfileActivity;
import com.example.adaminfiesto.droppit.Utils.BottomNavigationViewHelper;
import com.example.adaminfiesto.droppit.Utils.MainfeedListAdapter;
import com.example.adaminfiesto.droppit.Utils.Permissions;
import com.example.adaminfiesto.droppit.Utils.RecyclerImagerAdapter;
import com.example.adaminfiesto.droppit.Utils.UniversalImageLoader;
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
import com.google.firebase.storage.FirebaseStorage;
import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class FeedActivity extends AppCompatActivity implements RecyclerImagerAdapter.RecyclerViewClickListener, MainfeedListAdapter.OnLoadMoreItemsListener
{
    public Context mContext = FeedActivity.this;
    private static final int ACTIVITY_NUM = 1;
    //private static final int AR_FRAGMENT = 1;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseDatabase mFirebaseDatabase;
    private static final int MY_PERMISSION_REQUEST_FINE_LOCATION = 101;
    private static final int MY_PERMISSION_REQUEST_COARSE_LOCATION = 102;
    private DatabaseReference myRef;
    //vars
    private ArrayList<Photo> mPhotos;
    private ArrayList<Photo> mPaginatedPhotos;
    private ArrayList<String> mFollowing;
    private ListView mListView;
    private RecyclerView recyclerView;
    ArrayList<String> imgUrls;
    private MainfeedListAdapter mAdapter;
    private int mResults;
    private String TAG;
    private boolean permissionIsGranted = false;

    private ArrayList<String> mtrending;
    private ArrayList<Photo> tPhotos;
    private Integer count;
    private static final int VERIFY_PERMISSIONS_REQUEST = 3;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);
        mListView = (ListView)findViewById(R.id.listViewFeed);
        recyclerView = findViewById(R.id.recyclerview);
        mPaginatedPhotos = new ArrayList<>();
        mtrending = new ArrayList<>();
        tPhotos = new ArrayList<>();
        mFollowing = new ArrayList<>();
        mPhotos = new ArrayList<>();
        imgUrls = new ArrayList<>();
        setupFirebaseAuth();


        mPaginatedPhotos.clear();
        mPhotos.clear();
        tPhotos.clear();
        mFollowing.clear();
        imgUrls.clear();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_REQUEST_FINE_LOCATION);
        }
        else
            {
            permissionIsGranted = true;
        }

        if(checkPermissionsArray(Permissions.PERMISSIONS))
        {

            getrending();
        }
        else
        {
            verifyPermissions(Permissions.PERMISSIONS);
        }

        setupBottomNavigationView();

    }

    //this is the first screen that will A. Load for the user and B utilize the imageloader for maps
    private void initImageLoader()
    {
        UniversalImageLoader universalImageLoader = new UniversalImageLoader(mContext);
        ImageLoader.getInstance().init(universalImageLoader.getConfig());
    }


    private void getFollowing()
    {
        Log.d(TAG, "getFollowing: searching for following");

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
                    Log.d(TAG, "onDataChange: found user: " + singleSnapshot.child(getString(R.string.field_user_id)).getValue());

                    //mFollowing.add(singleSnapshot.child(getString(R.string.field_user_id)).getValue().toString());
                    mFollowing.add(singleSnapshot.getKey().toString());
                }

//                Log.d(TAG, "onDataChange: found user: " + mFollowing.get(0).toString() +" / "+ mFollowing.get(1).toString());

                getPhotos();

            }

            public void onCancelled(DatabaseError databaseError)
            {

            }
        });

    }

    public void getrending()
    {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference reference = database.getReference("trending");
        Query query = reference;

        query.addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                Log.d(TAG,"database of userphotos" +dataSnapshot.toString());

                for(DataSnapshot singleSnapshot : dataSnapshot.getChildren())
                {
                    Log.d(TAG, "onDataChange: found user: " + singleSnapshot.child(getString(R.string.field_user_id)).getValue());

                    //mFollowing.add(singleSnapshot.child(getString(R.string.field_user_id)).getValue().toString());
                    mtrending.add(singleSnapshot.getKey().toString());
                }

//                Log.d(TAG, "onDataChange: found user: " + mtrending.get(0).toString() +" / "+ mtrending.get(1).toString());

               getTrending();

            }

            public void onCancelled(DatabaseError databaseError)
            {

            }
        });
    }

    private void getTrending()
    {
//        Log.d(TAG, "getPhotos: getting photos "+ mtrending.get(0).toString()+ " index 2 " + mtrending.get(1).toString());

        tPhotos.clear();

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

        for(int i = 0; i < mtrending.size(); i++)
        {
            final int count = i;

            Query query = reference.child(getString(R.string.dbname_photos)).child(mtrending.get(i));
            query.addListenerForSingleValueEvent(new ValueEventListener()
            {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot)
                {

                    if(dataSnapshot.exists())
                    {
                        Photo photo = new Photo();
                        Map<String, Object> objectMap = (HashMap<String, Object>) dataSnapshot.getValue();

                        photo.setCaption(objectMap.get(getString(R.string.field_caption)).toString());
                        photo.setTags(objectMap.get(getString(R.string.field_tags)).toString());
                        photo.setPhoto_id(objectMap.get(getString(R.string.field_photo_id)).toString());
                        photo.setUser_id(objectMap.get(getString(R.string.field_user_id)).toString());
                        photo.setLocation(objectMap.get(getString(R.string.field_location)).toString());
                        photo.setmPrivate(objectMap.get("mPrivate").toString());
                        photo.setLocationlong(objectMap.get(getString(R.string.field_locationlong)).toString());
                        photo.setDate_created(objectMap.get(getString(R.string.field_date_created)).toString());
                        photo.setImage_path(objectMap.get(getString(R.string.field_image_path)).toString());

                        tPhotos.add(photo);

                    }

                }

                @Override
                public void onCancelled(DatabaseError databaseError)
                {

                }
            });
        }

        getFollowing();
    }

    private void getPhotos()
    {
        Log.d(TAG, "getPhotos: getting photos");
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

        mPhotos.clear();

        for(int i = 0; i < mFollowing.size(); i++)
        {
            final int count = i;
            Query query = reference.child(getString(R.string.dbname_user_photos)).child(mFollowing.get(i)).orderByChild(getString(R.string.field_user_id)).equalTo(mFollowing.get(i));
            query.addListenerForSingleValueEvent(new ValueEventListener()
            {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot)
                {
                    for(DataSnapshot singleSnapshot : dataSnapshot.getChildren())
                    {

                        Photo photo = new Photo();
                        Map<String, Object> objectMap = (HashMap<String, Object>) singleSnapshot.getValue();

                        photo.setCaption(objectMap.get(getString(R.string.field_caption)).toString());
                        photo.setTags(objectMap.get(getString(R.string.field_tags)).toString());
                        photo.setPhoto_id(objectMap.get(getString(R.string.field_photo_id)).toString());
                        photo.setUser_id(objectMap.get(getString(R.string.field_user_id)).toString());
                        photo.setLocation(objectMap.get(getString(R.string.field_location)).toString());
                        photo.setmPrivate(objectMap.get("mPrivate").toString());
                        photo.setLocationlong(objectMap.get(getString(R.string.field_locationlong)).toString());
                        photo.setDate_created(objectMap.get(getString(R.string.field_date_created)).toString());
                        photo.setImage_path(objectMap.get(getString(R.string.field_image_path)).toString());

                        ArrayList<Comment> comments = new ArrayList<Comment>();

                        for (DataSnapshot dSnapshot : singleSnapshot.child(getString(R.string.field_comments)).getChildren())
                        {
                            Comment comment = new Comment();
                            comment.setUser_id(dSnapshot.getValue(Comment.class).getUser_id());
                            comment.setComment(dSnapshot.getValue(Comment.class).getComment());
                            comment.setDate_created(dSnapshot.getValue(Comment.class).getDate_created());
                            comments.add(comment);
                        }

                        ArrayList<Like> likes = new ArrayList<Like>();

                        for (DataSnapshot lSnapshot : singleSnapshot.child("like").getChildren())
                        {

                            Like like = new Like();
                            like.setRating(lSnapshot.getValue(Like.class).getRating());
                            like.setUser_id(lSnapshot.getValue(Like.class).getUser_id());
                            likes.add(like);


                        }

                        photo.setLikes(likes);
                        photo.setComments(comments);
                        mPhotos.add(photo);

                    }

                    Handler h = new Handler();
                    h.post(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            imgUrls.clear();

                            imgUrls = new ArrayList<String>();

                            for(int i = 0; i < tPhotos.size(); i++)
                            {
                                imgUrls.add(tPhotos.get(i).getImage_path());
                                // mPhoto = mPhoto.g
                            }

                            if(count >= mFollowing.size() -1)
                            {
                                //display our photos
                                displayPhotos();
                            }
                        }
                    });

                }

                @Override
                public void onCancelled(DatabaseError databaseError)
                {

                }
            });
        }
    }


    private void displayPhotos()
    {

        mPaginatedPhotos.clear();

        if(mPhotos != null)
        {
            try
            {
                Collections.sort(mPhotos, new Comparator<Photo>()
                {
                    @Override
                    public int compare(Photo o1, Photo o2)
                    {
                        return o2.getDate_created().compareTo(o1.getDate_created());
                    }
                });

                int iterations = mPhotos.size();

                if(iterations > 10)
                {
                    iterations = 10;
                }

                mResults = 10;

                for(int i = 0; i < iterations; i++)
                {
                    mPaginatedPhotos.add(mPhotos.get(i));
                }

                mAdapter = new MainfeedListAdapter(this, R.layout.layout_mainfeed_listitem, mPaginatedPhotos);
                mListView.setAdapter(mAdapter);

                mListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
                {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
                    {
                        String photoID = mPhotos.get(position).getPhoto_id();
                        //send this uuid.
                        SharedPreferences sharedPreferences = mContext.getSharedPreferences("photoID", Context.MODE_PRIVATE);
                        SharedPreferences.Editor ed = sharedPreferences.edit();
                        ed.putInt("checker", 0);
                        ed.putString("photo", photoID);
                        ed.apply();

                        Intent detailActivity = new Intent(FeedActivity.this, DetailActivity.class);
                        FeedActivity.this.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                        detailActivity.putExtra(String.valueOf(R.string.to_detail), "detail");
                        startActivity(detailActivity);

                    }
                });

                setupRView();

            }
            catch (NullPointerException e)
            {
                Log.e(TAG, "displayPhotos: NullPointerException: " + e.getMessage());
            }
            catch (IndexOutOfBoundsException e)
            {
                Log.e(TAG, "displayPhotos: IndexOutOfBoundsException: " + e.getMessage() );
            }
        }
    }

    @Override
    public void recyclerViewListClicked(View v, int position)
    {
        //send this uuid.
        String photoID = tPhotos.get(position).getPhoto_id();
        SharedPreferences sharedPreferences = this.getSharedPreferences("photoID", Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = sharedPreferences.edit();
        ed.putString("photo", photoID);
        ed.putInt("checker", 1);
        ed.apply();

        Intent detailActivity = new Intent(this, DetailActivity.class);
        detailActivity.putExtra(String.valueOf(R.string.to_detail), "detail");
        startActivity(detailActivity);
    }

    public void setupRView()
    {
        if(imgUrls.size() > 0)
        {
            RecyclerImagerAdapter adapter = new RecyclerImagerAdapter(mContext, imgUrls,this);
            GridLayoutManager manager = new GridLayoutManager(mContext,1);
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

    public void displayMorePhotos()
    {
        Log.d(TAG, "displayMorePhotos: displaying more photos");

        try
        {
            if(mPhotos.size() > mResults && mPhotos.size() > 0)
            {
                int iterations;

                if(mPhotos.size() > (mResults + 10))
                {
                    Log.d(TAG, "displayMorePhotos: there are greater than 10 more photos");
                    iterations = 10;
                }
                else
                {
                    Log.d(TAG, "displayMorePhotos: there is less than 10 more photos");
                    iterations = mPhotos.size() - mResults;
                }

                //add the new photos to the paginated results
                for(int i = mResults; i < mResults + iterations; i++)
                {
                    mPaginatedPhotos.add(mPhotos.get(i));
                }

                mResults = mResults + iterations;
                mAdapter.notifyDataSetChanged();
            }
        }
        catch (NullPointerException e)
        {
            Log.e(TAG, "displayPhotos: NullPointerException: " + e.getMessage() );
        }
        catch (IndexOutOfBoundsException e)
        {
            Log.e(TAG, "displayPhotos: IndexOutOfBoundsException: " + e.getMessage() );
        }
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
    public void onStart()
    {
        super.onStart();

        GeofenceTransitionsIntentService.shouldContinue = false;
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
    protected void onPause()
    {
        super.onPause();

    }

    @Override
    protected void onResume()
    {
        super.onResume();

    }

    @Override
    protected void onRestart()
    {
        super.onRestart();
        //reload this activity if the back button is pressed from fragment
        finish();
        startActivity(getIntent());
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

    @Override
    public void onLoadMoreItems()
    {
        displayMorePhotos();
    }

    private void requestLocationUpdates()
    {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_REQUEST_FINE_LOCATION);
            } else {
                permissionIsGranted = true;
            }
            return;
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode)
        {
            case MY_PERMISSION_REQUEST_FINE_LOCATION:

                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    // permission granted
                    permissionIsGranted = true;
                }
                else
                {
                    //permission denied
                    permissionIsGranted = false;
                    Toast.makeText(getApplicationContext(), "This app requires location permission to be granted", Toast.LENGTH_SHORT).show();

                }
                break;
            case MY_PERMISSION_REQUEST_COARSE_LOCATION:
                // do something for coarse location
                break;
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

    public boolean checkPermissions(String permission)
    {
        Log.d(TAG, "checkPermissions: checking permission: " + permission);

        int permissionRequest = ActivityCompat.checkSelfPermission(FeedActivity.this, permission);
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

    public void verifyPermissions(String[] permissions)
    {
        Log.d(TAG, "verifyPermissions: verifying permissions.");

        ActivityCompat.requestPermissions(FeedActivity.this, permissions, VERIFY_PERMISSIONS_REQUEST);
    }
}
