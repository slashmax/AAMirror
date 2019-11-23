package com.github.slashmax.aamirror;

import android.content.res.Configuration;
import android.graphics.Point;
import android.view.OrientationEventListener;
import android.view.WindowManager;

import eu.chainfire.libsuperuser.Application;

import static android.view.Surface.ROTATION_0;
import static android.view.Surface.ROTATION_180;

public class CarApplication extends Application
{
    private static final String TAG = "CarApplication";

    public static int               ScreenRotation = ROTATION_0;
    public static Point             ScreenSize = new Point();
    public static Point             DisplaySize = new Point();

    private static OrientationEventListener m_OrientationListener;
    private WindowManager                   m_WindowManager;

    @Override
    public void onCreate()
    {
        super.onCreate();

        m_WindowManager = (WindowManager)getApplicationContext().getSystemService(WINDOW_SERVICE);
        UpdateScreenSizeAndRotation();
        UpdateDisplaySize();
        m_OrientationListener = new OrientationEventListener(this)
        {
            @Override
            public void onOrientationChanged(int orientation)
            {
                UpdateScreenSizeAndRotation();
            }
        };
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        UpdateScreenSizeAndRotation();
    }

    public static void EnableOrientationListener()
    {
        if (m_OrientationListener != null)
            m_OrientationListener.enable();
    }

    public static void DisableOrientationListener()
    {
        if (m_OrientationListener != null)
            m_OrientationListener.disable();
    }

    private void UpdateScreenSizeAndRotation()
    {
        if (m_WindowManager != null)
        {
            m_WindowManager.getDefaultDisplay().getRealSize(ScreenSize);
            ScreenRotation = m_WindowManager.getDefaultDisplay().getRotation();
        }
    }

    private void UpdateDisplaySize()
    {
        if (ScreenRotation == ROTATION_0 || ScreenRotation == ROTATION_180)
        {
            DisplaySize.x = ScreenSize.x;
            DisplaySize.y = ScreenSize.y;
        }
        else
        {
            DisplaySize.x = ScreenSize.y;
            DisplaySize.y = ScreenSize.x;
        }
    }
}
