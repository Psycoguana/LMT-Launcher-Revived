package com.noname81.lmt;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;

import java.util.Vector;

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

        PieItemAction(PieItem pieItem, Action shortPress, Action longPress, boolean isRecentApp) {
            this.mPieItem = pieItem;
            this.mClickAction = shortPress;
            this.mLongClickAction = longPress;
            this.mTempAction = isRecentApp;
        }
    }

    PieControl(Context context) {
        super(context);
        this.mPieItemActions = new Vector<>();
        this.mLauncher = Launcher.getInstance(context);
        this.mSettings = SettingsValues.getInstance(context);
    }

    @Override // com.noname81.lmt.PieControlBase
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
                                false));
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
                                false));
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
                                false));
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

    @Override // com.noname81.lmt.PieControlBase
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

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        for (int index = 0; index < this.mPieItemActions.size(); index++) {
            if (this.mPieItemActions.get(index).mPieItem.getView() == view) {
                this.mLauncher.fireAction(this.mPieItemActions.get(index).mClickAction);
                return;
            }
        }
    }

    @Override // android.view.View.OnLongClickListener
    public boolean onLongClick(View view) {
        for (int index = 0; index < this.mPieItemActions.size(); index++) {
            if (this.mPieItemActions.get(index).mPieItem.getView() == view) {
                if (this.mPieItemActions.get(index).mLongClickAction.getType() != 1) {
                    this.mLauncher.fireAction(this.mPieItemActions.get(index).mLongClickAction);
                    break;
                }
                this.mLauncher.fireAction(this.mPieItemActions.get(index).mClickAction);
                break;
            }
        }
        return true;
    }

    @Override // android.view.View.OnKeyListener
    public boolean onKey(View view, int keyCode, KeyEvent event) {
        for (int index = 0; index < this.mPieItemActions.size(); index++) {
            if (this.mPieItemActions.get(index).mPieItem.getView() == view) {
                if (keyCode == 36) {
                    if (this.mPieItemActions.get(index).mClickAction.getType() == 44) {
                        this.mPie.activatePiePointer();
                    } else if (this.mPieItemActions.get(index).mClickAction.getType() == 45) {
                        activatePieRecentApps();
                    }
                } else if (keyCode == 40) {
                    if (this.mPieItemActions.get(index).mLongClickAction.getType() == 44) {
                        this.mPie.activatePiePointer();
                    } else if (this.mPieItemActions.get(index).mLongClickAction.getType() == 45) {
                        activatePieRecentApps();
                    }
                }
            }
        }
        return true;
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
