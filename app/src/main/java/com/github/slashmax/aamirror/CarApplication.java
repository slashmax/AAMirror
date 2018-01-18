package com.github.slashmax.aamirror;

import android.content.res.Configuration;
import android.util.Log;

import eu.chainfire.libsuperuser.Application;

public class CarApplication extends Application
{
    private static final String TAG = "CarApplication";

    @Override
    public void onCreate()
    {
        Log.d(TAG, "onCreate");
        super.onCreate();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        Log.d(TAG, "onConfigurationChanged: " + (newConfig != null ? newConfig.toString() : "null"));
        super.onConfigurationChanged(newConfig);
    }
}
