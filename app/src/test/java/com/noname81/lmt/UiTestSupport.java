package com.noname81.lmt;

import static org.robolectric.Shadows.shadowOf;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;

import org.robolectric.Robolectric;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Drives the app's screens the way a user does - by visible row label - so the UI tests state their
 * expectations in terms that survive a rewrite. Only this driver is tied to any given screen's
 * widget structure (Preference tree, RecyclerView/SectionedRows model); the expectation tables in
 * the tests are not.
 */
final class UiTestSupport {

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

    /**
     * A bare host wearing the app's real theme, which Material components refuse to run without.
     * The theme is applied in onCreate, as a manifest theme would be, so it survives the recreate()
     * some settings rows trigger.
     */
    public static class ThemedHost extends FragmentActivity {
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            setTheme(R.style.AppTheme_Material3);
            super.onCreate(savedInstanceState);
        }
    }

    static FragmentActivity hostActivity() {
        return Robolectric.buildActivity(ThemedHost.class).setup().get();
    }

    static void idle() {
        shadowOf(Looper.getMainLooper()).idle();
    }

    /**
     * The first text field in a shown dialog, found in its real view tree - Material dialogs are
     * plain library code with no Robolectric shadow exposing the view passed to setView().
     */
    static EditText editTextIn(Dialog dialog) {
        return firstEditText(dialog.getWindow().getDecorView());
    }

    /** Every non-empty text a shown dialog displays (title, message, buttons), for messages. */
    static String textsIn(Dialog dialog) {
        StringBuilder texts = new StringBuilder();
        collectTexts(dialog.getWindow().getDecorView(), texts);
        return texts.toString().trim();
    }

    static com.google.android.material.slider.Slider sliderIn(Dialog dialog) {
        return firstOf(
                dialog.getWindow().getDecorView(), com.google.android.material.slider.Slider.class);
    }

    private static <T extends View> T firstOf(View view, Class<T> type) {
        if (type.isInstance(view)) {
            return type.cast(view);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                T found = firstOf(group.getChildAt(i), type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static EditText firstEditText(View view) {
        if (view instanceof EditText) {
            return (EditText) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                EditText found = firstEditText(group.getChildAt(i));
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void collectTexts(View view, StringBuilder texts) {
        if (view instanceof TextView && view.getVisibility() == View.VISIBLE) {
            CharSequence text = ((TextView) view).getText();
            if (text != null && text.length() > 0) {
                texts.append(text).append(" / ");
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                collectTexts(group.getChildAt(i), texts);
            }
        }
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
     * section), in screen order. {@code Preference.performClick()} drives a row exactly like a real
     * tap would - including opening whatever dialog its listener shows - so there is no need to
     * walk the rendered RecyclerView at all.
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
                    rows.add(
                            new PreferenceRow(
                                    section, String.valueOf(preference.getTitle()), preference));
                }
            }
        }
        return rows;
    }

    /**
     * One visible RecyclerView row: its label, the section header above it, and its flat position.
     */
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

    /**
     * Same as {@link #sectionedRows}, for a {@link SectionedRows} whose row payload is an {@link
     * Action} type constant (CommandSelectActivity) rather than a {@link SectionedRows.Labeled} -
     * title/caption come from {@code Action.names}/{@code Action.captions}.
     */
    static List<SectionedRow> sectionedActionRows(SectionedRows<Integer> rows) {
        List<SectionedRow> result = new ArrayList<>();
        for (int position = 0; position < rows.getItemCount(); position++) {
            if (!rows.isHeader(position)) {
                String section = rows.headerAt(position);
                int type = rows.rowAt(position);
                result.add(
                        new SectionedRow(
                                section, Action.names[type], Action.captions[type], position));
            }
        }
        return result;
    }
}
