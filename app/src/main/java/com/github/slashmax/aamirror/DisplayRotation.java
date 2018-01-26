package com.github.slashmax.aamirror;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;

import static android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS;
import static android.provider.Settings.System.ACCELEROMETER_ROTATION;
import static android.provider.Settings.System.USER_ROTATION;
import static android.view.Surface.ROTATION_0;
import static android.view.Surface.ROTATION_90;

public class DisplayRotation
{
    private static final String TAG = "DisplayRotation";

    private Context m_Context;
    private boolean m_AutoRotation;
    private int     m_UserRotation;

    DisplayRotation(Context context)
    {
        Log.d(TAG, "DisplayRotation");
        m_Context = context;
    }

    public void onCreate()
    {
        Log.d(TAG, "onCreate");

        m_AutoRotation = IsAutoRotation();
        m_UserRotation = GetUserRotation();
    }

    public void onDestroy()
    {
        Log.d(TAG, "onDestroy");

        if (CanWriteSettings())
        {
            SetUserRotation(m_UserRotation);
            SetAutoRotation(m_AutoRotation);
        }
    }

    public void onStart()
    {
        Log.d(TAG, "onStart");
        if (CanWriteSettings())
        {
            SetAutoRotation(false);
            SetUserRotation(ROTATION_90);
        }
    }

    public void onStop()
    {
        Log.d(TAG, "onStop");

        if (CanWriteSettings())
        {
            SetUserRotation(m_UserRotation);
            SetAutoRotation(m_AutoRotation);
        }
    }

    public void CheckWriteSettingsPermission()
    {
        if (!CanWriteSettings())
            RequestWritePermission();
    }

    private boolean CanWriteSettings()
    {
        Log.d(TAG, "CanWriteSettings");
        return (m_Context != null) && Settings.System.canWrite(m_Context);
    }

    private void RequestWritePermission()
    {
        Log.d(TAG, "RequestWritePermission");

        if (m_Context != null)
        {
            Intent intent = new Intent(ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + m_Context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            m_Context.startActivity(intent);
        }
    }

    private boolean IsAutoRotation()
    {
        Log.d(TAG, "IsAutoRotation");

        if (m_Context == null)
            return false;

        int autoRotation = 0;
        try
        {
            autoRotation = Settings.System.getInt(m_Context.getContentResolver(), ACCELEROMETER_ROTATION);
        }
        catch (Exception e)
        {
            Log.d(TAG, "IsAutoRotation exception: " + e.toString());
        }

        return (autoRotation == 1);
    }

    private void SetAutoRotation(boolean on)
    {
        Log.d(TAG, "SetAutoRotation");

        if (m_Context != null)
            Settings.System.putInt(m_Context.getContentResolver(), ACCELEROMETER_ROTATION, on ? 1 : 0);
    }

    private int GetUserRotation()
    {
        Log.d(TAG, "GetUserRotation");

        if (m_Context == null)
            return ROTATION_0;

        int userRotation = 0;
        try
        {
            userRotation = Settings.System.getInt(m_Context.getContentResolver(), USER_ROTATION);
        }
        catch (Exception e)
        {
            Log.d(TAG, "IsAutoRotation exception: " + e.toString());
        }

        return userRotation;
    }

    private void SetUserRotation(int rotation)
    {
        Log.d(TAG, "SetRotation");

        if (m_Context != null)
            Settings.System.putInt(m_Context.getContentResolver(), USER_ROTATION, rotation);
    }
}
