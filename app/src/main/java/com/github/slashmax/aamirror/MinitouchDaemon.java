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

    public boolean hasRoot()
    {
        Log.d(TAG, "hasRoot");
        return Shell.SU.available();
    }

    public void run(Context context)
    {
        Log.d(TAG, "run");

        if (!hasRoot())
            return;

        String mt = install(context);
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

    private String install(Context context)
    {
        Log.d(TAG, "install");

        try
        {
            FileOutputStream fileOutputStream = context.openFileOutput("minitouch", 0);
            String assetName = getAssetFile();
            InputStream assetFile = context.getAssets().open(assetName);
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

        return context.getFileStreamPath("minitouch").getAbsolutePath();
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
