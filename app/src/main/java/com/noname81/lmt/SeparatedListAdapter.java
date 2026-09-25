package com.noname81.lmt;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/** Presents several adapters as one list, each under its own header row. */
class SeparatedListAdapter extends BaseAdapter {
    static final String ITEM_CAPTION = "caption";
    static final String ITEM_TITLE = "title";
    private static final int TYPE_SECTION_HEADER = 0;
    private final ArrayAdapter<String> headers;
    private final Map<String, Adapter> sections = new LinkedHashMap();

    public boolean areAllItemsSelectable() {
        return false;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    static Map<String, ?> createItem(String title, String caption) {
        HashMap map = new HashMap();
        map.put(ITEM_TITLE, title);
        map.put(ITEM_CAPTION, caption);
        return map;
    }

    SeparatedListAdapter(Context context) {
        this.headers = new ArrayAdapter<>(context, R.layout.listheader);
    }

    void addSection(String header, Adapter adapter) {
        this.headers.add(header);
        this.sections.put(header, adapter);
    }

    @Override
    public Object getItem(int position) {
        for (String header : this.sections.keySet()) {
            Adapter adapter = this.sections.get(header);
            int count = adapter.getCount() + 1;
            if (position == 0) {
                return header;
            }
            if (position < count) {
                return adapter.getItem(position - 1);
            }
            position -= count;
        }
        return null;
    }

    @Override
    public int getCount() {
        Iterator<Adapter> it = this.sections.values().iterator();
        int count = 0;
        while (it.hasNext()) {
            count += it.next().getCount() + 1;
        }
        return count;
    }

    @Override
    public int getViewTypeCount() {
        Iterator<Adapter> it = this.sections.values().iterator();
        int viewTypeCount = 1;
        while (it.hasNext()) {
            viewTypeCount += it.next().getViewTypeCount();
        }
        return viewTypeCount;
    }

    @Override
    public int getItemViewType(int position) {
        Iterator<String> it = this.sections.keySet().iterator();
        int viewTypeCount = 1;
        while (it.hasNext()) {
            Adapter adapter = this.sections.get(it.next());
            int count = adapter.getCount() + 1;
            if (position == 0) {
                return 0;
            }
            if (position < count) {
                return viewTypeCount + adapter.getItemViewType(position - 1);
            }
            position -= count;
            viewTypeCount += adapter.getViewTypeCount();
        }
        return -1;
    }

    @Override
    public boolean isEnabled(int position) {
        if (getItemViewType(position) != 0) {
            return true;
        }
        return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Iterator<String> it = this.sections.keySet().iterator();
        int headerIndex = 0;
        while (it.hasNext()) {
            Adapter adapter = this.sections.get(it.next());
            int count = adapter.getCount() + 1;
            if (position == 0) {
                return this.headers.getView(headerIndex, convertView, parent);
            }
            if (position < count) {
                return adapter.getView(position - 1, convertView, parent);
            }
            position -= count;
            headerIndex++;
        }
        return null;
    }
}
