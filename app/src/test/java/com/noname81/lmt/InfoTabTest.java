package com.noname81.lmt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.text.Spanned;
import android.text.style.URLSpan;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** ROADMAP 1.18: the Info tab's credits, links, status card and permission tiles. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class InfoTabTest {
    private Context context;

    @Before
    public void setUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        UiTestSupport.resetSingletons(context);
    }

    @After
    public void tearDown() throws Exception {
        UiTestSupport.resetSingletons(context);
    }

    private View showInfo() {
        FragmentActivity host = UiTestSupport.hostActivity();
        host.getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, new InfoFragment())
                .commitNow();
        UiTestSupport.idle();
        return host.findViewById(R.id.info_status).getRootView();
    }

    @Test
    public void creditsLinkToNoname81AndPsycoguana() {
        View root = showInfo();
        TextView credits = root.findViewById(R.id.info_credits);
        assertEquals(
                "Original app by noname81\nRevived by Psycoguana", credits.getText().toString());
        URLSpan[] links =
                ((Spanned) credits.getText())
                        .getSpans(0, credits.getText().length(), URLSpan.class);
        assertEquals(2, links.length);
        assertEquals(InfoFragment.NONAME81, links[0].getURL());
        assertEquals(InfoFragment.PSYCOGUANA, links[1].getURL());
    }

    @Test
    public void buttonsAreGithubThenXdaThenDonate() {
        View root = showInfo();
        View github = root.findViewById(R.id.info_github);
        View xda = root.findViewById(R.id.info_xda);
        View donate = root.findViewById(R.id.info_donate);
        LinearLayout column = (LinearLayout) github.getParent();
        assertTrue(column.indexOfChild(github) < column.indexOfChild(xda));
        assertTrue(column.indexOfChild(xda) < column.indexOfChild(donate));
    }

    @Test
    public void versionChipAndSixTiles() {
        View root = showInfo();
        TextView version = root.findViewById(R.id.info_version);
        assertTrue(version.getText().toString().startsWith("Version "));
        LinearLayout tiles = root.findViewById(R.id.info_tiles);
        int count = 0;
        for (int row = 0; row < tiles.getChildCount(); row++) {
            count += ((LinearLayout) tiles.getChildAt(row)).getChildCount();
        }
        assertEquals(6, count);
    }

    @Test
    public void aStoppedServiceSaysSo() {
        View root = showInfo();
        TextView title = root.findViewById(R.id.info_status_title);
        String text = title.getText().toString();
        assertTrue(text, text.contains("stopped") || text.contains("can't show"));
    }
}
