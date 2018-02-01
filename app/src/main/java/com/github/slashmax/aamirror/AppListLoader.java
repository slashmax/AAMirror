package com.github.slashmax.aamirror;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.v4.content.AsyncTaskLoader;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AppListLoader extends AsyncTaskLoader<List<AppEntry>>
{
    private static final Comparator<AppEntry> ALPHA_COMPARATOR = new Comparator<AppEntry>()
    {
        private final Collator sCollator = Collator.getInstance();
        @Override
        public int compare(AppEntry object1, AppEntry object2)
        {
            return sCollator.compare(object1.getLabel(), object2.getLabel());
        }
    };

    final PackageManager    m_PackageManager;
    private List<AppEntry>  m_Apps;

    AppListLoader(Context context)
    {
        super(context);
        m_PackageManager = getContext().getPackageManager();
    }

    @Override
    public List<AppEntry> loadInBackground()
    {
        List<ApplicationInfo> apps = m_PackageManager.getInstalledApplications(0);
        if (apps == null)
            apps = new ArrayList<>();

        final Context context = getContext();
        List<AppEntry> entries = new ArrayList<>(apps.size());
        for (int i = 0; i < apps.size(); i++)
        {
            if (m_PackageManager.getLaunchIntentForPackage(apps.get(i).packageName) == null)
                continue;

            AppEntry entry = new AppEntry(this, apps.get(i));
            entry.loadLabel(context);
            entries.add(entry);
        }

        Collections.sort(entries, ALPHA_COMPARATOR);
        return entries;
    }

    @Override
    public void deliverResult(List<AppEntry> apps)
    {
        if (isReset() && apps != null)
            onReleaseResources(apps);

        List<AppEntry> oldApps = m_Apps;
        m_Apps = apps;

        if (isStarted())
            super.deliverResult(apps);

        if (oldApps != null)
            onReleaseResources(oldApps);
    }

    @Override
    protected void onStartLoading()
    {
        if (m_Apps != null)
            deliverResult(m_Apps);

        if (takeContentChanged() || m_Apps == null)
            forceLoad();
    }

    @Override
    protected void onStopLoading()
    {
        cancelLoad();
    }

    @Override
    public void onCanceled(List<AppEntry> apps)
    {
        super.onCanceled(apps);
        onReleaseResources(apps);
    }

    @Override
    protected void onReset()
    {
        super.onReset();
        onStopLoading();
        if (m_Apps != null)
        {
            onReleaseResources(m_Apps);
            m_Apps = null;
        }
    }

    private void onReleaseResources(List<AppEntry> apps)
    {

    }
}
