package com.example.adaminfiesto.droppit.Detail;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import com.example.adaminfiesto.droppit.R;
import com.example.adaminfiesto.droppit.UserProfile.ProfileActivity;
import com.example.adaminfiesto.droppit.UserProfile.ProfileFragment;

import static com.example.adaminfiesto.droppit.R.layout.detail_activity;

public class DetailActivity extends AppCompatActivity
{

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detail_activity);
        getData();

    }

    private void getData()
    {
        Intent getIntent = getIntent();
        String checker = getIntent.getStringExtra(String.valueOf(R.string.to_detail));

        if(checker.equals("detail"))
        {
            DetailFragmentPrivate fragment = new DetailFragmentPrivate();
            FragmentTransaction transaction = DetailActivity.this.getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.container, fragment);
            //transaction.addToBackStack(getString(R.string.profile_fragment));
            transaction.commit();
        }
    }

}
