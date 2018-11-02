package com.example.adaminfiesto.droppit.Utils;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.adaminfiesto.droppit.DataModels.Comment;
import com.example.adaminfiesto.droppit.DataModels.Like;
import com.example.adaminfiesto.droppit.DataModels.Photo;
import com.example.adaminfiesto.droppit.DataModels.User;
import com.example.adaminfiesto.droppit.DataModels.UserAccountSettings;
import com.example.adaminfiesto.droppit.Detail.CommentActivity;
import com.example.adaminfiesto.droppit.Detail.DetailActivity;
import com.example.adaminfiesto.droppit.Feed.FeedActivity;
import com.example.adaminfiesto.droppit.R;
import com.example.adaminfiesto.droppit.UserProfile.ProfileActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.support.constraint.Constraints.TAG;

public class MainfeedListAdapter extends ArrayAdapter<Photo> {

    public interface OnLoadMoreItemsListener {
        void onLoadMoreItems();
    }

    OnLoadMoreItemsListener mOnLoadMoreItemsListener;

    private static final String TAG = "MainfeedListAdapter";
    private LayoutInflater mInflater;
    private int mLayoutResource;
    private Context mContext;
    private DatabaseReference mReference;
    private String currentUsername = "";
    private LatLng thePlaceToShow;
    FusedLocationProviderClient fusedLocationProviderClient;

