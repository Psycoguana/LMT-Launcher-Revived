package com.noname81.lmt;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;

/** The Appearance page (ROADMAP 1.10), opened from the Settings tab's General section. */
public class AppearanceActivity extends ThemedActivity {

    @Override // androidx.fragment.app.FragmentActivity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.appearance);
        setSupportActionBar(findViewById(R.id.toolbar));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.appearance_title);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        if (bundle == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.appearance_container, new AppearanceFragment())
                    .commit();
        }
    }

    @Override // android.app.Activity
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
