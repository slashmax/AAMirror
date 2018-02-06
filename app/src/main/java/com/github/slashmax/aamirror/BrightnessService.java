package com.github.slashmax.aamirror;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.util.Log;

import static android.provider.Settings.System.SCREEN_BRIGHTNESS;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;

public class BrightnessService extends Service
{
    private static final String TAG = "BrightnessService";

    public static final String BRIGHTNESS       = "Brightness";
    public static final String BRIGHTNESS_MODE  = "BrightnessMode";

    public static final int SCREEN_BRIGHTNESS_MODE_MANUAL       = Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
    public static final int SCREEN_BRIGHTNESS_MODE_AUTOMATIC    = Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;

    private int m_Brightness;
    private int m_BrightnessMode;

    @Override
    public void onCreate()
    {
        Log.d(TAG, "onCreate");
        super.onCreate();
        ReadBrightnessSettings();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.d(TAG, "onStartCommand");
        if (intent != null)
        {
            int brightness = intent.getIntExtra(BRIGHTNESS, 255);
            int brightnessMode = intent.getIntExtra(BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
            WriteBrightnessSettings(brightness, brightnessMode);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy()
    {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        WriteBrightnessSettings(m_Brightness, m_BrightnessMode);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        Log.d(TAG, "onBind");
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        Log.d(TAG, "onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent)
    {
        Log.d(TAG, "onRebind");
        super.onRebind(intent);
    }

    private boolean CanWriteSettings()
    {
        Log.d(TAG, "CanWriteSettings");
        return Build.VERSION.SDK_INT < 23 ||
                Settings.System.canWrite(this);
    }

    private void ReadBrightnessSettings()
    {
        Log.d(TAG, "ReadBrightnessSettings");
        try
        {
            m_Brightness = Settings.System.getInt(getContentResolver(), SCREEN_BRIGHTNESS);
            m_BrightnessMode = Settings.System.getInt(getContentResolver(), SCREEN_BRIGHTNESS_MODE);
        }
        catch (Exception e)
        {
            Log.d(TAG, "ReadBrightnessSettings exception: " + e.toString());
        }
    }

    private void WriteBrightnessSettings(int brightness, int brightnessMode)
    {
        Log.d(TAG, "WriteBrightnessSettings");
        if (CanWriteSettings())
        {
            if (brightness < 0) brightness = 0;
            else if (brightness > 255) brightness = 255;
            Settings.System.putInt(getContentResolver(), SCREEN_BRIGHTNESS, brightness);
            Settings.System.putInt(getContentResolver(), SCREEN_BRIGHTNESS_MODE, brightnessMode);
        }
    }
}
