package com.noname81.lmt;

import android.content.ComponentName;
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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * Picks an installed app, an activity, or a launcher shortcut to bind to a slot.
 *
 * <p>Filterable by a search box, and by default system packages are hidden — a stock device lists a
 * couple of hundred otherwise. The toggle is remembered.
 */
public class MultiSelectActivity extends ThemedActivity {
    public static int AppSelectMode = 0;
    public static final int REQUEST_CREATE_SHORTCUT = 1;
    public static final int SelectActivity = 2;
    public static final int SelectBlacklisted = 3;
    public static final int SelectBlacklistedPie = 4;
    public static final int SelectPackage = 0;
    public static final int SelectPackageActivity = 1;
    public static final int SelectShortcut = 5;
    public static final Comparator<PackageInfo> comparator =
            (a, b) -> a.applicationInfo.name.compareToIgnoreCase(b.applicationInfo.name);

    PackageInfo mPackageInfo;
    PackageManager mPackageManager;
    SettingsValues mSettings;

    Adapter mAdapter;
    private String mQuery = "";
    private boolean mShowSystemApps;

    /** Every candidate before the search box and the system-app toggle are applied. */
    private List<PackageInfo> mAllPackageInfos;

    private List<ActivityInfo> mAllActivityInfos;
    private List<ResolveInfo> mAllShortcuts;

    @Override // androidx.fragment.app.FragmentActivity
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mSettings = SettingsValues.getInstance(getApplicationContext());
        this.mPackageManager = getPackageManager();
        this.mShowSystemApps = this.mSettings.loadShowSystemApps() > 0;

        gatherCandidates();

