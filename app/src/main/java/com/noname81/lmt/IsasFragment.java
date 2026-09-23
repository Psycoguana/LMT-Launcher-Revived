package com.noname81.lmt;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public class IsasFragment extends Fragment implements AdapterView.OnItemClickListener {
    public static final int offsetIsasBottom = 0;
    public static final int offsetIsasLeft = 6;
    private static final int offsetIsasRight = 9;
    public static final int offsetIsasTop = 3;
    private SeparatedListAdapter mAdapter;
    private ListView mListView;

    @Override // android.app.Fragment
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        setUserVisibleHint(true);
    }

    @Override // android.app.Fragment
    public void onResume() {
        SeparatedListAdapter separatedListAdapter;
        if (this.mListView != null && (separatedListAdapter = this.mAdapter) != null) {
            separatedListAdapter.notifyDataSetChanged();
            this.mListView.setAdapter((ListAdapter) this.mAdapter);
        }
        super.onResume();
    }

    @Override // android.app.Fragment
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        super.onCreateView(layoutInflater, viewGroup, bundle);
        LinkedList linkedList = new LinkedList();
        linkedList.add(
                SeparatedListAdapter.createItem("Isa 1", getString(R.string.isas_isa_bottom_left)));
        linkedList.add(
                SeparatedListAdapter.createItem(
                        "Isa 2", getString(R.string.isas_isa_bottom_center)));
        linkedList.add(
                SeparatedListAdapter.createItem(
                        "Isa 3", getString(R.string.isas_isa_bottom_right)));
        LinkedList linkedList2 = new LinkedList();
        linkedList2.add(
                SeparatedListAdapter.createItem("Isa 4", getString(R.string.isas_isa_top_left)));
        linkedList2.add(
                SeparatedListAdapter.createItem("Isa 5", getString(R.string.isas_isa_top_center)));
        linkedList2.add(
                SeparatedListAdapter.createItem("Isa 6", getString(R.string.isas_isa_top_right)));
        LinkedList linkedList3 = new LinkedList();
        linkedList3.add(
                SeparatedListAdapter.createItem("Isa 7", getString(R.string.isas_isa_left_top)));
        linkedList3.add(
                SeparatedListAdapter.createItem("Isa 8", getString(R.string.isas_isa_left_center)));
        linkedList3.add(
                SeparatedListAdapter.createItem("Isa 9", getString(R.string.isas_isa_left_bottom)));
        LinkedList linkedList4 = new LinkedList();
        linkedList4.add(
                SeparatedListAdapter.createItem("Isa 10", getString(R.string.isas_isa_right_top)));
        linkedList4.add(
                SeparatedListAdapter.createItem(
                        "Isa 11", getString(R.string.isas_isa_right_center)));
        linkedList4.add(
                SeparatedListAdapter.createItem(
                        "Isa 12", getString(R.string.isas_isa_right_bottom)));
        SeparatedListAdapter separatedListAdapter = new SeparatedListAdapter(getActivity());
        this.mAdapter = separatedListAdapter;
        separatedListAdapter.addSection(
                getString(R.string.isas_isa_areas_bottom),
                new CommandSimpleAdapterIsas(getActivity(), linkedList, 0));
        this.mAdapter.addSection(
                getString(R.string.isas_isa_areas_top),
                new CommandSimpleAdapterIsas(getActivity(), linkedList2, 3));
        this.mAdapter.addSection(
                getString(R.string.isas_isa_areas_left),
                new CommandSimpleAdapterIsas(getActivity(), linkedList3, 6));
        this.mAdapter.addSection(
                getString(R.string.isas_isa_areas_right),
                new CommandSimpleAdapterIsas(getActivity(), linkedList4, 9));
        ListView listView = new ListView(getActivity());
        this.mListView = listView;
        listView.setAdapter((ListAdapter) this.mAdapter);
        this.mListView.setOnItemClickListener(this);
        this.mListView.setDividerHeight(0);
        return this.mListView;
    }

    @Override // android.widget.AdapterView.OnItemClickListener
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
        if (i >= 0) {
            i--;
        }
        if (i >= 3) {
            i--;
        }
        if (i >= 6) {
            i--;
        }
        if (i >= 9) {
            i--;
        }
        SettingsValues.getInstance(getActivity()).setCurrentIsa(i);
        startActivityForResult(
                new Intent(view.getContext(), (Class<?>) CommandSelectActivity.class), 0);
    }

    class CommandSimpleAdapterIsas extends SimpleAdapter {
        private int mOffset;

        CommandSimpleAdapterIsas(Context context, List<? extends Map<String, ?>> list, int i) {
            super(
                    context,
                    list,
                    R.layout.listitem_icondescriptionicon,
                    new String[] {"title", "caption"},
                    new int[] {
                        R.id.listitem_icondescriptionicon_text,
                        R.id.listitem_icondescriptionicon_caption
                    });
            this.mOffset = i;
        }

        @Override // android.widget.SimpleAdapter, android.widget.Adapter
        public View getView(final int position, View view, ViewGroup viewGroup) {
            View view2 = super.getView(position, view, viewGroup);
            ImageView imageView =
                    (ImageView) view2.findViewById(R.id.listitem_icondescriptionicon_icon);
            ImageView imageView2 =
                    (ImageView) view2.findViewById(R.id.listitem_icondescriptionicon_icon2);
            IconUtils.setMaxSizeForImageView(IsasFragment.this.getActivity(), imageView);
            new AsyncDrawableTask(imageView, R.drawable.none) { // from class:
                // com.noname81.lmt.IsasFragment.CommandSimpleAdapterIsas.1
                /* JADX WARN: Can't rename method to resolve collision */
                @Override // com.noname81.lmt.AsyncDrawableTask, android.os.AsyncTask
                protected Drawable doInBackground(Void... voidArr) {
                    return IconUtils.getIconForISA(IsasFragment.this.getActivity());
                }
            }.execute(new Void[0]);
            IconUtils.setMaxSizeForImageView(IsasFragment.this.getActivity(), imageView2);
            new AsyncDrawableTask(imageView2, R.drawable.none) { // from class:
                // com.noname81.lmt.IsasFragment.CommandSimpleAdapterIsas.2
                /* JADX WARN: Can't rename method to resolve collision */
                @Override // com.noname81.lmt.AsyncDrawableTask, android.os.AsyncTask
                protected Drawable doInBackground(Void... voidArr) {
                    return IconUtils.getIconForAction(
                            IsasFragment.this.getActivity(),
                            SettingsValues.getInstance(IsasFragment.this.getActivity())
                                    .getIsaAction(CommandSimpleAdapterIsas.this.mOffset + position),
                            null);
                }
            }.execute(new Void[0]);
            return view2;
        }
    }
}
