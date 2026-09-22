package com.noname81.lmt;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;

import java.util.Vector;

/* JADX INFO: loaded from: classes.dex */
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

        PieItemAction(PieItem pieItem, Action action, Action action2, boolean z) {
            this.mPieItem = pieItem;
            this.mClickAction = action;
            this.mLongClickAction = action2;
            this.mTempAction = z;
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
        for (int i = 0; i < 9; i += 2) {
            if (this.mSettings.getPieAction(i).getType() > 1) {
                int i2 = i + 1;
                this.mPieItemActions.add(
                        new PieItemAction(
                                makeItem(
                                        this.mSettings
                                                .getPieAction(i)
                                                .getDrawable(
                                                        this.mContext,
                                                        IconUtils.getNamePie(i),
                                                        iLoadPieShowScaleUserImages,
                                                        iLoadPieShowScaleAppImages,
                                                        false),
                                        this.mSettings
                                                .getPieAction(i2)
                                                .getDrawable(
                                                        this.mContext,
                                                        IconUtils.getNamePie(i2),
                                                        iLoadPieShowScaleUserImages,
                                                        iLoadPieShowScaleAppImages,
                                                        false),
                                        1),
                                this.mSettings.getPieAction(i),
                                this.mSettings.getPieAction(i2),
                                false));
            }
        }
        for (int i3 = 10; i3 < 23; i3 += 2) {
            if (this.mSettings.getPieAction(i3).getType() > 1) {
                int i4 = i3 + 1;
                this.mPieItemActions.add(
                        new PieItemAction(
                                makeItem(
                                        this.mSettings
                                                .getPieAction(i3)
                                                .getDrawable(
                                                        this.mContext,
                                                        IconUtils.getNamePie(i3),
                                                        iLoadPieShowScaleUserImages,
                                                        iLoadPieShowScaleAppImages,
                                                        false),
                                        this.mSettings
                                                .getPieAction(i4)
                                                .getDrawable(
                                                        this.mContext,
                                                        IconUtils.getNamePie(i4),
                                                        iLoadPieShowScaleUserImages,
                                                        iLoadPieShowScaleAppImages,
                                                        false),
                                        2),
                                this.mSettings.getPieAction(i3),
                                this.mSettings.getPieAction(i4),
                                false));
            }
        }
        for (int i5 = 24; i5 < 37; i5 += 2) {
            if (this.mSettings.getPieAction(i5).getType() > 1) {
                int i6 = i5 + 1;
                this.mPieItemActions.add(
                        new PieItemAction(
                                makeItem(
                                        this.mSettings
                                                .getPieAction(i5)
                                                .getDrawable(
                                                        this.mContext,
                                                        IconUtils.getNamePie(i5),
                                                        iLoadPieShowScaleUserImages,
                                                        iLoadPieShowScaleAppImages,
                                                        false),
                                        this.mSettings
                                                .getPieAction(i6)
                                                .getDrawable(
                                                        this.mContext,
                                                        IconUtils.getNamePie(i6),
                                                        iLoadPieShowScaleUserImages,
                                                        iLoadPieShowScaleAppImages,
                                                        false),
                                        3),
                                this.mSettings.getPieAction(i5),
                                this.mSettings.getPieAction(i6),
                                false));
            }
        }
        if (this.mPieItemActions.size() == 0) {
            createDefaultPieActions();
        }
        for (int i7 = 0; i7 < this.mPieItemActions.size(); i7++) {
            this.mPie.addItem(this.mPieItemActions.get(i7).mPieItem);
            setClickListener(this, this.mPieItemActions.get(i7).mPieItem);
            setLongClickListener(this, this.mPieItemActions.get(i7).mPieItem);
            setKeyListener(this, this.mPieItemActions.get(i7).mPieItem);
        }
    }

    private void activatePieRecentApps() {
        int iLoadPieShowScaleUserImages = this.mSettings.loadPieShowScaleUserImages();
        int iLoadPieShowScaleAppImages = this.mSettings.loadPieShowScaleAppImages();
        String packageNamesOfRecentApps = this.mSettings.getPackageNamesOfRecentApps(12);
        if (packageNamesOfRecentApps.length() > 0) {
            String[] strArrSplit = packageNamesOfRecentApps.split(" ");
            Vector vector = new Vector();
            int i = 0;
            while (true) {
                int i2 = 1;
                if (i >= strArrSplit.length) {
                    break;
                }
                Action action = new Action(2, strArrSplit[i]);
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
                if (i > 4) {
                    i2 = 2;
                }
                Vector vector2 = vector;
                vector2.add(
                        new PieItemAction(
                                makeItem(drawable, drawable2, i2), action, action2, true));
                i++;
                vector = vector2;
            }
            Vector vector3 = vector;
            this.mPie.clearItems();
            for (int i3 = 0; i3 < vector3.size(); i3++) {
                this.mPie.addItem(((PieItemAction) vector3.get(i3)).mPieItem);
                setClickListener(this, ((PieItemAction) vector3.get(i3)).mPieItem);
                setLongClickListener(this, ((PieItemAction) vector3.get(i3)).mPieItem);
                setKeyListener(this, ((PieItemAction) vector3.get(i3)).mPieItem);
                this.mPieItemActions.add((PieItemAction) vector3.get(i3));
            }
            this.mPie.relayoutPie();
        }
    }

    @Override // com.noname81.lmt.PieControlBase
    protected void attachToContainer(FrameLayout frameLayout) {
        int i = 0;
        boolean z = false;
        while (i < this.mPieItemActions.size()) {
            if (this.mPieItemActions.get(i).mTempAction) {
                this.mPieItemActions.remove(i);
                if (i > 0) {
                    i--;
                }
                z = true;
            }
            i++;
        }
        if (z) {
            this.mPie.clearItems();
            for (int i2 = 0; i2 < this.mPieItemActions.size(); i2++) {
                this.mPie.addItem(this.mPieItemActions.get(i2).mPieItem);
            }
        }
        super.attachToContainer(frameLayout);
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        for (int i = 0; i < this.mPieItemActions.size(); i++) {
            if (this.mPieItemActions.get(i).mPieItem.getView() == view) {
                this.mLauncher.fireAction(this.mPieItemActions.get(i).mClickAction);
                return;
            }
        }
    }

    @Override // android.view.View.OnLongClickListener
    public boolean onLongClick(View view) {
        for (int i = 0; i < this.mPieItemActions.size(); i++) {
            if (this.mPieItemActions.get(i).mPieItem.getView() == view) {
                if (this.mPieItemActions.get(i).mLongClickAction.getType() != 1) {
                    this.mLauncher.fireAction(this.mPieItemActions.get(i).mLongClickAction);
                    break;
                }
                this.mLauncher.fireAction(this.mPieItemActions.get(i).mClickAction);
                break;
            }
        }
        return true;
    }

    @Override // android.view.View.OnKeyListener
    public boolean onKey(View view, int i, KeyEvent keyEvent) {
        for (int i2 = 0; i2 < this.mPieItemActions.size(); i2++) {
            if (this.mPieItemActions.get(i2).mPieItem.getView() == view) {
                if (i == 36) {
                    if (this.mPieItemActions.get(i2).mClickAction.getType() == 44) {
                        this.mPie.activatePiePointer();
                    } else if (this.mPieItemActions.get(i2).mClickAction.getType() == 45) {
                        activatePieRecentApps();
                    }
                } else if (i == 40) {
                    if (this.mPieItemActions.get(i2).mLongClickAction.getType() == 44) {
                        this.mPie.activatePiePointer();
                    } else if (this.mPieItemActions.get(i2).mLongClickAction.getType() == 45) {
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
