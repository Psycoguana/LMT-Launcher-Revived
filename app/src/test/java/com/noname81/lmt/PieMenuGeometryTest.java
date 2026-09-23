package com.noname81.lmt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import android.content.Context;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;

import androidx.test.core.app.ApplicationProvider;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Hit-testing and polar conversion for the pie overlay.
 *
 * <p>This is the geometry a third-ring regression would surface in: {@code findItem}
 * brackets a touch by each item's stored inner/outer radius and angle sweep, and the
 * outermost ring is special-cased when "expand trigger area" is on.
 *
 * <p>{@code getPolar} and {@code findItem} are private instance methods on a
 * {@link android.widget.FrameLayout} subclass, so they are driven here by reflection
 * against a real {@code PieMenu}. That is deliberate: it exercises the shipped code
 * rather than a copy of the maths, and it needs no production change — which matters
 * while the equivalence oracle is still the safety net. The scenario is installed by
 * writing the private geometry fields directly, so the test does not depend on
 * whatever {@code init()} happened to read out of preferences.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class PieMenuGeometryTest {

    private static final float TAU_QUARTER = (float) (Math.PI / 2); // the f4 == 0 default

    private PieMenu menu;
    private Method getPolar;
    private Method findItem;

    @Before
    public void setUp() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        menu = new PieMenu(context);

        getPolar = PieMenu.class.getDeclaredMethod("getPolar", float.class, float.class);
        getPolar.setAccessible(true);
        findItem = PieMenu.class.getDeclaredMethod("findItem", PointF.class, boolean.class);
        findItem.setAccessible(true);
    }

    private void set(String field, Object value) throws Exception {
        Field f = PieMenu.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(menu, value);
    }

    private PointF polar(float x, float y) throws Exception {
        return (PointF) getPolar.invoke(menu, x, y);
    }

    private PieItem find(PointF p, boolean expandTriggerArea) throws Exception {
        return (PieItem) findItem.invoke(menu, p, expandTriggerArea);
    }

    /** An item occupying [startAngle, startAngle+sweep) between two radii. */
    private static PieItem item(int level, float start, float sweep, int inner, int outer) {
        PieItem it = new PieItem(null, level);
        it.setGeometry(start, sweep, inner, outer, new Path());
        return it;
    }

    /** Centre the pie away from the left edge so getPolar does not mirror x. */
    private void centreAt(int cx, int cy, int slop) throws Exception {
        set("mCenter", new Point(cx, cy));
        set("mSlop", slop);
    }

    private void installItems(int levels, int touchOffset, PieItem... items) throws Exception {
        List<PieItem> list = new ArrayList<>();
        for (PieItem i : items) {
            list.add(i);
        }
        set("mItems", list);
        set("mLevels", levels);
        set("mTouchOffset", touchOffset);
    }

    // ---------------------------------------------------------------- getPolar

    @Test
    public void polarRadiusIsEuclideanDistanceFromCentre() throws Exception {
        centreAt(500, 500, 0);
        // 3-4-5 triangle: dx = 300, dy = 400 -> r = 500
        assertEquals(500f, polar(200, 100).y, 0.001f);
    }

    @Test
    public void polarAngleIsAQuarterTurnWhenLevelWithTheCentre() throws Exception {
        centreAt(500, 500, 0);
        // f4 == 0 takes neither asin branch and keeps the seeded default.
        assertEquals(TAU_QUARTER, polar(200, 500).x, 0.0001f);
    }

    @Test
    public void polarAngleAboveTheCentreUsesAsinDirectly() throws Exception {
        centreAt(500, 500, 0);
        // straight up: dx = 0, dy = +100 -> asin(0) = 0
        assertEquals(0f, polar(500, 400).x, 0.0001f);
    }

    @Test
    public void polarAngleBelowTheCentreIsReflectedThroughPi() throws Exception {
        centreAt(500, 500, 0);
        // straight down: dy < 0 -> pi - asin(0) = pi
        assertEquals((float) Math.PI, polar(500, 600).x, 0.0001f);
    }

    @Test
    public void polarMirrorsHorizontallyWhenThePieHangsOffTheLeftEdge() throws Exception {
        // mCenter.x < mSlop means the pie is anchored to the left edge, and getPolar
        // negates dx so the sweep runs the other way round. Same point, mirrored angle.
        centreAt(10, 500, 50);
        float mirrored = polar(10, 400).x;
        centreAt(10, 500, 0);
        float normal = polar(10, 400).x;
        assertEquals("straight up is asin(0) either way", normal, mirrored, 0.0001f);

        // off-axis is where the mirroring shows
        centreAt(10, 500, 50);
        float leftAnchored = polar(110, 400).x;
        centreAt(10, 500, 0);
        float notAnchored = polar(110, 400).x;
        assertEquals("dx flips sign, so the angle negates", -notAnchored, leftAnchored, 0.0001f);
    }

    // ---------------------------------------------------------------- findItem

    @Test
    public void findsTheItemBracketingTheTouch() throws Exception {
        centreAt(500, 500, 0);
        PieItem ring1 = item(1, 0f, 1f, 100, 200);
        PieItem ring2 = item(2, 0f, 1f, 200, 300);
        installItems(2, 0, ring1, ring2);

        PointF inRing1 = new PointF();
        inRing1.x = 0.5f;
        inRing1.y = 150f;
        assertSame(ring1, find(inRing1, false));

        PointF inRing2 = new PointF();
        inRing2.x = 0.5f;
        inRing2.y = 250f;
        assertSame(ring2, find(inRing2, false));
    }

    @Test
    public void returnsNullOutsideEveryRing() throws Exception {
        centreAt(500, 500, 0);
        installItems(1, 0, item(1, 0f, 1f, 100, 200));

        PointF tooClose = new PointF();
        tooClose.x = 0.5f;
        tooClose.y = 50f;
        assertNull("inside the inner radius", find(tooClose, false));

        PointF tooFar = new PointF();
        tooFar.x = 0.5f;
        tooFar.y = 500f;
        assertNull("beyond the outer radius", find(tooFar, false));
    }

    @Test
    public void returnsNullOutsideTheAngularSweep() throws Exception {
        centreAt(500, 500, 0);
        installItems(1, 0, item(1, 1f, 0.5f, 100, 200));

        PointF before = new PointF();
        before.x = 0.5f;
        before.y = 150f;
        assertNull(find(before, false));

        PointF after = new PointF();
        after.x = 2f;
        after.y = 150f;
        assertNull(find(after, false));
    }

    @Test
    public void sweepBoundsAreExclusiveAtBothEnds() throws Exception {
        // findItem uses strict < and >, so a touch exactly on a slice edge matches
        // nothing. Pinning this because adjacent slices share an edge, and flipping
        // either comparison to <=/>= would make two slices claim the same point.
        centreAt(500, 500, 0);
        installItems(1, 0, item(1, 1f, 1f, 100, 200));

        PointF onStart = new PointF();
        onStart.x = 1f;
        onStart.y = 150f;
        assertNull("exactly on the start angle", find(onStart, false));

        PointF onEnd = new PointF();
        onEnd.x = 2f;
        onEnd.y = 150f;
        assertNull("exactly on the end angle", find(onEnd, false));

        PointF justInside = new PointF();
        justInside.x = 1.5f;
        justInside.y = 150f;
        assertNotNull(find(justInside, false));
    }

    @Test
    public void touchOffsetWidensRingsInwards() throws Exception {
        centreAt(500, 500, 0);
        PieItem ring = item(1, 0f, 1f, 100, 200);
        installItems(1, 30, ring);

        // inner - offset = 70, so 80 now hits even though it is inside the drawn ring
        PointF justInsideTheDrawnRing = new PointF();
        justInsideTheDrawnRing.x = 0.5f;
        justInsideTheDrawnRing.y = 80f;
        assertSame(ring, find(justInsideTheDrawnRing, false));

        // outer - offset = 170, so 180 no longer hits
        PointF nearTheOuterEdge = new PointF();
        nearTheOuterEdge.x = 0.5f;
        nearTheOuterEdge.y = 180f;
        assertNull(find(nearTheOuterEdge, false));
    }

    // ------------------------------------------- outermost ring / third-ring path

    @Test
    public void expandTriggerAreaMakesTheOutermostRingUnbounded() throws Exception {
        // With expandTriggerArea on, an item whose level == mLevels ignores its outer
        // radius entirely, so a touch far beyond the pie still selects it. This is the
        // branch a third ring changes the meaning of: adding ring 3 moves mLevels to 3,
        // so ring 2 silently loses this behaviour and ring 3 gains it.
        centreAt(500, 500, 0);
        PieItem ring1 = item(1, 0f, 1f, 100, 200);
        PieItem outermost = item(2, 0f, 1f, 200, 300);
        installItems(2, 0, ring1, outermost);

        PointF wayOutside = new PointF();
        wayOutside.x = 0.5f;
        wayOutside.y = 5000f;

        assertSame("outermost ring swallows far-away touches", outermost,
                find(wayOutside, true));
        assertNull("but only when expandTriggerArea is on", find(wayOutside, false));
    }

    @Test
    public void addingAThirdRingMovesTheUnboundedBehaviourOutwards() throws Exception {
        centreAt(500, 500, 0);
        PieItem ring2 = item(2, 0f, 1f, 200, 300);
        PieItem ring3 = item(3, 0f, 1f, 300, 400);
        installItems(3, 0, ring2, ring3);

        PointF wayOutside = new PointF();
        wayOutside.x = 0.5f;
        wayOutside.y = 5000f;

        assertSame("ring 3 is now the outermost", ring3, find(wayOutside, true));
    }

    @Test
    public void addItemTracksLevelCountAndPerLevelTally() throws Exception {
        // mLevels drives both layout radius and the expandTriggerArea branch above,
        // so it must follow the highest level added.
        menu.addItem(item(1, 0f, 1f, 100, 200));
        menu.addItem(item(3, 0f, 1f, 300, 400));
        menu.addItem(item(2, 0f, 1f, 200, 300));

        Field levels = PieMenu.class.getDeclaredField("mLevels");
        levels.setAccessible(true);
        assertEquals("mLevels is the maximum, not the count", 3, levels.getInt(menu));

        Field counts = PieMenu.class.getDeclaredField("mCounts");
        counts.setAccessible(true);
        int[] perLevel = (int[]) counts.get(menu);
        assertEquals(1, perLevel[1]);
        assertEquals(1, perLevel[2]);
        assertEquals(1, perLevel[3]);
    }
}
