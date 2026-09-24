package com.noname81.lmt;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

/** Settings tab binding each pie slot, across all three rings, to an action. */
public class PieFragment extends Fragment {
    Adapter mAdapter;
    int mCurrentPos;

    @Override // androidx.fragment.app.Fragment
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        RecyclerView recyclerView = new RecyclerView(requireContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        this.mAdapter = new Adapter();
        recyclerView.setAdapter(this.mAdapter);
        return recyclerView;
    }

    @Override // androidx.fragment.app.Fragment
    public void onResume() {
        super.onResume();
        if (this.mAdapter != null) {
            this.mAdapter.notifyDataSetChanged();
        }
    }

    private void handleRowClick(int absoluteIndex) {
        SettingsValues.getInstance(requireActivity()).setCurrentPie(absoluteIndex);
        startActivityForResult(new Intent(requireActivity(), CommandSelectActivity.class), 0);
    }

    private void handleIconClick(int absoluteIndex) {
        this.mCurrentPos = absoluteIndex;
        String path = SettingsValues.getInstance(requireActivity()).loadResourceSearchPath();
        String fileName = IconUtils.getNamePie(this.mCurrentPos);
        if (FileUtils.isFileAvailable(path, fileName)) {
            new MaterialAlertDialogBuilder(requireActivity())
                    .setTitle("Icon selection")
                    .setMessage("Do you want to delete the current icon or set a new one?")
                    .setPositiveButton(
                            "New",
                            (dialogInterface, which) ->
                                    startActivityForResult(
                                            new Intent(
                                                    Intent.ACTION_PICK,
                                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI),
                                            1))
                    .setNegativeButton(
                            "Delete",
                            (dialogInterface, which) -> {
                                FileUtils.deleteFile(path, fileName);
                                this.mAdapter.notifyDataSetChanged();
                                SettingsValues.getInstance(requireActivity())
                                        .restartServiceIfRequired();
                            })
                    .show();
        } else {
            startActivityForResult(
                    new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI),
                    1);
        }
    }

    /** Test-only entry point: reproduces a real tap on the row at this flat position. */
    void clickRowForTest(int flatPosition) {
        handleRowClick(this.mAdapter.mRows.absoluteRowIndex(flatPosition));
    }

    /** Test-only entry point: reproduces a real tap on the row's icon2 button. */
    void clickIconForTest(int flatPosition) {
        handleIconClick(this.mAdapter.mRows.absoluteRowIndex(flatPosition));
    }

    SectionedRows<SectionedRows.Labeled> rowsForTest() {
        return this.mAdapter.mRows;
    }

    @Override // androidx.fragment.app.Fragment
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            IconUtils.saveImageToFile(
                    IconUtils.getScaledBitmapFromUri(requireActivity(), data.getData(), 150),
                    SettingsValues.getInstance(requireActivity()).loadResourceSearchPath(),
                    IconUtils.getNamePie(this.mCurrentPos));
            SettingsValues.getInstance(requireActivity()).restartServiceIfRequired();
        }
    }

    final class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        final SectionedRows<SectionedRows.Labeled> mRows = new SectionedRows<>();

        Adapter() {
            this.mRows.addSection(getString(R.string.pie_level_1), level1Rows());
            this.mRows.addSection(getString(R.string.pie_level_2), level2Rows());
            this.mRows.addSection(getString(R.string.pie_level_3), level3Rows());
        }

        private List<SectionedRows.Labeled> level1Rows() {
            List<SectionedRows.Labeled> rows = new ArrayList<>();
            rows.add(new SectionedRows.Labeled("Pie item 1", getString(R.string.pie_level_1_item_1)));
            rows.add(
                    new SectionedRows.Labeled(
                            "Pie item 1 longpress", getString(R.string.pie_level_1_item_1_longpress)));
            rows.add(new SectionedRows.Labeled("Pie item 2", getString(R.string.pie_level_1_item_2)));
            rows.add(
                    new SectionedRows.Labeled(
                            "Pie item 2 longpress", getString(R.string.pie_level_1_item_2_longpress)));
            rows.add(new SectionedRows.Labeled("Pie item 3", getString(R.string.pie_level_1_item_3)));
            rows.add(
                    new SectionedRows.Labeled(
                            "Pie item 3 longpress", getString(R.string.pie_level_1_item_3_longpress)));
            rows.add(new SectionedRows.Labeled("Pie item 4", getString(R.string.pie_level_1_item_4)));
            rows.add(
                    new SectionedRows.Labeled(
                            "Pie item 4 longpress", getString(R.string.pie_level_1_item_4_longpress)));
            rows.add(new SectionedRows.Labeled("Pie item 5", getString(R.string.pie_level_1_item_5)));
            rows.add(
                    new SectionedRows.Labeled(
                            "Pie item 5 longpress", getString(R.string.pie_level_1_item_5_longpress)));
            return rows;
        }

        private List<SectionedRows.Labeled> level2Rows() {
            List<SectionedRows.Labeled> rows = new ArrayList<>();
            rows.add(new SectionedRows.Labeled("Pie item 1", getString(R.string.pie_level_2_item_1)));
            rows.add(
                    new SectionedRows.Labeled(
                            "Pie item 1 longpress", getString(R.string.pie_level_2_item_1_longpress)));
            rows.add(new SectionedRows.Labeled("Pie item 2", getString(R.string.pie_level_2_item_2)));
            rows.add(
                    new SectionedRows.Labeled(
                            "Pie item 2 longpress", getString(R.string.pie_level_2_item_2_longpress)));
            rows.add(new SectionedRows.Labeled("Pie item 3", getString(R.string.pie_level_2_item_3)));
            rows.add(
                    new SectionedRows.Labeled(
                            "Pie item 3 longpress", getString(R.string.pie_level_2_item_3_longpress)));
            rows.add(new SectionedRows.Labeled("Pie item 4", getString(R.string.pie_level_2_item_4)));
            rows.add(
                    new SectionedRows.Labeled(
                            "Pie item 4 longpress", getString(R.string.pie_level_2_item_4_longpress)));
            rows.add(new SectionedRows.Labeled("Pie item 5", getString(R.string.pie_level_2_item_5)));
            rows.add(
                    new SectionedRows.Labeled(
                            "Pie item 5 longpress", getString(R.string.pie_level_2_item_5_longpress)));
            rows.add(new SectionedRows.Labeled("Pie item 6", getString(R.string.pie_level_2_item_6)));
            rows.add(
                    new SectionedRows.Labeled(
                            "Pie item 6 longpress", getString(R.string.pie_level_2_item_6_longpress)));
            rows.add(new SectionedRows.Labeled("Pie item 7", getString(R.string.pie_level_2_item_7)));
            rows.add(
                    new SectionedRows.Labeled(
                            "Pie item 7 longpress", getString(R.string.pie_level_2_item_7_longpress)));
            return rows;
        }

        private List<SectionedRows.Labeled> level3Rows() {
            List<SectionedRows.Labeled> rows = new ArrayList<>();
            rows.add(new SectionedRows.Labeled("Pie item 1", getString(R.string.pie_level_3_item_1)));
            rows.add(
                    new SectionedRows.Labeled(
                            "Pie item 1 longpress", getString(R.string.pie_level_3_item_1_longpress)));
            rows.add(new SectionedRows.Labeled("Pie item 2", getString(R.string.pie_level_3_item_2)));
            rows.add(
                    new SectionedRows.Labeled(
                            "Pie item 2 longpress", getString(R.string.pie_level_3_item_2_longpress)));
            rows.add(new SectionedRows.Labeled("Pie item 3", getString(R.string.pie_level_3_item_3)));
            rows.add(
                    new SectionedRows.Labeled(
                            "Pie item 3 longpress", getString(R.string.pie_level_3_item_3_longpress)));
            rows.add(new SectionedRows.Labeled("Pie item 4", getString(R.string.pie_level_3_item_4)));
            rows.add(
                    new SectionedRows.Labeled(
                            "Pie item 4 longpress", getString(R.string.pie_level_3_item_4_longpress)));
            rows.add(new SectionedRows.Labeled("Pie item 5", getString(R.string.pie_level_3_item_5)));
            rows.add(
                    new SectionedRows.Labeled(
                            "Pie item 5 longpress", getString(R.string.pie_level_3_item_5_longpress)));
            rows.add(new SectionedRows.Labeled("Pie item 6", getString(R.string.pie_level_3_item_6)));
            rows.add(
                    new SectionedRows.Labeled(
                            "Pie item 6 longpress", getString(R.string.pie_level_3_item_6_longpress)));
            rows.add(new SectionedRows.Labeled("Pie item 7", getString(R.string.pie_level_3_item_7)));
            rows.add(
                    new SectionedRows.Labeled(
                            "Pie item 7 longpress", getString(R.string.pie_level_3_item_7_longpress)));
            return rows;
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public int getItemCount() {
            return this.mRows.getItemCount();
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public int getItemViewType(int position) {
            return this.mRows.getItemViewType(position);
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public RecyclerView.ViewHolder onCreateViewHolder(
                @NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == SectionedRows.VIEW_TYPE_HEADER) {
                return new HeaderViewHolder(inflater.inflate(R.layout.listheader, parent, false));
            }
            return new RowViewHolder(
                    inflater.inflate(R.layout.listitem_icondescriptionicon, parent, false));
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof HeaderViewHolder) {
                ((HeaderViewHolder) holder).text.setText(this.mRows.headerAt(position));
                return;
            }
            RowViewHolder rowHolder = (RowViewHolder) holder;
            SectionedRows.Labeled row = this.mRows.rowAt(position);
            int absoluteIndex = this.mRows.absoluteRowIndex(position);
            rowHolder.title.setText(row.title);
            rowHolder.caption.setText(row.caption);
            IconUtils.setMaxSizeForImageView(requireActivity(), rowHolder.icon);
            IconLoader.load(
                    rowHolder.icon,
                    R.drawable.none,
                    () ->
                            absoluteIndex % 2 > 0
                                    ? IconUtils.getIconForPieLongPress(requireActivity())
                                    : IconUtils.getIconForPieShortPress(requireActivity()));
            IconUtils.setMaxSizeForImageView(requireActivity(), rowHolder.icon2);
            IconLoader.load(
                    rowHolder.icon2,
                    R.drawable.none,
                    () ->
                            IconUtils.getIconForAction(
                                    requireActivity(),
                                    SettingsValues.getInstance(requireActivity())
                                            .getPieAction(absoluteIndex),
                                    IconUtils.getNamePie(absoluteIndex)));
            rowHolder.itemView.setOnClickListener(v -> handleRowClick(absoluteIndex));
            rowHolder.icon2.setOnClickListener(v -> handleIconClick(absoluteIndex));
        }
    }

    private static final class HeaderViewHolder extends RecyclerView.ViewHolder {
        final TextView text;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            this.text = (TextView) itemView;
        }
    }

    private static final class RowViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView caption;
        final ImageView icon;
        final ImageView icon2;

        RowViewHolder(@NonNull View itemView) {
            super(itemView);
            this.title = itemView.findViewById(R.id.listitem_icondescriptionicon_text);
            this.caption = itemView.findViewById(R.id.listitem_icondescriptionicon_caption);
            this.icon = itemView.findViewById(R.id.listitem_icondescriptionicon_icon);
            this.icon2 = itemView.findViewById(R.id.listitem_icondescriptionicon_icon2);
        }
    }
}
