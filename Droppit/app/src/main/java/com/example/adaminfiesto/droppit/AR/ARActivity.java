package com.example.adaminfiesto.droppit.AR;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.adaminfiesto.droppit.R;
import com.example.adaminfiesto.droppit.Utils.BottomNavigationViewHelper;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableException;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import uk.co.appoly.arcorelocation.LocationMarker;
import uk.co.appoly.arcorelocation.LocationScene;


public class ARActivity extends AppCompatActivity
{
    private static final String TAG = ARActivity.class.getSimpleName();
    public Context mContext = ARActivity.this;
    private static final int ACTIVITY_NUM = 3;
    Button mArButton;
    private ArFragment arFragment;
    //ar
    private Snackbar loadingMessageSnackbar = null;
    private boolean installRequested;
    private ModelRenderable andyRenderable;
    private LocationScene locationScene;
    private ArSceneView arSceneView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar);
        arSceneView = findViewById(R.id.arFrameLayout);
        //arSceneView = new ArSceneView(this);
        //arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

        CompletableFuture<ModelRenderable> andy = ModelRenderable.builder()
                .setSource(this, R.raw.andy)
                .build();

        CompletableFuture.allOf(andy)
                .handle(
                        (notUsed, throwable) ->
                        {
                            if (throwable != null)
                            {
                                DemoUtils.displayError(this, "Unable to load renderables", throwable);
                                return null;
                            }
                            try
                            {
                                andyRenderable = andy.get();
                            }
                            catch (InterruptedException | ExecutionException ex)
                            {
                                DemoUtils.displayError(this, "Unable to load renderables", ex);
                            }
                            return null;
                        });


        arSceneView
                .getScene()
                .setOnUpdateListener(frameTime -> {

            Frame frame = arSceneView.getArFrame();

            if (frame == null)
            {
                return;
            }

            if (frame.getCamera().getTrackingState() != TrackingState.TRACKING)
            {
                return;
            }

            if (locationScene == null)
            {
                locationScene = new LocationScene(this, this, arSceneView);

                locationScene.mLocationMarkers.add(new LocationMarker(-81.3290023803711,28.808021545410156, getAndy("Drop")));

            }

            if (locationScene != null)
            {
                //hideLoadingMessage();
                locationScene.processFrame(arSceneView.getArFrame());
            }

        });

        setupBottomNavigationView();
    }


    private Node getAndy(String name)
    {
        Node base = new Node();
        base.setParent(arSceneView.getScene());
        base.setRenderable(andyRenderable);
        Context c = this;

        base.setOnTapListener((v, event) -> Toast.makeText(
                c, "Location: "+name, Toast.LENGTH_LONG)
                .show());

        return base;
    }



    @Override
    protected void onResume()
    {
        super.onResume();

        if (arSceneView == null)
        {
            return;
        }
        if (arSceneView.getSession() == null)
        {
            // If the session wasn't created yet, don't resume rendering.
            // This can happen if ARCore needs to be updated or permissions are not granted yet.
            try {
                Session session = DemoUtils.createArSession(this, installRequested);
                if (session == null)
                {
                    installRequested = DemoUtils.hasCameraPermission(this);
                    return;
                }
                else
                    {
                    arSceneView.setupSession(session);
                }
            }
            catch (UnavailableException e)
            {
                DemoUtils.handleSessionException(this, e);
            }
        }

        if(locationScene!=null)
        {
            locationScene.resume();
        }

        try
        {
            arSceneView.resume();

        }
        catch (CameraNotAvailableException ex)
        {
            finish();
            return;
        }
    }


    @Override
    public void onPause()
    {
        super.onPause();
        if(locationScene!=null)
        {
            locationScene.pause();
        }
        arSceneView.pause();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        if(locationScene!=null)
        {
            locationScene.pause();
            locationScene = null;
        }
        arSceneView.destroy();
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
