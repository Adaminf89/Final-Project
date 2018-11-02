package com.example.adaminfiesto.droppit.Login;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.adaminfiesto.droppit.Feed.FeedActivity;
import com.example.adaminfiesto.droppit.Main.HomeActivity;
import com.example.adaminfiesto.droppit.R;
import com.example.adaminfiesto.droppit.Utils.UniversalImageLoader;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.nostra13.universalimageloader.core.ImageLoader;


/*
Ref Links
https://developer.android.com/guide/components/processes-and-threads
https://stackoverflow.com/questions/14963776/get-users-by-name-property-using-firebase
https://stackoverflow.com/questions/44286535/how-to-get-all-users-data-from-firebase-database-in-custom-listview-in-android
https://www.youtube.com/watch?v=Vyqz_-sJGFk&list=PLgCYzUzKIBE-4SBxHzntuDDEREffjyoUj
https://github.com/gdpremium/GD-Premium
libs to be added later
 * */
public class LoginActivity extends AppCompatActivity
{
    private static final String TAG = "LoginActivity";

    //firebase
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private Context mContext;
    private ProgressBar mProgressBar;
    private EditText mEmail, mPassword, cPassword;
    private TextView mPleaseWait;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mProgressBar = findViewById(R.id.progressBar);
        mPleaseWait = findViewById(R.id.pleaseWait);
        mEmail = findViewById(R.id.input_email);
        mPassword = findViewById(R.id.input_password);
        mContext = LoginActivity.this;
        Log.d(TAG, "onCreate: started.");
        //set the progession base to gone untill tasks are started
        mPleaseWait.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.GONE);
        initImageLoader();
        //setup
        setupFirebaseAuth();
        init();

    }

    //this is the first screen that will A. Load for the user and B utilize the imageloader for maps
    private void initImageLoader()
    {
        UniversalImageLoader universalImageLoader = new UniversalImageLoader(mContext);
        ImageLoader.getInstance().init(universalImageLoader.getConfig());
    }

    private boolean isStringNull(String string)
    {
        Log.d(TAG, "isStringNull: checking string if null.");

        return string.equals("");
    }

     /*
    ------------------------------------ Firebase ---------------------------------------------
     */

    private void init()
    {

        //initialize the button for logging in
        Button btnLogin = findViewById(R.id.btn_login);
        btnLogin.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Log.d(TAG, "onClick: attempting to log in.");

                String email = mEmail.getText().toString();
                String password = mPassword.getText().toString();

                if(isStringNull(email) && isStringNull(password))
                {
                    Toast.makeText(mContext, "You must fill out all the fields", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    mProgressBar.setVisibility(View.VISIBLE);
                    mPleaseWait.setVisibility(View.VISIBLE);

                    mAuth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>()
                            {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task)
                                {
                                    Log.d(TAG, "signInWithEmail:onComplete:" + task.isSuccessful());
                                    FirebaseUser user = mAuth.getCurrentUser();

                                    // If sign in fails, display a message to the user. If sign in succeeds
                                    // the auth state listener will be notified and logic to handle the
                                    // signed in user can be handled in the listener.
                                    if (!task.isSuccessful())
                                    {
                                        Log.w(TAG, "signInWithEmail:failed", task.getException());

                                        Toast.makeText(LoginActivity.this, getString(R.string.auth_failed), Toast.LENGTH_SHORT).show();
                                        mProgressBar.setVisibility(View.GONE);
                                        mPleaseWait.setVisibility(View.GONE);
                                    }
                                    else
                                        {
                                        try
                                        {
                                            //check if this users email is varified
                                            if(user.isEmailVerified())
                                            {
                                                Log.d(TAG, "onComplete: success. email is verified.");
                                                Intent intent = new Intent(LoginActivity.this, FeedActivity.class);
                                                startActivity(intent);
                                                finish();

                                            }
                                            //if it is not dont do nothing
                                            else
                                                {
                                                Toast.makeText(mContext, "Email is not verified \n check your email inbox.", Toast.LENGTH_SHORT).show();
                                                mProgressBar.setVisibility(View.GONE);
                                                mPleaseWait.setVisibility(View.GONE);
                                                mAuth.signOut();
                                            }
                                        }
                                        catch (NullPointerException e)
                                        {
                                            Log.e(TAG, "onComplete: NullPointerException: " + e.getMessage() );
                                        }
                                    }


                                    // ...
                                }
                            });
                }

            }
        });

        TextView linkSignUp = findViewById(R.id.link_signup);
        linkSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: navigating to register screen");
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

         /*
         If the user is logged in then navigate to HomeActivity and call 'finish()'
          */
        if(mAuth.getCurrentUser() != null){
            Intent intent = new Intent(LoginActivity.this, FeedActivity.class);
            startActivity(intent);
            finish();
        }
    }

    /**
     * Setup the firebase auth object
     */
    private void setupFirebaseAuth(){
        Log.d(TAG, "setupFirebaseAuth: setting up firebase auth.");

        mAuth = FirebaseAuth.getInstance();

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();

                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
                // ...
            }
        };
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }
}
