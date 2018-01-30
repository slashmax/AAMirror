package com.github.slashmax.aamirror;

import android.content.Context;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;

public class MinitouchDaemon
{
    private static final String TAG = "MinitouchDaemon";

    private Context m_Context;

    MinitouchDaemon(Context context)
    {
        Log.d(TAG, "MinitouchDaemon");
        m_Context = context;
    }

    public void run()
    {
        Log.d(TAG, "run");

        String mt = install();
        if (mt == null || mt.isEmpty())
            return;

        LogShell(Shell.SU.run("chmod 755 " + mt));
        LogShell(Shell.SU.run(mt));
    }

    public void kill(int pid)
    {
        Log.d(TAG, "kill: " + pid);
        if (pid != 0)
            LogShell(Shell.SU.run("kill " + pid));
    }

    private String install()
    {
        Log.d(TAG, "install");

        try
        {
            FileOutputStream fileOutputStream = m_Context.openFileOutput("minitouch", 0);
            String assetName = getAssetFile();
            InputStream assetFile = m_Context.getAssets().open(assetName);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = assetFile.read(buffer)) != -1)
                fileOutputStream.write(buffer, 0, read);

            assetFile.close();
            fileOutputStream.close();
        }
        catch (Exception e)
        {
            Log.d(TAG, "install exception: " + e.toString());
            return null;
        }

        return m_Context.getFileStreamPath("minitouch").getAbsolutePath();
    }

    private String getAssetFile()
    {
        Log.d(TAG, "getAssetFile");
        return ("libs/" + detectAbi() + "/minitouch");
    }

    private String detectAbi()
    {
        Log.d(TAG, "detectAbi");
        List<String> result = Shell.SH.run("getprop ro.product.cpu.abi");
        LogShell(result);

        if (result != null && !result.isEmpty())
            return new String(result.get(0));

        return new String("armeabi");
    }

    private int detectSdk()
    {
        Log.d(TAG, "detectSdk");
        List<String> result = Shell.SH.run("getprop ro.build.version.sdk");
        LogShell(result);

        if (result != null && !result.isEmpty())
            return Integer.parseInt(result.get(0));

        return 0;
    }

    private void LogShell(List<String> list)
    {
        if (list != null)
        {
            for (int i = 0; i < list.size(); i++)
                Log.d(TAG, "LogShell: " + list.get(i));
        }
    }
}
