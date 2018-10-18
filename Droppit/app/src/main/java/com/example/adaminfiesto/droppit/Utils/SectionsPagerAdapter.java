package com.example.adaminfiesto.droppit.Utils;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.ArrayList;
import java.util.List;

//used for holding small amount of fragments
public class SectionsPagerAdapter extends FragmentPagerAdapter
{

    private static final String TAG = "SectionsPagerAdapter";

    private final List<Fragment> mFragmentList = new ArrayList<>();


    public SectionsPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position)
    {
        return mFragmentList.get(position);
    }

    @Override
    public int getItemPosition(@NonNull Object object)
    {
        return POSITION_NONE;
    }

    @Override
    public int getCount() {
        return mFragmentList.size();
    }

    public void addFragment(Fragment fragment ){
        mFragmentList.add(fragment);
    }


}