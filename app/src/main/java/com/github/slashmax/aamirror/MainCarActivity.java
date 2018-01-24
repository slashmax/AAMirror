package com.github.slashmax.aamirror;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.google.android.apps.auto.sdk.CarActivity;
import com.google.android.apps.auto.sdk.CarUiController;
import com.google.android.apps.auto.sdk.DayNightStyle;

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
        implements View.OnTouchListener, Handler.Callback, AdapterView.OnItemClickListener
{
    private static final String TAG = "MainCarActivity";

    private static final int        REQUEST_MEDIA_PROJECTION = 1;

    private Surface                 m_Surface;
    private SurfaceView             m_SurfaceView;
    private boolean                 m_AppsDrawerOpen;

    private VirtualDisplay          m_VirtualDisplay;
    private MediaProjection         m_MediaProjection;

    private MinitouchDaemon         m_MinitouchDaemon;
    private MinitouchSocket         m_MinitouchSocket;
    private MinitouchTask           m_MinitouchTask;
    private InputKeyEvent           m_InputKeyEvent;

    private int                     m_ScreenRotation;
    private double                  m_ProjectionOffsetX;
    private double                  m_ProjectionOffsetY;
    private double                  m_ProjectionWidth;
    private double                  m_ProjectionHeight;

    private int                     m_ProjectionCode;
    private Intent                  m_ProjectionIntent;

    @Override
    public void onCreate(Bundle bundle)
    {
        Log.d(TAG, "onCreate: " + (bundle != null ? bundle.toString() : "null"));

        setTheme(R.style.AppTheme);
        super.onCreate(bundle);
        setContentView(R.layout.activity_car_main);

        setIgnoreConfigChanges(0x200);
        InitCarUiController(getCarUiController());

        InitMinitouch();

        m_SurfaceView = (SurfaceView)findViewById(R.id.m_SurfaceView);
        m_SurfaceView.getHolder().setFormat(PixelFormat.RGBA_8888);
        m_Surface = m_SurfaceView.getHolder().getSurface();
        m_SurfaceView.setOnTouchListener(this);

        m_AppsDrawerOpen = false;

        AppsGridFragment gridFragment = (AppsGridFragment)getSupportFragmentManager().findFragmentById(R.id.m_AppsGridFragment);
        if (gridFragment != null)
            gridFragment.setOnItemClickListener(this);

        UpdateConfiguration(getResources().getConfiguration());
        InitButtonsActions();
        UpdateTouchTransformations(true);

        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if(mediaProjectionManager != null)
            ResultRequestActivity.startActivityForResult(this, new Handler(this), REQUEST_MEDIA_PROJECTION, mediaProjectionManager.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION);
    }

    @Override
    public void onDestroy()
    {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        stopScreenCapture();
        FreeMinitouch();
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
    }

    @Nullable
    @Override
    public View onCreateView(String s, @NonNull Context context, @NonNull AttributeSet attributeSet)
    {
        Log.d(TAG, "onCreateView: " + s + " (" + (context != null ? context.toString() : "null") + ")");
        return super.onCreateView(s, context, attributeSet);
    }

    @Override
    public View findViewById(int i)
    {
        Log.d(TAG, "findViewById: " + i);
        return super.findViewById(i);
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent)
    {
        Log.d(TAG, "onKeyDown: " + (keyEvent != null ? keyEvent.toString() : "null"));
        return super.onKeyDown(i, keyEvent);
    }

    @Override
    public boolean onKeyLongPress(int i, KeyEvent keyEvent) {
        Log.d(TAG, "onKeyLongPress: " + (keyEvent != null ? keyEvent.toString() : "null"));
        return super.onKeyLongPress(i, keyEvent);
    }

    @Override
    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        Log.d(TAG, "onKeyUp: " + (keyEvent != null ? keyEvent.toString() : "null"));
        return super.onKeyUp(i, keyEvent);
    }

    @Override
    public void onBackPressed()
    {
        Log.d(TAG, "onBackPressed");
        super.onBackPressed();
    }

    @Override
    public void onNewIntent(Intent intent)
    {
        Log.d(TAG, "onNewIntent: " + (intent != null ? intent.toString() : "null"));
        super.onNewIntent(intent);
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
    }

    @Override
    public void onStart()
    {
        Log.d(TAG, "onStart");
        super.onStart();

        if (CarApplication.OrientationListener != null)
            CarApplication.OrientationListener.enable();
        m_SurfaceView.setKeepScreenOn(true);

        PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        if (pm != null)
        {
            pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "ScreenOnWakeLock").acquire(1);
        }
    }

    @Override
    public void onStop()
    {
        Log.d(TAG, "onStop");
        super.onStop();
        stopScreenCapture();

        if (CarApplication.OrientationListener != null)
            CarApplication.OrientationListener.disable();
        m_SurfaceView.setKeepScreenOn(false);
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

    private void SetImageButtonColorState(int buttonId, ColorStateList foreTint, ColorStateList backTint)
    {
        Log.d(TAG, "SetImageButtonColorState");
        ImageButton button = (ImageButton)findViewById(buttonId);
        if (button != null)
        {
            button.setForegroundTintList(foreTint);
            button.setBackgroundTintList(backTint);
        }
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

        int textColor = getColor(R.color.colorCarText);

        int[][] states = new int[][] {
                new int[] { android.R.attr.state_enabled}, // enabled
                new int[] {-android.R.attr.state_enabled}, // disabled
                new int[] {-android.R.attr.state_checked}, // unchecked
                new int[] { android.R.attr.state_pressed}  // pressed
        };

        int[] foreColors = new int[] {textColor, textColor, textColor, textColor};

        int[] backColors = new int[] {backgroundColor, backgroundColor, backgroundColor, backgroundColor};

        ColorStateList foreTint = new ColorStateList(states, foreColors);
        ColorStateList backTint = new ColorStateList(states, backColors);

        LinearLayout buttonsLayout = (LinearLayout)findViewById(R.id.m_ButtonsLayout);
        if (buttonsLayout != null)
            buttonsLayout.setBackgroundColor(backgroundColor);

        FrameLayout appsGridLayout = (FrameLayout)findViewById(R.id.m_AppsGridLayout);
        if (appsGridLayout != null)
            appsGridLayout.setBackgroundColor(backgroundColor);

        SetImageButtonColorState(R.id.m_Fav1, foreTint, backTint);
        SetImageButtonColorState(R.id.m_Fav2, foreTint, backTint);
        SetImageButtonColorState(R.id.m_Fav3, foreTint, backTint);
        SetImageButtonColorState(R.id.m_Back, foreTint, backTint);
        SetImageButtonColorState(R.id.m_Menu, foreTint, backTint);
        SetImageButtonColorState(R.id.m_Apps, foreTint, backTint);
    }

    @Override
    public void onConfigurationChanged(Configuration configuration)
    {
        Log.d(TAG, "onConfigurationChanged: " + (configuration != null ? configuration.toString() : "null"));
        super.onConfigurationChanged(configuration);
        UpdateConfiguration(configuration);
    }

    @Override
    public void onLowMemory()
    {
        Log.d(TAG, "onLowMemory");
        super.onLowMemory();
    }

    @Override
    public void onFrameRateChange(int i)
    {
        Log.d(TAG, "onFrameRateChange: " + i);
        super.onFrameRateChange(i);
    }

    @Override
    public void onPowerStateChange(int i)
    {
        Log.d(TAG, "onPowerStateChange: " + i);
        super.onPowerStateChange(i);
    }

    @Override
    public Intent getIntent()
    {
        Log.d(TAG, "getIntent");
        return super.getIntent();
    }

    @Override
    public void setIntent(Intent intent)
    {
        Log.d(TAG, "setIntent: " + (intent != null ? intent.toString() : "null"));
        super.setIntent(intent);
    }

    @Override
    public void startCarActivity(Intent intent)
    {
        Log.d(TAG, "startCarActivity: " + (intent != null ? intent.toString() : "null"));
        super.startCarActivity(intent);
    }

    @Override
    public void onAccessibilityScanRequested(IBinder iBinder)
    {
        Log.d(TAG, "onAccessibilityScanRequested: " + (iBinder != null ? iBinder.toString() : "null"));
        super.onAccessibilityScanRequested(iBinder);
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

    private void InitButtonsActions()
    {
        Log.d(TAG, "InitButtonsActions");

        ImageButton m_Fav1 = (ImageButton)findViewById(R.id.m_Fav1);
        if (m_Fav1 != null)
            m_Fav1.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    Log.d(TAG, "m_Fav1.onClick");
                    SwitchToMirrorSurface();
                }
            });

        ImageButton m_Fav2 = (ImageButton)findViewById(R.id.m_Fav2);
        if (m_Fav2 != null)
            m_Fav2.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    Log.d(TAG, "m_Fav2.onClick");
                    SwitchToMirrorSurface();
                }
            });


        ImageButton m_Fav3 = (ImageButton)findViewById(R.id.m_Fav3);
        if (m_Fav3 != null)
            m_Fav3.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    Log.d(TAG, "m_Fav3.onClick");
                    SwitchToMirrorSurface();
                }
            });

        ImageButton m_Back =(ImageButton)findViewById(R.id.m_Back);
        if (m_Back != null)
            m_Back.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    Log.d(TAG, "m_Back.onClick");
                    SwitchToMirrorSurface();
                    GenerateKeyEvent(KeyEvent.KEYCODE_BACK, false);
                }
            });

        ImageButton m_Menu = (ImageButton)findViewById(R.id.m_Menu);
        if (m_Menu != null)
        {
            m_Menu.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    Log.d(TAG, "m_Menu.onClick");
                    SwitchToMirrorSurface();
                    GenerateKeyEvent(KeyEvent.KEYCODE_MENU, false);
                }
            });
            m_Menu.setOnLongClickListener(new View.OnLongClickListener()
            {
                @Override
                public boolean onLongClick(View v)
                {
                    Log.d(TAG, "m_Menu.onLongClick");
                    SwitchToMirrorSurface();
                    return GenerateKeyEvent(KeyEvent.KEYCODE_MENU, true);
                }
            });
        }

        ImageButton m_Apps = (ImageButton)findViewById(R.id.m_Apps);
        if (m_Apps != null)
            m_Apps.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    Log.d(TAG, "m_Apps.onClick");
                    if (m_AppsDrawerOpen)
                        SwitchToMirrorSurface();
                    else
                        SwitchToAppsGrid();
                }
            });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        Log.d(TAG, "onItemClick");
        SwitchToMirrorSurface();
    }

    private void SwitchToAppsGrid()
    {
        Log.d(TAG, "SwitchToAppsGrid");
        FrameLayout m_AppsGridLayout = (FrameLayout)findViewById(R.id.m_AppsGridLayout);
        if (m_AppsGridLayout != null) m_AppsGridLayout.bringToFront();
        m_AppsDrawerOpen = true;
    }
    private void SwitchToMirrorSurface()
    {
        Log.d(TAG, "SwitchToMirrorSurface");
        if (m_SurfaceView != null) m_SurfaceView.bringToFront();
        m_AppsDrawerOpen = false;
    }

    private boolean GenerateKeyEvent(int keyCode, boolean longPress)
    {
        Log.d(TAG, "GenerateKeyEvent");
        if (m_MinitouchDaemon == null || !m_MinitouchDaemon.HasRoot())
            return false;

        if (m_InputKeyEvent == null)
            m_InputKeyEvent = new InputKeyEvent();

        m_InputKeyEvent.generate(keyCode, longPress);
        return true;
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

    private class MinitouchTask extends AsyncTask<Context, Void, Void>
    {
        @Override
        protected Void doInBackground(Context... contexts)
        {
            Log.d(TAG, "doInBackground");
            m_MinitouchDaemon.run(contexts[0]);
            return null;
        }
    }

    private boolean InitMinitouch()
    {
        Log.d(TAG, "InitMinitouch");
        m_MinitouchDaemon = new MinitouchDaemon();
        m_MinitouchSocket = new MinitouchSocket();
        m_MinitouchTask = new MinitouchTask();

        m_MinitouchTask.execute(this);

        return true;
    }

    private boolean FreeMinitouch()
    {
        Log.d(TAG, "FreeMinitouch");

        m_MinitouchSocket.disconnect();
        m_MinitouchDaemon.kill(m_MinitouchSocket.getPid());
        m_MinitouchTask.cancel(true);

        return true;
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

        Log.d(TAG, "UpdateTouchTransformations Screen: " + ScreenWidth + " x " + ScreenHeight);
        Log.d(TAG, "UpdateTouchTransformations Surface: " + SurfaceWidth + " x " + SurfaceHeight);

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
            if (msg.what == REQUEST_MEDIA_PROJECTION)
            {
                m_ProjectionCode = msg.arg2;
                m_ProjectionIntent = (Intent)msg.obj;

                startScreenCapture();
            }
        }
        return false;
    }
}
