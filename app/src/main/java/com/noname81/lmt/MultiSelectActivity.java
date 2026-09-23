package com.noname81.lmt;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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

    @Override // android.app.Activity
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mSettings = SettingsValues.getInstance(getApplicationContext());
        this.mPackageManager = getPackageManager();
        LinkedList linkedList = new LinkedList();
        SeparatedListAdapter separatedListAdapter = new SeparatedListAdapter(this);
        int mode = AppSelectMode;
        if (mode == 2) {
            try {
                this.mPackageInfo =
                        this.mPackageManager.getPackageInfo(
                                this.mSettings.getCurrentAction().getString(), 1);
                this.mActivityInfos = new LinkedList();
                if (this.mPackageInfo.activities != null) {
                    for (int index = 0; index < this.mPackageInfo.activities.length; index++) {
                        if (this.mPackageInfo.activities[index].exported) {
                            this.mActivityInfos.add(this.mPackageInfo.activities[index]);
                        }
                    }
                }
                String string =
                        this.mPackageInfo
                                .applicationInfo
                                .loadLabel(this.mPackageManager)
                                .toString();
                for (int activityIndex = 0;
                        activityIndex < this.mActivityInfos.size();
                        activityIndex++) {
                    linkedList.add(
                            SeparatedListAdapter.createItem(
                                    string, this.mActivityInfos.get(activityIndex).name));
                }
                separatedListAdapter.addSection(
                        getString(R.string.app_choose_an_activity),
                        new AppSimpleAdapter(this, linkedList));
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        } else if (mode == 5) {
            try {
                this.mShortcuts =
                        this.mPackageManager.queryIntentActivities(
                                new Intent("android.intent.action.CREATE_SHORTCUT"), 0);
                for (int shortcutIndex = 0;
                        shortcutIndex < this.mShortcuts.size();
                        shortcutIndex++) {
                    linkedList.add(
                            SeparatedListAdapter.createItem(
                                    this.mShortcuts
                                            .get(shortcutIndex)
                                            .loadLabel(this.mPackageManager)
                                            .toString(),
                                    this.mShortcuts.get(shortcutIndex).activityInfo.packageName));
                }
                separatedListAdapter.addSection(
                        getString(R.string.app_choose_a_shortcut),
                        new AppSimpleAdapter(this, linkedList));
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        } else {
            this.mPackageInfos = this.mPackageManager.getInstalledPackages(1);
            int count = 0;
            while (count < this.mPackageInfos.size()) {
                PackageInfo packageInfo = this.mPackageInfos.get(count);
                if (packageInfo.activities != null) {
                    packageInfo.applicationInfo.name =
                            packageInfo.applicationInfo.loadLabel(this.mPackageManager).toString();
                } else {
                    this.mPackageInfos.remove(count);
                    count--;
                }
                count++;
            }
            Collections.sort(this.mPackageInfos, comparator);
            for (int packageIndex = 0; packageIndex < this.mPackageInfos.size(); packageIndex++) {
                PackageInfo packageInfo2 = this.mPackageInfos.get(packageIndex);
                linkedList.add(
                        SeparatedListAdapter.createItem(
                                packageInfo2.applicationInfo.name,
                                packageInfo2.applicationInfo.packageName));
            }
            separatedListAdapter.addSection(
                    getString(R.string.app_choose_an_app), new AppSimpleAdapter(this, linkedList));
        }
        ListView listView = new ListView(this);
        listView.setAdapter((ListAdapter) separatedListAdapter);
        listView.setOnItemClickListener(this);
        listView.setDividerHeight(0);
        setContentView(listView);
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

    class AppSimpleAdapter extends SimpleAdapter {
        AppSimpleAdapter(Context context, List<? extends Map<String, ?>> list) {
            super(
                    context,
                    list,
                    R.layout.listitem_icondescription,
                    new String[] {"title", "caption"},
                    new int[] {
                        R.id.listitem_icondescription_text, R.id.listitem_icondescription_caption
                    });
        }

        @Override // android.widget.SimpleAdapter, android.widget.Adapter
        public View getView(final int position, View view, ViewGroup viewGroup) {
            View view2 = super.getView(position, view, viewGroup);
            ImageView imageView =
                    (ImageView) view2.findViewById(R.id.listitem_icondescription_icon);
            IconUtils.setMaxSizeForImageView(
                    MultiSelectActivity.this.getApplicationContext(), imageView);
            new AsyncDrawableTask(imageView, R.drawable.none) { // from class:
                // com.noname81.lmt.MultiSelectActivity.AppSimpleAdapter.1
                @Override // com.noname81.lmt.AsyncDrawableTask, android.os.AsyncTask
                protected Drawable doInBackground(Void... voidArr) {
                    if (MultiSelectActivity.AppSelectMode == 2) {
                        return MultiSelectActivity.this.mPackageInfo.applicationInfo.loadIcon(
                                MultiSelectActivity.this.mPackageManager);
                    }
                    if (MultiSelectActivity.AppSelectMode == 5) {
                        return MultiSelectActivity.this
                                .mShortcuts
                                .get(position)
                                .loadIcon(MultiSelectActivity.this.mPackageManager);
                    }
                    return MultiSelectActivity.this
                            .mPackageInfos
                            .get(position)
                            .applicationInfo
                            .loadIcon(MultiSelectActivity.this.mPackageManager);
                }
            }.execute(new Void[0]);
            return view2;
        }
    }
}
