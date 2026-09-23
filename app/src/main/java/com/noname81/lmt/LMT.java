package com.noname81.lmt;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Debug;
import android.util.Log;

import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;

/**
 * The settings activity: a ViewPager of tabs whose set depends on the feature mode.
 *
 * <p>{@code TSMode} chooses which tabs exist — gestures and ISAs only appear when that feature set
 * is enabled, and Info is always last.
 */
public class LMT extends Activity {
    private static final String TAG = "LMT::LMT";
    TabsAdapter mTabsAdapter;
    ViewPager mViewPager;

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Log.d(TAG, "Starting GUI");
        ViewPager viewPager = new ViewPager(this);
        this.mViewPager = viewPager;
        viewPager.setId(R.id.pager);
        setContentView(this.mViewPager);
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(2);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayShowTitleEnabled(false);
        createTabs();
        if (bundle != null) {
            actionBar.setSelectedNavigationItem(bundle.getInt("tab", 0));
        }
        if (Debug.isDebuggerConnected()) {
            DebugHelper.getInstance().showDebugMenu(this);
        }
        checkAndRequestPermissions();
    }

    private void createTabs() {
        ActionBar actionBar = getActionBar();
        this.mTabsAdapter = new TabsAdapter(this, this.mViewPager);
        int iLoadTouchServiceMode =
                SettingsValues.getInstance(getApplicationContext()).loadTouchServiceMode();
        this.mTabsAdapter.addTab(
                actionBar.newTab().setText(R.string.navigation_settings),
                SettingsViewFragment.class);
        if (iLoadTouchServiceMode < 2) {
            this.mTabsAdapter.addTab(
                    actionBar.newTab().setText(R.string.navigation_gestures),
                    GesturesFragment.class);
            this.mTabsAdapter.addTab(
                    actionBar.newTab().setText(R.string.navigation_isas), IsasFragment.class);
        }
        if (iLoadTouchServiceMode > 0) {
            this.mTabsAdapter.addTab(
                    actionBar.newTab().setText(R.string.navigation_pie), PieFragment.class);
        }
        this.mTabsAdapter.addTab(
                getActionBar().newTab().setText(R.string.navigation_info), InfoFragment.class);
    }

    @Override // android.app.Activity
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putInt("tab", getActionBar().getSelectedNavigationIndex());
    }

    private void checkAndRequestPermissions() {
        PermissionChecker permissionChecker = PermissionChecker.getInstance();
        permissionChecker.checkAndRequestExternalStorageReadPermission(this, true);
        permissionChecker.checkAndRequestDrawOverAppsPermission(this, true);
        permissionChecker.hasExternalStorageWritePermission(this, true);
        permissionChecker.hasPhoneCallPermission(this, true);
        permissionChecker.hasUsageStatsPermission(this, true);
    }

    @Override // android.app.Activity
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PermissionChecker.DRAW_OVER_APPS_REQUEST_CODE) {
            PermissionChecker.getInstance().hasDrawOverAppsPermission(this, true);
        }
        if (requestCode == PermissionChecker.USAGE_STATS_REQUEST_CODE) {
            PermissionChecker.getInstance().hasUsageStatsPermission(this, true);
        }
    }

    public static class TabsAdapter extends FrameworkFragmentStatePagerAdapter
            implements ActionBar.TabListener, ViewPager.OnPageChangeListener {
        private final ActionBar mActionBar;
        private final Context mContext;
        private final ArrayList<TabInfo> mTabs;
        private final ViewPager mViewPager;

        @Override // androidx.viewpager.widget.ViewPager.OnPageChangeListener
        public void onPageScrollStateChanged(int state) {}

        @Override // androidx.viewpager.widget.ViewPager.OnPageChangeListener
        public void onPageScrolled(int position, float offset, int offsetPixels) {}

        @Override // android.app.ActionBar.TabListener
        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {}

        @Override // android.app.ActionBar.TabListener
        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {}

        static final class TabInfo {
            private final Bundle args;
            private final Class<?> clss;

            TabInfo(Class<?> cls, Bundle bundle) {
                this.clss = cls;
                this.args = bundle;
            }
        }

        TabsAdapter(Activity activity, ViewPager viewPager) {
            super(activity.getFragmentManager());
            this.mTabs = new ArrayList<>();
            this.mContext = activity;
            this.mActionBar = activity.getActionBar();
            this.mViewPager = viewPager;
            viewPager.setAdapter(this);
            viewPager.setOnPageChangeListener(this);
        }

        void addTab(ActionBar.Tab tab, Class<?> cls) {
            TabInfo tabInfo = new TabInfo(cls, null);
            tab.setTag(tabInfo);
            tab.setTabListener(this);
            this.mTabs.add(tabInfo);
            this.mActionBar.addTab(tab);
            notifyDataSetChanged();
        }

        @Override // androidx.viewpager.widget.PagerAdapter
        public int getCount() {
            return this.mTabs.size();
        }

        @Override // FrameworkFragmentStatePagerAdapter
        public Fragment getItem(int position) {
            TabInfo tabInfo = this.mTabs.get(position);
            return Fragment.instantiate(this.mContext, tabInfo.clss.getName(), tabInfo.args);
        }

        @Override // androidx.viewpager.widget.ViewPager.OnPageChangeListener
        public void onPageSelected(int position) {
            this.mActionBar.setSelectedNavigationItem(position);
        }

        @Override // android.app.ActionBar.TabListener
        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
            Object tag = tab.getTag();
            for (int index = 0; index < this.mTabs.size(); index++) {
                if (this.mTabs.get(index) == tag) {
                    this.mViewPager.setCurrentItem(index);
                }
            }
        }
    }
}
