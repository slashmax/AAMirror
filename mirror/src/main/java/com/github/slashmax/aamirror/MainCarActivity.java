package com.github.slashmax.aamirror;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import androidx.annotation.Nullable;
import android.support.car.Car;
import android.support.car.CarConnectionCallback;
import android.support.car.media.CarAudioManager;
import androidx.drawerlayout.widget.DrawerLayout;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.google.android.apps.auto.sdk.CarActivity;
import com.google.android.apps.auto.sdk.CarUiController;
import com.google.android.apps.auto.sdk.DayNightStyle;

import java.util.concurrent.Executor;

import eu.chainfire.libsuperuser.Shell;

import static android.content.Intent.ACTION_SCREEN_OFF;
import static android.content.Intent.ACTION_SCREEN_ON;
import static android.content.Intent.ACTION_USER_PRESENT;
import static android.media.AudioManager.AUDIOFOCUS_GAIN;
import static android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP;
import static android.os.PowerManager.ON_AFTER_RELEASE;
import static android.os.PowerManager.SCREEN_DIM_WAKE_LOCK;
import static android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION;
import static android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS;
import static android.support.car.media.CarAudioManager.CAR_AUDIO_USAGE_DEFAULT;
import static androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_LOCKED_CLOSED;
import static androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_UNLOCKED;
import static android.view.MotionEvent.ACTION_CANCEL;
import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_POINTER_DOWN;
import static android.view.MotionEvent.ACTION_POINTER_UP;
import static android.view.MotionEvent.ACTION_UP;
import static android.view.Surface.ROTATION_0;
import static android.view.Surface.ROTATION_180;
import static android.view.Surface.ROTATION_270;
import static android.view.Surface.ROTATION_90;
import static android.view.View.SYSTEM_UI_FLAG_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
import static android.view.View.SYSTEM_UI_FLAG_IMMERSIVE;
import static android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

