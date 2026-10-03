package com.noname81.lmt;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.google.android.material.color.ColorRoles;
import com.google.android.material.color.MaterialColors;

/**
 * About tab (ROADMAP 1.18): a header card with the pie, the credits and links, a status card that
 * says whether the pie can work (and fixes what it can with a tap), and a tile per permission LMT
 * can use. Five double-taps on the version chip open the hidden debug menu.
 */
public class InfoFragment extends Fragment {
    static final String XDA_THREAD = "https://xdaforums.com/t/app-root-lmt-launcher-v3-2.1330150/";
    static final String NONAME81 = "https://xdaforums.com/m/noname81.1864347/";
    static final String PSYCOGUANA = "https://github.com/Psycoguana";
    static final String REPOSITORY = "https://github.com/Psycoguana/LMT-Launcher-Revived";
    static final String DONATE =
            "https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=9C5JKBHDM6QSE";

    private View mRoot;

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View root = layoutInflater.inflate(R.layout.info, viewGroup, false);
        this.mRoot = root;
        Context context = requireContext();

        TextView version = root.findViewById(R.id.info_version);
        String name = SettingsValues.getInstance(context).getVersion();
        version.setText("Version " + (name.startsWith("v") ? name.substring(1) : name));
        addGestureListener(version);

        TextView credits = root.findViewById(R.id.info_credits);
        String html =
                "Original app by <a href='"
                        + NONAME81
                        + "'><b>noname81</b></a><br>Revived by <a href='"
                        + PSYCOGUANA
                        + "'><b>Psycoguana</b></a>";
        credits.setText(Build.VERSION.SDK_INT >= 24 ? Html.fromHtml(html, 0) : Html.fromHtml(html));
        credits.setMovementMethod(LinkMovementMethod.getInstance());
        credits.setLinkTextColor(
                MaterialColors.getColor(root, com.google.android.material.R.attr.colorPrimary));

        root.findViewById(R.id.info_github).setOnClickListener(v -> open(REPOSITORY));
        root.findViewById(R.id.info_xda).setOnClickListener(v -> open(XDA_THREAD));
        root.findViewById(R.id.info_donate).setOnClickListener(v -> open(DONATE));
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    private void open(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (RuntimeException e) {
            // No browser.
        }
    }

