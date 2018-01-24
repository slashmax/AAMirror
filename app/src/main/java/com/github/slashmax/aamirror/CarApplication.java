package com.github.slashmax.aamirror;

import android.content.res.Configuration;
import android.graphics.Point;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.WindowManager;

import eu.chainfire.libsuperuser.Application;

import static android.view.Surface.ROTATION_0;

public class CarApplication extends Application
{
    private static final String TAG = "CarApplication";

    public static int   ScreenRotation = ROTATION_0;
    public static Point ScreenSize = new Point();
    public static OrientationEventListener OrientationListener;

    private WindowManager windowManager;

    @Override
    public void onCreate()
    {
        Log.d(TAG, "onCreate");
        super.onCreate();

        windowManager = (WindowManager)getSystemService(WINDOW_SERVICE);
        UpdateScreenRotation();
        OrientationListener = new OrientationEventListener(this)
        {
            @Override
            public void onOrientationChanged(int orientation)
            {
                UpdateScreenRotation();
            }
        };
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        Log.d(TAG, "onConfigurationChanged: " + (newConfig != null ? newConfig.toString() : "null"));
        super.onConfigurationChanged(newConfig);
        UpdateScreenRotation();
    }

    private void UpdateScreenRotation()
    {
        if (windowManager != null)
        {
            ScreenRotation = windowManager.getDefaultDisplay().getRotation();
            windowManager.getDefaultDisplay().getSize(ScreenSize);
        }
    }
}
