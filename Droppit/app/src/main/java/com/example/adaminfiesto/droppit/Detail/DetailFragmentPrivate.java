package com.example.adaminfiesto.droppit.Detail;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.adaminfiesto.droppit.DataModels.Photo;
import com.example.adaminfiesto.droppit.DataModels.UserAccountSettings;
import com.example.adaminfiesto.droppit.DataModels.UserSettings;
import com.example.adaminfiesto.droppit.R;
import com.example.adaminfiesto.droppit.Utils.UniversalImageLoader;
import com.google.android.gms.maps.model.LatLng;

public class DetailFragmentPrivate extends Fragment
{

    private static final String TAG = "";
    Photo pData;
    TextView tvCaption;
    TextView tvDate;
    TextView tvDistance;
    ImageView ivDropPhoto;

    public static DetailFragmentPrivate newInstance(Photo pdata) {

        Bundle args = new Bundle();
        DetailFragmentPrivate fragment = new DetailFragmentPrivate();
        fragment.setArguments(args);
        args.putParcelable("Photo", pdata);


        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_drop_private, null);
        tvCaption = view.findViewById(R.id.caption);
        tvDate = view.findViewById(R.id.textDate);
        tvDistance = view.findViewById(R.id.textDistance);
        ivDropPhoto = view.findViewById(R.id.event_image);

        if(getArguments() != null)
        {
            pData = (Photo) getArguments().getParcelable("Photo");
        }

        setProfileWidgets();

        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null)
        {
            pData = (Photo) getArguments().getParcelable("Photo");
        }
    }

    private void setProfileWidgets()
    {

        Log.d(TAG, "setProfileWidgets: is the user nil ");
//
//        UserAccountSettings settings = userSettings.getSettings();
//
//        UniversalImageLoader.setImage(settings.getProfile_photo(), mProfilePhoto, mProgressBar, "");
//        mUsername.setText(settings.getUsername());
//        mDescription.setText(settings.getDescription());
//        mProgressBar.setVisibility(View.GONE);

        UniversalImageLoader.setImage(pData.getImage_path(), ivDropPhoto,null,"");
        tvCaption.setText(pData.getCaption());
        tvDate.setText(pData.getDate_created());
        tvDistance.setText("loading...");

    }
}
