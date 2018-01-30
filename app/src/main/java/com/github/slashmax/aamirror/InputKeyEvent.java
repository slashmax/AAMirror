package com.github.slashmax.aamirror;

import android.os.AsyncTask;
import android.util.Log;

import java.util.concurrent.Executor;

import eu.chainfire.libsuperuser.Shell;

public class InputKeyEvent
{
    private static final String TAG = "InputKeyEvent";

    private class KeyCodeParam
    {
        private int m_KeyCode;
        private boolean m_LongPress;

        KeyCodeParam(int keyCode, boolean longPress)
        {
            m_KeyCode = keyCode;
            m_LongPress = longPress;
        }

        public int GetKeyCode()
        {
            return m_KeyCode;
        }

        public  boolean IsLongPress()
        {
            return m_LongPress;
        }
    }

    private class TaskDirectExecutor implements Executor
    {
        public void execute(Runnable r) {
            new Thread(r).start();
        }
    }

    private class InputKeyEventTask extends AsyncTask<KeyCodeParam, Void, Void>
    {
        @Override
        protected Void doInBackground(KeyCodeParam... params)
        {
            Log.d(TAG, "doInBackground");
            if (params[0].IsLongPress())
                m_Shell.addCommand("input keyevent --longpress " + params[0].GetKeyCode());
            else
                m_Shell.addCommand("input keyevent " + params[0].GetKeyCode());
            return null;
        }
    }

    private TaskDirectExecutor m_Executor;
    private Shell.Interactive  m_Shell;

    public void init()
    {
        Log.d(TAG, "init");
        m_Executor = new TaskDirectExecutor();
        m_Shell = new Shell.Builder().useSU().setMinimalLogging(true).open();
    }

    public void generate(int keyCode, boolean longPress)
    {
        Log.d(TAG, "generate");
        if (m_Executor != null && m_Shell != null)
        {
            InputKeyEventTask task = new InputKeyEventTask();
            task.executeOnExecutor(m_Executor, new KeyCodeParam(keyCode, longPress));
        }
    }
}
