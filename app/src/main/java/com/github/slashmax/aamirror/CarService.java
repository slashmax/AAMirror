package com.github.slashmax.aamirror;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.IBinder;
import android.util.Log;

import com.google.android.apps.auto.sdk.CarActivity;
import com.google.android.apps.auto.sdk.CarActivityService;

public class CarService extends CarActivityService
{
    private static final String TAG = "CarService";

    @Override
    public Class<? extends CarActivity> getCarActivity()
    {
        Log.d(TAG, "getCarActivity");
        return MainCarActivity.class;
    }

    @Override
    public void onCreate()
    {
        Log.d(TAG, "onCreate");
        super.onCreate();
    }

    @Override
    public void onDestroy()
    {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        Log.d(TAG, "onBind intent: " + (intent != null ? intent.toString() : "null"));
        return super.onBind(intent);
    }

    @Override
    public void onRebind(Intent intent)
    {
        Log.d(TAG, "onRebind intent: " + (intent != null ? intent.toString() : "null"));
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        Log.d(TAG, "onUnbind: " + (intent != null ? intent.toString() : "null"));
        return super.onUnbind(intent);
    }

    @Override
    public int getHandledConfigChanges()
    {
        Log.d(TAG, "getHandledConfigChanges");
        return super.getHandledConfigChanges();
    }

    @Override
    public void onConfigurationChanged(Configuration configuration)
    {
        Log.d(TAG, "onConfigurationChanged " + (configuration != null ? configuration.toString() : "null"));
        super.onConfigurationChanged(configuration);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.d(TAG, "onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onLowMemory()
    {
        Log.d(TAG, "onLowMemory");
        super.onLowMemory();
    }

    @Override
    public ComponentName startService(Intent service)
    {
        Log.d(TAG, "startService: " + (service != null ? service.toString() : "null"));
        return super.startService(service);
    }

    @Override
    public boolean stopService(Intent name)
    {
        Log.d(TAG, "stopService: " + (name != null ? name.toString() : "null"));
        return super.stopService(name);
    }

    @Override
    public boolean bindService(Intent service, ServiceConnection conn, int flags)
    {
        Log.d(TAG, "bindService: " + (service != null ? service.toString() : "null"));
        return super.bindService(service, conn, flags);
    }

    @Override
    public void unbindService(ServiceConnection conn)
    {
        Log.d(TAG, "unbindService: " + (conn != null ? conn.toString() : "null"));
        super.unbindService(conn);
    }

    @Override
    public Object getSystemService(String name)
    {
        Log.d(TAG, "getSystemService: " + name);
        return super.getSystemService(name);
    }

    @Override
    public String getSystemServiceName(Class<?> serviceClass)
    {
        Log.d(TAG, "getSystemServiceName: " + (serviceClass != null ? serviceClass.toString() : "null"));
        return super.getSystemServiceName(serviceClass);
    }
}
