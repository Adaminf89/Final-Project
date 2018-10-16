package com.example.adaminfiesto.droppit.UserProfile;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.example.adaminfiesto.droppit.Main.NextActivity;
import com.example.adaminfiesto.droppit.R;
import com.example.adaminfiesto.droppit.Utils.BottomNavigationViewHelper;
import com.example.adaminfiesto.droppit.Utils.FirebaseMethods;
import com.example.adaminfiesto.droppit.Utils.SectionsStatePagerAdapter;
import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;

import java.util.ArrayList;

public class AccountSettingActivity extends AppCompatActivity
{

    private static final String TAG = "AccountSettingsActivity";
    private static final int ACTIVITY_NUM = 2;
    private Button btnSignout;
    private Button btnEdit;
    private Context mContext;
    public SectionsStatePagerAdapter pagerAdapter;
    private ViewPager mViewPager;
    private RelativeLayout mRelativeLayout;
    private static final int CAMERA_REQUEST_CODE = 6;

    //TODO:add about btn and info
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_settings);

        mContext = AccountSettingActivity.this;
        Log.d(TAG, "onCreate: started.");
        btnSignout = findViewById(R.id.btn_logout);
        btnEdit = findViewById(R.id.btn_info);
        mViewPager = findViewById(R.id.viewpager_container);
        mRelativeLayout = findViewById(R.id.relLayout1);
        Bitmap bitmap;
        setupSettingsList();
        setupBottomNavigationView();
        setupFragments();
        getIncomingIntent();

    }



    private void getIncomingIntent()
    {
        Intent intent = getIntent();

//        if(intent.hasExtra(getString(R.string.selected_image)) || intent.hasExtra(getString(R.string.selected_bitmap)))
//        {
//
//            //if there is an imageUrl attached as an extra, then it was chosen from the gallery/photo fragment
//            Log.d(TAG, "getIncomingIntent: New incoming imgUrl");
//            if(intent.getStringExtra(getString(R.string.return_to_fragment)).equals(getString(R.string.edit_profile_fragment)))
//            {
//
//                if(intent.hasExtra(getString(R.string.selected_image)))
//                {
//                    //set the new profile picture
////                    FirebaseMethods firebaseMethods = new FirebaseMethods(AccountSettingActivity.this);
////                    firebaseMethods.uploadNewPhoto(getString(R.string.profile_photo), null, 0,
////                            intent.getStringExtra(getString(R.string.selected_image)), null);
//                    return;
//                }
//                else if(intent.hasExtra(getString(R.string.selected_bitmap)))
//                {
//                    //set the new profile picture
//                    FirebaseMethods firebaseMethods = new FirebaseMethods(AccountSettingActivity.this);
//                    firebaseMethods.uploadNewPhoto(getString(R.string.new_photo),null, null,
//                            0,null, (Bitmap) intent.getParcelableExtra(getString(R.string.selected_bitmap)),
//                            null, null);
//                }
//
//            }
//
//        }

        if(intent.hasExtra(getString(R.string.calling_activity)))
        {
            Log.d(TAG, "getIncomingIntent: received incoming intent from " + getString(R.string.profile_activity));
            setViewPager(pagerAdapter.getFragmentNumber(getString(R.string.edit_profile_fragment)));
        }
    }


    private void setupFragments()
    {
        pagerAdapter = new SectionsStatePagerAdapter(getSupportFragmentManager());
        pagerAdapter.addFragment(new EditProfileFragment(), getString(R.string.edit_profile_fragment)); //fragment 0
        pagerAdapter.addFragment(new SignOutFragment(), getString(R.string.sign_out_fragment)); //fragment 1
    }

    public void setViewPager(int fragmentNumber)
    {
        mRelativeLayout.setVisibility(View.GONE);
        Log.d(TAG, "setViewPager: navigating to fragment #: " + fragmentNumber);
        //instantiates the view fragment
        mViewPager.setAdapter(pagerAdapter);
        //nav to the item clicked
        mViewPager.setCurrentItem(fragmentNumber);
    }

    private void setupSettingsList()
    {
        Log.d(TAG, "setupSettingsList: initializing 'Account Settings' list.");
        Log.d(TAG, "setupSettingsList: initializing 'Account Settings' list.");


        btnSignout.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                setViewPager(0);
            }
        });

        btnEdit.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                setViewPager(1);
            }
        });

    }

    private void setupBottomNavigationView()
    {
        Log.d(TAG, "setupBottomNavigationView: setting up BottomNavigationView");
        BottomNavigationViewEx bottomNavigationViewEx = findViewById(R.id.bottomNavView);
        BottomNavigationViewHelper.setupBottomNavigationView(bottomNavigationViewEx);
        BottomNavigationViewHelper.enableNavigation(mContext, this,bottomNavigationViewEx);
        Menu menu = bottomNavigationViewEx.getMenu();
        MenuItem menuItem = menu.getItem(ACTIVITY_NUM);
        menuItem.setChecked(true);
    }





}
