package com.noname81.lmt;

import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;

import org.robolectric.Robolectric;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Drives the list-based screens the way a user does - by visible row label - so the UI tests
 * state their expectations in terms that survive a rewrite. Only this driver is tied to today's
 * ListView + SeparatedListAdapter structure; the expectation tables in the tests are not.
 */
final class UiTestSupport {

    /** One visible row: its label, the section header above it, and its adapter position. */
    static final class Row {
        final String section;
        final String title;
        final String caption;
        final int position;

        Row(String section, String title, String caption, int position) {
            this.section = section;
            this.title = title;
            this.caption = caption;
            this.position = position;
        }

        @Override
        public String toString() {
            return section + " / " + title;
        }
    }

    private UiTestSupport() {}

    /** Drops every singleton that caches settings, so each test starts from a clean store. */
    static void resetSingletons(Context context) throws Exception {
        for (Class<?> c :
                new Class<?>[] {
                    SettingsValues.class, Launcher.class, RootContext.class, Toaster.class
                }) {
            Field f = c.getDeclaredField("instance");
            f.setAccessible(true);
            f.set(null, null);
        }
        context.getSharedPreferences("LMT", 0).edit().clear().commit();
    }

    static FragmentActivity hostActivity() {
        return Robolectric.buildActivity(FragmentActivity.class).setup().get();
    }

    static ListView show(FragmentActivity host, Fragment fragment) {
        host.getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, fragment)
                .commitNow();
        idle();
        return (ListView) fragment.getView();
    }

    static void idle() {
        shadowOf(Looper.getMainLooper()).idle();
    }

    /** Every non-header row, with the header it sits under. */
    @SuppressWarnings("unchecked")
    static List<Row> rows(ListView list) {
        ListAdapter adapter = list.getAdapter();
        List<Row> rows = new ArrayList<>();
        String section = "";
        for (int i = 0; i < adapter.getCount(); i++) {
            Object item = adapter.getItem(i);
            if (item instanceof String) {
                section = (String) item;
            } else {
                Map<String, ?> map = (Map<String, ?>) item;
                rows.add(
                        new Row(
                                section,
                                (String) map.get("title"),
                                (String) map.get("caption"),
                                i));
            }
        }
        return rows;
    }

    static View rowView(ListView list, Row row) {
        View view = list.getAdapter().getView(row.position, null, list);
        // A detached row view would make view.getContext() fine but layout params null.
        view.setLayoutParams(
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return view;
    }

    static void click(ListView list, Row row) {
        list.performItemClick(rowView(list, row), row.position, row.position);
        idle();
    }

    static int privateInt(Object target, Class<?> declaring, String field) throws Exception {
        Field f = declaring.getDeclaredField(field);
        f.setAccessible(true);
        return f.getInt(target);
    }

    /** One visible Preference row: its label, the category title above it, and the row itself. */
    static final class PreferenceRow {
        final String section;
        final String title;
        final Preference preference;

        PreferenceRow(String section, String title, Preference preference) {
            this.section = section;
            this.title = title;
            this.preference = preference;
        }

        @Override
        public String toString() {
            return section + " / " + title;
        }
    }

    static void showPreferences(FragmentActivity host, Fragment fragment) {
        host.getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, fragment)
                .commitNow();
        idle();
    }

    /**
     * Every preference under every top-level {@link PreferenceGroup} (i.e. every row under every
     * section), in screen order. {@code Preference.performClick()} drives a row exactly like a
     * real tap would - including opening whatever dialog its listener shows - so there is no need
     * to walk the rendered RecyclerView at all.
     */
    static List<PreferenceRow> preferenceRows(PreferenceFragmentCompat fragment) {
        List<PreferenceRow> rows = new ArrayList<>();
        PreferenceGroup screen = fragment.getPreferenceScreen();
        for (int i = 0; i < screen.getPreferenceCount(); i++) {
            Preference category = screen.getPreference(i);
            String section = String.valueOf(category.getTitle());
            if (category instanceof PreferenceGroup) {
                PreferenceGroup group = (PreferenceGroup) category;
                for (int j = 0; j < group.getPreferenceCount(); j++) {
                    Preference preference = group.getPreference(j);
                    rows.add(new PreferenceRow(section, String.valueOf(preference.getTitle()), preference));
                }
            }
        }
        return rows;
    }

    /** One visible RecyclerView row: its label, the section header above it, and its flat position. */
    static final class SectionedRow {
        final String section;
        final String title;
        final String caption;
        final int position;

        SectionedRow(String section, String title, String caption, int position) {
            this.section = section;
            this.title = title;
            this.caption = caption;
            this.position = position;
        }

        @Override
        public String toString() {
            return section + " / " + title;
        }
    }

    static void showRecyclerView(FragmentActivity host, Fragment fragment) {
        host.getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, fragment)
                .commitNow();
        idle();
    }

    /**
     * Every row (not header) in a {@link SectionedRows}, in flat position order - read directly
     * from the model, not the rendered RecyclerView, the same way {@link #preferenceRows} avoids
     * walking rendered Preference views.
     */
    static List<SectionedRow> sectionedRows(SectionedRows<SectionedRows.Labeled> rows) {
        List<SectionedRow> result = new ArrayList<>();
        for (int position = 0; position < rows.getItemCount(); position++) {
            if (!rows.isHeader(position)) {
                String section = rows.headerAt(position);
                SectionedRows.Labeled row = rows.rowAt(position);
                result.add(new SectionedRow(section, row.title, row.caption, position));
            }
        }
        return result;
    }
}
