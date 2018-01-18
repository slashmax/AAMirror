package com.github.slashmax.aamirror;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.hardware.input.InputManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;

import com.google.android.apps.auto.sdk.CarActivity;
import com.google.android.apps.auto.sdk.CarUiController;
import com.google.android.apps.auto.sdk.DayNightStyle;

import java.lang.reflect.Method;

public class MainCarActivity extends CarActivity implements View.OnTouchListener, Handler.Callback
{
    private static final String TAG = "MainCarActivity";

    private static final int    REQUEST_MEDIA_PROJECTION = 1;

    private static int          DEFAULT_WIDTH    = 2160;
    private static int          DEFAULT_HEIGHT   = 1080;

    private Surface             m_Surface;
    private SurfaceView         m_SurfaceView;

    private VirtualDisplay      m_VirtualDisplay;
    private MediaProjection     m_MediaProjection;

    InputManager                m_InputManager;
    Method                      m_injectInputEvent;

    Handler                     m_Handler;
    int                         m_ProjectionCode;
    Intent                      m_ProjectionIntent;

    @Override
    public void onCreate(Bundle bundle)
    {
        Log.d(TAG, "onCreate: " + (bundle != null ? bundle.toString() : "null"));

        setTheme(R.style.AppTheme);
        super.onCreate(bundle);
        setContentView(R.layout.activity_car_main);

        setIgnoreConfigChanges(0xFFFF);//0x200
        InitCarUiController(getCarUiController());
        UpdateConfiguration(getResources().getConfiguration());

        InitInputInject();

        m_SurfaceView = (SurfaceView)findViewById(R.id.m_SurfaceView);
        m_SurfaceView.getHolder().setFixedSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        m_SurfaceView.getHolder().setFormat(PixelFormat.RGBA_8888);
        m_Surface = m_SurfaceView.getHolder().getSurface();
        m_SurfaceView.setOnTouchListener(this);

        m_Handler = new Handler(this);

        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if(mediaProjectionManager != null)
            ResultRequestActivity.startActivityForResult(this, m_Handler, REQUEST_MEDIA_PROJECTION, mediaProjectionManager.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION);
    }

    @Override
    public void onDestroy()
    {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        stopScreenCapture();
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

        m_SurfaceView.setKeepScreenOn(true);
    }

    @Override
    public void onStop()
    {
        Log.d(TAG, "onStop");
        super.onStop();
        stopScreenCapture();

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
    public void onWindowFocusChanged(boolean b, boolean b1)
    {
        Log.d(TAG, "onWindowFocusChanged");
        super.onWindowFocusChanged(b, b1);

        startScreenCapture();
    }

    private void UpdateConfiguration(Configuration configuration)
    {
        Log.d(TAG, "UpdateConfiguration: " + configuration.toString());
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

    private void startScreenCapture()
    {
        Log.d(TAG, "startScreenCapture");

        stopScreenCapture();

        DisplayMetrics metrics = new DisplayMetrics();
        c().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int ScreenDensity = metrics.densityDpi;

        MediaProjectionManager  mediaProjectionManager = (MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (mediaProjectionManager != null)
            m_MediaProjection = mediaProjectionManager.getMediaProjection(m_ProjectionCode, m_ProjectionIntent);

        if (m_MediaProjection != null)
        {
            int c_width = DEFAULT_WIDTH;
            int c_height = DEFAULT_HEIGHT;

            Log.d(TAG, "c_width: " + c_width);
            Log.d(TAG, "c_height: " + c_height);
            Log.d(TAG, "ScreenDensity: " + ScreenDensity);
            m_VirtualDisplay = m_MediaProjection.createVirtualDisplay("ScreenCapture",
                    c_width, c_height, ScreenDensity,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    m_Surface, null, null);
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

    private void InitInputInject()
    {
        Log.d(TAG, "InitInputInject");

        try
        {
            m_InputManager = (InputManager)InputManager.class.getDeclaredMethod("getInstance", new Class[0]).invoke(null, new Object[0]);
            m_injectInputEvent = InputManager.class.getMethod("injectInputEvent", new Class[]{InputEvent.class, Integer.TYPE});
        }
        catch (Exception e)
        {
            Log.d(TAG, "InitInputInject exception : " + e.toString());
        }
    }

    private void InjectInput(MotionEvent event)
    {
        Log.d(TAG, "InjectInput: " + (event != null ? event.toString() : "null"));
        try
        {
            if (m_injectInputEvent != null && m_InputManager != null)
                m_injectInputEvent.invoke(m_InputManager, new Object[]{event, Integer.valueOf(0)});
        }
        catch (Exception e)
        {
            Log.d(TAG, "InjectInput exception : " + e.toString());
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        Log.d(TAG, "onTouch: " + (event != null ? event.toString() : "null"));

//        InjectInput(event);
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
