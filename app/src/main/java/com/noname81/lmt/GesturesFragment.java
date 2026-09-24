package com.noname81.lmt;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings tab binding each recognised gesture to an action.
 *
 * <p>Only present when the feature set includes gestures ({@code TSMode < 2}), since recognition
 * needs root and the native library.
 */
public class GesturesFragment extends Fragment {
    Adapter mAdapter;

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
        SettingsValues.getInstance(requireActivity()).setCurrentGesture(absoluteIndex);
        startActivityForResult(new Intent(requireActivity(), CommandSelectActivity.class), 0);
    }

    /** Test-only entry point: reproduces a real tap on the row at this flat position. */
    void clickRowForTest(int flatPosition) {
        handleRowClick(this.mAdapter.mRows.absoluteRowIndex(flatPosition));
    }

    SectionedRows<SectionedRows.Labeled> rowsForTest() {
        return this.mAdapter.mRows;
    }

    final class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        final SectionedRows<SectionedRows.Labeled> mRows = new SectionedRows<>();

        Adapter() {
            this.mRows.addSection(
                    getString(R.string.gestures_single_touch_gestures), labeled(0, 3));
            this.mRows.addSection(getString(R.string.gestures_swipe_gestures), labeled(3, 11));
            this.mRows.addSection(getString(R.string.gestures_dual_touch_gestures), labeled(11, 14));
        }

        private List<SectionedRows.Labeled> labeled(int from, int to) {
            List<SectionedRows.Labeled> rows = new ArrayList<>();
            for (int i = from; i < to; i++) {
                rows.add(
                        new SectionedRows.Labeled(
                                TouchServiceResult.names[i], TouchServiceResult.captions[i]));
            }
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
                    absoluteIndex,
                    () ->
                            IconUtils.getIconForGesture(
                                    requireActivity(),
                                    TouchServiceResult.names[absoluteIndex].toLowerCase()));
            IconUtils.setMaxSizeForImageView(requireActivity(), rowHolder.icon2);
            IconLoader.load(
                    rowHolder.icon2,
                    R.drawable.none,
                    absoluteIndex,
                    () ->
                            IconUtils.getIconForAction(
                                    requireActivity(),
                                    SettingsValues.getInstance(requireActivity())
                                            .getGestureAction(absoluteIndex),
                                    null));
            rowHolder.itemView.setOnClickListener(v -> handleRowClick(absoluteIndex));
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
