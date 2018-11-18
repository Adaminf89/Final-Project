package com.example.adaminfiesto.droppit.UserProfile;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.adaminfiesto.droppit.DataModels.User;
import com.example.adaminfiesto.droppit.Google.GoogleActivity;
import com.example.adaminfiesto.droppit.R;
import com.example.adaminfiesto.droppit.Utils.BottomNavigationViewHelper;
import com.example.adaminfiesto.droppit.Utils.Permissions;
import com.google.firebase.auth.FirebaseAuth;
import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;

public class ProfileActivity extends AppCompatActivity
{

    private static final String TAG = "ProfileActivity";
    public Context mContext = ProfileActivity.this;
    private static final int ACTIVITY_NUM = 2;
    private static final int HOME_FRAGMENT = 1;
    private static final int VERIFY_PERMISSIONS_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        if(checkPermissionsArray(Permissions.PERMISSIONS))
        {
            init();
        }
        else
        {
            verifyPermissions(Permissions.PERMISSIONS);
        }

    }

    private void init()
    {
        Log.d(TAG, "init: inflating " + getString(R.string.profile_fragment));

        Intent intent = getIntent();

        if(intent.hasExtra(getString(R.string.calling_activity)))
        {
            Log.d(TAG, "init: searching for user object attached as intent extra");

            if(intent.hasExtra(getString(R.string.intent_user)))
            {
                User user = intent.getParcelableExtra(getString(R.string.intent_user));

                if(!user.getUser_id().equals(FirebaseAuth.getInstance().getCurrentUser().getUid()))
                {

                    //todo: load the other profile fragment
                    ViewProfileFragment fragment = new ViewProfileFragment();
                    Bundle args = new Bundle();
                    args.putParcelable(getString(R.string.intent_user), intent.getParcelableExtra(getString(R.string.intent_user)));
                    fragment.setArguments(args);

                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    transaction.replace(R.id.container, fragment);
                    transaction.commit();

                }
                else
                {
                    Log.d(TAG, "init: inflating Profile");
                    ProfileFragment fragment = new ProfileFragment();
                    FragmentTransaction transaction = ProfileActivity.this.getSupportFragmentManager().beginTransaction();
                    transaction.replace(R.id.container, fragment);
//                    transaction.addToBackStack(getString(R.string.profile_fragment));
                    transaction.commit();
                }
            }
            else
            {
                Toast.makeText(mContext, "something went wrong", Toast.LENGTH_SHORT).show();
            }

        }
        else
        {
            Log.d(TAG, "init: inflating Profile");
            ProfileFragment fragment = new ProfileFragment();
            FragmentTransaction transaction = ProfileActivity.this.getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.container, fragment);
//            transaction.addToBackStack(getString(R.string.profile_fragment));
            transaction.commit();
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

        int permissionRequest = ActivityCompat.checkSelfPermission(ProfileActivity.this, permission);
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

        ActivityCompat.requestPermissions(ProfileActivity.this, permissions, VERIFY_PERMISSIONS_REQUEST);
    }

}