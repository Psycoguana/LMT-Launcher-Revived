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

/* JADX INFO: loaded from: classes.dex */
class SeparatedListAdapter extends BaseAdapter {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    static final String ITEM_CAPTION = "caption";
    static final String ITEM_TITLE = "title";
    private static final int TYPE_SECTION_HEADER = 0;
    private final ArrayAdapter<String> headers;
    private final Map<String, Adapter> sections = new LinkedHashMap();

    public boolean areAllItemsSelectable() {
        return $assertionsDisabled;
    }

    @Override // android.widget.Adapter
    public long getItemId(int i) {
        return i;
    }

    static Map<String, ?> createItem(String str, String str2) {
        HashMap map = new HashMap();
        map.put(ITEM_TITLE, str);
        map.put(ITEM_CAPTION, str2);
        return map;
    }

    SeparatedListAdapter(Context context) {
        this.headers = new ArrayAdapter<>(context, R.layout.listheader);
    }

    void addSection(String str, Adapter adapter) {
        this.headers.add(str);
        this.sections.put(str, adapter);
    }

    @Override // android.widget.Adapter
    public Object getItem(int i) {
        for (String str : this.sections.keySet()) {
            Adapter adapter = this.sections.get(str);
            int count = adapter.getCount() + 1;
            if (i == 0) {
                return str;
            }
            if (i < count) {
                return adapter.getItem(i - 1);
            }
            i -= count;
        }
        return null;
    }

    @Override // android.widget.Adapter
    public int getCount() {
        Iterator<Adapter> it = this.sections.values().iterator();
        int count = 0;
        while (it.hasNext()) {
            count += it.next().getCount() + 1;
        }
        return count;
    }

    @Override // android.widget.BaseAdapter, android.widget.Adapter
    public int getViewTypeCount() {
        Iterator<Adapter> it = this.sections.values().iterator();
        int viewTypeCount = 1;
        while (it.hasNext()) {
            viewTypeCount += it.next().getViewTypeCount();
        }
        return viewTypeCount;
    }

    @Override // android.widget.BaseAdapter, android.widget.Adapter
    public int getItemViewType(int i) {
        Iterator<String> it = this.sections.keySet().iterator();
        int viewTypeCount = 1;
        while (it.hasNext()) {
            Adapter adapter = this.sections.get(it.next());
            int count = adapter.getCount() + 1;
            if (i == 0) {
                return 0;
            }
            if (i < count) {
                return viewTypeCount + adapter.getItemViewType(i - 1);
            }
            i -= count;
            viewTypeCount += adapter.getViewTypeCount();
        }
        return -1;
    }

    @Override // android.widget.BaseAdapter, android.widget.ListAdapter
    public boolean isEnabled(int i) {
        if (getItemViewType(i) != 0) {
            return true;
        }
        return $assertionsDisabled;
    }

    @Override // android.widget.Adapter
    public View getView(int i, View view, ViewGroup viewGroup) {
        Iterator<String> it = this.sections.keySet().iterator();
        int i2 = 0;
        while (it.hasNext()) {
            Adapter adapter = this.sections.get(it.next());
            int count = adapter.getCount() + 1;
            if (i == 0) {
                return this.headers.getView(i2, view, viewGroup);
            }
            if (i < count) {
                return adapter.getView(i - 1, view, viewGroup);
            }
            i -= count;
            i2++;
        }
        return null;
    }
}
