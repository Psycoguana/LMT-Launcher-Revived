package com.noname81.lmt;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleAdapter;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Picks an installed app, an activity, or a launcher shortcut to bind to a slot.
 *
 * <p>Filterable by a search box, and by default system packages are hidden — a stock device lists a
 * couple of hundred otherwise. The toggle is remembered.
 *
 * <p><b>Filtering rebuilds the model, not just the view.</b> {@code onItemClick} resolves a tap as
 * {@code mPackageInfos.get(position - 1)} — the displayed row index, less the section header, used
 * to index the backing list. Filtering only the adapter would leave those two out of step and bind
 * the wrong app, silently. So every filter change rebuilds the backing list and the adapter
 * together.
 */
public class MultiSelectActivity extends Activity implements AdapterView.OnItemClickListener {
    public static int AppSelectMode = 0;
    public static final int REQUEST_CREATE_SHORTCUT = 1;
    public static final int SelectActivity = 2;
    public static final int SelectBlacklisted = 3;
    public static final int SelectBlacklistedPie = 4;
    public static final int SelectPackage = 0;
    public static final int SelectPackageActivity = 1;
    public static final int SelectShortcut = 5;
    public static final Comparator<PackageInfo> comparator = new Comparator() { // from class:
                // com.noname81.lmt.MultiSelectActivity$$ExternalSyntheticLambda1
                @Override // java.util.Comparator
                public final int compare(Object obj, Object obj2) {
                    return ((PackageInfo) obj)
                            .applicationInfo.name.compareToIgnoreCase(
                                    ((PackageInfo) obj2).applicationInfo.name);
                }
            };
    List<ActivityInfo> mActivityInfos;
    PackageInfo mPackageInfo;
    List<PackageInfo> mPackageInfos;
    PackageManager mPackageManager;
    SettingsValues mSettings;
    List<ResolveInfo> mShortcuts;

    private ListView mListView;
    private String mQuery = "";
    private boolean mShowSystemApps;

    /** Every candidate before the search box and the system-app toggle are applied. */
    private List<PackageInfo> mAllPackageInfos;

    private List<ActivityInfo> mAllActivityInfos;
    private List<ResolveInfo> mAllShortcuts;

    @Override // android.app.Activity
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mSettings = SettingsValues.getInstance(getApplicationContext());
        this.mPackageManager = getPackageManager();
        this.mShowSystemApps = this.mSettings.loadShowSystemApps() > 0;

        gatherCandidates();

