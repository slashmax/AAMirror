package com.github.slashmax.aamirror;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;

import java.io.File;

public class AppEntry
{
    private final AppListLoader     m_Loader;
    private final ApplicationInfo   m_Info;
    private final File              m_ApkFile;
    private String                  m_Label;
    private Drawable                m_Icon;
    private boolean                 m_Mounted;

    AppEntry(AppListLoader loader, ApplicationInfo info)
    {
        m_Loader = loader;
        m_Info = info;
        m_ApkFile = new File(info.sourceDir);
    }

    ApplicationInfo getApplicationInfo()
    {
        return m_Info;
    }

    String getLabel()
    {
        return m_Label;
    }

    public Drawable getIcon()
    {
        if (m_Icon == null)
        {
            if (m_ApkFile.exists())
            {
                m_Icon = m_Info.loadIcon(m_Loader.m_PackageManager);
                return m_Icon;
            }
            else
            {
                m_Mounted = false;
            }
        }
        else if (!m_Mounted)
        {
            if (m_ApkFile.exists())
            {
                m_Mounted = true;
                m_Icon = m_Info.loadIcon(m_Loader.m_PackageManager);
                return m_Icon;
            }
        }
        else
        {
            return m_Icon;
        }

        return m_Loader.getContext().getDrawable(android.R.drawable.sym_def_app_icon);
    }

    @Override
    public String toString()
    {
        return m_Label;
    }

    void loadLabel(Context context)
    {
        if (m_Label == null || !m_Mounted)
        {
            if (!m_ApkFile.exists())
            {
                m_Mounted = false;
                m_Label = m_Info.packageName;
            }
            else
            {
                m_Mounted = true;
                CharSequence label = m_Info.loadLabel(context.getPackageManager());
                m_Label = label != null ? label.toString() : m_Info.packageName;
            }
        }
    }
}
