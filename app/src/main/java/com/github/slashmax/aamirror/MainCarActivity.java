package com.github.slashmax.aamirror;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.apps.auto.sdk.CarActivity;
import com.google.android.apps.auto.sdk.CarUiController;
import com.google.android.apps.auto.sdk.DayNightStyle;

import eu.chainfire.libsuperuser.Shell;

import static android.content.Intent.ACTION_SCREEN_OFF;
import static android.content.Intent.ACTION_SCREEN_ON;
import static android.content.Intent.ACTION_USER_PRESENT;
import static android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP;
import static android.os.PowerManager.ON_AFTER_RELEASE;
import static android.os.PowerManager.SCREEN_DIM_WAKE_LOCK;
import static android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION;
import static android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS;
import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_POINTER_DOWN;
import static android.view.MotionEvent.ACTION_POINTER_UP;
import static android.view.MotionEvent.ACTION_UP;
import static android.view.Surface.ROTATION_0;
import static android.view.Surface.ROTATION_180;
import static android.view.Surface.ROTATION_270;
import static android.view.Surface.ROTATION_90;

public class MainCarActivity extends CarActivity
        implements View.OnTouchListener, Handler.Callback,
        AppsGridFragment.OnAppClickListener, AppsGridFragment.OnAppLongClickListener
{
    private static final String TAG = "MainCarActivity";
    private static final String PREFERENCES = "com.github.slashmax.aamirror.preferences";

    private static final int        REQUEST_MEDIA_PROJECTION_PERMISSION = 1;

    private static final int        ACTION_APP_LAUNCH   = 0;
    private static final int        ACTION_APP_FAV_1    = 1;
    private static final int        ACTION_APP_FAV_2    = 2;
    private static final int        ACTION_APP_FAV_3    = 3;

    private String                  m_AppFav1;
    private String                  m_AppFav2;
    private String                  m_AppFav3;

    private int                     m_AppsAction;
    private boolean                 m_AppsDrawerOpen;

    private boolean                 m_HasRoot;
    private MinitouchDaemon         m_MinitouchDaemon;
    private MinitouchSocket         m_MinitouchSocket;
    private InputKeyEvent           m_InputKeyEvent;
    private MinitouchAsyncTask      m_MinitouchTask;

    private UnlockReceiver          m_UnlockReceiver;
    private Handler                 m_RequestHandler;
    private PowerManager.WakeLock   m_WakeLock;

    private SurfaceView             m_SurfaceView;
    private Surface                 m_Surface;

    private VirtualDisplay          m_VirtualDisplay;
    private MediaProjection         m_MediaProjection;
    private int                     m_ProjectionCode;
    private Intent                  m_ProjectionIntent;

    private int                     m_ScreenRotation;
    private double                  m_ProjectionOffsetX;
    private double                  m_ProjectionOffsetY;
    private double                  m_ProjectionWidth;
    private double                  m_ProjectionHeight;

    private class MinitouchAsyncTask extends AsyncTask<Void, Void, Void>
    {
        @Override
        protected Void doInBackground(Void... voids)
        {
            Log.d(TAG, "MinitouchTask.doInBackground");
            m_MinitouchDaemon.run();
            return null;
        }
    }

    private class UnlockReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            Log.d(TAG, "UnlockReceiver.onReceive: " + (intent != null ? intent.toString() : "null"));

            if (intent != null)
            {
                if (intent.getAction().equals(ACTION_USER_PRESENT))
                    OnUnlock();
                else if (intent.getAction().equals(ACTION_SCREEN_ON))
                    OnScreenOn();
                else if (intent.getAction().equals(ACTION_SCREEN_OFF))
                    OnScreenOff();
            }
        }
    }

    @Override
    public void onCreate(Bundle bundle)
    {
        Log.d(TAG, "onCreate: " + (bundle != null ? bundle.toString() : "null"));
        setTheme(R.style.AppTheme);
        super.onCreate(bundle);
        setContentView(R.layout.activity_car_main);

        InitCarUiController(getCarUiController());

        m_AppsAction = ACTION_APP_LAUNCH;
        m_AppsDrawerOpen = false;

        m_HasRoot = Shell.SU.available();

        m_MinitouchDaemon = new MinitouchDaemon(this);
        m_MinitouchSocket = new MinitouchSocket();
        m_InputKeyEvent = new InputKeyEvent();
        m_MinitouchTask = new MinitouchAsyncTask();

        m_UnlockReceiver = new UnlockReceiver();
        m_RequestHandler = new Handler(this);

        PowerManager powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
        if (powerManager != null)
            m_WakeLock = powerManager.newWakeLock(SCREEN_DIM_WAKE_LOCK | ACQUIRE_CAUSES_WAKEUP, "AAMirrorWakeLock");

        m_SurfaceView = (SurfaceView)findViewById(R.id.m_SurfaceView);
        m_SurfaceView.getHolder().setFormat(PixelFormat.RGBA_8888);
        m_SurfaceView.setOnTouchListener(this);
        m_Surface = m_SurfaceView.getHolder().getSurface();

        AppsGridFragment gridFragment = (AppsGridFragment)getSupportFragmentManager().findFragmentById(R.id.m_AppsGridFragment);
        if (gridFragment != null)
        {
            gridFragment.setOnAppClickListener(this);
            gridFragment.setOnAppLongClickListener(this);
        }

        UpdateConfiguration(getResources().getConfiguration());
        InitButtonsActions();
        UpdateTouchTransformations(true);
        LoadSharedPreferences();

        RequestProjectionPermission();

        if (m_HasRoot)
        {
            m_InputKeyEvent.init();
            m_MinitouchTask.execute();
        }
    }

    @Override
    public void onDestroy()
    {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        stopScreenCapture();
        stopService(new Intent(this, OrientationService.class));
        stopService(new Intent(this, BrightnessService.class));
        m_MinitouchSocket.disconnect();
        if (m_HasRoot)
        {
            m_MinitouchDaemon.kill(m_MinitouchSocket.getPid());
            m_MinitouchTask.cancel(true);
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle bundle)
    {
        Log.d(TAG, "onRestoreInstanceState: " + (bundle != null ? bundle.toString() : "null"));
        super.onRestoreInstanceState(bundle);
    }

    @Override
    public void onSaveInstanceState(Bundle bundle)
    {
        Log.d(TAG, "onSaveInstanceState: " + (bundle != null ? bundle.toString() : "null"));
        super.onSaveInstanceState(bundle);
        SaveSharedPreferences();
    }

    @Override
    public void onStart()
    {
        Log.d(TAG, "onStart");
        super.onStart();
        CarApplication.EnableOrientationListener();
        if (m_WakeLock != null)
            m_WakeLock.acquire();
        m_SurfaceView.setKeepScreenOn(true);
        if (!IsLocked())
            OnUnlock();

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USER_PRESENT);
        filter.addAction(ACTION_SCREEN_ON);
        filter.addAction(ACTION_SCREEN_OFF);
        registerReceiver(m_UnlockReceiver, filter);
    }

    @Override
    public void onStop()
    {
        Log.d(TAG, "onStop");
        super.onStop();
        stopScreenCapture();
        CarApplication.DisableOrientationListener();
        if (m_WakeLock != null && m_WakeLock.isHeld())
            m_WakeLock.release(ON_AFTER_RELEASE);
        m_SurfaceView.setKeepScreenOn(false);
        startOrientationService(OrientationService.METHOD_NONE, OrientationService.ROTATION_0);
        stopService(new Intent(this, BrightnessService.class));
        unregisterReceiver(m_UnlockReceiver);

    }

    @Override
    public void onPause()
    {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    public void onResume()
    {
        Log.d(TAG, "onResume");
        super.onResume();
    }

    @Override
    public void onPostResume()
    {
        Log.d(TAG, "onPostResume");
        super.onPostResume();
    }

    @Override
    public void onWindowFocusChanged(boolean focus, boolean b1)
    {
        Log.d(TAG, "onWindowFocusChanged: " + focus);
        super.onWindowFocusChanged(focus, b1);

        if (focus)
            startScreenCapture();
    }

    @Override
    public void onConfigurationChanged(Configuration configuration)
    {
        Log.d(TAG, "onConfigurationChanged: " + (configuration != null ? configuration.toString() : "null"));
        super.onConfigurationChanged(configuration);
        UpdateConfiguration(configuration);
    }

    private void InitCarUiController(CarUiController controller)
    {
        Log.d(TAG, "InitCarUiController");
        controller.getStatusBarController().setTitle(getString(R.string.app_name));
        controller.getStatusBarController().hideAppHeader();
        controller.getStatusBarController().setAppBarAlpha(0.0f);
        controller.getStatusBarController().setAppBarBackgroundColor(Color.WHITE);
        controller.getStatusBarController().setDayNightStyle(DayNightStyle.AUTO);
        controller.getMenuController().hideMenuButton();

        setIgnoreConfigChanges(0xFFFF);
    }

    private void UpdateConfiguration(Configuration configuration)
    {
        if (configuration == null)
            return;

        Log.d(TAG, "UpdateConfiguration: " + configuration.toString());

        int backgroundColor;
        if ((configuration.uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES)
            backgroundColor = getColor(R.color.colorCarBackgroundNight);
        else
            backgroundColor = getColor(R.color.colorCarBackgroundDay);

        LinearLayout buttonsLayout = (LinearLayout)findViewById(R.id.m_ButtonsLayout);
        if (buttonsLayout != null)
            buttonsLayout.setBackgroundColor(backgroundColor);

        LinearLayout appsGridLayout = (LinearLayout)findViewById(R.id.m_AppsGridLayout);
        if (appsGridLayout != null)
            appsGridLayout.setBackgroundColor(backgroundColor);
    }

    private void OnUnlock()
    {
        Log.d(TAG, "OnUnlock");
        startOrientationService(OrientationService.METHOD_FORCE, OrientationService.ROTATION_270);
        m_SurfaceView.setKeepScreenOn(false);
        startBrightnessService(BrightnessService.SCREEN_BRIGHTNESS_MODE_MANUAL, 0);
    }

    private void OnScreenOn()
    {
        Log.d(TAG, "OnScreenOn");
    }

    private  void OnScreenOff()
    {
        Log.d(TAG, "OnScreenOff");
        startOrientationService(OrientationService.METHOD_NONE, OrientationService.ROTATION_0);
    }

    private boolean IsLocked()
    {
        Log.d(TAG, "IsLocked");
        KeyguardManager km = (KeyguardManager)getSystemService(KEYGUARD_SERVICE);
        return (km != null && km.isDeviceLocked());
    }

    private void startOrientationService(int method, int rotation)
    {
        startService(new Intent(this, OrientationService.class)
                .putExtra(OrientationService.METHOD, method)
                .putExtra(OrientationService.ROTATION, rotation));
    }

    private void startBrightnessService(int brightness, int brightnessMode)
    {
        startService(new Intent(this, BrightnessService.class)
                .putExtra(BrightnessService.BRIGHTNESS, brightness)
                .putExtra(BrightnessService.BRIGHTNESS_MODE, brightnessMode));
    }

    private void InitButtonsActions()
    {
        Log.d(TAG, "InitButtonsActions");

        ImageView m_Back =(ImageView)findViewById(R.id.m_Back);
        if (m_Back != null)
        {
            m_Back.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    Log.d(TAG, "m_Back.onClick");
                    if (m_AppsDrawerOpen)
                        SwitchToMirrorSurface();
                    else
                        GenerateKeyEvent(KeyEvent.KEYCODE_BACK, false);
                }
            });
            m_Back.setOnLongClickListener(new View.OnLongClickListener()
            {
                @Override
                public boolean onLongClick(View v)
                {
                    Log.d(TAG, "m_Back.onLongClick");
                    if (m_AppsDrawerOpen)
                        SwitchToMirrorSurface();
                    else
                        GenerateKeyEvent(KeyEvent.KEYCODE_BACK, true);
                    return true;
                }
            });
        }

        ImageView m_Menu = (ImageView)findViewById(R.id.m_Menu);
        if (m_Menu != null)
        {
            m_Menu.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    Log.d(TAG, "m_Menu.onClick");
                    if (m_AppsDrawerOpen)
                        SwitchToMirrorSurface();
                    else
                        GenerateKeyEvent(KeyEvent.KEYCODE_MENU, false);
                }
            });
            m_Menu.setOnLongClickListener(new View.OnLongClickListener()
            {
                @Override
                public boolean onLongClick(View v)
                {
                    Log.d(TAG, "m_Menu.onLongClick");
                    if (m_AppsDrawerOpen)
                        SwitchToMirrorSurface();
                    else
                        GenerateKeyEvent(KeyEvent.KEYCODE_MENU, true);
                    return true;
                }
            });
        }

        ImageView m_Apps = (ImageView)findViewById(R.id.m_Apps);
        if (m_Apps != null)
        {
            m_Apps.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    Log.d(TAG, "m_Apps.onClick");
                    if (m_AppsDrawerOpen)
                        SwitchToMirrorSurface();
                    else
                        SwitchToAppsGrid(ACTION_APP_LAUNCH);
                }
            });
            m_Apps.setOnLongClickListener(new View.OnLongClickListener()
            {
                @Override
                public boolean onLongClick(View v)
                {
                    Log.d(TAG, "m_Apps.onLongClick");
                    //todo : ideas???
                    return false;
                }
            });
        }

        ImageView m_Fav1 = (ImageView)findViewById(R.id.m_Fav1);
        if (m_Fav1 != null)
        {
            m_Fav1.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    Log.d(TAG, "m_Fav1.onClick");
                    DoFavClick(ACTION_APP_FAV_1, false);
                }
            });
            m_Fav1.setOnLongClickListener(new View.OnLongClickListener()
            {
                @Override
                public boolean onLongClick(View v)
                {
                    Log.d(TAG, "m_Fav1.onLongClick");
                    DoFavClick(ACTION_APP_FAV_1, true);
                    return true;
                }
            });
        }

        ImageView m_Fav2 = (ImageView)findViewById(R.id.m_Fav2);
        if (m_Fav2 != null)
        {
            m_Fav2.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    Log.d(TAG, "m_Fav2.onClick");
                    DoFavClick(ACTION_APP_FAV_2, false);
                }
            });
            m_Fav2.setOnLongClickListener(new View.OnLongClickListener()
            {
                @Override
                public boolean onLongClick(View v)
                {
                    Log.d(TAG, "m_Fav2.onLongClick");
                    DoFavClick(ACTION_APP_FAV_2, true);
                    return true;
                }
            });
        }

        ImageView m_Fav3 = (ImageView)findViewById(R.id.m_Fav3);
        if (m_Fav3 != null)
        {
            m_Fav3.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    Log.d(TAG, "m_Fav3.onClick");
                    DoFavClick(ACTION_APP_FAV_3, false);
                }
            });
            m_Fav3.setOnLongClickListener(new View.OnLongClickListener()
            {
                @Override
                public boolean onLongClick(View v)
                {
                    Log.d(TAG, "m_Fav3.onLongClick");
                    DoFavClick(ACTION_APP_FAV_3, true);
                    return true;
                }
            });
        }
    }

    @Override
    public void onAppClick(AppsGridFragment sender, AppEntry appEntry)
    {
        Log.d(TAG, "onAppClick");
        if (m_AppsAction == ACTION_APP_LAUNCH)
        {
            LaunchActivity(appEntry.getApplicationInfo().packageName);
        }
        else
        {
            UpdateFavApp(m_AppsAction, appEntry.getApplicationInfo().packageName);
            SwitchToMirrorSurface();
        }
    }

    @Override
    public boolean onAppLongClick(AppsGridFragment sender, AppEntry appEntry)
    {
        Log.d(TAG, "onAppLongClick");
        return false;
    }

    private void LaunchActivity(String packageName)
    {
        Log.d(TAG, "LaunchActivity");
        Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent != null)
            startActivity(intent);
        SwitchToMirrorSurface();
    }

    private void DoFavClick(int action, boolean longPress)
    {
        Log.d(TAG, "DoFavClick");

        String packageName = null;
        switch (action)
        {
            case ACTION_APP_FAV_1: packageName = m_AppFav1;break;
            case ACTION_APP_FAV_2: packageName = m_AppFav2;break;
            case ACTION_APP_FAV_3: packageName = m_AppFav3;break;
        }

        if (longPress || packageName == null || packageName.isEmpty())
            SwitchToAppsGrid(action);
        else
            LaunchActivity(packageName);
    }

    private void UpdateFavApp(int action, String packageName)
    {
        Log.d(TAG, "UpdateFavApp");

        Drawable icon = getDrawable(R.drawable.ic_star_black);
        try
        {
            if (packageName != null && !packageName.isEmpty())
                icon = getPackageManager().getApplicationIcon(packageName);

        }
        catch (Exception e)
        {
            Log.d(TAG, "UpdateFavApp exception: " + e.toString());
        }

        switch (action)
        {
            case ACTION_APP_FAV_1:
                m_AppFav1 = packageName;
                ImageView favImage1 = (ImageView)findViewById(R.id.m_Fav1);
                if (favImage1 != null) favImage1.setImageDrawable(icon);
                break;
            case ACTION_APP_FAV_2:
                m_AppFav2 = packageName;
                ImageView favImage2 = (ImageView)findViewById(R.id.m_Fav2);
                if (favImage2 != null) favImage2.setImageDrawable(icon);
                break;
            case ACTION_APP_FAV_3:
                m_AppFav3 = packageName;
                ImageView favImage3 = (ImageView)findViewById(R.id.m_Fav3);
                if (favImage3 != null) favImage3.setImageDrawable(icon);
                break;
        }
    }

    private void SwitchToAppsGrid(int action)
    {
        Log.d(TAG, "SwitchToAppsGrid");

        LinearLayout m_AppsGridLayout = (LinearLayout)findViewById(R.id.m_AppsGridLayout);
        if (m_AppsGridLayout != null) m_AppsGridLayout.bringToFront();

        m_AppsAction = action;
        m_AppsDrawerOpen = true;

        TextView appTitle = (TextView)findViewById(R.id.m_AppsTitle);
        if (appTitle != null)
        {
            if (m_AppsAction == ACTION_APP_LAUNCH)
                appTitle.setText(R.string.launch_activity);
            else
                appTitle.setText(R.string.set_favourite_activity);
        }
    }
    private void SwitchToMirrorSurface()
    {
        Log.d(TAG, "SwitchToMirrorSurface");
        if (m_SurfaceView != null) m_SurfaceView.bringToFront();

        m_AppsAction = ACTION_APP_LAUNCH;
        m_AppsDrawerOpen = false;
    }

    private void GenerateKeyEvent(int keyCode, boolean longPress)
    {
        Log.d(TAG, "GenerateKeyEvent");

        if (m_InputKeyEvent != null)
            m_InputKeyEvent.generate(keyCode, longPress);
    }

    private void startScreenCapture()
    {
        Log.d(TAG, "startScreenCapture");

        stopScreenCapture();

        UpdateTouchTransformations(true);

        DisplayMetrics metrics = new DisplayMetrics();
        c().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int ScreenDensity = metrics.densityDpi;

        MediaProjectionManager  mediaProjectionManager = (MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (mediaProjectionManager != null)
            m_MediaProjection = mediaProjectionManager.getMediaProjection(m_ProjectionCode, m_ProjectionIntent);

        if (m_MediaProjection != null)
        {
            int c_width = m_SurfaceView.getWidth();
            int c_height = m_SurfaceView.getHeight();

            Log.d(TAG, "c_width: " + c_width);
            Log.d(TAG, "c_height: " + c_height);
            Log.d(TAG, "ScreenDensity: " + ScreenDensity);

            if (c_width > 0 && c_height > 0)
            {
                m_VirtualDisplay = m_MediaProjection.createVirtualDisplay("ScreenCapture",
                        c_width, c_height, ScreenDensity,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                        m_Surface, null, null);
            }
        }
    }

    private void stopScreenCapture()
    {
        Log.d(TAG, "stopScreenCapture");
        if (m_VirtualDisplay != null)
        {
            m_VirtualDisplay.release();
            m_VirtualDisplay = null;
        }

        if (m_MediaProjection != null)
        {
            m_MediaProjection.stop();
            m_MediaProjection = null;
        }
    }

    private void UpdateTouchTransformations(boolean force)
    {
        if (CarApplication.ScreenRotation == m_ScreenRotation && !force)
            return;

        m_ScreenRotation = CarApplication.ScreenRotation;
        double ScreenWidth = CarApplication.ScreenSize.x;
        double ScreenHeight = CarApplication.ScreenSize.y;

        double SurfaceWidth = m_SurfaceView.getWidth();
        double SurfaceHeight = m_SurfaceView.getHeight();

        double factX = SurfaceWidth / ScreenWidth;
        double factY = SurfaceHeight / ScreenHeight;

        double fact = (factX < factY ? factX : factY);

        m_ProjectionWidth = fact * ScreenWidth;
        m_ProjectionHeight = fact * ScreenHeight;

        m_ProjectionOffsetX = (SurfaceWidth - m_ProjectionWidth) / 2.0;
        m_ProjectionOffsetY = (SurfaceHeight - m_ProjectionHeight) / 2.0;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        UpdateTouchTransformations(false);

        if (m_MinitouchSocket != null && event != null)
        {
            if (!m_MinitouchSocket.isConnected())
                m_MinitouchSocket.connect();

            boolean ok = m_MinitouchSocket.isConnected();
            int action = event.getActionMasked();
            for (int i = 0; i < event.getPointerCount() && ok; i++)
            {
                int id = event.getPointerId(i);
                double x = (event.getX(i) - m_ProjectionOffsetX) / m_ProjectionWidth;
                double y = (event.getY(i) - m_ProjectionOffsetY) / m_ProjectionHeight;
                double pressure = event.getPressure(i);

                double rx = x;
                double ry = y;
                switch (m_ScreenRotation)
                {
                    case ROTATION_0:
                    {
                        rx = x;
                        ry = y;
                        break;
                    }
                    case ROTATION_90:
                    {
                        rx = 1.0 - y;
                        ry = x;
                        break;
                    }
                    case ROTATION_180:
                    {
                        rx = 1.0 - x;
                        ry = 1.0 - y;
                        break;
                    }
                    case ROTATION_270:
                    {
                        rx = y;
                        ry = 1.0 - x;
                        break;
                    }
                }
                switch (action)
                {
                    case ACTION_DOWN:
                    case ACTION_POINTER_DOWN:
                        ok = ok && m_MinitouchSocket.TouchDown(id, rx, ry, pressure);
                        break;
                    case ACTION_MOVE:
                        ok = ok && m_MinitouchSocket.TouchMove(id, rx, ry, pressure);
                        break;
                    case ACTION_UP:
                        ok = ok && m_MinitouchSocket.TouchUpAll();
                        break;
                    case ACTION_POINTER_UP:
                        ok = ok && m_MinitouchSocket.TouchUp(id);
                        break;
                }
            }

            if (ok) m_MinitouchSocket.TouchCommit();
        }

        return true;
    }

    @Override
    public boolean handleMessage(Message msg)
    {
        Log.d(TAG, "handleMessage: " + (msg != null ? msg.toString() : "null"));

        if (msg != null)
        {
            if (msg.what == REQUEST_MEDIA_PROJECTION_PERMISSION)
            {
                m_ProjectionCode = msg.arg2;
                m_ProjectionIntent = (Intent)msg.obj;

                startScreenCapture();
                RequestWriteSettingsPermission();
                RequestOverlayPermission();
            }
        }
        return false;
    }

    private void startActivityForResult(int what, Intent intent)
    {
        Log.d(TAG, "startActivityForResult");
        ResultRequestActivity.startActivityForResult(this, m_RequestHandler, what, intent, what);
    }

    private void RequestProjectionPermission()
    {
        Log.d(TAG, "RequestProjectionPermission");
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if(mediaProjectionManager != null)
            startActivityForResult(REQUEST_MEDIA_PROJECTION_PERMISSION, mediaProjectionManager.createScreenCaptureIntent());
    }

    private void startActivity(String action)
    {
        Intent intent = new Intent(action);
        intent.setData(Uri.parse("package:" + getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void RequestWriteSettingsPermission()
    {
        Log.d(TAG, "RequestWriteSettingsPermission");
        if (!Settings.System.canWrite(this))
            startActivity(ACTION_MANAGE_WRITE_SETTINGS);
    }

    private void RequestOverlayPermission()
    {
        Log.d(TAG, "RequestOverlayPermission");
        if (!Settings.canDrawOverlays(this))
            startActivity(ACTION_MANAGE_OVERLAY_PERMISSION);
    }

    private void LoadSharedPreferences()
    {
        Log.d(TAG, "LoadSharedPreferences");

        SharedPreferences sharedPref = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
        UpdateFavApp(ACTION_APP_FAV_1, sharedPref.getString("m_AppFav1", null));
        UpdateFavApp(ACTION_APP_FAV_2, sharedPref.getString("m_AppFav2", null));
        UpdateFavApp(ACTION_APP_FAV_3, sharedPref.getString("m_AppFav3", null));
    }

    private void SaveSharedPreferences()
    {
        Log.d(TAG, "SaveSharedPreferences");

        SharedPreferences sharedPref = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("m_AppFav1", m_AppFav1);
        editor.putString("m_AppFav2", m_AppFav2);
        editor.putString("m_AppFav3", m_AppFav3);
        editor.apply();
    }
}
