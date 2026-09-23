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
public class GesturesFragment extends Fragment implements AdapterView.OnItemClickListener {
    public static final int offsetDualTouchGestures = 11;
    public static final int offsetSingleTouchGestures = 0;
    public static final int offsetSwipeGestures = 3;
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
                SeparatedListAdapter.createItem(
                        TouchServiceResult.names[0], TouchServiceResult.captions[0]));
        linkedList.add(
                SeparatedListAdapter.createItem(
                        TouchServiceResult.names[1], TouchServiceResult.captions[1]));
        linkedList.add(
                SeparatedListAdapter.createItem(
                        TouchServiceResult.names[2], TouchServiceResult.captions[2]));
        LinkedList linkedList2 = new LinkedList();
        linkedList2.add(
                SeparatedListAdapter.createItem(
                        TouchServiceResult.names[3], TouchServiceResult.captions[3]));
        linkedList2.add(
                SeparatedListAdapter.createItem(
                        TouchServiceResult.names[4], TouchServiceResult.captions[4]));
        linkedList2.add(
                SeparatedListAdapter.createItem(
                        TouchServiceResult.names[5], TouchServiceResult.captions[5]));
        linkedList2.add(
                SeparatedListAdapter.createItem(
                        TouchServiceResult.names[6], TouchServiceResult.captions[6]));
        linkedList2.add(
                SeparatedListAdapter.createItem(
                        TouchServiceResult.names[7], TouchServiceResult.captions[7]));
        linkedList2.add(
                SeparatedListAdapter.createItem(
                        TouchServiceResult.names[8], TouchServiceResult.captions[8]));
        linkedList2.add(
                SeparatedListAdapter.createItem(
                        TouchServiceResult.names[9], TouchServiceResult.captions[9]));
        linkedList2.add(
                SeparatedListAdapter.createItem(
                        TouchServiceResult.names[10], TouchServiceResult.captions[10]));
        LinkedList linkedList3 = new LinkedList();
        linkedList3.add(
                SeparatedListAdapter.createItem(
                        TouchServiceResult.names[11], TouchServiceResult.captions[11]));
        linkedList3.add(
                SeparatedListAdapter.createItem(
                        TouchServiceResult.names[12], TouchServiceResult.captions[12]));
        linkedList3.add(
                SeparatedListAdapter.createItem(
                        TouchServiceResult.names[13], TouchServiceResult.captions[13]));
        SeparatedListAdapter separatedListAdapter = new SeparatedListAdapter(getActivity());
        this.mAdapter = separatedListAdapter;
        separatedListAdapter.addSection(
                getString(R.string.gestures_single_touch_gestures),
                new CommandSimpleAdapterGestures(getActivity(), linkedList, 0));
        this.mAdapter.addSection(
                getString(R.string.gestures_swipe_gestures),
                new CommandSimpleAdapterGestures(getActivity(), linkedList2, 3));
        this.mAdapter.addSection(
                getString(R.string.gestures_dual_touch_gestures),
                new CommandSimpleAdapterGestures(getActivity(), linkedList3, 11));
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
        if (i >= 11) {
            i--;
        }
        SettingsValues.getInstance(getActivity()).setCurrentGesture(i);
        startActivityForResult(
                new Intent(view.getContext(), (Class<?>) CommandSelectActivity.class), 0);
    }

    class CommandSimpleAdapterGestures extends SimpleAdapter {
        private int mOffset;

        CommandSimpleAdapterGestures(Context context, List<? extends Map<String, ?>> list, int i) {
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
            IconUtils.setMaxSizeForImageView(GesturesFragment.this.getActivity(), imageView);
            new AsyncDrawableTask(imageView, R.drawable.none) { // from class:
                // com.noname81.lmt.GesturesFragment.CommandSimpleAdapterGestures.1
                /* JADX WARN: Can't rename method to resolve collision */
                @Override // com.noname81.lmt.AsyncDrawableTask, android.os.AsyncTask
                protected Drawable doInBackground(Void... voidArr) {
                    return IconUtils.getIconForGesture(
                            GesturesFragment.this.getActivity(),
                            TouchServiceResult.names[
                                    CommandSimpleAdapterGestures.this.mOffset + position]
                                    .toLowerCase());
                }
            }.execute(new Void[0]);
            IconUtils.setMaxSizeForImageView(GesturesFragment.this.getActivity(), imageView2);
            new AsyncDrawableTask(imageView2, R.drawable.none) { // from class:
                // com.noname81.lmt.GesturesFragment.CommandSimpleAdapterGestures.2
                /* JADX WARN: Can't rename method to resolve collision */
                @Override // com.noname81.lmt.AsyncDrawableTask, android.os.AsyncTask
                protected Drawable doInBackground(Void... voidArr) {
                    return IconUtils.getIconForAction(
                            GesturesFragment.this.getActivity(),
                            SettingsValues.getInstance(GesturesFragment.this.getActivity())
                                    .getGestureAction(
                                            CommandSimpleAdapterGestures.this.mOffset + position),
                            null);
                }
            }.execute(new Void[0]);
            return view2;
        }
    }
}
