package com.example.adaminfiesto.droppit.Detail;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.adaminfiesto.droppit.DataModels.Photo;
import com.example.adaminfiesto.droppit.DataModels.UserAccountSettings;
import com.example.adaminfiesto.droppit.DataModels.UserSettings;
import com.example.adaminfiesto.droppit.Main.HomeActivity;
import com.example.adaminfiesto.droppit.R;
import com.example.adaminfiesto.droppit.Utils.FirebaseMethods;
import com.example.adaminfiesto.droppit.Utils.UniversalImageLoader;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DetailFragmentPrivate extends Fragment
{

    private static final String TAG = "";
    Photo pData;
    TextView tvCaption;
    TextView tvDate;
    TextView tvDistance;
    TextView tvDropTitle;
    ImageView ivDropPhoto;
    Button deleteBtn;
    FirebaseUser currentUser;
    private String Uuid;
    private DatabaseReference myRef;

    //photo data that is needed to be passed.
    public static DetailFragmentPrivate newInstance(Photo pdata)
    {

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
        tvDropTitle = view.findViewById(R.id.dropName);
        tvDistance = view.findViewById(R.id.textDistance);
        ivDropPhoto = view.findViewById(R.id.event_image);
        deleteBtn = view.findViewById(R.id.delete_btn);

        //since were doing this page a bit differencly were bandaiding some already created fb methods
        myRef = FirebaseDatabase.getInstance().getReference();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        Uuid = currentUser.getUid().toString();

        //user the passed data from arg
        if(getArguments() != null)
        {
            pData = (Photo) getArguments().getParcelable("Photo");
        }
        //show the btn if this drop matches the user id
        if(pData.getUser_id().equals(Uuid))
        {
            deleteBtn.setVisibility(View.VISIBLE);
        }

        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                myRef.child(getContext().getString(R.string.dbname_user_photos))
                        .child(Uuid)
                        .child(pData.getPhoto_id())
                        .removeValue();

                myRef.child(getContext().getString(R.string.dbname_photos))
                        .child(pData.getPhoto_id())
                        .removeValue();

                Intent intentHome = new Intent(getContext(), HomeActivity.class);
                intentHome.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                getContext().startActivity(intentHome);
                getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);


            }
        });


        setProfileWidgets();

        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if(getArguments() != null)
        {
            pData = (Photo) getArguments().getParcelable("Photo");
        }
    }

    private void setProfileWidgets()
    {
        Log.d(TAG, "setProfileWidgets: is the user nil ");
        UniversalImageLoader.setImage(pData.getImage_path(), ivDropPhoto,null,"");
        tvDropTitle.setText("Drop Details");
        tvCaption.setText(pData.getCaption());
        tvDate.setText(pData.getDate_created());
        tvDistance.setText("loading...");

    }
}
