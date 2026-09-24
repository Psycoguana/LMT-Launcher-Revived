package com.noname81.lmt;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import androidx.fragment.app.Fragment;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/** Settings tab binding each pie slot, across all three rings, to an action. */
public class PieFragment extends Fragment implements AdapterView.OnItemClickListener {
    public static final int offsetLevel1 = 0;
    public static final int offsetLevel2 = 10;
    public static final int offsetLevel3 = 24;
    private SeparatedListAdapter mAdapter;
    private int mCurrentPos;
    private ListView mListView;

    @Override // androidx.fragment.app.Fragment
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        setUserVisibleHint(true);
    }

    @Override // androidx.fragment.app.Fragment
    public void onResume() {
        SeparatedListAdapter separatedListAdapter;
        if (this.mListView != null && (separatedListAdapter = this.mAdapter) != null) {
            separatedListAdapter.notifyDataSetChanged();
            this.mListView.setAdapter((ListAdapter) this.mAdapter);
        }
        super.onResume();
    }

    @Override // androidx.fragment.app.Fragment
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        super.onCreateView(layoutInflater, viewGroup, bundle);
        LinkedList linkedList = new LinkedList();
        linkedList.add(
                SeparatedListAdapter.createItem(
                        "Pie item 1", getString(R.string.pie_level_1_item_1)));
        linkedList.add(
                SeparatedListAdapter.createItem(
                        "Pie item 1 longpress", getString(R.string.pie_level_1_item_1_longpress)));
        linkedList.add(
                SeparatedListAdapter.createItem(
                        "Pie item 2", getString(R.string.pie_level_1_item_2)));
        linkedList.add(
                SeparatedListAdapter.createItem(
                        "Pie item 2 longpress", getString(R.string.pie_level_1_item_2_longpress)));
        linkedList.add(
                SeparatedListAdapter.createItem(
                        "Pie item 3", getString(R.string.pie_level_1_item_3)));
        linkedList.add(
                SeparatedListAdapter.createItem(
                        "Pie item 3 longpress", getString(R.string.pie_level_1_item_3_longpress)));
        linkedList.add(
                SeparatedListAdapter.createItem(
                        "Pie item 4", getString(R.string.pie_level_1_item_4)));
        linkedList.add(
                SeparatedListAdapter.createItem(
                        "Pie item 4 longpress", getString(R.string.pie_level_1_item_4_longpress)));
        linkedList.add(
                SeparatedListAdapter.createItem(
                        "Pie item 5", getString(R.string.pie_level_1_item_5)));
        linkedList.add(
                SeparatedListAdapter.createItem(
                        "Pie item 5 longpress", getString(R.string.pie_level_1_item_5_longpress)));
        LinkedList linkedList2 = new LinkedList();
        linkedList2.add(
                SeparatedListAdapter.createItem(
                        "Pie item 1", getString(R.string.pie_level_2_item_1)));
        linkedList2.add(
                SeparatedListAdapter.createItem(
                        "Pie item 1 longpress", getString(R.string.pie_level_2_item_1_longpress)));
        linkedList2.add(
                SeparatedListAdapter.createItem(
                        "Pie item 2", getString(R.string.pie_level_2_item_2)));
        linkedList2.add(
                SeparatedListAdapter.createItem(
                        "Pie item 2 longpress", getString(R.string.pie_level_2_item_2_longpress)));
        linkedList2.add(
                SeparatedListAdapter.createItem(
                        "Pie item 3", getString(R.string.pie_level_2_item_3)));
        linkedList2.add(
                SeparatedListAdapter.createItem(
                        "Pie item 3 longpress", getString(R.string.pie_level_2_item_3_longpress)));
        linkedList2.add(
                SeparatedListAdapter.createItem(
                        "Pie item 4", getString(R.string.pie_level_2_item_4)));
        linkedList2.add(
                SeparatedListAdapter.createItem(
                        "Pie item 4 longpress", getString(R.string.pie_level_2_item_4_longpress)));
        linkedList2.add(
                SeparatedListAdapter.createItem(
                        "Pie item 5", getString(R.string.pie_level_2_item_5)));
        linkedList2.add(
                SeparatedListAdapter.createItem(
                        "Pie item 5 longpress", getString(R.string.pie_level_2_item_5_longpress)));
        linkedList2.add(
                SeparatedListAdapter.createItem(
                        "Pie item 6", getString(R.string.pie_level_2_item_6)));
        linkedList2.add(
                SeparatedListAdapter.createItem(
                        "Pie item 6 longpress", getString(R.string.pie_level_2_item_6_longpress)));
        linkedList2.add(
                SeparatedListAdapter.createItem(
                        "Pie item 7", getString(R.string.pie_level_2_item_7)));
        linkedList2.add(
                SeparatedListAdapter.createItem(
                        "Pie item 7 longpress", getString(R.string.pie_level_2_item_7_longpress)));
        SeparatedListAdapter separatedListAdapter = new SeparatedListAdapter(getActivity());
        this.mAdapter = separatedListAdapter;
        separatedListAdapter.addSection(
                getString(R.string.pie_level_1),
                new CommandSimpleAdapter(getActivity(), linkedList, 0));
        SeparatedListAdapter separatedListAdapter2 = this.mAdapter;
        separatedListAdapter2.addSection(
                getString(R.string.pie_level_2),
                new CommandSimpleAdapter(getActivity(), linkedList2, 10));
        LinkedList linkedList3 = new LinkedList();
        linkedList3.add(
                SeparatedListAdapter.createItem(
                        "Pie item 1", getString(R.string.pie_level_3_item_1)));
        linkedList3.add(
                SeparatedListAdapter.createItem(
                        "Pie item 1 longpress", getString(R.string.pie_level_3_item_1_longpress)));
        linkedList3.add(
                SeparatedListAdapter.createItem(
                        "Pie item 2", getString(R.string.pie_level_3_item_2)));
        linkedList3.add(
                SeparatedListAdapter.createItem(
                        "Pie item 2 longpress", getString(R.string.pie_level_3_item_2_longpress)));
        linkedList3.add(
                SeparatedListAdapter.createItem(
                        "Pie item 3", getString(R.string.pie_level_3_item_3)));
        linkedList3.add(
                SeparatedListAdapter.createItem(
                        "Pie item 3 longpress", getString(R.string.pie_level_3_item_3_longpress)));
        linkedList3.add(
                SeparatedListAdapter.createItem(
                        "Pie item 4", getString(R.string.pie_level_3_item_4)));
        linkedList3.add(
                SeparatedListAdapter.createItem(
                        "Pie item 4 longpress", getString(R.string.pie_level_3_item_4_longpress)));
        linkedList3.add(
                SeparatedListAdapter.createItem(
                        "Pie item 5", getString(R.string.pie_level_3_item_5)));
        linkedList3.add(
                SeparatedListAdapter.createItem(
                        "Pie item 5 longpress", getString(R.string.pie_level_3_item_5_longpress)));
        linkedList3.add(
                SeparatedListAdapter.createItem(
                        "Pie item 6", getString(R.string.pie_level_3_item_6)));
        linkedList3.add(
                SeparatedListAdapter.createItem(
                        "Pie item 6 longpress", getString(R.string.pie_level_3_item_6_longpress)));
        linkedList3.add(
                SeparatedListAdapter.createItem(
                        "Pie item 7", getString(R.string.pie_level_3_item_7)));
        linkedList3.add(
                SeparatedListAdapter.createItem(
                        "Pie item 7 longpress", getString(R.string.pie_level_3_item_7_longpress)));
        separatedListAdapter2.addSection(
                getString(R.string.pie_level_3),
                new CommandSimpleAdapter(getActivity(), linkedList3, 24));
        ListView listView = new ListView(getActivity());
        this.mListView = listView;
        listView.setAdapter((ListAdapter) this.mAdapter);
        this.mListView.setOnItemClickListener(this);
        this.mListView.setDividerHeight(0);
        return this.mListView;
    }

    @Override // android.widget.AdapterView.OnItemClickListener
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (position >= 0) {
            position--;
        }
        if (position >= 10) {
            position--;
        }
        if (position >= 24) {
            position--;
        }
        SettingsValues.getInstance(getActivity()).setCurrentPie(position);
        startActivityForResult(
                new Intent(view.getContext(), (Class<?>) CommandSelectActivity.class), 0);
    }

    @Override // androidx.fragment.app.Fragment
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == -1) {
            IconUtils.saveImageToFile(
                    IconUtils.getScaledBitmapFromUri(getActivity(), data.getData(), 150),
                    SettingsValues.getInstance(getActivity()).loadResourceSearchPath(),
                    IconUtils.getNamePie(this.mCurrentPos));
            SettingsValues.getInstance(getActivity()).restartServiceIfRequired();
        }
    }

    class CommandSimpleAdapter extends SimpleAdapter {
        private int mOffset;

        CommandSimpleAdapter(Context context, List<? extends Map<String, ?>> items, int resource) {
            super(
                    context,
                    items,
                    R.layout.listitem_icondescriptionicon,
                    new String[] {"title", "caption"},
                    new int[] {
                        R.id.listitem_icondescriptionicon_text,
                        R.id.listitem_icondescriptionicon_caption
                    });
            this.mOffset = resource;
        }

        @Override // android.widget.SimpleAdapter, android.widget.Adapter
        public View getView(final int position, View view, ViewGroup viewGroup) {
            View view2 = super.getView(position, view, viewGroup);
            ImageView imageView =
                    (ImageView) view2.findViewById(R.id.listitem_icondescriptionicon_icon);
            ImageView imageView2 =
                    (ImageView) view2.findViewById(R.id.listitem_icondescriptionicon_icon2);
            IconUtils.setMaxSizeForImageView(PieFragment.this.getActivity(), imageView);
            new AsyncDrawableTask(imageView, R.drawable.none) { // from class:
                // com.noname81.lmt.PieFragment.CommandSimpleAdapter.1
                @Override // com.noname81.lmt.AsyncDrawableTask, android.os.AsyncTask
                protected Drawable doInBackground(Void... voidArr) {
                    if (position % 2 > 0) {
                        return IconUtils.getIconForPieLongPress(PieFragment.this.getActivity());
                    }
                    return IconUtils.getIconForPieShortPress(PieFragment.this.getActivity());
                }
            }.execute(new Void[0]);
            IconUtils.setMaxSizeForImageView(PieFragment.this.getActivity(), imageView2);
            new AsyncDrawableTask(imageView2, R.drawable.none) { // from class:
                // com.noname81.lmt.PieFragment.CommandSimpleAdapter.2
                @Override // com.noname81.lmt.AsyncDrawableTask, android.os.AsyncTask
                protected Drawable doInBackground(Void... voidArr) {
                    return IconUtils.getIconForAction(
                            PieFragment.this.getActivity(),
                            SettingsValues.getInstance(PieFragment.this.getActivity())
                                    .getPieAction(CommandSimpleAdapter.this.mOffset + position),
                            IconUtils.getNamePie(CommandSimpleAdapter.this.mOffset + position));
                }
            }.execute(new Void[0]);
            // Named local class in the original (OnClickListenerWithPosition); jadx
            // rendered it as an anonymous class taking a constructor argument, which
            // is not valid Java. Per the smali constructor, mPosition is the argument
            // (mOffset + position), NOT position as jadx's initializer claimed.
            final int mPosition = this.mOffset + position;
            imageView2.setOnClickListener(
                    new View.OnClickListener() {

                        @Override // android.view.View.OnClickListener
                        public void onClick(View view3) {
                            PieFragment.this.mCurrentPos = mPosition;
                            final String path =
                                    SettingsValues.getInstance(PieFragment.this.getActivity())
                                            .loadResourceSearchPath();
                            final String fileName =
                                    IconUtils.getNamePie(PieFragment.this.mCurrentPos);
                            if (FileUtils.isFileAvailable(path, fileName)) {
                                new AlertDialog.Builder(PieFragment.this.getActivity())
                                        .setTitle("Icon selection")
                                        .setMessage(
                                                "Do you want to delete the current icon or set a"
                                                        + " new one?")
                                        .setPositiveButton(
                                                "New",
                                                new DialogInterface
                                                        .OnClickListener() { // from class:
                                                    // com.noname81.lmt.PieFragment.CommandSimpleAdapter.1OnClickListenerWithPosition.2
                                                    @Override // android.content.DialogInterface.OnClickListener
                                                    public void onClick(
                                                            DialogInterface dialogInterface,
                                                            int which) {
                                                        PieFragment.this.startActivityForResult(
                                                                new Intent(
                                                                        "android.intent.action.PICK",
                                                                        MediaStore.Images.Media
                                                                                .EXTERNAL_CONTENT_URI),
                                                                1);
                                                    }
                                                })
                                        .setNegativeButton(
                                                "Delete",
                                                new DialogInterface
                                                        .OnClickListener() { // from class:
                                                    // com.noname81.lmt.PieFragment.CommandSimpleAdapter.1OnClickListenerWithPosition.1
                                                    @Override // android.content.DialogInterface.OnClickListener
                                                    public void onClick(
                                                            DialogInterface dialogInterface,
                                                            int which) {
                                                        FileUtils.deleteFile(path, fileName);
                                                        PieFragment.this.mAdapter
                                                                .notifyDataSetChanged();
                                                        SettingsValues.getInstance(
                                                                        PieFragment.this
                                                                                .getActivity())
                                                                .restartServiceIfRequired();
                                                    }
                                                })
                                        .show();
                            } else {
                                PieFragment.this.startActivityForResult(
                                        new Intent(
                                                "android.intent.action.PICK",
                                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI),
                                        1);
                            }
                        }
                    });
            return view2;
        }
    }
}
