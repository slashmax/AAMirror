package com.github.slashmax.aamirror;

import android.support.v4.app.Fragment;
import android.util.Log;

import com.google.android.apps.auto.sdk.CarActivity;

public class CarFragment extends Fragment
{
    private static final String TAG = "CarFragment";

    private CarActivity m_Activity;

    public CarFragment() {
        super();
        Log.d(TAG, "CarFragment");
    }

    public void SetActivity(CarActivity activity)
    {
        Log.d(TAG, "SetActivity");
        m_Activity = activity;
    }

    public CarActivity GetActivity()
    {
        Log.d(TAG, "GetActivity");
        return m_Activity;
    }
}
