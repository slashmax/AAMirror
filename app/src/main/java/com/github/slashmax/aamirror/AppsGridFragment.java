package com.github.slashmax.aamirror;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import java.util.List;

public class AppsGridFragment extends CarFragment
        implements LoaderManager.LoaderCallbacks<List<AppEntry>>, AdapterView.OnItemClickListener
{
    private static final String TAG = "AppsGridFragment";

    private GridView        m_GridView;
    private AppListAdapter  m_AppListAdapter;

    private AdapterView.OnItemClickListener m_OnItemClickListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        m_AppListAdapter = new AppListAdapter(getContext());
        m_OnItemClickListener= null;
    }

    @Override
    public void onDestroy()
    {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        Log.d(TAG, "onCreateView");
        return inflater.inflate(R.layout.apps_grid_view, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        Log.d(TAG, "onViewCreated");
        super.onViewCreated(view, savedInstanceState);

        m_GridView = view.findViewById(R.id.m_AppsGridView);
    }

    @Override
    public void onDestroyView()
    {
        Log.d(TAG, "onDestroyView");
        super.onDestroyView();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState)
    {
        Log.d(TAG, "onActivityCreated");
        super.onActivityCreated(savedInstanceState);

        if (m_GridView != null)
        {
            m_GridView.setAdapter(m_AppListAdapter);
            m_GridView.setOnItemClickListener(this);
        }

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<List<AppEntry>> onCreateLoader(int id, Bundle args)
    {
        Log.d(TAG, "onCreateLoader");
        return new AppListLoader(getContext());
    }

    @Override
    public void onLoadFinished(Loader<List<AppEntry>> loader, List<AppEntry> data)
    {
        Log.d(TAG, "onLoadFinished: " + data.size());
        m_AppListAdapter.setData(data);
    }

    @Override
    public void onLoaderReset(Loader<List<AppEntry>> loader)
    {
        Log.d(TAG, "onLoaderReset");
        m_AppListAdapter.setData(null);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        Log.d(TAG, "onItemClick");
        AppEntry app = m_AppListAdapter.getItem(position);
        if (app != null)
        {
            Intent intent = getContext().getPackageManager().getLaunchIntentForPackage(app.getApplicationInfo().packageName);
            if (intent != null)
            {
                startActivity(intent);
            }
        }

        if (m_OnItemClickListener != null)
            m_OnItemClickListener.onItemClick(parent, view, position, id);
    }

    public void setOnItemClickListener(@Nullable AdapterView.OnItemClickListener listener)
    {
        Log.d(TAG, "setOnItemClickListener");
        m_OnItemClickListener = listener;
    }
}
