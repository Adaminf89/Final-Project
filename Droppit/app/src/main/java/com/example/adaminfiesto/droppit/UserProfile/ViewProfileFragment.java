package com.example.adaminfiesto.droppit.UserProfile;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.adaminfiesto.droppit.DataModels.Comment;
import com.example.adaminfiesto.droppit.DataModels.Like;
import com.example.adaminfiesto.droppit.DataModels.Photo;
import com.example.adaminfiesto.droppit.DataModels.User;
import com.example.adaminfiesto.droppit.DataModels.UserAccountSettings;
import com.example.adaminfiesto.droppit.DataModels.UserSettings;
import com.example.adaminfiesto.droppit.Detail.DetailActivity;
import com.example.adaminfiesto.droppit.R;
import com.example.adaminfiesto.droppit.Utils.BottomNavigationViewHelper;
import com.example.adaminfiesto.droppit.Utils.RecyclerImagerAdapter;
import com.example.adaminfiesto.droppit.Utils.UniversalImageLoader;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ViewProfileFragment extends Fragment implements RecyclerImagerAdapter.RecyclerViewClickListener
{

    private static final String TAG = "ProfileFragment";

    public interface OnGridImageSelectedListener
    {
        void onGridImageSelected(Photo photo, int activityNumber);
    }
    OnGridImageSelectedListener mOnGridImageSelectedListener;

    private static final int ACTIVITY_NUM = 2;

    //firebase
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference myRef;

    //widgets
    private TextView mUsername, mDescription;
    private ProgressBar mProgressBar;
    private CircleImageView mProfilePhoto;
    private Button editProfile;
    private Button mUnfollow;
    private RecyclerView recyclerView;
    private Toolbar toolbar;
    private ImageView profileMenu;
    private BottomNavigationViewEx bottomNavigationView;
    private Context mContext;
    private ArrayList<Photo> mPhotos;
    ArrayList<String> imgUrls;

    //vars
    private User mUser;
    private int mFollowersCount = 0;
    private int mFollowingCount = 0;
    private int mPostsCount = 0;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_user_profile, container, false);
        bottomNavigationView = view.findViewById(R.id.bottomNavView);
        mProfilePhoto = view.findViewById(R.id.profile_photo);
        mUsername =  view.findViewById(R.id.profile_username);
        mDescription = view.findViewById(R.id.display_bio);
        mProgressBar = view.findViewById(R.id.profileProgressBar);
        toolbar = view.findViewById(R.id.profileToolBar);
        profileMenu = view.findViewById(R.id.profileMenu);
        editProfile = view.findViewById(R.id.editButton);
        mUnfollow = view.findViewById(R.id.unfollowbtn);
        recyclerView = view.findViewById(R.id.recyclerview);
        profileMenu.setVisibility(View.GONE);
        editProfile.setText("FOLLOW");
        mContext = getActivity();
        mPhotos = new ArrayList<>();
        imgUrls = new ArrayList<>();
        Log.d(TAG, "onCreateView: stared.");

        try
        {
            mUser = getUserFromBundle();
            init();
        }
        catch (NullPointerException e)
        {
            Log.e(TAG, "onCreateView: NullPointerException: "  + e.getMessage() );
            Toast.makeText(mContext, "something went wrong", Toast.LENGTH_SHORT).show();
            getActivity().getSupportFragmentManager().popBackStack();
        }

        setupBottomNavigationView();
        setupFirebaseAuth();
        isFollowing();
        getFollowingCount();
        getFollowersCount();
        getPostsCount();

        editProfile.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                FirebaseDatabase.getInstance().getReference()
                        .child(getString(R.string.dbname_following))
                        .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .child(mUser.getUser_id())
                        .child(getString(R.string.field_user_id))
                        .setValue(mUser.getUser_id());

                FirebaseDatabase.getInstance().getReference()
                        .child(getString(R.string.dbname_followers))
                        .child(mUser.getUser_id())
                        .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .child(getString(R.string.field_user_id))
                        .setValue(FirebaseAuth.getInstance().getCurrentUser().getUid());
                setFollowing();

            }
        });

        mUnfollow.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: now unfollowing: " + mUser.getUsername());

                FirebaseDatabase.getInstance().getReference()
                        .child(getString(R.string.dbname_following))
                        .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .child(mUser.getUser_id())
                        .removeValue();

                FirebaseDatabase.getInstance().getReference()
                        .child(getString(R.string.dbname_followers))
                        .child(mUser.getUser_id())
                        .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .removeValue();
                setUnfollowing();
            }
        });

        return view;
    }

    @Override
    public void recyclerViewListClicked(View v, int position)
    {
        //send this uuid.
        String photoID = mPhotos.get(position).getPhoto_id();
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("photoID", Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = sharedPreferences.edit();
        ed.putString("photo", photoID);
        ed.putInt("checker",1);
        ed.apply();

        Intent detailActivity = new Intent(getActivity(), DetailActivity.class);
        detailActivity.putExtra(String.valueOf(R.string.to_detail), "detail");
        startActivity(detailActivity);
    }

    public void setupRView()
    {
        if(imgUrls.size() > 0)
        {
            RecyclerImagerAdapter adapter = new RecyclerImagerAdapter(getContext(), imgUrls,this);
            GridLayoutManager manager = new GridLayoutManager(getActivity(),1);
            manager.setOrientation(LinearLayoutManager.HORIZONTAL);
            recyclerView.setHasFixedSize(true);
            recyclerView.setLayoutManager(manager);
            recyclerView.setAdapter(adapter);
        }
        else
        {
            return;
        }
    }

    private void init()
    {

        //set the profile widgets
        DatabaseReference reference1 = FirebaseDatabase.getInstance().getReference();
        Query query1 = reference1.child(getString(R.string.dbname_user_account_settings))
                .orderByChild(getString(R.string.field_user_id)).equalTo(mUser.getUser_id());
        query1.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot singleSnapshot :  dataSnapshot.getChildren()){
                    Log.d(TAG, "onDataChange: found user:" + singleSnapshot.getValue(UserAccountSettings.class).toString());

                    UserSettings settings = new UserSettings();
                    settings.setUser(mUser);
                    settings.setSettings(singleSnapshot.getValue(UserAccountSettings.class));
                    setProfileWidgets(settings);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        //get the users profile photos
        DatabaseReference reference2 = FirebaseDatabase.getInstance().getReference();
        //looking into userphoto
        Query query2 = reference2
                .child(getString(R.string.dbname_user_photos))
                .child(mUser.getUser_id());

        query2.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {

                ArrayList<Photo> photos = new ArrayList<Photo>();
                for ( DataSnapshot singleSnapshot :  dataSnapshot.getChildren()){

                    Photo photo = new Photo();
                    Map<String, Object> objectMap = (HashMap<String, Object>) singleSnapshot.getValue();

                    photo.setCaption(objectMap.get(getString(R.string.field_caption)).toString());
                    photo.setTags(objectMap.get(getString(R.string.field_tags)).toString());
                    photo.setPhoto_id(objectMap.get(getString(R.string.field_photo_id)).toString());
                    photo.setUser_id(objectMap.get(getString(R.string.field_user_id)).toString());
                    photo.setDate_created(objectMap.get(getString(R.string.field_date_created)).toString());
                    photo.setImage_path(objectMap.get(getString(R.string.field_image_path)).toString());

                    ArrayList<Comment> comments = new ArrayList<Comment>();
                    for (DataSnapshot dSnapshot : singleSnapshot
                            .child(getString(R.string.field_comments)).getChildren()){
                        Comment comment = new Comment();
                        comment.setUser_id(dSnapshot.getValue(Comment.class).getUser_id());
                        comment.setComment(dSnapshot.getValue(Comment.class).getComment());
                        comment.setDate_created(dSnapshot.getValue(Comment.class).getDate_created());
                        comments.add(comment);
                    }

                    photo.setComments(comments);

                    List<Like> likesList = new ArrayList<Like>();
                    for (DataSnapshot dSnapshot : singleSnapshot.child(getString(R.string.field_likes)).getChildren())
                    {
                        Like like = new Like();
                        like.setUser_id(dSnapshot.getValue(Like.class).getUser_id());
                        likesList.add(like);
                    }

                    photo.setLikes(likesList);
                    mPhotos.add(photo);
                }

                imgUrls = new ArrayList<String>();

                for(int i = 0; i < mPhotos.size(); i++)
                {
                    imgUrls.add(mPhotos.get(i).getImage_path());

                }

                setupRView();

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "onCancelled: query cancelled.");
            }

        });


    }

    private void isFollowing()
    {
        Log.d(TAG, "isFollowing: checking if following this users.");
        setUnfollowing();

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference.child(getString(R.string.dbname_following))
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .orderByChild(getString(R.string.field_user_id)).equalTo(mUser.getUser_id());
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot singleSnapshot :  dataSnapshot.getChildren()){
                    Log.d(TAG, "onDataChange: found user:" + singleSnapshot.getValue());

                    setFollowing();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    private void getFollowersCount()
    {
        mFollowersCount = 0;

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference.child(getString(R.string.dbname_followers))
                .child(mUser.getUser_id());
        query.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                for(DataSnapshot singleSnapshot :  dataSnapshot.getChildren())
                {
                    Log.d(TAG, "onDataChange: found follower:" + singleSnapshot.getValue());
                    mFollowersCount++;
                }
                //mFollowers.setText(String.valueOf(mFollowersCount));
            }

            @Override
            public void onCancelled(DatabaseError databaseError)
            {

            }
        });
    }

    private void getFollowingCount()
    {
        mFollowingCount = 0;

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference.child(getString(R.string.dbname_following))
                .child(mUser.getUser_id());
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot singleSnapshot :  dataSnapshot.getChildren()){
                    Log.d(TAG, "onDataChange: found following user:" + singleSnapshot.getValue());
                    mFollowingCount++;
                }
                //mFollowing.setText(String.valueOf(mFollowingCount));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void getPostsCount(){
        mPostsCount = 0;

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference.child(getString(R.string.dbname_user_photos))
                .child(mUser.getUser_id());
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot singleSnapshot :  dataSnapshot.getChildren()){
                    Log.d(TAG, "onDataChange: found post:" + singleSnapshot.getValue());
                    mPostsCount++;
                }
                //mPosts.setText(String.valueOf(mPostsCount));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void setFollowing()
    {
        Log.d(TAG, "setFollowing: updating UI for following this user");
        mUnfollow.setVisibility(View.VISIBLE);
        editProfile.setVisibility(View.GONE);
    }

    private void setUnfollowing()
    {
        Log.d(TAG, "setFollowing: updating UI for unfollowing this user");
        //mFollow.setVisibility(View.VISIBLE);
        mUnfollow.setVisibility(View.GONE);
        editProfile.setVisibility(View.VISIBLE);
    }

    private void setCurrentUsersProfile()
    {
        Log.d(TAG, "setFollowing: updating UI for showing this user their own profile");
        mUnfollow.setVisibility(View.GONE);
        editProfile.setVisibility(View.GONE);
    }

    private User getUserFromBundle()
    {
        Log.d(TAG, "getUserFromBundle: arguments: " + getArguments());

        Bundle bundle = this.getArguments();
        if(bundle != null)
        {
            return bundle.getParcelable(getString(R.string.intent_user));
        }
        else
            {
            return null;
        }
    }

    @Override
    public void onAttach(Context context) {
        try
        {
            mOnGridImageSelectedListener = (OnGridImageSelectedListener) getActivity();
        }catch (ClassCastException e){
            Log.e(TAG, "onAttach: ClassCastException: " + e.getMessage() );
        }
        super.onAttach(context);
    }


    private void setProfileWidgets(UserSettings userSettings)
    {

        UserAccountSettings settings = userSettings.getSettings();
        UniversalImageLoader.setImage(settings.getProfile_photo(), mProfilePhoto, mProgressBar, "");
        mUsername.setText(settings.getUsername());
        mDescription.setText(settings.getDescription());
        mProgressBar.setVisibility(View.GONE);

    }


        /**
     * BottomNavigationView setup
     */
    private void setupBottomNavigationView()
    {
        Log.d(TAG, "setupBottomNavigationView: setting up BottomNavigationView");
        BottomNavigationViewHelper.setupBottomNavigationView(bottomNavigationView);
        BottomNavigationViewHelper.enableNavigation(mContext,getActivity() ,bottomNavigationView);
        Menu menu = bottomNavigationView.getMenu();
        MenuItem menuItem = menu.getItem(ACTIVITY_NUM);
        menuItem.setChecked(true);
    }


    private void setupFirebaseAuth()
    {
        Log.d(TAG, "setupFirebaseAuth: setting up firebase auth.");

        mAuth = FirebaseAuth.getInstance();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        myRef = mFirebaseDatabase.getReference();

        mAuthListener = new FirebaseAuth.AuthStateListener()
        {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth)
            {
                FirebaseUser user = firebaseAuth.getCurrentUser();

                if (user != null)
                {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                }
                else
                    {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }

            }
        };


    }


    @Override
    public void onStart()
    {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop()
    {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }
}
