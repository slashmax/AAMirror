package com.github.slashmax.aamirror;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class AppListAdapter extends ArrayAdapter<AppEntry>
{
    private final LayoutInflater m_Inflater;

    AppListAdapter(Context context)
    {
        super(context, android.R.layout.simple_list_item_2);
        m_Inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setData(List<AppEntry> data)
    {
        clear();
        if (data != null)
            addAll(data);
    }

    @Override
    public @NonNull View getView(int position, View convertView, @NonNull ViewGroup parent)
    {
        View view;

        if (convertView == null)
            view = m_Inflater.inflate(R.layout.list_item_icon_text, parent, false);
        else
            view = convertView;

        AppEntry item = getItem(position);
        if (item != null)
        {
            ((ImageView) view.findViewById(R.id.list_item_icon)).setImageDrawable(item.getIcon());
            ((TextView) view.findViewById(R.id.list_item_text)).setText(item.getLabel());
        }

        return view;
    }
}
