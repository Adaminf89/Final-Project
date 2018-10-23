package com.example.adaminfiesto.droppit.AR;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.example.adaminfiesto.droppit.R;
import com.example.adaminfiesto.droppit.Utils.BottomNavigationViewHelper;
import com.example.adaminfiesto.droppit.Utils.FirebaseMethods;
import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;

public class ARActivity extends AppCompatActivity
{
    public Context mContext = ARActivity.this;
    private static final int ACTIVITY_NUM = 3;
    private static final int AR_FRAGMENT = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar);
        setupBottomNavigationView();
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
