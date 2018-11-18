package com.example.adaminfiesto.droppit.Utils;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;
import com.example.adaminfiesto.droppit.DataModels.Like;
import com.example.adaminfiesto.droppit.DataModels.Photo;
import com.example.adaminfiesto.droppit.DataModels.Trending;
import com.example.adaminfiesto.droppit.DataModels.UserSettings;
import com.example.adaminfiesto.droppit.Google.GoogleActivity;
import com.example.adaminfiesto.droppit.R;
import com.example.adaminfiesto.droppit.DataModels.User;
import com.example.adaminfiesto.droppit.DataModels.UserAccountSettings;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class FirebaseMethods
{

    private static final String TAG = "FirebaseMethods";

    //firebase
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseDatabase mFirebaseDatabase;
    private StorageReference mStorageReference;
    private double mPhotoUploadProgress = 0;
    private DatabaseReference myRef;
    private String userID;
    private Context mContext;
    Integer count = 0;

    public FirebaseMethods(Context context)
    {
        mAuth = FirebaseAuth.getInstance();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mStorageReference = FirebaseStorage.getInstance().getReference();
        myRef = mFirebaseDatabase.getReference();
        mContext = context;

        if(mAuth.getCurrentUser() != null)
        {
            userID = mAuth.getCurrentUser().getUid();
        }
    }

    //Will update the useraccountnode
    public void updateUserAccountSettings(String displayName, String website, String description, long phoneNumber)
    {
        Log.d(TAG, "updateUserAccountSettings: updating user account settings.");

        if(displayName != null)
        {
            myRef.child(mContext.getString(R.string.dbname_user_account_settings))
                    .child(userID)
                    .child(mContext.getString(R.string.field_display_name))
                    .setValue(displayName);
        }


        if(website != null)
        {
            myRef.child(mContext.getString(R.string.dbname_user_account_settings))
                    .child(userID)
                    .child(mContext.getString(R.string.field_website))
                    .setValue(website);
        }

        if(description != null)
        {
            myRef.child(mContext.getString(R.string.dbname_user_account_settings))
                    .child(userID)
                    .child(mContext.getString(R.string.field_description))
                    .setValue(description);
        }

        if(phoneNumber != 0)
        {
            myRef.child(mContext.getString(R.string.dbname_user_account_settings))
                    .child(userID)
                    .child(mContext.getString(R.string.field_phone_number))
                    .setValue(phoneNumber);
        }
    }


    //update username in the 'users' node and 'user_account_settings' node @param username
    public void updateUsername(String username)
    {
        Log.d(TAG, "updateUsername: upadting username to: " + username);

        myRef.child(mContext.getString(R.string.dbname_users))
                .child(userID)
                .child(mContext.getString(R.string.field_username))
                .setValue(username);

        myRef.child(mContext.getString(R.string.dbname_user_account_settings))
                .child(userID)
                .child(mContext.getString(R.string.field_username))
                .setValue(username);
    }

    public void sendVerificationEmail()
    {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if(user != null)
        {
            user.sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>()
                    {
                        @Override
                        public void onComplete(@NonNull Task<Void> task)
                        {
                            if(task.isSuccessful())
                            {
                                Log.d(TAG, "onComplete: email was fine");
                            }
                            else
                                {

                                Toast.makeText(mContext, "couldn't send verification email.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    // Register a new email and password to Firebase Authentication
    public void registerNewEmail(final String email, String password, final String username)
    {
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>()
                {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task)
                    {
                        Log.d(TAG, "createUserWithEmail:onComplete:" + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful())
                        {
                            Toast.makeText(mContext, "Failed",Toast.LENGTH_SHORT).show();

                        }
                        else if(task.isSuccessful())
                        {
                            sendVerificationEmail();
                            userID = mAuth.getCurrentUser().getUid();
                            Log.d(TAG, "onComplete: Authstate changed: " + userID);
                        }

                    }
                });
    }

    public void addNewUser(String email, String username, String description, String profile_photo)
    {

        User user = new User( userID,  email,  StringManipulation.condenseUsername(username));

        myRef.child(mContext.getString(R.string.dbname_users)).child(userID).setValue(user);


        UserAccountSettings settings = new UserAccountSettings(
                description,
                username,
                0,
                0,
                0,
                profile_photo,
                StringManipulation.condenseUsername(username),
                userID
        );

        myRef.child(mContext.getString(R.string.dbname_user_account_settings))
                .child(userID)
                .setValue(settings);

    }

    private String getTimestamp()
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("US/EST"));
        return sdf.format(new Date());
    }

    public int getImageCount(DataSnapshot dataSnapshot)
    {
        int count = 0;
        for(DataSnapshot ds: dataSnapshot.child(mContext.getString(R.string.dbname_user_photos)).child(FirebaseAuth.getInstance().getCurrentUser().getUid()).getChildren())
        {
            count++;
        }
        return count;
    }

    //this will get/pull: user account settings from the respected node
    //this is looping thru the top lvl nodes of the data base
    public UserSettings getUserSettings(DataSnapshot dataSnapshot)
    {
        Log.d(TAG, "getUserAccountSettings: retrieving user account settings from firebase.");


        UserAccountSettings settings  = new UserAccountSettings();
        User user = new User();

        for(DataSnapshot ds: dataSnapshot.getChildren())
        {
            //user_account_settings node will get the settings for the user with this USERID's data from firebase
            //looking at the key
            if(ds.getKey().equals(mContext.getString(R.string.dbname_user_account_settings)))
            {
                //note where we are in db
                Log.d(TAG, "getUserAccountSettings: user account settings node datasnapshot: " + ds);

                try {

                    settings.setDisplay_name(
                            ds.child(userID)
                                    .getValue(UserAccountSettings.class)
                                    .getDisplay_name()
                    );
                    settings.setUsername(
                            ds.child(userID)
                                    .getValue(UserAccountSettings.class)
                                    .getUsername()
                    );
                    settings.setDescription(
                            ds.child(userID)
                                    .getValue(UserAccountSettings.class)
                                    .getDescription()
                    );
                    settings.setProfile_photo(
                            ds.child(userID)
                                    .getValue(UserAccountSettings.class)
                                    .getProfile_photo()
                    );
                    settings.setPosts(
                            ds.child(userID)
                                    .getValue(UserAccountSettings.class)
                                    .getPosts()
                    );
                    settings.setFollowing(
                            ds.child(userID)
                                    .getValue(UserAccountSettings.class)
                                    .getFollowing()
                    );
                    settings.setFollowers(
                            ds.child(userID)
                                    .getValue(UserAccountSettings.class)
                                    .getFollowers()
                    );

                    Log.d(TAG, "getUserAccountSettings: retrieved user_account_settings information: " + settings.toString());
                }
                catch (NullPointerException e)
                {
                    Log.e(TAG, "getUserAccountSettings: NullPointerException: " + e.getMessage());
                }
            }

            // users node
            Log.d(TAG, "getUserSettings: snapshot key: " + ds.getKey());

            if(ds.getKey().equals(mContext.getString(R.string.dbname_users)))
            {
                Log.d(TAG, "getUserAccountSettings: users node datasnapshot: " + ds);

                user.setUsername(
                        ds.child(userID)
                                .getValue(User.class)
                                .getUsername()
                );
                user.setEmail(
                        ds.child(userID)
                                .getValue(User.class)
                                .getEmail()
                );
                user.setUser_id(
                        ds.child(userID)
                                .getValue(User.class)
                                .getUser_id()
                );

                Log.d(TAG, "getUserAccountSettings: retrieved users information: " + user.toString());
            }
        }

        //So that we dont run this twice we will return both user settings and acct settings
        return new UserSettings(user, settings);

    }

    private void addPhotoToDatabase(String caption, String url, String location, String locationlong, String mprivate)
    {
        Log.d(TAG, "addPhotoToDatabase: adding photo to database.");

        String tags = StringManipulation.getTags(caption);

        String newPhotoKey = myRef.child(mContext.getString(R.string.dbname_photos)).push().getKey();

        Photo photo = new Photo();
        photo.setCaption(caption);
        photo.setDate_created(getTimestamp());
        photo.setImage_path(url);
        photo.setTags(tags);
        photo.setLocation(location);
        photo.setLocationlong(locationlong);
        photo.setmPrivate(mprivate);
        photo.setUser_id(FirebaseAuth.getInstance().getCurrentUser().getUid());
        photo.setPhoto_id(newPhotoKey);

        //insert into database
        myRef.child(mContext.getString(R.string.dbname_user_photos)).child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child(newPhotoKey).setValue(photo);
        myRef.child(mContext.getString(R.string.dbname_photos)).child(newPhotoKey).setValue(photo);


    }

    public void likeChecker(String photoID)
    {
        int num = Integer.valueOf(photoID);

        if(num > 0 && num < 1.5)
        {

        }

        Query query = myRef.child("Rating").child(photoID);
        query.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                for(DataSnapshot singleSnapshot : dataSnapshot.getChildren())
                {
                    count = singleSnapshot.getValue(Integer.class);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError)
            {

            }
        });

    }

    public void updateTrending(String photoID)
    {
        String newPhotoKey = myRef.child("trending").push().child(photoID).getKey();

        count += 1;
        myRef.child("trending").child(photoID).setValue(count);


    }

    public UserAccountSettings getProfilePhoto(DataSnapshot dataSnapshot, String id)
    {

        UserAccountSettings settings  = new UserAccountSettings();
        String temp;

        for(DataSnapshot ds: dataSnapshot.getChildren())
        {
            if(ds.getKey().equals(mContext.getString(R.string.dbname_user_account_settings)))
            {
                settings.setProfile_photo(ds.child(id).getValue(UserAccountSettings.class).getProfile_photo());
            }
        }

        return settings;
    }

    private void setProfilePhoto(String url)
    {
        Log.d(TAG, "setProfilePhoto: setting new profile image: " + url);

        myRef.child(mContext.getString(R.string.dbname_user_account_settings))
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child(mContext.getString(R.string.profile_photo))
                .setValue(url);
    }

    public void collectPhoto(Photo cphoto)
    {
        Photo photo = new Photo();

        photo.setCaption(cphoto.getCaption());
        photo.setDate_created(cphoto.getDate_created());
        photo.setImage_path(cphoto.getImage_path());
        photo.setLocation(cphoto.getLocation());
        photo.setLocationlong(cphoto.getLocationlong());
        photo.setmPrivate(cphoto.getmPrivate());
        photo.setUser_id(cphoto.getUser_id());
        photo.setPhoto_id(cphoto.getPhoto_id());
        photo.setTags(cphoto.getTags());
        photo.setUser_id(cphoto.getUser_id());

        myRef.child(mContext.getString(R.string.dbname_user_photos)).
                child(FirebaseAuth.getInstance().getCurrentUser().getUid()).
                child(cphoto.getPhoto_id()).setValue(photo);
    }


    public void updateEvent(String photoID, String imgUrl, String cap)
    {
        //todo tweak the back feat for when edit is done
        myRef.child("photos")
                .child(photoID)
                .child("image_path")
                .setValue(imgUrl);

        myRef.child("photos")
                .child(photoID)
                .child("caption")
                .setValue(cap);

        myRef.child(mContext.getString(R.string.dbname_user_photos))
                .child(FirebaseAuth.getInstance()
                        .getCurrentUser()
                        .getUid())
                        .child(photoID)
                        .child("image_path")
                        .setValue(imgUrl);

        myRef.child(mContext.getString(R.string.dbname_user_photos))
                .child(FirebaseAuth.getInstance().getCurrentUser()
                        .getUid()).child(photoID).child("caption").setValue(cap);

//        Query q = myRef.child("user_photos");
//
//        //query user photo node 1st node
//        q.addValueEventListener(new ValueEventListener()
//        {
//
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
//            {
//                //users in node 2nd node
//                for(DataSnapshot singleSnapshot : dataSnapshot.getChildren())
//                {
//                    singleSnapshot.getRef().child(photoID).child("image_path").push().setValue(imgUrl);
//                    singleSnapshot.getRef().child(photoID).child("caption").push().setValue(cap);
//                    //photos in user  3rd node
////                    for(DataSnapshot photo : singleSnapshot.getChildren())
////                    {
////                        //inside the matching node
////                        photo.getRef().child(photoID).child("image_path").setValue(imgUrl);
////                    }
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError)
//            {
//
//            }
//        });
    }


    public void setLikesPhoto(Like like, String photokey)
    {

        myRef.child("Likes")
                .child(userID)
                .child(photokey)
                .setValue(like);

        //todo create another node for pure ratings that just has the Like number under the photo id's
        //todo that way we can just pull it and adverage the numbers
        FirebaseDatabase.getInstance().getReference()
                .child("Rating")
                .child(photokey)
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .setValue(like.getRating());


    }

    public void setGeoTrending()
    {

    }

    //add the photos to the database
    public void uploadNewPhoto(String photoType, final String caption, final String privatedata, final int count, final String imgUrl, Bitmap bm, final String location, final String locationlong, final String photoId)
    {
        Log.d(TAG, "uploadNewPhoto: attempting to uplaod new photo.");

        FilePaths filePaths = new FilePaths();

        //case1 new photo
        if(photoType.equals(mContext.getString(R.string.new_photo)))
        {
            Log.d(TAG, "uploadNewPhoto: uploading NEW photo.");

            String user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();

            final StorageReference storageReference = mStorageReference.child(filePaths.FIREBASE_IMAGE_STORAGE + "/" + user_id + "/photo" + (count + 1));

            //convert image url to bitmap
            if(bm == null)
            {

                bm = ImageManagers.getBitmap(imgUrl);
            }

            byte[] bytes = ImageManagers.getBytesFromBitmap(bm, 100);

            UploadTask uploadTask = null;

            uploadTask = storageReference.putBytes(bytes);

            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>()
            {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot)
                {

                    storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>()
                        {
                            @Override
                            public void onSuccess(Uri uri)
                            {
                                Uri firebaseUrl = uri;

                                Toast.makeText(mContext, "photo upload success", Toast.LENGTH_SHORT).show();
                                //add the new photo to 'photos' node and 'user_photos' node
                                addPhotoToDatabase(caption, firebaseUrl.toString(), location, locationlong, privatedata);

                                Intent intent = new Intent(mContext, GoogleActivity.class);
                                //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                mContext.startActivity(intent);
                            }
                        });
                }
            }).addOnFailureListener(new OnFailureListener()
            {
                @Override
                public void onFailure(@NonNull Exception e) {

                    Log.d(TAG, "onFailure: Photo upload failed.");
                    Toast.makeText(mContext, "Photo upload failed ", Toast.LENGTH_SHORT).show();

                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>()
            {
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot)
                {
                    double progress = (100 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();

                    if(progress - 15 > mPhotoUploadProgress)
                    {
                        Toast.makeText(mContext, "photo upload progress: " + String.format("%.0f", progress) + "%", Toast.LENGTH_SHORT).show();
                        mPhotoUploadProgress = progress;
                    }

                    Log.d(TAG, "onProgress: upload progress: " + progress + "% done");
                }
            });

        }
        //case new profile photo
        else if(photoType.equals(mContext.getString(R.string.profile_photo)))
        {
            Log.d(TAG, "uploadNewPhoto: uploading new PROFILE photo");

            String user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();

            final StorageReference storageReference = mStorageReference.child(filePaths.FIREBASE_IMAGE_STORAGE + "/" + user_id + "/profile_photo");

            //convert image url to bitmap
            if(bm == null)
            {
                bm = ImageManagers.getBitmap(imgUrl);
            }

            byte[] bytes = ImageManagers.getBytesFromBitmap(bm, 100);

            UploadTask uploadTask = null;
            uploadTask = storageReference.putBytes(bytes);

            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>()
            {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot)
                {

                    storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>()
                    {
                        @Override
                        public void onSuccess(Uri uri)
                        {
                            Uri firebaseurl = uri;
                            setProfilePhoto(firebaseurl.toString());
                        }
                    });

                }
            }).addOnFailureListener(new OnFailureListener()
            {
                @Override
                public void onFailure(@NonNull Exception e)
                {
                    Log.d(TAG, "onFailure: Photo upload failed.");
                    Toast.makeText(mContext, "Photo upload failed ", Toast.LENGTH_SHORT).show();
                }

            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>()
            {
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot)
                {
                    double progress = (100 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();

                    if(progress - 15 > mPhotoUploadProgress)
                    {
                        Toast.makeText(mContext, "photo upload progress: " + String.format("%.0f", progress) + "%", Toast.LENGTH_SHORT).show();
                        mPhotoUploadProgress = progress;
                    }
                    Log.d(TAG, "onProgress: upload progress: " + progress + "% done");
                }
            });
        }
        else if (photoType.equals("detail"))
            {

                String user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();

                final StorageReference storageReference = mStorageReference.child(filePaths.FIREBASE_IMAGE_STORAGE + "/" + user_id + "/profile_photo");

                //convert image url to bitmap
                if(bm == null)
                {
                    bm = ImageManagers.getBitmap(imgUrl);
                }

                byte[] bytes = ImageManagers.getBytesFromBitmap(bm, 100);

                UploadTask uploadTask = null;
                uploadTask = storageReference.putBytes(bytes);

                uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>()
                {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot)
                    {
                        storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>()
                        {
                            @Override
                            public void onSuccess(Uri uri)
                            {
                                Uri firebaseUrl = uri;
                                updateEvent(photoId, firebaseUrl.toString(), caption);

//                        Toast.makeText(mContext, "photo upload success", Toast.LENGTH_SHORT).show();
//                        //add the new photo to 'photos' node and 'user_photos' node
//                        addPhotoToDatabase(caption, firebaseUrl.toString(), location, locationlong, privatedata);

                            }
                        }).addOnFailureListener(new OnFailureListener()
                        {
                            @Override
                            public void onFailure(@NonNull Exception e)
                            {
                                Log.d(TAG, "onFailure: Photo upload failed.");
                                Toast.makeText(mContext, "Photo upload failed ", Toast.LENGTH_SHORT).show();
                            }

                        });
                    }
                });
            }

    }

}

