    /** Fills in the status card and the tiles from what LMT currently has. */
    void refresh() {
        if (this.mRoot == null || getContext() == null) {
            return;
        }
        Context context = requireContext();
        SettingsValues settings = SettingsValues.getInstance(context);
        PermissionChecker permissions = PermissionChecker.getInstance();
        // Set active features: 0 gestures and ISAs, 1 gestures, ISAs and pie, 2 pie.
        int mode = settings.loadTouchServiceMode();
        boolean pie = mode > 0;
        boolean gestures = mode < 2;
        boolean running = settings.getServiceState() > 0;
        boolean overlay = permissions.hasDrawOverAppsPermission(context, false);
        boolean accessibility = AccessibilityHandler.isAccessibilityAvailable(context, false);
        RootContext root = RootContext.getInstance(context.getApplicationContext());
        String features =
                mode == 0 ? "Gestures and ISAs" : mode == 1 ? "Pie, gestures and ISAs" : "Pie";

        if (pie && !overlay) {
            showStatus(
                    false,
                    "The pie can't show",
                    "Tap to allow LMT to draw over other apps",
                    v ->
                            permissions.checkAndRequestDrawOverAppsPermission(
                                    requireActivity(), true));
        } else if (!running) {
            showStatus(
                    false,
                    "LMT is stopped",
                    "Tap to start it",
                    v -> {
                        settings.startService();
                        this.mRoot.postDelayed(this::refresh, 600);
                    });
        } else if (gestures && root.isRootDenied()) {
            showStatus(
                    false,
                    "Gestures and ISAs need root",
                    pie
                            ? "The pie works. Grant root, or pick Pie in Set active features"
                            : "Grant root, or pick Pie in Set active features",
                    null);
        } else if (!accessibility) {
            showStatus(
                    false,
                    "Accessibility is off",
                    "Back, Home, blur and more need it. Tap to turn it on",
                    v -> openSettings(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        } else {
            showStatus(true, "LMT is ready", features + " · Accessibility on", null);
        }

        LinearLayout tiles = this.mRoot.findViewById(R.id.info_tiles);
        tiles.removeAllViews();
        boolean rootKnownGranted = !root.isRootDenied() && root.isRootAvailable(false);
        Tile[] all = {
            new Tile(
                    "Accessibility",
                    accessibility,
                    () -> openSettings(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)),
                    new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)),
            new Tile(
                    "Draw over apps",
                    overlay,
                    () ->
                            permissions.checkAndRequestDrawOverAppsPermission(
                                    requireActivity(), true),
                    forThisApp(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)),
            new Tile(
                    "Usage access",
                    permissions.hasUsageStatsPermission(context, false),
                    () -> permissions.checkAndRequestUsageStatsPermission(requireActivity(), true),
                    new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)),
            new Tile("Root", rootKnownGranted, null, null),
            new Tile(
                    "Modify settings",
                    PieBrightness.canWrite(context),
                    () -> PermissionChecker.checkAndRequestWriteSettingsPermission(context),
                    forThisApp(Settings.ACTION_MANAGE_WRITE_SETTINGS)),
            new Tile(
                    "Phone calls",
                    permissions.hasPhoneCallPermission(context, false),
                    () -> permissions.checkAndRequestPhoneCallPermission(requireActivity(), true),
                    forThisApp(Settings.ACTION_APPLICATION_DETAILS_SETTINGS))
        };
        LinearLayout row = null;
        for (int i = 0; i < all.length; i++) {
            if (i % 2 == 0) {
                row = new LinearLayout(context);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rowParams =
                        new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);
                if (i > 0) {
                    rowParams.topMargin = dp(10);
                }
                tiles.addView(row, rowParams);
            }
            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i % 2 == 1) {
                params.setMarginStart(dp(10));
            }
            row.addView(tileView(all[i]), params);
        }
        // Root's answer may still be on its way.
        root.whenRootKnown(
                () -> {
                    if (isAdded() && rootKnownGranted != (!root.isRootDenied())) {
                        refresh();
                    }
                });
    }

    private void showStatus(boolean ok, String title, String text, View.OnClickListener onClick) {
        View card = this.mRoot.findViewById(R.id.info_status);
        int container;
        int onContainer;
        int badge;
        int iconTint;
        int subtitle;
        if (ok) {
            // A neutral card with only the badge in green: a green card or green text looked
            // off in the light style.
            container =
                    MaterialColors.getColor(
                            card, com.google.android.material.R.attr.colorSurfaceContainerHigh);
            onContainer =
                    MaterialColors.getColor(
                            card, com.google.android.material.R.attr.colorOnSurface);
            subtitle =
                    MaterialColors.getColor(
                            card, com.google.android.material.R.attr.colorOnSurfaceVariant);
            ColorRoles green = green(requireContext());
            badge = green.getAccent();
            iconTint = green.getOnAccent();
        } else {
            container =
                    MaterialColors.getColor(
                            card, com.google.android.material.R.attr.colorErrorContainer);
            onContainer =
                    MaterialColors.getColor(
                            card, com.google.android.material.R.attr.colorOnErrorContainer);
            badge = MaterialColors.getColor(card, com.google.android.material.R.attr.colorError);
            iconTint = container;
            subtitle = onContainer;
        }
        card.setBackgroundTintList(ColorStateList.valueOf(container));
        this.mRoot
                .findViewById(R.id.info_status_badge)
                .setBackgroundTintList(ColorStateList.valueOf(badge));
        ImageView icon = this.mRoot.findViewById(R.id.info_status_icon);
        icon.setImageResource(ok ? R.drawable.ic_check : R.drawable.ic_warning);
        icon.setImageTintList(ColorStateList.valueOf(iconTint));
        TextView titleView = this.mRoot.findViewById(R.id.info_status_title);
        titleView.setText(title);
        titleView.setTextColor(onContainer);
        TextView textView = this.mRoot.findViewById(R.id.info_status_text);
        textView.setText(text);
        textView.setTextColor(subtitle);
        card.setOnClickListener(onClick);
        card.setClickable(onClick != null);
        if (onClick != null) {
            ripple(card, 28);
        } else {
            card.setForeground(null);
        }
    }

    /** A green harmonized with the theme, for the "on" dot and the ready badge. */
    private static ColorRoles green(Context context) {
        return MaterialColors.getColorRoles(
                context, MaterialColors.harmonizeWithPrimary(context, 0xFF43A047));
    }

    private Intent forThisApp(String action) {
        return new Intent(action, Uri.parse("package:" + requireContext().getPackageName()));
    }

    /** A rounded ripple, so a tap shows the card's or tile's own shape like the buttons do. */
    private void ripple(View view, int radiusDp) {
        android.graphics.drawable.GradientDrawable mask =
                new android.graphics.drawable.GradientDrawable();
        mask.setColor(0xFFFFFFFF);
        mask.setCornerRadius(dp(radiusDp));
        view.setForeground(
                new android.graphics.drawable.RippleDrawable(
                        ColorStateList.valueOf(
                                MaterialColors.getColor(
                                        view,
                                        com.google.android.material.R.attr.colorControlHighlight)),
                        null,
                        mask));
    }

    private static final class Tile {
        final String name;
        final boolean on;
        final Runnable fix;

        /** Where an allowed permission is managed, or null (root). */
        final Intent manage;

        Tile(String name, boolean on, Runnable fix, Intent manage) {
            this.name = name;
            this.on = on;
            this.fix = fix;
            this.manage = manage;
        }
    }

    private View tileView(Tile tile) {
        Context context = requireContext();
        LinearLayout view = new LinearLayout(context);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setBackgroundResource(R.drawable.info_tile);
        view.setPadding(dp(14), dp(12), dp(14), dp(12));
        TextView status = new TextView(context);
        status.setTextSize(12);
        TextView name = new TextView(context);
        name.setTextSize(15);
        name.setText(tile.name);
        name.setTextColor(
                MaterialColors.getColor(view, com.google.android.material.R.attr.colorOnSurface));
        if (tile.on) {
            // Only the dot is green; the word stays in the theme's text color.
            android.text.SpannableString on = new android.text.SpannableString("● On");
            on.setSpan(
                    new android.text.style.ForegroundColorSpan(green(context).getAccent()),
                    0,
                    1,
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            status.setText(on);
            status.setTextColor(
                    MaterialColors.getColor(
                            view, com.google.android.material.R.attr.colorOnSurfaceVariant));
            if (tile.manage != null) {
                ripple(view, 20);
                view.setOnClickListener(v -> openSettings(tile.manage));
            }
        } else if (tile.fix != null) {
            status.setText("○ Off · tap to allow");
            status.setTextColor(
                    MaterialColors.getColor(view, com.google.android.material.R.attr.colorPrimary));
            ripple(view, 20);
            view.setOnClickListener(v -> tile.fix.run());
        } else {
            status.setText("○ Not granted");
            status.setTextColor(
                    MaterialColors.getColor(
                            view, com.google.android.material.R.attr.colorOnSurfaceVariant));
        }
        view.addView(status);
        view.addView(name);
        view.setContentDescription(tile.name + ", " + status.getText());
        return view;
    }

    private void openSettings(Intent intent) {
        try {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (RuntimeException e) {
            // No such settings screen on this phone.
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    /**
     * Five double-taps on the version chip open the hidden debug menu, which toggles the
     * per-component logging that {@code DebugHelper} drives.
     */
    private void addGestureListener(View view) {
        final GestureDetector gestureDetector =
                new GestureDetector(
                        getActivity(),
                        new GestureDetector.SimpleOnGestureListener() {
                            private int count = 0;

                            @Override
                            // android.view.GestureDetector.OnDoubleTapListener
                            public boolean onDoubleTap(MotionEvent event) {
                                int taps = this.count + 1;
                                this.count = taps;
                                if (taps < 5) { // five double-taps reveals it
                                    return false;
                                }
                                DebugHelper.getInstance()
                                        .showDebugMenu(InfoFragment.this.getActivity());
                                return false;
                            }

                            @Override
                            public boolean onDown(MotionEvent event) {
                                return true;
                            }
                        });
        view.setOnTouchListener(
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View touched, MotionEvent event) {
                        gestureDetector.onTouchEvent(event);
                        return true;
                    }
                });
    }
}
