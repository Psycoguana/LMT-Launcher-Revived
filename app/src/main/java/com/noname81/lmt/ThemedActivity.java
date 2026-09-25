package com.noname81.lmt;

import android.content.Context;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Base for every screen, applying the Appearance settings (ROADMAP 1.10) at the two points they
 * must land: night mode before AppCompat attaches the base context - set any later, AppCompat
 * patches the configuration in place, which discards a dynamic colour scheme applied to the same
 * resources - and the colour scheme before {@code onCreate} inflates anything.
 */
public abstract class ThemedActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        ThemeHelper.applyNightMode(newBase);
        super.attachBaseContext(newBase);
    }

    @Override
    protected void onCreate(Bundle bundle) {
        ThemeHelper.applyColors(this);
        super.onCreate(bundle);
    }
}