        setContentView(R.layout.multi_select);
        setSupportActionBar(findViewById(R.id.toolbar));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        EditText search = findViewById(R.id.search);
        search.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        MultiSelectActivity.this.mQuery = s.toString().trim();
                        MultiSelectActivity.this.render();
                    }
                });
        search.setOnEditorActionListener(
                (view, actionId, event) -> {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        hideKeyboard(view);
                        return true;
                    }
                    return false;
                });

        // Only the app list has system packages to hide.
        Chip showSystemApps = findViewById(R.id.show_system_apps);
        if (AppSelectMode == SelectActivity || AppSelectMode == SelectShortcut) {
            findViewById(R.id.filters).setVisibility(View.GONE);
        } else {
            showSystemApps.setChecked(this.mShowSystemApps);
            showSystemApps.setOnCheckedChangeListener(
                    (chip, checked) -> {
                        this.mShowSystemApps = checked;
                        this.mSettings.saveShowSystemApps(checked ? 1 : 0);
                        render();
                    });
        }

        RecyclerView recyclerView = findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        this.mAdapter = new Adapter();
        recyclerView.setAdapter(this.mAdapter);
        // Dragging the list is a sign the user is done typing.
        recyclerView.addOnScrollListener(
                new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrollStateChanged(@NonNull RecyclerView list, int newState) {
                        if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                            hideKeyboard(search);
                        }
                    }
                });
        render();
    }

    private void hideKeyboard(View view) {
        view.clearFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
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

    /** Rebuilds the row list, applying the search query and the system-app toggle. */
    private void render() {
        List<Row> rows = new ArrayList<>();
        int mode = AppSelectMode;
        String header;

        if (mode == SelectActivity) {
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
                rows.add(new Row(appLabel, activityInfo.name, activityInfo));
            }
            header = getString(R.string.app_choose_an_activity);
        } else if (mode == SelectShortcut) {
            for (ResolveInfo resolveInfo : this.mAllShortcuts) {
                String label = resolveInfo.loadLabel(this.mPackageManager).toString();
                String packageName = resolveInfo.activityInfo.packageName;
                if (!matches(this.mQuery, label, packageName)) {
                    continue;
                }
                rows.add(new Row(label, packageName, resolveInfo));
            }
            header = getString(R.string.app_choose_a_shortcut);
        } else {
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
                rows.add(
                        new Row(
                                packageInfo.applicationInfo.name,
                                packageInfo.applicationInfo.packageName,
                                packageInfo));
            }
            header = getString(R.string.app_choose_an_app);
        }
        this.mAdapter.update(header, rows);
    }

    @Override // androidx.fragment.app.FragmentActivity
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override // androidx.fragment.app.FragmentActivity
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Drawable drawable;
        if (requestCode == 1 && resultCode == RESULT_OK) {
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

    private void handleRowClick(Row row) {
        int mode = AppSelectMode;
        if (mode == SelectPackage) {
            this.mSettings.setCurrentAction(this, new Action(2, ((PackageInfo) row.model).packageName));
            onBackPressed();
            return;
        }
        if (mode == SelectPackageActivity) {
            this.mSettings.setCurrentAction(
                    this, new Action(2, ((PackageInfo) row.model).applicationInfo.packageName));
            AppSelectMode = SelectActivity;
            startActivityForResult(new Intent(this, MultiSelectActivity.class), 0);
            finish();
            return;
        }
        if (mode == SelectBlacklisted) {
            this.mSettings.setBlacklisted(((PackageInfo) row.model).packageName);
            onBackPressed();
            return;
        }
        if (mode == SelectBlacklistedPie) {
            this.mSettings.setBlacklistedPie(((PackageInfo) row.model).packageName);
            onBackPressed();
            return;
        }
        if (mode == SelectShortcut) {
            ActivityInfo activityInfo = ((ResolveInfo) row.model).activityInfo;
            ComponentName componentName =
                    new ComponentName(activityInfo.applicationInfo.packageName, activityInfo.name);
            Intent intent = new Intent();
            intent.setComponent(componentName);
            intent.setAction("android.intent.action.CREATE_SHORTCUT");
            startActivityForResult(intent, 1);
            return;
        }
        // mode == SelectActivity
        ActivityInfo activityInfo = (ActivityInfo) row.model;
        this.mSettings.setCurrentAction(
                this, new Action(27, activityInfo.packageName + "/" + activityInfo.name));
        onBackPressed();
    }

    private Drawable iconFor(Object model) {
        if (AppSelectMode == SelectActivity) {
            return this.mPackageInfo == null
                    ? null
                    : this.mPackageInfo.applicationInfo.loadIcon(this.mPackageManager);
        }
        if (model instanceof ResolveInfo) {
            return ((ResolveInfo) model).loadIcon(this.mPackageManager);
        }
        if (model instanceof PackageInfo) {
            return ((PackageInfo) model).applicationInfo.loadIcon(this.mPackageManager);
        }
        return null;
    }

    /** A row plus the model it was built from - PackageInfo, ActivityInfo, or ResolveInfo. */
    private static final class Row {
        final String title;
        final String caption;
        final Object model;

        Row(String title, String caption, Object model) {
            this.title = title;
            this.caption = caption;
            this.model = model;
        }
    }

    final class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private SectionedRows<Row> mRows = new SectionedRows<>();

        void update(String header, List<Row> rows) {
            SectionedRows<Row> updated = new SectionedRows<>();
            updated.addSection(header, rows);
            this.mRows = updated;
            notifyDataSetChanged();
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
                    inflater.inflate(R.layout.listitem_icondescription, parent, false));
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof HeaderViewHolder) {
                ((HeaderViewHolder) holder).text.setText(this.mRows.headerAt(position));
                return;
            }
            RowViewHolder rowHolder = (RowViewHolder) holder;
            Row row = this.mRows.rowAt(position);
            rowHolder.title.setText(row.title);
            rowHolder.caption.setText(row.caption);
            IconUtils.setMaxSizeForImageView(getApplicationContext(), rowHolder.icon);
            IconLoader.load(rowHolder.icon, R.drawable.none, () -> iconFor(row.model));
            rowHolder.itemView.setOnClickListener(v -> handleRowClick(row));
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

        RowViewHolder(@NonNull View itemView) {
            super(itemView);
            this.title = itemView.findViewById(R.id.listitem_icondescription_text);
            this.caption = itemView.findViewById(R.id.listitem_icondescription_caption);
            this.icon = itemView.findViewById(R.id.listitem_icondescription_icon);
        }
    }
}
