package com.example.adaminfiesto.droppit.Main;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.example.adaminfiesto.droppit.R;
import com.example.adaminfiesto.droppit.Utils.BottomNavigationViewHelper;
import com.example.adaminfiesto.droppit.Utils.Permissions;
import com.example.adaminfiesto.droppit.Utils.SectionsPagerAdapter;
import com.example.adaminfiesto.droppit.Utils.UniversalImageLoader;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;
import com.nostra13.universalimageloader.core.ImageLoader;


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



    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        mViewPager = findViewById(R.id.viewpager_container);
        mFrameLayout = findViewById(R.id.container);
        mRelativeLayout = findViewById(R.id.relLayoutParent);
        fab = findViewById(R.id.floatingActionButton);
        setupBottomNavigationView();

        if(checkPermissionsArray(Permissions.PERMISSIONS))
        {
            setupViewPager();
        }
        else
        {
            verifyPermissions(Permissions.PERMISSIONS);
        }

        //starts the universal image loader
        initImageLoader();

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
//                    Intent intent = new Intent(getActivity(), ShareActivity.class);
//                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                    startActivity(intent);
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

//                    Intent next = new Intent(String.valueOf(R.string.intent_location));
//                    next.putExtra("latlang", dalocation.toString());
//                    LocalBroadcastManager.getInstance(HomeActivity.this).sendBroadcast(next);

                SharedPreferences sharedPreferences = getSharedPreferences("Test",Context.MODE_PRIVATE);
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

    //we have merged our bottom and top tabs with the view to work along with the fragment
    //here we can use the layout_top_tabs.xml with the center_viewpager.xml to cycle through our fragments
    //remember to "include" the above xml into the activity_home.xml
    public int getCurrentTabNumber()
    {
        return mViewPager.getCurrentItem();
    }

    //top tab fragments that connect to the view pager
    private void setupViewPager()
    {
        SectionsPagerAdapter adapter = new SectionsPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new FragmentAR()); //index 0
        adapter.addFragment(new FragmentMap()); //index 1
        adapter.addFragment(new EventFragment());//index 2
        mViewPager.setAdapter(adapter);

        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);


        tabLayout.getTabAt(0).setText("AR");
        tabLayout.getTabAt(1).setText("Map");
        tabLayout.getTabAt(2).setText("Event");
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

    @Override
    protected void onStart()
    {
        super.onStart();
        mViewPager.setCurrentItem(1);
    }

    //TODO:Reload the fragment
    @Override
    protected void onResume()
    {
        super.onResume();

        Log.d(TAG, "onResume: " + dalocation);
    }

    @Override
    public void location(LatLng lat) {
        dalocation = lat;
    }

}
