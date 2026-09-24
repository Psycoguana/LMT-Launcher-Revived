package com.noname81.lmt;

import java.util.ArrayList;
import java.util.List;

/**
 * Flattens named sections of rows into one [header, row, row, ..., header, row, ...] sequence -
 * the same shape {@code SeparatedListAdapter} (the ListView-era sectioned adapter) exposed, with
 * one shared header view type and an absolute row index counting only rows, not headers, across
 * every section combined. That absolute index is exactly the pie-slot/gesture/ISA/action index
 * the pre-RecyclerView screens used to reconstruct by hand from a flat ListView position (a
 * fragile pattern - one section in SettingsViewFragment's own pre-rewrite dispatcher already
 * passed the wrong offset this way). ROADMAP 1.6 phase 3.
 */
final class SectionedRows<T> {
    static final int VIEW_TYPE_HEADER = 0;
    static final int VIEW_TYPE_ROW = 1;

    private final List<String> mHeaders = new ArrayList<>();
    private final List<List<T>> mSections = new ArrayList<>();

    /** A row that only ever needs a title and a caption to display. */
    static final class Labeled {
        final String title;
        final String caption;

        Labeled(String title, String caption) {
            this.title = title;
            this.caption = caption;
        }
    }

    void addSection(String header, List<T> rows) {
        this.mHeaders.add(header);
        this.mSections.add(rows);
    }

    int getItemCount() {
        int count = 0;
        for (List<T> rows : this.mSections) {
            count += rows.size() + 1;
        }
        return count;
    }

    boolean isHeader(int position) {
        return locate(position).rowIndex < 0;
    }

    int getItemViewType(int position) {
        return isHeader(position) ? VIEW_TYPE_HEADER : VIEW_TYPE_ROW;
    }

    String headerAt(int position) {
        return this.mHeaders.get(locate(position).section);
    }

    T rowAt(int position) {
        Location location = locate(position);
        return this.mSections.get(location.section).get(location.rowIndex);
    }

    /** The row's 0-based index counting only rows, not headers, across every section combined. */
    int absoluteRowIndex(int position) {
        Location location = locate(position);
        int index = location.rowIndex;
        for (int i = 0; i < location.section; i++) {
            index += this.mSections.get(i).size();
        }
        return index;
    }

    private static final class Location {
        final int section;
        final int rowIndex; // -1 for a header row

        Location(int section, int rowIndex) {
            this.section = section;
            this.rowIndex = rowIndex;
        }
    }

    private Location locate(int position) {
        int remaining = position;
        for (int section = 0; section < this.mSections.size(); section++) {
            int count = this.mSections.get(section).size() + 1;
            if (remaining == 0) {
                return new Location(section, -1);
            }
            if (remaining < count) {
                return new Location(section, remaining - 1);
            }
            remaining -= count;
        }
        throw new IndexOutOfBoundsException("position " + position);
    }
}