        this.mListView = new ListView(this);
        this.mListView.setOnItemClickListener(this);
        this.mListView.setDividerHeight(0);
        setContentView(this.mListView);
        render();
    }

    /** Collects every candidate for the current mode, before any filtering. */
    private void gatherCandidates() {
        int mode = AppSelectMode;
        if (mode == SelectActivity) {
            this.mAllActivityInfos = new LinkedList<>();
            try {
                this.mPackageInfo =
                        this.mPackageManager.getPackageInfo(
                                this.mSettings.getCurrentAction().getString(),
                                PackageManager.GET_ACTIVITIES);
                if (this.mPackageInfo.activities != null) {
                    for (ActivityInfo activityInfo : this.mPackageInfo.activities) {
                        if (activityInfo.exported) {
                            this.mAllActivityInfos.add(activityInfo);
                        }
                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        } else if (mode == SelectShortcut) {
            this.mAllShortcuts = new LinkedList<>();
            try {
                this.mAllShortcuts =
                        this.mPackageManager.queryIntentActivities(
                                new Intent("android.intent.action.CREATE_SHORTCUT"), 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            this.mAllPackageInfos = new LinkedList<>();
            for (PackageInfo packageInfo :
                    this.mPackageManager.getInstalledPackages(PackageManager.GET_ACTIVITIES)) {
                // Without activities there is nothing to launch, so nothing to bind.
                if (packageInfo.activities == null) {
                    continue;
                }
                packageInfo.applicationInfo.name =
                        packageInfo.applicationInfo.loadLabel(this.mPackageManager).toString();
                this.mAllPackageInfos.add(packageInfo);
            }
            Collections.sort(this.mAllPackageInfos, comparator);
        }
    }

    static boolean isSystem(ApplicationInfo applicationInfo) {
        // An updated system app behaves like a user app, so it stays visible.
        return (applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                && (applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0;
    }

    /** Visible for testing. */
    static boolean matches(String query, String label, String secondary) {
        if (query == null || query.isEmpty()) {
            return true;
        }
        String needle = query.toLowerCase();
        return (label != null && label.toLowerCase().contains(needle))
                || (secondary != null && secondary.toLowerCase().contains(needle));
    }

    /**
     * Rebuilds the backing list and the adapter together, applying the search query and the
     * system-app toggle. Both must stay in step — see the class javadoc.
     */
    private void render() {
        LinkedList<Map<String, ?>> rows = new LinkedList<>();
        SeparatedListAdapter adapter = new SeparatedListAdapter(this);
        int mode = AppSelectMode;

        if (mode == SelectActivity) {
            this.mActivityInfos = new LinkedList<>();
            String appLabel =
                    this.mPackageInfo == null
                            ? ""
                            : this.mPackageInfo
                                    .applicationInfo
                                    .loadLabel(this.mPackageManager)
                                    .toString();
            for (ActivityInfo activityInfo : this.mAllActivityInfos) {
                if (!matches(this.mQuery, appLabel, activityInfo.name)) {
                    continue;
                }
                this.mActivityInfos.add(activityInfo);
                rows.add(SeparatedListAdapter.createItem(appLabel, activityInfo.name));
            }
            adapter.addSection(
                    getString(R.string.app_choose_an_activity),
                    new AppSimpleAdapter(this, rows, this.mActivityInfos));
        } else if (mode == SelectShortcut) {
            this.mShortcuts = new LinkedList<>();
            for (ResolveInfo resolveInfo : this.mAllShortcuts) {
                String label = resolveInfo.loadLabel(this.mPackageManager).toString();
                String packageName = resolveInfo.activityInfo.packageName;
                if (!matches(this.mQuery, label, packageName)) {
                    continue;
                }
                this.mShortcuts.add(resolveInfo);
                rows.add(SeparatedListAdapter.createItem(label, packageName));
            }
            adapter.addSection(
                    getString(R.string.app_choose_a_shortcut),
                    new AppSimpleAdapter(this, rows, this.mShortcuts));
        } else {
            this.mPackageInfos = new LinkedList<>();
            for (PackageInfo packageInfo : this.mAllPackageInfos) {
                if (!this.mShowSystemApps && isSystem(packageInfo.applicationInfo)) {
                    continue;
                }
                if (!matches(
                        this.mQuery,
                        packageInfo.applicationInfo.name,
                        packageInfo.applicationInfo.packageName)) {
                    continue;
                }
                this.mPackageInfos.add(packageInfo);
                rows.add(
                        SeparatedListAdapter.createItem(
                                packageInfo.applicationInfo.name,
                                packageInfo.applicationInfo.packageName));
            }
            adapter.addSection(
                    getString(R.string.app_choose_an_app),
                    new AppSimpleAdapter(this, rows, this.mPackageInfos));
        }
        this.mListView.setAdapter((ListAdapter) adapter);
    }

    @Override // android.app.Activity
    public boolean onCreateOptionsMenu(Menu menu) {
        SearchView searchView = new SearchView(this);
        searchView.setQueryHint(getString(R.string.app_search_hint));
        searchView.setIconifiedByDefault(false);
        searchView.setOnQueryTextListener(
                new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(String query) {
                        MultiSelectActivity.this.mQuery = query == null ? "" : query.trim();
                        MultiSelectActivity.this.render();
                        return true;
                    }
                });
        menu.add(R.string.app_search_hint)
                .setActionView(searchView)
                .setShowAsActionFlags(
                        MenuItem.SHOW_AS_ACTION_ALWAYS
                                | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);

        // Only the app list has system packages to hide.
        if (AppSelectMode != SelectActivity && AppSelectMode != SelectShortcut) {
            menu.add(R.string.app_show_system_apps)
                    .setCheckable(true)
                    .setChecked(this.mShowSystemApps)
                    .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER);
        }
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        return true;
    }

    @Override // android.app.Activity
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        if (item.isCheckable()) {
            this.mShowSystemApps = !this.mShowSystemApps;
            item.setChecked(this.mShowSystemApps);
            this.mSettings.saveShowSystemApps(this.mShowSystemApps ? 1 : 0);
            render();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override // android.app.Activity
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Drawable drawable;
        if (requestCode == 1 && resultCode == -1) {
            Bundle extras = data.getExtras();
            extras.getClass();
            String uri = ((Intent) extras.get("android.intent.extra.shortcut.INTENT")).toUri(0);
            Resources resources = getResources();
            Bitmap bitmap = (Bitmap) data.getParcelableExtra("android.intent.extra.shortcut.ICON");
            if (bitmap != null) {
                drawable = new BitmapDrawable(resources, bitmap);
            } else {
                drawable = null;
                try {
                    Intent.ShortcutIconResource shortcutIconResource =
                            (Intent.ShortcutIconResource)
                                    data.getParcelableExtra(
                                            "android.intent.extra.shortcut.ICON_RESOURCE");
                    Resources resourcesForApplication =
                            getPackageManager()
                                    .getResourcesForApplication(shortcutIconResource.packageName);
                    drawable =
                            resourcesForApplication.getDrawable(
                                    resourcesForApplication.getIdentifier(
                                            shortcutIconResource.resourceName, null, null));
                } catch (Exception unused) {
                }
            }
            this.mSettings.setCurrentAction(this, new Action(33, uri, drawable));
        }
        super.onActivityResult(requestCode, resultCode, data);
        finish();
    }

    @Override // android.widget.AdapterView.OnItemClickListener
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        int mode = AppSelectMode;
        if (mode == 0) {
            this.mSettings.setCurrentAction(
                    this, new Action(2, this.mPackageInfos.get(position - 1).packageName));
            onBackPressed();
            return;
        }
        if (mode == 1) {
            this.mSettings.setCurrentAction(
                    this,
                    new Action(
                            2, this.mPackageInfos.get(position - 1).applicationInfo.packageName));
            AppSelectMode = 2;
            startActivityForResult(
                    new Intent(view.getContext(), (Class<?>) MultiSelectActivity.class), 0);
            finish();
            return;
        }
        if (mode == 3) {
            this.mSettings.setBlacklisted(this.mPackageInfos.get(position - 1).packageName);
            onBackPressed();
            return;
        }
        if (mode == 4) {
            this.mSettings.setBlacklistedPie(this.mPackageInfos.get(position - 1).packageName);
            onBackPressed();
            return;
        }
        if (mode == 5) {
            ActivityInfo activityInfo = this.mShortcuts.get(position - 1).activityInfo;
            ComponentName componentName =
                    new ComponentName(activityInfo.applicationInfo.packageName, activityInfo.name);
            Intent intent = new Intent();
            intent.setComponent(componentName);
            intent.setAction("android.intent.action.CREATE_SHORTCUT");
            startActivityForResult(intent, 1);
            return;
        }
        SettingsValues settingsValues = this.mSettings;
        StringBuilder sb = new StringBuilder();
        int selected = position - 1;
        sb.append(this.mActivityInfos.get(selected).packageName);
        sb.append("/");
        sb.append(this.mActivityInfos.get(selected).name);
        settingsValues.setCurrentAction(this, new Action(27, sb.toString()));
        onBackPressed();
    }

    /**
     * Rows plus the model they were built from.
     *
     * <p>The icon loader used to reach back into the activity's list by position. That was safe
     * only while the list never changed; now that filtering rebuilds it, an in-flight load from the
     * previous render would index past the end of a shorter list. So the adapter keeps its own
     * snapshot and never looks outside it.
     */
    /**
     * Rows, plus the model they were built from.
     *
     * <p>The icon loader used to reach back into the activity's list by position. That was safe
     * only while the list never changed; now that filtering rebuilds it, an in-flight load from the
     * previous render would index past the end of a shorter list and crash. So the adapter carries
     * its own snapshot and never looks outside it.
     */
    class AppSimpleAdapter extends SimpleAdapter {

        /** Parallel to the rows: PackageInfo, ResolveInfo, or empty for activities. */
        private final List<?> mModel;

        AppSimpleAdapter(Context context, List<? extends Map<String, ?>> rows, List<?> model) {
            super(
                    context,
                    rows,
                    R.layout.listitem_icondescription,
                    new String[] {"title", "caption"},
                    new int[] {
                        R.id.listitem_icondescription_text, R.id.listitem_icondescription_caption
                    });
            this.mModel = model;
        }

        @Override // android.widget.SimpleAdapter, android.widget.Adapter
        public View getView(final int position, View convertView, ViewGroup parent) {
            View row = super.getView(position, convertView, parent);
            ImageView imageView = (ImageView) row.findViewById(R.id.listitem_icondescription_icon);
            IconUtils.setMaxSizeForImageView(
                    MultiSelectActivity.this.getApplicationContext(), imageView);
            new AsyncDrawableTask(imageView, R.drawable.none) {
                @Override
                protected Drawable doInBackground(Void... unused) {
                    return iconAt(position);
                }
            }.execute(new Void[0]);
            return row;
        }

        private Drawable iconAt(int position) {
            PackageManager packageManager = MultiSelectActivity.this.mPackageManager;
            if (AppSelectMode == SelectActivity) {
                PackageInfo packageInfo = MultiSelectActivity.this.mPackageInfo;
                return packageInfo == null
                        ? null
                        : packageInfo.applicationInfo.loadIcon(packageManager);
            }
            // A render that happened after this task was queued can leave it holding
            // a position past the end of the new, shorter list.
            if (this.mModel == null || position < 0 || position >= this.mModel.size()) {
                return null;
            }
            Object item = this.mModel.get(position);
            if (item instanceof ResolveInfo) {
                return ((ResolveInfo) item).loadIcon(packageManager);
            }
            if (item instanceof PackageInfo) {
                return ((PackageInfo) item).applicationInfo.loadIcon(packageManager);
            }
            return null;
        }
    }
}
