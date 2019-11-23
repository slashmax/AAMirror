package com.github.slashmax.aamirror;

import android.content.Context;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;

class MinitouchDaemon
{
    private static final String TAG = "MinitouchDaemon";

    private Context m_Context;

    MinitouchDaemon(Context context)
    {
        m_Context = context;
    }

    void start()
    {
        String path = install();
        if (path == null || path.isEmpty())
            return;

        Shell.SU.run("chmod 755 " + path);
        Shell.SU.run(path);
    }

    void stop(int pid)
    {
        if (pid != 0)
            Shell.SU.run("kill " + pid);
    }

    private String install()
    {
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
        return ("libs/" + detectAbi() + "/minitouch");
    }

    private String detectAbi()
    {
        List<String> result = Shell.SH.run("getprop ro.product.cpu.abi");
        if (result != null && !result.isEmpty())
            return result.get(0);

        return "armeabi";
    }
}
