package com.github.slashmax.aamirror;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import java.util.List;

public class AppsGridFragment extends CarFragment
        implements LoaderManager.LoaderCallbacks<List<AppEntry>>,
        AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener
{
    private static final String TAG = "AppsGridFragment";

    private GridView                m_GridView;
    private AppListAdapter          m_AppListAdapter;

    private OnAppClickListener      m_OnAppClickListener;
    private OnAppLongClickListener  m_OnAppLongClickListener;

    public interface OnAppClickListener
    {
        void onAppClick(AppsGridFragment sender, AppEntry appEntry);
    }

    public interface OnAppLongClickListener
    {
        boolean onAppLongClick(AppsGridFragment sender, AppEntry appEntry);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        m_AppListAdapter = new AppListAdapter(getContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.apps_grid_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        m_GridView = view.findViewById(R.id.m_AppsGridView);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        if (m_GridView != null)
        {
            m_GridView.setAdapter(m_AppListAdapter);
            m_GridView.setOnItemClickListener(this);
            m_GridView.setOnItemLongClickListener(this);
        }
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<List<AppEntry>> onCreateLoader(int id, Bundle args)
    {
        return new AppListLoader(getContext());
    }

    @Override
    public void onLoadFinished(Loader<List<AppEntry>> loader, List<AppEntry> data)
    {
        m_AppListAdapter.setData(data);
    }

    @Override
    public void onLoaderReset(Loader<List<AppEntry>> loader)
    {
        m_AppListAdapter.setData(null);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        AppEntry app = m_AppListAdapter.getItem(position);
        if (app != null && m_OnAppClickListener != null)
            m_OnAppClickListener.onAppClick(this, app);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
    {
        AppEntry app = m_AppListAdapter.getItem(position);
        return (app != null) &&
                (m_OnAppLongClickListener != null) &&
                m_OnAppLongClickListener.onAppLongClick(this, app);
    }

    public void setOnAppClickListener(@Nullable OnAppClickListener listener)
    {
        m_OnAppClickListener = listener;
    }

    public void setOnAppLongClickListener(@Nullable OnAppLongClickListener listener)
    {
        m_OnAppLongClickListener = listener;
    }
}
