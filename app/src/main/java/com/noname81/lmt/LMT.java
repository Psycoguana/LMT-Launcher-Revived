package com.noname81.lmt;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Debug;
import android.util.Log;
import android.widget.LinearLayout;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;

/**
 * The settings activity: a ViewPager2 of tabs whose set depends on the feature mode.
 *
 * <p>{@code TSMode} chooses which tabs exist — gestures and ISAs only appear when that feature set
 * is enabled, and Info is always last.
 */
public class LMT extends ThemedActivity {
    private static final String TAG = "LMT::LMT";
    ViewPager2 mViewPager;
    TabLayout mTabLayout;
    private String mThemeStamp;

    @Override // androidx.fragment.app.FragmentActivity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mThemeStamp = ThemeHelper.stamp(this);
        Log.d(TAG, "Starting GUI");

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        AppBarLayout appBarLayout = new AppBarLayout(this);
        MaterialToolbar toolbar = new MaterialToolbar(this);
        appBarLayout.addView(
                toolbar,
                new AppBarLayout.LayoutParams(
                        AppBarLayout.LayoutParams.MATCH_PARENT,
                        AppBarLayout.LayoutParams.WRAP_CONTENT));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle("LMT Launcher");

        this.mTabLayout = new TabLayout(this);
        appBarLayout.addView(
                this.mTabLayout,
                new AppBarLayout.LayoutParams(
                        AppBarLayout.LayoutParams.MATCH_PARENT,
                        AppBarLayout.LayoutParams.WRAP_CONTENT));
        root.addView(
                appBarLayout,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

        ViewPager2 viewPager = new ViewPager2(this);
        this.mViewPager = viewPager;
        viewPager.setId(R.id.pager);
        root.addView(viewPager, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
        createTabs();
        if (bundle != null) {
            this.mViewPager.setCurrentItem(bundle.getInt("tab", 0), false);
        }
        if (Debug.isDebuggerConnected()) {
            DebugHelper.getInstance().showDebugMenu(this);
        }
        checkAndRequestPermissions();
    }

    private void createTabs() {
        TabsAdapter adapter = new TabsAdapter(this);
        int iLoadTouchServiceMode =
                SettingsValues.getInstance(getApplicationContext()).loadTouchServiceMode();
        adapter.addTab(getString(R.string.navigation_settings), SettingsViewFragment.class);
        if (iLoadTouchServiceMode < 2) {
            adapter.addTab(getString(R.string.navigation_gestures), GesturesFragment.class);
            adapter.addTab(getString(R.string.navigation_isas), IsasFragment.class);
        }
        if (iLoadTouchServiceMode > 0) {
            adapter.addTab(getString(R.string.navigation_pie), PieFragment.class);
        }
        adapter.addTab(getString(R.string.navigation_info), InfoFragment.class);
        this.mViewPager.setAdapter(adapter);
        new TabLayoutMediator(
                        this.mTabLayout,
                        this.mViewPager,
                        (tab, position) -> tab.setText(adapter.getTitle(position)))
                .attach();
    }

    @Override // androidx.fragment.app.FragmentActivity
    protected void onResume() {
        super.onResume();
        // A colour picked on the Appearance page (or restored from a backup) doesn't recreate
        // this activity by itself, unlike a night mode change.
        if (!ThemeHelper.stamp(this).equals(this.mThemeStamp)) {
            recreate();
        }
    }

    @Override // androidx.fragment.app.FragmentActivity
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putInt("tab", this.mViewPager.getCurrentItem());
    }

    private void checkAndRequestPermissions() {
        PermissionChecker permissionChecker = PermissionChecker.getInstance();
        permissionChecker.checkAndRequestExternalStorageReadPermission(this, true);
        permissionChecker.checkAndRequestDrawOverAppsPermission(this, true);
        permissionChecker.hasExternalStorageWritePermission(this, true);
        permissionChecker.hasPhoneCallPermission(this, true);
        permissionChecker.hasUsageStatsPermission(this, true);
    }

    @Override // androidx.fragment.app.FragmentActivity
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PermissionChecker.DRAW_OVER_APPS_REQUEST_CODE) {
            PermissionChecker.getInstance().hasDrawOverAppsPermission(this, true);
        }
        if (requestCode == PermissionChecker.USAGE_STATS_REQUEST_CODE) {
            PermissionChecker.getInstance().hasUsageStatsPermission(this, true);
        }
    }

    private static class TabsAdapter extends FragmentStateAdapter {
        private final List<CharSequence> mTitles = new ArrayList<>();
        private final List<Class<? extends Fragment>> mFragmentClasses = new ArrayList<>();

        TabsAdapter(FragmentActivity activity) {
            super(activity);
        }

        void addTab(CharSequence title, Class<? extends Fragment> cls) {
            this.mTitles.add(title);
            this.mFragmentClasses.add(cls);
        }

        CharSequence getTitle(int position) {
            return this.mTitles.get(position);
        }

        @Override // androidx.viewpager2.adapter.FragmentStateAdapter
        public int getItemCount() {
            return this.mFragmentClasses.size();
        }

        @Override // androidx.viewpager2.adapter.FragmentStateAdapter
        public Fragment createFragment(int position) {
            try {
                return this.mFragmentClasses.get(position).getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
