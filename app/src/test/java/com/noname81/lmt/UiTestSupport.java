package com.noname81.lmt;

import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;

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

    static Activity hostActivity() {
        return Robolectric.buildActivity(Activity.class).setup().get();
    }

    static ListView show(Activity host, Fragment fragment) {
        host.getFragmentManager().beginTransaction().add(android.R.id.content, fragment).commit();
        host.getFragmentManager().executePendingTransactions();
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
}