public class MainCarActivity extends CarActivity
        implements Handler.Callback,
        View.OnTouchListener, TwoFingerGestureDetector.OnTwoFingerGestureListener,
        AppsGridFragment.OnAppClickListener, AppsGridFragment.OnAppLongClickListener
{
    private static final String         TAG = "MainCarActivity";

    private static final int            REQUEST_MEDIA_PROJECTION_PERMISSION = 1;

    private static final int            ACTION_APP_LAUNCH   = 0;
    private static final int            ACTION_APP_FAV_1    = 1;
    private static final int            ACTION_APP_FAV_2    = 2;    
    private static final int            ACTION_APP_FAV_3    = 3;
    private static final int            ACTION_APP_FAV_4    = 4;
    private static final int            ACTION_APP_FAV_5    = 5;

    private String                      m_AppFav1;
    private String                      m_AppFav2;
    private String                      m_AppFav3;
    private String                      m_AppFav4;
    private String                      m_AppFav5;

    private Car                         m_Car;

    private int                         m_AppsAction;

    private boolean                     m_ScreenResized;
    private boolean                     m_HasRoot;
    private MinitouchDaemon             m_MinitouchDaemon;
    private MinitouchSocket             m_MinitouchSocket;
    private MinitouchAsyncTask          m_MinitouchTask;

    private ShellDirectExecutor         m_ShellExecutor;
    private static Shell.Interactive    m_Shell;

    private UnlockReceiver              m_UnlockReceiver;
    private Handler                     m_RequestHandler;
    private PowerManager.WakeLock       m_WakeLock;

    private DrawerLayout                m_DrawerLayout;
    private LinearLayout                m_TaskBarDrawer;
    private LinearLayout                m_AppsDrawer;
    private SurfaceView                 m_SurfaceView;
    private Surface                     m_Surface;

    private SurfaceDrawerListener       m_DrawerListener;
    private TwoFingerGestureDetector    m_TwoFingerDetector;

    private VirtualDisplay              m_VirtualDisplay;
    private MediaProjection             m_MediaProjection;
    private int                         m_ProjectionCode;
    private Intent                      m_ProjectionIntent;

    private int                         m_ScreenRotation;
    private int                         m_ScreenWidth;
    private int                         m_ScreenHeight;
    private double                      m_ProjectionOffsetX;
    private double                      m_ProjectionOffsetY;
    private double                      m_ProjectionWidth;
    private double                      m_ProjectionHeight;

    private class MinitouchAsyncTask extends AsyncTask<Void, Void, Void>
    {
        @Override
        protected Void doInBackground(Void... voids)
        {
            m_MinitouchDaemon.start();
            return null;
        }
    }

    private class ShellDirectExecutor implements Executor
    {
        public void execute(Runnable r) {
            new Thread(r).start();
        }
    }

    private static class ShellAsyncTask extends AsyncTask<String, Void, Void>
    {
        @Override
        protected Void doInBackground(String... params)
        {
            if (m_Shell != null)
                m_Shell.addCommand(params[0]);
            return null;
        }
    }

    private class UnlockReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (intent != null && intent.getAction() != null)
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

    private class SurfaceDrawerListener extends DrawerLayout.SimpleDrawerListener
    {
        private DrawerLayout m_Drawer;
        SurfaceDrawerListener(DrawerLayout drawerLayout)
        {
            m_Drawer = drawerLayout;
            if (m_Drawer != null)
                m_Drawer.addDrawerListener(this);
        }

        void onDestroy()
        {
            if (m_Drawer != null)
            {
                m_Drawer.removeDrawerListener(this);
                m_Drawer = null;
            }
        }

        @Override
        public void onDrawerSlide(View drawerView, float slideOffset)
        {
            super.onDrawerSlide(drawerView, slideOffset);
            if (m_Drawer != null)
                m_Drawer.bringChildToFront(drawerView);
        }

        @Override
        public void onDrawerOpened(View drawerView)
        {
            super.onDrawerOpened(drawerView);
            if (m_Drawer != null)
                m_Drawer.bringChildToFront(drawerView);
        }

        @Override
        public void onDrawerClosed(View drawerView)
        {
            super.onDrawerClosed(drawerView);
            if (drawerView == m_AppsDrawer)
                m_AppsAction = ACTION_APP_LAUNCH;
        }
    }

    @Override
    public void onTwoFingerTapUp()
    {
        if (getDefaultSharedPreferences("open_left_drawer_on_two_finger_tap", true))
            m_DrawerLayout.openDrawer(m_TaskBarDrawer);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate(Bundle bundle)
    {
        super.onCreate(bundle);
        setTheme(R.style.AppTheme);
        setContentView(R.layout.activity_car_main);

        ForegroundService.startForegroundService(this);

        this.c().getDecorView().setSystemUiVisibility(
                SYSTEM_UI_FLAG_FULLSCREEN |
                SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                SYSTEM_UI_FLAG_IMMERSIVE | SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        setIgnoreConfigChanges(0xFFFF);

        InitCarUiController(getCarUiController());

        m_AppsAction = ACTION_APP_LAUNCH;

        m_MinitouchDaemon = new MinitouchDaemon(this);
        m_MinitouchSocket = new MinitouchSocket();
        m_MinitouchTask = new MinitouchAsyncTask();
        m_ShellExecutor= new ShellDirectExecutor();

        m_UnlockReceiver = new UnlockReceiver();
        m_RequestHandler = new Handler(this);

        PowerManager powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
        if (powerManager != null)
            m_WakeLock = powerManager.newWakeLock(SCREEN_DIM_WAKE_LOCK | ACQUIRE_CAUSES_WAKEUP, "aamirror:wakelock");

        m_DrawerLayout = (DrawerLayout)findViewById(R.id.m_DrawerLayout);
        m_TaskBarDrawer = (LinearLayout)findViewById(R.id.m_TaskBarDrawer);
        m_AppsDrawer = (LinearLayout)findViewById(R.id.m_AppsDrawer);
        m_SurfaceView = (SurfaceView)findViewById(R.id.m_SurfaceView);

        m_SurfaceView.setOnTouchListener(this);
        m_Surface = m_SurfaceView.getHolder().getSurface();

        m_DrawerListener = new SurfaceDrawerListener(m_DrawerLayout);
        m_TwoFingerDetector = new TwoFingerGestureDetector(this, this);

        AppsGridFragment gridFragment = (AppsGridFragment)getSupportFragmentManager().findFragmentById(R.id.m_AppsGridFragment);
        if (gridFragment != null)
        {
            gridFragment.setOnAppClickListener(this);
            gridFragment.setOnAppLongClickListener(this);
        }

        UpdateTouchTransformations(true);

        m_ScreenResized = false;
        m_HasRoot = Shell.SU.available();
        if (m_HasRoot)
        {
            m_MinitouchTask.execute();
            m_Shell = new Shell.Builder().useSU().open();
        }

        m_Car = Car.createCar(this, new CarConnectionCallback()
        {
            @Override
            public void onConnected(Car car)
            {
                RequestAudioFocus();
            }

            @Override
            public void onDisconnected(Car car)
            {
                AbandonAudioFocus();
                ResetScreenSize();
            }
        });
        m_Car.connect();

        InitButtonsActions();
        LoadSharedPreferences();

        SetAllowMediaProjection();
        RequestProjectionPermission();
    }

    @Override
    public void onDestroy()
    {
        ResetScreenSize();
        ResetImmersiveMode();
        m_DrawerListener.onDestroy();
        stopBrightnessService();
        stopOrientationService();
        stopScreenCapture();
        m_MinitouchSocket.disconnect();
        if (m_HasRoot)
        {
            m_MinitouchDaemon.stop(m_MinitouchSocket.getPid());
            m_MinitouchTask.cancel(true);
        }

        if (m_Car.isConnected())
            m_Car.disconnect();

        if (m_Shell != null)
            m_Shell.closeWhenIdle();

        ForegroundService.stopForegroundService(this);

        super.onDestroy();
    }

    @Override
    public void onRestoreInstanceState(Bundle bundle)
    {
        super.onRestoreInstanceState(bundle);
    }

    @Override
    public void onSaveInstanceState(Bundle bundle)
    {
        super.onSaveInstanceState(bundle);
        SaveSharedPreferences();
    }

    @Override
    public void onStart()
    {
        super.onStart();
        CarApplication.EnableOrientationListener();
        if (m_WakeLock != null)
            m_WakeLock.acquire();

        OnScreenOn();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USER_PRESENT);
        filter.addAction(ACTION_SCREEN_ON);
        filter.addAction(ACTION_SCREEN_OFF);
        registerReceiver(m_UnlockReceiver, filter);

        if (getDefaultSharedPreferences("disable_drawer_swipe", false))
            m_DrawerLayout.setDrawerLockMode(LOCK_MODE_LOCKED_CLOSED);
        else
            m_DrawerLayout.setDrawerLockMode(LOCK_MODE_UNLOCKED);
    }

    @Override
    public void onStop()
    {
        super.onStop();
        OnScreenOff();
        CarApplication.DisableOrientationListener();
        if (m_WakeLock != null && m_WakeLock.isHeld())
            m_WakeLock.release(ON_AFTER_RELEASE);

        unregisterReceiver(m_UnlockReceiver);
    }

    @Override
    public void onWindowFocusChanged(boolean focus, boolean b1)
    {
        super.onWindowFocusChanged(focus, b1);

        if (focus)
        {
            startScreenCapture();
            SetScreenSize();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration configuration)
    {
        super.onConfigurationChanged(configuration);
        UpdateTouchTransformations(true);
    }

    private void InitCarUiController(CarUiController controller)
    {
        controller.getStatusBarController().setTitle("");
        controller.getStatusBarController().hideAppHeader();
        controller.getStatusBarController().setAppBarAlpha(0.0f);
        controller.getStatusBarController().setAppBarBackgroundColor(Color.WHITE);
        controller.getStatusBarController().setDayNightStyle(DayNightStyle.AUTO);
        controller.getMenuController().hideMenuButton();
    }

    private void OnUnlock()
    {
        m_SurfaceView.setKeepScreenOn(false);
        startBrightnessService();
        startOrientationService();
        SetScreenSize();
        SetImmersiveMode();
        if (getDefaultSharedPreferences("open_left_drawer_on_start", false))
            m_DrawerLayout.openDrawer(m_TaskBarDrawer);
    }

    private void OnScreenOn()
    {
        m_SurfaceView.setKeepScreenOn(true);
        startScreenCapture();
        if (!IsLocked())
            OnUnlock();
    }

    private  void OnScreenOff()
    {
        m_SurfaceView.setKeepScreenOn(false);
        if (getDefaultSharedPreferences("reset_screen_size_on_stop", true))
            ResetScreenSize();
        ResetImmersiveMode();
        stopBrightnessService();
        if (getDefaultSharedPreferences("reset_screen_rotation_on_stop", true))
            stopOrientationService();
        stopScreenCapture();
    }

    private boolean IsLocked()
    {
        if (Build.VERSION.SDK_INT < 22)
            return false;

        KeyguardManager km = (KeyguardManager)getSystemService(KEYGUARD_SERVICE);
        return (km != null && km.isDeviceLocked());
    }

    private void startOrientationService()
    {
        int method = getDefaultSharedPreferences("orientation_method", 0);
        int rotation = getDefaultSharedPreferences("orientation_rotation", 0);

        startService(new Intent(this, OrientationService.class)
                .putExtra(OrientationService.METHOD, method)
                .putExtra(OrientationService.ROTATION, rotation));
    }

    private void stopOrientationService()
    {
        stopService(new Intent(this, OrientationService.class));
    }

    private void startBrightnessService()
    {
        boolean do_it = getDefaultSharedPreferences("overwrite_brightness", false);
        if (do_it)
        {
            int brightness = getDefaultSharedPreferences("overwrite_brightness_value", 0);
            startService(new Intent(this, BrightnessService.class)
                    .putExtra(BrightnessService.BRIGHTNESS, brightness)
                    .putExtra(BrightnessService.BRIGHTNESS_MODE, BrightnessService.SCREEN_BRIGHTNESS_MODE_MANUAL));
        }
    }

    private void stopBrightnessService()
    {
        stopService(new Intent(this, BrightnessService.class));
    }

    private void InitButtonsActions()
    {
        ImageView m_Back =(ImageView)findViewById(R.id.m_Back);
        if (m_Back != null)
        {
            if (!m_HasRoot) m_Back.setVisibility(View.GONE);

            m_Back.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    GenerateKeyEvent(KeyEvent.KEYCODE_BACK, false);
                }
            });
            m_Back.setOnLongClickListener(new View.OnLongClickListener()
            {
                @Override
                public boolean onLongClick(View v)
                {
                    GenerateKeyEvent(KeyEvent.KEYCODE_BACK, true);
                    return true;
                }
            });
        }

        ImageView m_Menu = (ImageView)findViewById(R.id.m_Menu);
        if (m_Menu != null)
        {
            if (!m_HasRoot) m_Menu.setVisibility(View.GONE);

            m_Menu.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    GenerateKeyEvent(KeyEvent.KEYCODE_MENU, false);
                }
            });
            m_Menu.setOnLongClickListener(new View.OnLongClickListener()
            {
                @Override
                public boolean onLongClick(View v)
                {
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
                    if  (m_DrawerLayout.isDrawerOpen(m_AppsDrawer))
                        m_DrawerLayout.closeDrawer(m_AppsDrawer);
                    else
                        OpenAppsDrawer(ACTION_APP_LAUNCH);
                }
            });
            m_Apps.setOnLongClickListener(new View.OnLongClickListener()
            {
                @Override
                public boolean onLongClick(View v)
                {
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
                    DoFavClick(ACTION_APP_FAV_1, false);
                }
            });
            m_Fav1.setOnLongClickListener(new View.OnLongClickListener()
            {
                @Override
                public boolean onLongClick(View v)
                {
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
                    DoFavClick(ACTION_APP_FAV_2, false);
                }
            });
            m_Fav2.setOnLongClickListener(new View.OnLongClickListener()
            {
                @Override
                public boolean onLongClick(View v)
                {
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
                    DoFavClick(ACTION_APP_FAV_3, false);
                }
            });
            m_Fav3.setOnLongClickListener(new View.OnLongClickListener()
            {
                @Override
                public boolean onLongClick(View v)
                {
                    DoFavClick(ACTION_APP_FAV_3, true);
                    return true;
                }
            });
        }

        ImageView m_Fav4 = (ImageView)findViewById(R.id.m_Fav4);
        if (m_Fav4 != null)
        {
            m_Fav4.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    DoFavClick(ACTION_APP_FAV_4, false);
                }
            });
            m_Fav4.setOnLongClickListener(new View.OnLongClickListener()
            {
                @Override
                public boolean onLongClick(View v)
                {
                    DoFavClick(ACTION_APP_FAV_4, true);
                    return true;
                }
            });
        }

        ImageView m_Fav5 = (ImageView)findViewById(R.id.m_Fav5);
        if (m_Fav5 != null)
        {
            m_Fav5.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    DoFavClick(ACTION_APP_FAV_5, false);
                }
            });
            m_Fav5.setOnLongClickListener(new View.OnLongClickListener()
            {
                @Override
                public boolean onLongClick(View v)
                {
                    DoFavClick(ACTION_APP_FAV_5, true);
                    return true;
                }
            });
        }
    }

    @Override
    public void onAppClick(AppsGridFragment sender, AppEntry appEntry)
    {
        if (m_AppsAction == ACTION_APP_LAUNCH)
        {
            LaunchActivity(appEntry.getApplicationInfo().packageName);
        }
        else
        {
            UpdateFavApp(m_AppsAction, appEntry.getApplicationInfo().packageName);
        }
    }

    @Override
    public boolean onAppLongClick(AppsGridFragment sender, AppEntry appEntry)
    {
        return false;
    }

    private void LaunchActivity(String packageName)
    {
        Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent != null)
            startActivity(intent);
        m_DrawerLayout.closeDrawers();
    }

    private void DoFavClick(int action, boolean longPress)
    {
        String packageName = null;
        switch (action)
        {
            case ACTION_APP_FAV_1: packageName = m_AppFav1;break;
            case ACTION_APP_FAV_2: packageName = m_AppFav2;break;
            case ACTION_APP_FAV_3: packageName = m_AppFav3;break;
            case ACTION_APP_FAV_4: packageName = m_AppFav4;break;
            case ACTION_APP_FAV_5: packageName = m_AppFav5;break;
        }

        if (longPress || packageName == null || packageName.isEmpty())
            OpenAppsDrawer(action);
        else
            LaunchActivity(packageName);
    }

    private void UpdateFavApp(int action, String packageName)
    {
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
            case ACTION_APP_FAV_4:
                m_AppFav4 = packageName;
                ImageView favImage4 = (ImageView)findViewById(R.id.m_Fav4);
                if (favImage4 != null) favImage4.setImageDrawable(icon);
                break;
            case ACTION_APP_FAV_5:
                m_AppFav5 = packageName;
                ImageView favImage5 = (ImageView)findViewById(R.id.m_Fav5);
                if (favImage5 != null) favImage5.setImageDrawable(icon);
                break;        
        }
    }

    private void OpenAppsDrawer(int action)
    {
        m_AppsAction = action;
        m_DrawerLayout.openDrawer(m_AppsDrawer);
    }

    private void GenerateKeyEvent(int keyCode, boolean longPress)
    {
        if (longPress)
            new ShellAsyncTask().executeOnExecutor(m_ShellExecutor, "input keyevent --longpress " + keyCode);
        else
            new ShellAsyncTask().executeOnExecutor(m_ShellExecutor, "input keyevent " + keyCode);
    }

    private void SetAllowMediaProjection()
    {
        new ShellAsyncTask().executeOnExecutor(m_ShellExecutor, "cmd appops set com.github.slashmax.aamirror PROJECT_MEDIA allow");
    }

    private void SetScreenSize()
    {
        boolean do_it = getDefaultSharedPreferences("set_screen_size_on_start", false);

        double c_width = m_SurfaceView.getWidth();
        double c_height = m_SurfaceView.getHeight();
        if (do_it && !IsLocked() && c_width > 0 && c_height > 0)
        {
            double ratio = c_width / c_height;
            double s_width = CarApplication.DisplaySize.x;
            if (s_width > 0)
            {
                SetScreenSize((int)s_width, (int)(s_width * ratio));
            }
        }
    }
    private void SetScreenSize(int width, int height)
    {
        if (!m_ScreenResized)
        {
            m_ScreenResized = true;
            new ShellAsyncTask().executeOnExecutor(m_ShellExecutor, "wm size " + width + "x" + height);
        }
    }

    private void ResetScreenSize()
    {
        m_ScreenResized = false;
        new ShellAsyncTask().executeOnExecutor(m_ShellExecutor, "wm size reset");
    }

    private void SetImmersiveMode()
    {
        String immersiveMode = getDefaultSharedPreferences("immersive_mode", "");
        if (immersiveMode.contains("immersive"))
            new ShellAsyncTask().executeOnExecutor(m_ShellExecutor, "settings put global policy_control " + immersiveMode);
    }

    private void ResetImmersiveMode()
    {
        new ShellAsyncTask().executeOnExecutor(m_ShellExecutor, "settings put global policy_control none*");
    }

    private void startScreenCapture()
    {
        stopScreenCapture();
        UpdateTouchTransformations(true);

        DisplayMetrics metrics = new DisplayMetrics();
        c().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int ScreenDensity = metrics.densityDpi;

        MediaProjectionManager  mediaProjectionManager = (MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (mediaProjectionManager != null && m_ProjectionIntent != null)
            m_MediaProjection = mediaProjectionManager.getMediaProjection(m_ProjectionCode, m_ProjectionIntent);

        if (m_MediaProjection != null && m_SurfaceView!= null)
        {
            int c_width = m_SurfaceView.getWidth();
            int c_height = m_SurfaceView.getHeight();

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
        if ((CarApplication.ScreenRotation == m_ScreenRotation) &&
            CarApplication.ScreenSize.equals(m_ScreenWidth, m_ScreenHeight) &&
            !force)
            return;

        if (m_SurfaceView == null)
            return;

        m_ScreenRotation = CarApplication.ScreenRotation;
        m_ScreenWidth = CarApplication.ScreenSize.x;
        m_ScreenHeight = CarApplication.ScreenSize.y;
        double ScreenWidth = m_ScreenWidth;
        double ScreenHeight = m_ScreenHeight;

        double SurfaceWidth = m_SurfaceView.getWidth();
        double SurfaceHeight = m_SurfaceView.getHeight();

        double factX = SurfaceWidth / ScreenWidth;
        double factY = SurfaceHeight / ScreenHeight;

        double fact = (factX < factY ? factX : factY);

        m_ProjectionWidth = fact * ScreenWidth;
        m_ProjectionHeight = fact * ScreenHeight;

        m_ProjectionOffsetX = (SurfaceWidth - m_ProjectionWidth) / 2.0;
        m_ProjectionOffsetY = (SurfaceHeight - m_ProjectionHeight) / 2.0;

        if (m_ScreenRotation == ROTATION_0 || m_ScreenRotation == ROTATION_180)
            m_MinitouchSocket.UpdateTouchTransformations(m_ScreenWidth, m_ScreenHeight, CarApplication.DisplaySize);
        else
            m_MinitouchSocket.UpdateTouchTransformations(m_ScreenHeight, m_ScreenWidth, CarApplication.DisplaySize);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        if (m_MinitouchSocket != null && event != null)
        {
            if (!m_MinitouchSocket.isConnected())
            {
                m_MinitouchSocket.connect(true);
                UpdateTouchTransformations(true);
            }
            else
            {
                UpdateTouchTransformations(false);
            }

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
                        ok = m_MinitouchSocket.TouchDown(id, rx, ry, pressure);
                        break;
                    case ACTION_MOVE:
                        ok = m_MinitouchSocket.TouchMove(id, rx, ry, pressure);
                        break;
                    case ACTION_UP:
                    case ACTION_CANCEL:
                        ok = m_MinitouchSocket.TouchUpAll();
                        break;
                    case ACTION_POINTER_UP:
                        ok = m_MinitouchSocket.TouchUp(id);
                        break;
                }
            }

            if (ok) m_MinitouchSocket.TouchCommit();
        }

        if (event != null)
            m_TwoFingerDetector.onTouchEvent(event);
        return true;
    }

    @Override
    public boolean handleMessage(Message msg)
    {
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
        ResultRequestActivity.startActivityForResult(this, m_RequestHandler, what, intent, what);
    }

    private void RequestProjectionPermission()
    {
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
        if (Build.VERSION.SDK_INT >= 23 && !Settings.System.canWrite(this))
            startActivity(ACTION_MANAGE_WRITE_SETTINGS);
    }

    private void RequestOverlayPermission()
    {
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this))
            startActivity(ACTION_MANAGE_OVERLAY_PERMISSION);
    }

    private void RequestAudioFocus()
    {
        if (!getDefaultSharedPreferences("request_audio_focus_on_connect", false))
            return;

        try
        {
            CarAudioManager carAM = m_Car.getCarManager(CarAudioManager.class);
            carAM.requestAudioFocus(null, carAM.getAudioAttributesForCarUsage(CAR_AUDIO_USAGE_DEFAULT), AUDIOFOCUS_GAIN, 0);
        }
        catch (Exception e)
        {
            Log.d(TAG, "RequestAudioFocus exception: " + e.toString());
        }
    }

    private void AbandonAudioFocus()
    {
        try
        {
            CarAudioManager carAM = m_Car.getCarManager(CarAudioManager.class);
            carAM.abandonAudioFocus(null, carAM.getAudioAttributesForCarUsage(CAR_AUDIO_USAGE_DEFAULT));
        }
        catch (Exception e)
        {
            Log.d(TAG, "AbandonAudioFocus exception: " + e.toString());
        }
    }

    private String getDefaultSharedPreferences(String key, @Nullable String defValue)
    {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPref.getString(key, defValue);
    }

    private int getDefaultSharedPreferences(String key, int defValue)
    {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String result = sharedPref.getString(key, Integer.toString(defValue));

        try
        {
            return Integer.parseInt(result);
        }
        catch (Exception e)
        {
            return  defValue;
        }
    }

    private boolean getDefaultSharedPreferences(String key, boolean defValue)
    {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPref.getBoolean(key, defValue);
    }

    private void LoadSharedPreferences()
    {
        UpdateFavApp(ACTION_APP_FAV_1, getDefaultSharedPreferences("m_AppFav1", null));
        UpdateFavApp(ACTION_APP_FAV_2, getDefaultSharedPreferences("m_AppFav2", null));
        UpdateFavApp(ACTION_APP_FAV_3, getDefaultSharedPreferences("m_AppFav3", null));
        UpdateFavApp(ACTION_APP_FAV_4, getDefaultSharedPreferences("m_AppFav4", null));
        UpdateFavApp(ACTION_APP_FAV_5, getDefaultSharedPreferences("m_AppFav5", null));
    }

    private void SaveSharedPreferences()
    {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("m_AppFav1", m_AppFav1);
        editor.putString("m_AppFav2", m_AppFav2);
        editor.putString("m_AppFav3", m_AppFav3);
        editor.putString("m_AppFav4", m_AppFav4);
        editor.putString("m_AppFav5", m_AppFav5);
        editor.apply();
    }
}
