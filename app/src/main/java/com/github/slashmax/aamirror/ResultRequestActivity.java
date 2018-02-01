package com.github.slashmax.aamirror;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class ResultRequestActivity extends Activity
{
    private static final String TAG = "ResultRequestActivity";

    private static Handler  ResultHandler;
    private static int      RequestWhat;
    private static Intent   RequestIntent;
    private static int      RequestCode;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_request);
        startActivityForResult();
    }

    @Override
    protected void onDestroy()
    {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        Log.d(TAG, "onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);
        if (ResultHandler != null)
        {
            Message msg = Message.obtain(ResultHandler, RequestWhat, requestCode, resultCode, data);
            msg.sendToTarget();
        }
        finish();
    }

    private void startActivityForResult()
    {
        Log.d(TAG, "startActivityForResult");
        if (ResultHandler != null && RequestIntent != null)
            startActivityForResult(RequestIntent, RequestCode);
        else
            finish();
    }

    public static void startActivityForResult(Context context, Handler handler, int what, Intent intent, int requestCod)
    {
        Log.d(TAG, "startActivityForResult");

        ResultHandler   = handler;
        RequestWhat     = what;
        RequestIntent   = intent;
        RequestCode     = requestCod;

        Intent this_intent = new Intent(context, ResultRequestActivity.class);
        this_intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(this_intent);
    }
}
