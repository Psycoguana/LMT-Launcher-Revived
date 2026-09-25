package com.noname81.lmt;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Builds the pie's contents from the stored slots and handles selection.
 *
 * <p>{@code populateMenu} walks three hardcoded slot ranges — ring 1, ring 2, ring 3 — stepping by
 * two because each position stores a short-press and a long-press action. Those ranges are a
 * contract with {@code SettingsValuesBase.loadActions}; SettingsSlotSchemaTest guards it.
 *
 * <p>Two action types replace the pie's contents rather than firing: the pointer extension and the
 * recent-apps list.
 */
class PieControl extends PieControlBase
        implements View.OnClickListener, View.OnLongClickListener, View.OnKeyListener {
    private Launcher mLauncher;
    private Vector<PieItemAction> mPieItemActions;
    private SettingsValues mSettings;

    class PieItemAction {
        Action mClickAction;
        Action mLongClickAction;
        PieItem mPieItem;
        boolean mTempAction;

        /** The settings slot of the short press, -1 for items not from a slot (ROADMAP 1.17). */
        int mSlot = -1;

        PieItemAction(PieItem pieItem, Action shortPress, Action longPress, boolean isRecentApp) {
            this.mPieItem = pieItem;
            this.mClickAction = shortPress;
            this.mLongClickAction = longPress;
            this.mTempAction = isRecentApp;
        }

        PieItemAction(
                PieItem pieItem,
                Action shortPress,
                Action longPress,
                boolean isRecentApp,
                int slot) {
            this(pieItem, shortPress, longPress, isRecentApp);
            this.mSlot = slot;
        }
    }

    /** The open folder's items (ROADMAP 1.17); PieMenu holds them while the folder is open. */
    private final Vector<PieItemAction> mFolderItemActions = new Vector<>();

    PieControl(Context context) {
        super(context);
        this.mPieItemActions = new Vector<>();
        this.mLauncher = Launcher.getInstance(context);
        this.mSettings = SettingsValues.getInstance(context);
    }

    @Override
    protected void populateMenu() {
        int iLoadPieShowScaleUserImages = this.mSettings.loadPieShowScaleUserImages();
        int iLoadPieShowScaleAppImages = this.mSettings.loadPieShowScaleAppImages();
        for (int slot1 = 0; slot1 < 9; slot1 += 2) {
            if (this.mSettings.getPieAction(slot1).getType() > 1) {
                int longSlot1 = slot1 + 1;
                this.mPieItemActions.add(
                        new PieItemAction(
                                makeItem(
                                        this.mSettings
                                                .getPieAction(slot1)
                                                .getDrawable(
                                                        this.mContext,
                                                        IconUtils.getNamePie(slot1),
                                                        iLoadPieShowScaleUserImages,
                                                        iLoadPieShowScaleAppImages,
                                                        false),
                                        this.mSettings
                                                .getPieAction(longSlot1)
                                                .getDrawable(
                                                        this.mContext,
                                                        IconUtils.getNamePie(longSlot1),
                                                        iLoadPieShowScaleUserImages,
                                                        iLoadPieShowScaleAppImages,
                                                        false),
                                        1),
                                this.mSettings.getPieAction(slot1),
                                this.mSettings.getPieAction(longSlot1),
                                false,
                                slot1));
            }
        }
        for (int slot2 = 10; slot2 < 23; slot2 += 2) {
            if (this.mSettings.getPieAction(slot2).getType() > 1) {
                int longSlot2 = slot2 + 1;
                this.mPieItemActions.add(
                        new PieItemAction(
                                makeItem(
                                        this.mSettings
                                                .getPieAction(slot2)
                                                .getDrawable(
                                                        this.mContext,
                                                        IconUtils.getNamePie(slot2),
                                                        iLoadPieShowScaleUserImages,
                                                        iLoadPieShowScaleAppImages,
                                                        false),
                                        this.mSettings
                                                .getPieAction(longSlot2)
                                                .getDrawable(
                                                        this.mContext,
                                                        IconUtils.getNamePie(longSlot2),
                                                        iLoadPieShowScaleUserImages,
                                                        iLoadPieShowScaleAppImages,
                                                        false),
                                        2),
                                this.mSettings.getPieAction(slot2),
                                this.mSettings.getPieAction(longSlot2),
                                false,
                                slot2));
            }
        }
        for (int slot3 = 24; slot3 < 37; slot3 += 2) {
            if (this.mSettings.getPieAction(slot3).getType() > 1) {
                int longSlot3 = slot3 + 1;
                this.mPieItemActions.add(
                        new PieItemAction(
                                makeItem(
                                        this.mSettings
                                                .getPieAction(slot3)
                                                .getDrawable(
                                                        this.mContext,
                                                        IconUtils.getNamePie(slot3),
                                                        iLoadPieShowScaleUserImages,
                                                        iLoadPieShowScaleAppImages,
                                                        false),
                                        this.mSettings
                                                .getPieAction(longSlot3)
                                                .getDrawable(
                                                        this.mContext,
                                                        IconUtils.getNamePie(longSlot3),
                                                        iLoadPieShowScaleUserImages,
                                                        iLoadPieShowScaleAppImages,
                                                        false),
                                        3),
                                this.mSettings.getPieAction(slot3),
                                this.mSettings.getPieAction(longSlot3),
                                false,
                                slot3));
            }
        }
        if (this.mPieItemActions.size() == 0) {
            createDefaultPieActions();
        }
        for (int index = 0; index < this.mPieItemActions.size(); index++) {
            this.mPie.addItem(this.mPieItemActions.get(index).mPieItem);
            setClickListener(this, this.mPieItemActions.get(index).mPieItem);
            setLongClickListener(this, this.mPieItemActions.get(index).mPieItem);
            setKeyListener(this, this.mPieItemActions.get(index).mPieItem);
        }
    }

    private void activatePieRecentApps() {
        int iLoadPieShowScaleUserImages = this.mSettings.loadPieShowScaleUserImages();
        int iLoadPieShowScaleAppImages = this.mSettings.loadPieShowScaleAppImages();
        String packageNamesOfRecentApps = this.mSettings.getPackageNamesOfRecentApps(12);
        if (packageNamesOfRecentApps.length() > 0) {
            String[] strArrSplit = packageNamesOfRecentApps.split(" ");
            Vector vector = new Vector();
            int slot = 0;
            while (true) {
                int level = 1;
                if (slot >= strArrSplit.length) {
                    break;
                }
                Action action = new Action(2, strArrSplit[slot]);
                Action action2 = new Action(1, "");
                Drawable drawable =
                        action.getDrawable(
                                this.mContext,
                                null,
                                iLoadPieShowScaleUserImages,
                                iLoadPieShowScaleAppImages,
                                false);
                Drawable drawable2 =
                        action2.getDrawable(
                                this.mContext,
                                null,
                                iLoadPieShowScaleUserImages,
                                iLoadPieShowScaleAppImages,
                                false);
                if (slot > 4) {
                    level = 2;
                }
                Vector vector2 = vector;
                vector2.add(
                        new PieItemAction(
                                makeItem(drawable, drawable2, level), action, action2, true));
                slot++;
                vector = vector2;
            }
            Vector vector3 = vector;
            this.mPie.clearItems();
            for (int index = 0; index < vector3.size(); index++) {
                this.mPie.addItem(((PieItemAction) vector3.get(index)).mPieItem);
                setClickListener(this, ((PieItemAction) vector3.get(index)).mPieItem);
                setLongClickListener(this, ((PieItemAction) vector3.get(index)).mPieItem);
                setKeyListener(this, ((PieItemAction) vector3.get(index)).mPieItem);
                this.mPieItemActions.add((PieItemAction) vector3.get(index));
            }
            this.mPie.relayoutPie();
        }
    }

    @Override
    protected void attachToContainer(FrameLayout frameLayout) {
        int level = 0;
        boolean hasRecentApps = false;
        while (level < this.mPieItemActions.size()) {
            if (this.mPieItemActions.get(level).mTempAction) {
                this.mPieItemActions.remove(level);
                if (level > 0) {
                    level--;
                }
                hasRecentApps = true;
            }
            level++;
        }
        if (hasRecentApps) {
            this.mPie.clearItems();
            for (int index = 0; index < this.mPieItemActions.size(); index++) {
                this.mPie.addItem(this.mPieItemActions.get(index).mPieItem);
            }
        }
        super.attachToContainer(frameLayout);
    }

    /** The slice or open-folder item whose view this is. */
    private PieItemAction find(View view) {
        for (PieItemAction action : this.mPieItemActions) {
            if (action.mPieItem.getView() == view) {
                return action;
            }
        }
        for (PieItemAction action : this.mFolderItemActions) {
            if (action.mPieItem.getView() == view) {
                return action;
            }
        }
        return null;
    }

    @Override
    public void onClick(View view) {
        PieItemAction action = find(view);
        if (action != null) {
            this.mLauncher.fireAction(action.mClickAction);
        }
    }

    @Override
    public boolean onLongClick(View view) {
        PieItemAction action = find(view);
        if (action != null) {
            this.mLauncher.fireAction(
                    action.mLongClickAction.getType() != 1
                            ? action.mLongClickAction
                            : action.mClickAction);
        }
        return true;
    }

    /**
     * PieMenu's hover (36) and long press (40) on a slice: extensions arm, folders open or close.
     */
    @Override
    public boolean onKey(View view, int keyCode, KeyEvent event) {
        PieItemAction hovered = find(view);
        if (hovered == null || this.mFolderItemActions.contains(hovered)) {
            return true;
        }
        Action click = hovered.mClickAction;
        Action longClick = hovered.mLongClickAction;
        boolean openOnHover = this.mSettings.loadPieFolderOpen() == 0;
        // After the long press timer fires every hover arrives as 40, so both close folders.
        if (keyCode == 36 && click.getType() == Action.Folder && openOnHover) {
            openFolder(hovered, hovered.mSlot);
            return true;
        }
        if (keyCode == 40 && longClick.getType() == Action.Folder) {
            openFolder(hovered, hovered.mSlot + 1);
            return true;
        }
        if (keyCode == 40 && click.getType() == Action.Folder) {
            openFolder(hovered, hovered.mSlot);
            return true;
        }
        closeFolderUnlessOutside(hovered);
        Action action = keyCode == 36 ? click : keyCode == 40 ? longClick : null;
        if (action == null) {
            return true;
        }
        if (action.getType() == Action.BrightnessDial) {
            this.mPie.armBrightness(hovered.mPieItem);
        } else if (action.getType() == 44) {
            this.mPie.activatePiePointer();
        } else if (action.getType() == 45) {
            activatePieRecentApps();
        }
        return true;
    }

    /** Builds the folder on {@code slot}'s items and has PieMenu show them around the slice. */
    private void openFolder(PieItemAction owner, int slot) {
        if (slot < 0 || this.mPie.getOpenFolder() == owner.mPieItem) {
            return;
        }
        this.mPie.closeFolder();
        this.mFolderItemActions.clear();
        int scaleUser = this.mSettings.loadPieShowScaleUserImages();
        int scaleApp = this.mSettings.loadPieShowScaleAppImages();
        int level = this.mPie.getLevels() + 1;
        List<PieItem> items = new ArrayList<>();
        for (int index = 0; index < SettingsValuesBase.FOLDER_ITEMS; index++) {
            Action action = this.mSettings.getFolderAction(slot, index);
            if (action.getType() <= Action.None) {
                continue;
            }
            Drawable icon = action.getDrawable(this.mContext, null, scaleUser, scaleApp, false);
            PieItem item = makeItem(icon, null, level);
            setClickListener(this, item);
            setLongClickListener(this, item);
            setKeyListener(this, item);
            this.mFolderItemActions.add(new PieItemAction(item, action, new Action(1), false));
            items.add(item);
        }
        this.mPie.openFolder(owner.mPieItem, items);
    }

    /**
     * Hovering a slice on the folder's ring or an inner one closes it; slices on rings between the
     * folder and its items are crossed on the way out, so they leave it open.
     */
    private void closeFolderUnlessOutside(PieItemAction hovered) {
        PieItem open = this.mPie.getOpenFolder();
        if (open != null
                && hovered.mPieItem != open
                && hovered.mPieItem.getLevel() <= open.getLevel()) {
            this.mPie.closeFolder();
        }
    }

    private void createDefaultPieActions() {
        int iLoadPieShowScaleUserImages = this.mSettings.loadPieShowScaleUserImages();
        int iLoadPieShowScaleAppImages = this.mSettings.loadPieShowScaleAppImages();
        this.mPieItemActions.clear();
        Action action = new Action(7);
        Action action2 = new Action(30);
        Action action3 = new Action(3);
        Action action4 = new Action(26, "26");
        Action action5 = new Action(9);
        Action action6 = new Action(31);
        Action action7 = new Action(5);
        Action action8 = new Action(2, "com.android.settings");
        Action action9 = new Action(10);
        Action action10 = new Action(11);
        this.mPieItemActions.add(
                new PieItemAction(
                        makeItem(
                                action.getDrawable(
                                        this.mContext,
                                        IconUtils.getNamePie(1),
                                        iLoadPieShowScaleUserImages,
                                        iLoadPieShowScaleAppImages,
                                        false),
                                action2.getDrawable(
                                        this.mContext,
                                        IconUtils.getNamePie(2),
                                        iLoadPieShowScaleUserImages,
                                        iLoadPieShowScaleAppImages,
                                        false),
                                1),
                        action,
                        action2,
                        false));
        this.mPieItemActions.add(
                new PieItemAction(
                        makeItem(
                                action3.getDrawable(
                                        this.mContext,
                                        IconUtils.getNamePie(3),
                                        iLoadPieShowScaleUserImages,
                                        iLoadPieShowScaleAppImages,
                                        false),
                                action4.getDrawable(
                                        this.mContext,
                                        IconUtils.getNamePie(4),
                                        iLoadPieShowScaleUserImages,
                                        iLoadPieShowScaleAppImages,
                                        false),
                                1),
                        action3,
                        action4,
                        false));
        this.mPieItemActions.add(
                new PieItemAction(
                        makeItem(
                                action5.getDrawable(
                                        this.mContext,
                                        IconUtils.getNamePie(5),
                                        iLoadPieShowScaleUserImages,
                                        iLoadPieShowScaleAppImages,
                                        false),
                                action6.getDrawable(
                                        this.mContext,
                                        IconUtils.getNamePie(6),
                                        iLoadPieShowScaleUserImages,
                                        iLoadPieShowScaleAppImages,
                                        false),
                                1),
                        action5,
                        action6,
                        false));
        this.mPieItemActions.add(
                new PieItemAction(
                        makeItem(
                                action7.getDrawable(
                                        this.mContext,
                                        IconUtils.getNamePie(7),
                                        iLoadPieShowScaleUserImages,
                                        iLoadPieShowScaleAppImages,
                                        false),
                                action8.getDrawable(
                                        this.mContext,
                                        IconUtils.getNamePie(8),
                                        iLoadPieShowScaleUserImages,
                                        iLoadPieShowScaleAppImages,
                                        false),
                                1),
                        action7,
                        action8,
                        false));
        this.mPieItemActions.add(
                new PieItemAction(
                        makeItem(
                                action9.getDrawable(
                                        this.mContext,
                                        IconUtils.getNamePie(9),
                                        iLoadPieShowScaleUserImages,
                                        iLoadPieShowScaleAppImages,
                                        false),
                                action10.getDrawable(
                                        this.mContext,
                                        IconUtils.getNamePie(10),
                                        iLoadPieShowScaleUserImages,
                                        iLoadPieShowScaleAppImages,
                                        false),
                                1),
                        action9,
                        action10,
                        false));
    }
}
