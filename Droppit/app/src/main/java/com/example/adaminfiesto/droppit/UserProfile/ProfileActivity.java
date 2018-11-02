package com.example.adaminfiesto.droppit.UserProfile;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.adaminfiesto.droppit.DataModels.User;
import com.example.adaminfiesto.droppit.Main.HomeActivity;
import com.example.adaminfiesto.droppit.R;
import com.example.adaminfiesto.droppit.Utils.BottomNavigationViewHelper;
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
        init();
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

}