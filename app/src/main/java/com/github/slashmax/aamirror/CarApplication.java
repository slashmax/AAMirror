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

    private static OrientationEventListener m_OrientationListener;
    private WindowManager                   m_WindowManager;

    @Override
    public void onCreate()
    {
        Log.d(TAG, "onCreate");
        super.onCreate();

        m_WindowManager = (WindowManager)getSystemService(WINDOW_SERVICE);
        UpdateScreenRotation();
        m_OrientationListener = new OrientationEventListener(this)
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

    public static void EnableOrientationListener()
    {
        Log.d(TAG, "EnableOrientationListener");
        if (m_OrientationListener != null)
            m_OrientationListener.enable();
    }

    public static void DisableOrientationListener()
    {
        Log.d(TAG, "DisableOrientationListener");
        if (m_OrientationListener != null)
            m_OrientationListener.disable();
    }

    private void UpdateScreenRotation()
    {
        if (m_WindowManager != null)
        {
            ScreenRotation = m_WindowManager.getDefaultDisplay().getRotation();
            m_WindowManager.getDefaultDisplay().getSize(ScreenSize);
        }
    }
}
