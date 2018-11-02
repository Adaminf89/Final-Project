package com.example.adaminfiesto.droppit.Utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.view.MenuItem;

import com.example.adaminfiesto.droppit.AR.ARActivity;
import com.example.adaminfiesto.droppit.Feed.FeedActivity;
import com.example.adaminfiesto.droppit.Google.GoogleActivity;
import com.example.adaminfiesto.droppit.Main.HomeActivity;
import com.example.adaminfiesto.droppit.R;
import com.example.adaminfiesto.droppit.Search.SearchActivity;
import com.example.adaminfiesto.droppit.UserProfile.ProfileActivity;
import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;

public class BottomNavigationViewHelper
{
    public static void setupBottomNavigationView(BottomNavigationViewEx bottomNavigationViewEx)
    {

        bottomNavigationViewEx.enableAnimation(false);
        bottomNavigationViewEx.enableItemShiftingMode(false);
        bottomNavigationViewEx.enableShiftingMode(false);
        bottomNavigationViewEx.setTextVisibility(false);
    }

    //determines which activity we're going to and from
    public static void enableNavigation(final Context context, final Activity callingActivity, BottomNavigationViewEx view)
    {
        //changer for each bottom tab pressed set to transition to another activity
        view.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener()
        {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item)
            {

                //get the bottom bar menu items that are from menu.xml file
                switch (item.getItemId())
                {
                    case R.id.ic_home:

                        Intent intentHome = new Intent(context, GoogleActivity.class);
                        intentHome.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        context.startActivity(intentHome);
                        callingActivity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

                        break;

//                    case R.id.ic_search:
//
//                        Intent intentSea = new Intent(context, SearchActivity.class);
//                        intentSea.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                        context.startActivity(intentSea);
//                        callingActivity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
//                        break;

                    case R.id.ic_feed:

                        Intent intentFeed = new Intent(context, FeedActivity.class);
                        intentFeed.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        context.startActivity(intentFeed);
                        callingActivity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                        break;

                    case R.id.ic_account:

                        Intent intentAcc = new Intent(context, ProfileActivity.class);
                        //intentAcc.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        context.startActivity(intentAcc);
                        callingActivity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                        break;

//                    case R.id.ic_vr:
//
//                        Intent intentAR = new Intent(context, ARActivity.class);
//                        intentAR.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                        context.startActivity(intentAR);
//                        callingActivity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
//                        break;
                }
                return false;
            }
        });
    }

}
