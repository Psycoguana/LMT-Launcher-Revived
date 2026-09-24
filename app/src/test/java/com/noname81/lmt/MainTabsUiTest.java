package com.noname81.lmt;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.google.android.material.tabs.TabLayout;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Pre-UI-rewrite safety net: which tabs the main screen shows for each feature mode (TSMode).
 * Settings always comes first and Info always last; Gestures + ISAs need the gesture engine
 * (modes 0 and 1), Pie needs the pie (modes 1 and 2).
 */
@RunWith(RobolectricTestRunner.class)
@Config(
        sdk = 33,
        shadows = ShadowTouchServiceNative.class,
        instrumentedPackages = "com.noname81.lmt")
public class MainTabsUiTest {

    private Context context;

    @Before
    public void setUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();
    }

    @After
    public void tearDown() throws Exception {
        // SettingsValues caches slots in a static; later test classes must not inherit ours.
        UiTestSupport.resetSingletons(context);
    }

    @Test
    public void gesturesOnlyMode() throws Exception {
        assertEquals(Arrays.asList("SETTINGS", "GESTURES", "ISAS", "INFO"), tabsFor(0));
    }

    @Test
    public void gesturesAndPieMode() throws Exception {
        assertEquals(Arrays.asList("SETTINGS", "GESTURES", "ISAS", "PIE", "INFO"), tabsFor(1));
    }

    @Test
    public void pieOnlyModeWhichIsTheDefault() throws Exception {
        assertEquals(Arrays.asList("SETTINGS", "PIE", "INFO"), tabsFor(2));
        UiTestSupport.resetSingletons(context);
        assertEquals(2, SettingsValues.getInstance(context).loadTouchServiceMode());
    }

    private List<String> tabsFor(int mode) throws Exception {
        UiTestSupport.resetSingletons(context);
        SettingsValues.getInstance(context).saveTouchServiceMode(mode);
        LMT activity = Robolectric.buildActivity(LMT.class).setup().get();
        TabLayout tabLayout = activity.mTabLayout;
        List<String> tabs = new ArrayList<>();
        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            tabs.add(String.valueOf(tabLayout.getTabAt(i).getText()).toUpperCase());
        }
        return tabs;
    }
}
