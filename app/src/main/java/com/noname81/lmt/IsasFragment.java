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
 * Settings tab for the Individual Swipe Areas.
 *
 * <p>Twelve zones, three along each screen edge, each bound to its own action. See {@code
 * SettingsValues.getIsaAction} for how a swipe is mapped to a zone.
 */
public class IsasFragment extends Fragment {
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
        SettingsValues.getInstance(requireActivity()).setCurrentIsa(absoluteIndex);
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
                    getString(R.string.isas_isa_areas_bottom),
                    labeled(
                            "Isa 1", getString(R.string.isas_isa_bottom_left),
                            "Isa 2", getString(R.string.isas_isa_bottom_center),
                            "Isa 3", getString(R.string.isas_isa_bottom_right)));
            this.mRows.addSection(
                    getString(R.string.isas_isa_areas_top),
                    labeled(
                            "Isa 4", getString(R.string.isas_isa_top_left),
                            "Isa 5", getString(R.string.isas_isa_top_center),
                            "Isa 6", getString(R.string.isas_isa_top_right)));
            this.mRows.addSection(
                    getString(R.string.isas_isa_areas_left),
                    labeled(
                            "Isa 7", getString(R.string.isas_isa_left_top),
                            "Isa 8", getString(R.string.isas_isa_left_center),
                            "Isa 9", getString(R.string.isas_isa_left_bottom)));
            this.mRows.addSection(
                    getString(R.string.isas_isa_areas_right),
                    labeled(
                            "Isa 10", getString(R.string.isas_isa_right_top),
                            "Isa 11", getString(R.string.isas_isa_right_center),
                            "Isa 12", getString(R.string.isas_isa_right_bottom)));
        }

        private List<SectionedRows.Labeled> labeled(
                String t1, String c1, String t2, String c2, String t3, String c3) {
            List<SectionedRows.Labeled> rows = new ArrayList<>();
            rows.add(new SectionedRows.Labeled(t1, c1));
            rows.add(new SectionedRows.Labeled(t2, c2));
            rows.add(new SectionedRows.Labeled(t3, c3));
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
                    rowHolder.icon, R.drawable.none, () -> IconUtils.getIconForISA(requireActivity()));
            IconUtils.setMaxSizeForImageView(requireActivity(), rowHolder.icon2);
            IconLoader.load(
                    rowHolder.icon2,
                    R.drawable.none,
                    () ->
                            IconUtils.getIconForAction(
                                    requireActivity(),
                                    SettingsValues.getInstance(requireActivity())
                                            .getIsaAction(absoluteIndex),
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