    public MainfeedListAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull List<Photo> objects) {
        super(context, resource, objects);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mLayoutResource = resource;
        this.mContext = context;
        mReference = FirebaseDatabase.getInstance().getReference();
    }

    static class ViewHolder
    {
        ListView mListView;
        CircleImageView mprofileImage;
        CircleImageView image;
        String likesString;
        TextView username, distance, timeDetla, caption, comments;

        //var for each post user
        UserAccountSettings settings = new UserAccountSettings();
        User user = new User();
        StringBuilder users;
        boolean likeByCurrentUser;
        GestureDetector detector;
        Photo photo;
    }

    @NonNull
    @Override
    public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        final ViewHolder holder;

        if (convertView == null)
        {
            convertView = mInflater.inflate(mLayoutResource, parent, false);
            holder = new ViewHolder();

            holder.mprofileImage = convertView.findViewById(R.id.profile_photo);
            holder.username = convertView.findViewById(R.id.username);
            holder.caption = convertView.findViewById(R.id.image_caption);
            holder.comments = convertView.findViewById(R.id.image_comments_link);
            holder.distance = convertView.findViewById(R.id.image_Distance);
            holder.timeDetla = convertView.findViewById(R.id.image_time_posted);
            holder.image = convertView.findViewById(R.id.post_image);
            holder.mListView = convertView.findViewById(R.id.listViewFeed);
            holder.photo = getItem(position);
            holder.detector = new GestureDetector(mContext, new GestureListener(holder));
            holder.users = new StringBuilder();
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(mContext);

            convertView.setTag(holder);

        } else {
            holder = (ViewHolder) convertView.getTag();
        }


        //get the current users username (need for checking likes string)
        getCurrentUsername();

        //get likes string
        getLikesString(holder);

        //set the caption
        holder.caption.setText(getItem(position).getCaption());

        if (ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return null;
        }

        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>()
        {
            public void onSuccess(Location location)
            {

                thePlaceToShow = new LatLng(location.getLatitude(), location.getLongitude());
                LatLng latM = new LatLng(Double.valueOf(getItem(position).getLocation()), Double.valueOf(getItem(position).getLocationlong()));
                double dis = CalculationByDistance(latM, thePlaceToShow);
                DecimalFormat df = new DecimalFormat("#.##");

                holder.distance.setText(String.valueOf(df.format(dis))+"m");

            }
        });

        //set the comment
        List<Comment> comments = getItem(position).getComments();

        holder.comments.setText("View all " + comments.size() + " comments");

        holder.comments.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {

                Intent intentCom = new Intent(getContext(), CommentActivity.class);
                intentCom.putExtra("ComData", getItem(position));
                intentCom.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                getContext().startActivity(intentCom);
            }
        });

        holder.image.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String id = getItem(position).getPhoto_id();

                SharedPreferences sharedPreferences = getContext().getSharedPreferences("photoID", Context.MODE_PRIVATE);
                SharedPreferences.Editor ed = sharedPreferences.edit();
                ed.putInt("checker", 0);
                ed.putString("photo", id);
                ed.apply();


                Intent detailActivity = new Intent(mContext, DetailActivity.class);
                //overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                detailActivity.putExtra(String.valueOf(R.string.to_detail), "detail");
                mContext.startActivity(detailActivity);
            }
        });

        //set the time it was posted
        String timestampDifference = getTimestampDifference(getItem(position));

        if(!timestampDifference.equals("0"))
        {
            holder.timeDetla.setText(timestampDifference + " DAYS AGO");
        }
        else{
            holder.timeDetla.setText("TODAY");
        }

        //set the profile image
        final ImageLoader imageLoader = ImageLoader.getInstance();
        imageLoader.displayImage(getItem(position).getImage_path(), holder.image);

        //get the profile image and username
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference.child(mContext.getString(R.string.dbname_user_account_settings)).orderByChild(mContext.getString(R.string.field_user_id)).equalTo(getItem(position).getUser_id());

        query.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                for(DataSnapshot singleSnapshot : dataSnapshot.getChildren())
                {

                    Log.d(TAG, "onDataChange: found user: " + singleSnapshot.getValue(UserAccountSettings.class).getUsername());

                    holder.username.setText(singleSnapshot.getValue(UserAccountSettings.class).getUsername());
                    holder.username.setOnClickListener(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View v)
                        {
                            Log.d(TAG, "onClick: navigating to profile of: " +
                                    holder.user.getUsername());

                            Intent intent = new Intent(mContext, ProfileActivity.class);

                            intent.putExtra(mContext.getString(R.string.calling_activity), mContext.getString(R.string.home_activity));

                            intent.putExtra(mContext.getString(R.string.intent_user), holder.user);
                            mContext.startActivity(intent);
                        }

                    });

                    //todo: create the flow to the users profile other than current users
                    imageLoader.displayImage(singleSnapshot.getValue(UserAccountSettings.class).getProfile_photo(),holder.mprofileImage);
                    holder.mprofileImage.setOnClickListener(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View v)
                        {
                            Log.d(TAG, "onClick: navigating to profile of: " +
                                    holder.user.getUsername());

                            Intent intent = new Intent(mContext, ProfileActivity.class);
                            intent.putExtra(mContext.getString(R.string.calling_activity),mContext.getString(R.string.home_activity));
                            intent.putExtra(mContext.getString(R.string.intent_user), holder.user);

                            mContext.startActivity(intent);
                        }
                    });

                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError)
            {

            }
        });

        //get the user object
        Query userQuery = mReference
                .child(mContext.getString(R.string.dbname_users))
                .orderByChild(mContext.getString(R.string.field_user_id))
                .equalTo(getItem(position).getUser_id());

        userQuery.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                for(DataSnapshot singleSnapshot : dataSnapshot.getChildren())
                {
                    Log.d(TAG, "onDataChange: found user: " +
                    singleSnapshot.getValue(User.class).getUsername());

                    holder.user = singleSnapshot.getValue(User.class);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError)
            {

            }
        });

        if(reachedEndOfList(position))
        {
            loadMoreData();
        }

        return convertView;
    }

    private boolean reachedEndOfList(int position)
    {
        return position == getCount() - 1;
    }

    private void loadMoreData()
    {

        try
        {
            mOnLoadMoreItemsListener = (OnLoadMoreItemsListener) getContext();
        }
        catch (ClassCastException e)
        {
            Log.e(TAG, "loadMoreData: ClassCastException: " +e.getMessage() );
        }

        try
        {
            mOnLoadMoreItemsListener.onLoadMoreItems();
        }
        catch (NullPointerException e)
        {
            Log.e(TAG, "loadMoreData: ClassCastException: " +e.getMessage() );
        }
    }

    public class GestureListener extends GestureDetector.SimpleOnGestureListener
    {

        ViewHolder mHolder;
        public GestureListener(ViewHolder holder) {
            mHolder = holder;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            Log.d(TAG, "onDoubleTap: double tap detected.");

            DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
            Query query = reference.child(mContext.getString(R.string.dbname_photos)).child(mHolder.photo.getPhoto_id()).child(mContext.getString(R.string.field_likes));

            query.addListenerForSingleValueEvent(new ValueEventListener()
            {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot)
                {
                    for(DataSnapshot singleSnapshot : dataSnapshot.getChildren())
                    {

                        String keyID = singleSnapshot.getKey();

                        //case1: Then user already liked the photo
                        if(mHolder.likeByCurrentUser && singleSnapshot.getValue(Like.class).getUser_id()
                                .equals(FirebaseAuth.getInstance().getCurrentUser().getUid()))
                        {

                            mReference.child(mContext.getString(R.string.dbname_photos))
                                    .child(mHolder.photo.getPhoto_id())
                                    .child(mContext.getString(R.string.field_likes))
                                    .child(keyID)
                                    .removeValue();
///
                            mReference.child(mContext.getString(R.string.dbname_user_photos))
                                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                    .child(mHolder.photo.getPhoto_id())
                                    .child(mContext.getString(R.string.field_likes))
                                    .child(keyID)
                                    .removeValue();

//                            mHolder.heart.toggleLike();
                            getLikesString(mHolder);
                        }
                        //case2: The user has not liked the photo
                        else if(!mHolder.likeByCurrentUser)
                        {
                            //add new like
                            addNewLike(mHolder);
                            break;
                        }
                    }
                    if(!dataSnapshot.exists()){
                        //add new like
                        addNewLike(mHolder);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError)
                {

                }
            });

            return true;
        }
    }

    private void addNewLike(final ViewHolder holder)
    {
        Log.d(TAG, "addNewLike: adding new like");

        String newLikeID = mReference.push().getKey();
        Like like = new Like();
        like.setUser_id(FirebaseAuth.getInstance().getCurrentUser().getUid());

//        mReference.child(mContext.getString(R.string.dbname_photos))
//                .child(holder.photo.getPhoto_id())
//                .child(mContext.getString(R.string.field_likes))
//                .child(newLikeID)
//                .setValue(like);
//
//        mReference.child(mContext.getString(R.string.dbname_user_photos))
//                .child(holder.photo.getUser_id())
//                .child(holder.photo.getPhoto_id())
//                .child(mContext.getString(R.string.field_likes))
//                .child(newLikeID)
//                .setValue(like);

        getLikesString(holder);
    }

    private void getCurrentUsername()
    {
        Log.d(TAG, "getCurrentUsername: retrieving user account settings");
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

        Query query = reference
                .child(mContext.getString(R.string.dbname_users))
                .orderByChild(mContext.getString(R.string.field_user_id))
                .equalTo(FirebaseAuth.getInstance().getCurrentUser().getUid());
        query.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot singleSnapshot : dataSnapshot.getChildren()){
                    currentUsername = singleSnapshot.getValue(UserAccountSettings.class).getUsername();
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void getDeviceLocation()
    {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>()
        {
            public void onSuccess(Location location)
            {

//                thePlaceToShow = new LatLng(location.getLatitude(), location.getLongitude());
//
//
//                LatLng latM = new LatLng(Double.valueOf(getItem(position).getLocation()), Double.valueOf(getItem(position).getLocationlong()));
//                double dis = CalculationByDistance(latM, thePlaceToShow);

            }
        });

    }

    public double CalculationByDistance(LatLng StartP, LatLng EndP)
    {
        int Radius = 6371;// radius of earth in Km
        double lat1 = StartP.latitude;
        double lat2 = EndP.latitude;
        double lon1 = StartP.longitude;
        double lon2 = EndP.longitude;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);

        double c = 2 * Math.asin(Math.sqrt(a));
        double valueResult = Radius * c;
        double km = valueResult / 1;
        DecimalFormat newFormat = new DecimalFormat("####");
        int kmInDec = Integer.valueOf(newFormat.format(km));
        double meter = valueResult % 1000;
        int meterInDec = Integer.valueOf(newFormat.format(meter));
        Log.i("Radius Value", "" + valueResult + "   KM  " + kmInDec
                + " Meter   " + meterInDec);

        return Radius * c;
    }

    private void getLikesString(final ViewHolder holder)
    {
        Log.d(TAG, "getLikesString: getting likes string");

        try
        {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

        Query query = reference
                .child(mContext.getString(R.string.dbname_photos))
                .child(holder.photo.getPhoto_id())
                .child(mContext.getString(R.string.field_likes));

        query.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                holder.users = new StringBuilder();
                for(DataSnapshot singleSnapshot : dataSnapshot.getChildren()){

                    DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
                    Query query = reference
                            .child(mContext.getString(R.string.dbname_users))
                            .orderByChild(mContext.getString(R.string.field_user_id))
                            .equalTo(singleSnapshot.getValue(Like.class).getUser_id());

                    query.addListenerForSingleValueEvent(new ValueEventListener()
                    {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot)
                        {
                            for(DataSnapshot singleSnapshot : dataSnapshot.getChildren())
                            {
                                Log.d(TAG, "onDataChange: found like: " +
                                        singleSnapshot.getValue(User.class).getUsername());

                                holder.users.append(singleSnapshot.getValue(User.class).getUsername());
                                holder.users.append(",");
                            }

                            String[] splitUsers = holder.users.toString().split(",");

                            if(holder.users.toString().contains(currentUsername + ",")){//mitch, mitchell.tabian
                                holder.likeByCurrentUser = true;
                            }
                            else
                                {
                                holder.likeByCurrentUser = false;
                            }

                            int length = splitUsers.length;

                            if(length == 1)
                            {
                                holder.likesString = "Liked by " + splitUsers[0];
                            }
                            else if(length == 2)
                            {
                                holder.likesString = "Liked by " + splitUsers[0]
                                        + " and " + splitUsers[1];
                            }
                            else if(length == 3)
                            {
                                holder.likesString = "Liked by " + splitUsers[0]
                                        + ", " + splitUsers[1]
                                        + " and " + splitUsers[2];

                            }
                            else if(length == 4){
                                holder.likesString = "Liked by " + splitUsers[0]
                                        + ", " + splitUsers[1]
                                        + ", " + splitUsers[2]
                                        + " and " + splitUsers[3];
                            }
                            else if(length > 4)
                            {
                                holder.likesString = "Liked by " + splitUsers[0]
                                        + ", " + splitUsers[1]
                                        + ", " + splitUsers[2]
                                        + " and " + (splitUsers.length - 3) + " others";
                            }
                            Log.d(TAG, "onDataChange: likes string: " + holder.likesString);
                            //setup likes string
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }
                if(!dataSnapshot.exists())
                {
                    holder.likesString = "";
                    holder.likeByCurrentUser = false;
                    //setup likes string
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        }
        catch (NullPointerException e)
        {
            Log.e(TAG, "getLikesString: NullPointerException: " + e.getMessage() );
            holder.likesString = "";
            holder.likeByCurrentUser = false;
            //setup likes string
        }
    }

    private String getTimestampDifference(Photo photo)
    {
        Log.d(TAG, "getTimestampDifference: getting timestamp difference.");

        String difference = "";
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.CANADA);
        sdf.setTimeZone(TimeZone.getTimeZone("Canada/Pacific"));//google 'android list of timezones'
        Date today = c.getTime();
        sdf.format(today);
        Date timestamp;
        final String photoTimestamp = photo.getDate_created();

        try
        {
            timestamp = sdf.parse(photoTimestamp);
            difference = String.valueOf(Math.round(((today.getTime() - timestamp.getTime()) / 1000 / 60 / 60 / 24 )));
        }
        catch (ParseException e)
        {
            Log.e(TAG, "getTimestampDifference: ParseException: " + e.getMessage() );
            difference = "0";
        }
        return difference;
    }

}





























