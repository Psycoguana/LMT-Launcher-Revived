package com.noname81.lmt;

import android.app.Fragment;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.noname81.lmt.SettingsViewHelper.SettingsSimpleAdapter;

import java.util.LinkedList;

/* JADX INFO: loaded from: classes.dex */
public class SettingsViewFragment extends Fragment {
    SettingsViewHelper mSettingsHelper;

    @Override // android.app.Fragment
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        setUserVisibleHint(true);
    }

    @Override // android.app.Fragment
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        super.onCreateView(layoutInflater, viewGroup, bundle);
        this.mSettingsHelper = new SettingsViewHelper(getActivity());
        SeparatedListAdapter separatedListAdapter = new SeparatedListAdapter(getActivity());
        int iLoadTouchServiceMode =
                SettingsValues.getInstance(getActivity()).loadTouchServiceMode();
        LinkedList linkedList = new LinkedList();
        linkedList.add(
                SeparatedListAdapter.createItem("Activate LMT", "Activate or deactivate LMT"));
        linkedList.add(
                SeparatedListAdapter.createItem(
                        "Autostart LMT", "Configure if LMT should run at startup"));
        linkedList.add(
                SeparatedListAdapter.createItem(
                        "Set active features", "Configure LMT's feature set"));
        this.mSettingsHelper.posOffsetTouchservice = 0;
        this.mSettingsHelper.posActivateTouchService = 1;
        this.mSettingsHelper.posAutostartTouchService = 2;
        this.mSettingsHelper.posSetMode = 3;
        SettingsViewHelper settingsViewHelper = this.mSettingsHelper;
        settingsViewHelper.getClass();
        separatedListAdapter.addSection(
                "General",
                settingsViewHelper
                .new SettingsSimpleAdapter(
                        getActivity(),
                        linkedList,
                        this.mSettingsHelper.posOffsetTouchservice,
                        false));
        int i = 4;
        if (iLoadTouchServiceMode < 2) {
            LinkedList linkedList2 = new LinkedList();
            linkedList2.add(
                    SeparatedListAdapter.createItem(
                            "Auto configuration",
                            "If auto configuration fails, set the input device and touchscreen to"
                                    + " screen factors below manually"));
            linkedList2.add(
                    SeparatedListAdapter.createItem(
                            "Set gesture input", "Set the input device for gesture recognition"));
            linkedList2.add(
                    SeparatedListAdapter.createItem(
                            "Touchscreen to screen factor X",
                            "Resolution factor in %, default = 100%"));
            linkedList2.add(
                    SeparatedListAdapter.createItem(
                            "Touchscreen to screen factor Y",
                            "Resolution factor in %, default = 100%"));
            this.mSettingsHelper.posOffsetGestureRecognition = 4;
            this.mSettingsHelper.posAutoConfiguration = 5;
            this.mSettingsHelper.posSetInput = 6;
            this.mSettingsHelper.posTouchscreenScreenFactorX = 7;
            this.mSettingsHelper.posTouchscreenScreenFactorY = 8;
            SettingsViewHelper settingsViewHelper2 = this.mSettingsHelper;
            settingsViewHelper2.getClass();
            separatedListAdapter.addSection(
                    "Gesture recognition",
                    settingsViewHelper2
                    .new SettingsSimpleAdapter(
                            getActivity(), linkedList2, this.mSettingsHelper.posOffsetIsas, false));
            LinkedList linkedList3 = new LinkedList();
            linkedList3.add(
                    SeparatedListAdapter.createItem(
                            "Feedback style", "Define the gesture feedback style"));
            linkedList3.add(
                    SeparatedListAdapter.createItem(
                            "Vibration Time", "Define the vibration time in ms, default = 30"));
            linkedList3.add(
                    SeparatedListAdapter.createItem(
                            "Single touch gestures",
                            "Configure if single touch gestures should be recognized, default ="
                                    + " enabled"));
            linkedList3.add(
                    SeparatedListAdapter.createItem(
                            "Min gesture score", "Min score in %, default = 70%"));
            linkedList3.add(
                    SeparatedListAdapter.createItem(
                            "Min gesture path length", "Min number of touch points, default = 7"));
            linkedList3.add(
                    SeparatedListAdapter.createItem(
                            "Add to gesture blacklist", "Exclude an app from gesture recognition"));
            linkedList3.add(
                    SeparatedListAdapter.createItem(
                            "Clear gesture blacklist", "Clear all excluded apps"));
            this.mSettingsHelper.posOffsetGestures = 9;
            this.mSettingsHelper.posFeedbackStyle = 10;
            this.mSettingsHelper.posVibrationTime = 11;
            this.mSettingsHelper.posSingleTouchGestures = 12;
            this.mSettingsHelper.posMinScore = 13;
            this.mSettingsHelper.posMinPathLength = 14;
            this.mSettingsHelper.posAddBlacklist = 15;
            this.mSettingsHelper.posClearBlacklist = 16;
            SettingsViewHelper settingsViewHelper3 = this.mSettingsHelper;
            settingsViewHelper3.getClass();
            separatedListAdapter.addSection(
                    "Gestures",
                    settingsViewHelper3
                    .new SettingsSimpleAdapter(
                            getActivity(),
                            linkedList3,
                            this.mSettingsHelper.posOffsetGestures,
                            false));
            LinkedList linkedList4 = new LinkedList();
            linkedList4.add(
                    SeparatedListAdapter.createItem(
                            "Min bounding box size",
                            "Min bounding box size in pixel, 0 = disabled, 1 = enabled, >1 ="
                                    + " enabled and size in pixel, default = 1"));
            linkedList4.add(
                    SeparatedListAdapter.createItem(
                            "Activation area thickness",
                            "Activation area width in pixel, default = 60"));
            this.mSettingsHelper.posOffsetIsas = 17;
            this.mSettingsHelper.posSingleSwipesBBox = 18;
            this.mSettingsHelper.posSingleSwipesAArea = 19;
            SettingsViewHelper settingsViewHelper4 = this.mSettingsHelper;
            settingsViewHelper4.getClass();
            separatedListAdapter.addSection(
                    "ISAS",
                    settingsViewHelper4
                    .new SettingsSimpleAdapter(
                            getActivity(), linkedList4, this.mSettingsHelper.posOffsetIsas, false));
            i = 20;
        }
        if (iLoadTouchServiceMode > 0) {
            LinkedList linkedList5 = new LinkedList();
            linkedList5.add(
                    SeparatedListAdapter.createItem(
                            "Activation area position", "Activation area position, default = all"));
            linkedList5.add(
                    SeparatedListAdapter.createItem(
                            "Activation area thickness",
                            "Activation area thickness in pixel, default = 50"));
            linkedList5.add(
                    SeparatedListAdapter.createItem(
                            "Activation area length",
                            "Activation area length in pixel, default = 600"));
            linkedList5.add(
                    SeparatedListAdapter.createItem(
                            "Activation area gravity", "Configure the gravity, default = center"));
            linkedList5.add(
                    SeparatedListAdapter.createItem(
                            "Activation area behind keyboard",
                            "Configure if the activation area should be placed behind the keyboard,"
                                    + " default = deactivated"));
            if (Build.VERSION.SDK_INT < 26) {
                linkedList5.add(
                        SeparatedListAdapter.createItem(
                                "Activation area on lockscreen",
                                "Configure if pie should be active on lockscreen, default ="
                                        + " deactivated"));
            }
            linkedList5.add(
                    SeparatedListAdapter.createItem(
                            "Add to pie blacklist", "Exclude an app from pie"));
            linkedList5.add(
                    SeparatedListAdapter.createItem(
                            "Clear pie blacklist", "Clear all excluded apps"));
            int i2 = i + 1;
            this.mSettingsHelper.posOffsetPieActivation = i;
            int i3 = i2 + 1;
            this.mSettingsHelper.posPiePos = i2;
            int i4 = i3 + 1;
            this.mSettingsHelper.posPieAreaX = i3;
            int i5 = i4 + 1;
            this.mSettingsHelper.posPieAreaY = i4;
            int i6 = i5 + 1;
            this.mSettingsHelper.posPieAreaGravity = i5;
            int i7 = i6 + 1;
            this.mSettingsHelper.posPieAreaBehindKeyboard = i6;
            if (Build.VERSION.SDK_INT < 26) {
                this.mSettingsHelper.posPieOnLockScreen = i7;
                i7++;
            }
            int i8 = i7 + 1;
            this.mSettingsHelper.posAddBlacklistPie = i7;
            int i9 = i8 + 1;
            this.mSettingsHelper.posClearBlacklistPie = i8;
            SettingsViewHelper settingsViewHelper5 = this.mSettingsHelper;
            settingsViewHelper5.getClass();
            separatedListAdapter.addSection(
                    "Pie Activation",
                    settingsViewHelper5
                    .new SettingsSimpleAdapter(
                            getActivity(),
                            linkedList5,
                            this.mSettingsHelper.posOffsetPieActivation,
                            false));
            LinkedList linkedList6 = new LinkedList();
            linkedList6.add(
                    SeparatedListAdapter.createItem(
                            "Pie colors",
                            "0 = blue, 1 = red, 2 = grey, 3 = none, 4 = white, 5 = magenta/yellow,"
                                    + " 6 = green/outline, 7 = red/outline, default = 0"));
            linkedList6.add(
                    SeparatedListAdapter.createItem(
                            "Pie inner radius", "Radius in dip, default = 60"));
            linkedList6.add(
                    SeparatedListAdapter.createItem(
                            "Pie outer radius", "Radius in dip, default = 80"));
            linkedList6.add(
                    SeparatedListAdapter.createItem(
                            "Pie shift size",
                            "Size of the pie shift effect in pixel, default = 0"));
            linkedList6.add(
                    SeparatedListAdapter.createItem(
                            "Pie outline size", "Size of the outline in pixel, default = 3"));
            linkedList6.add(
                    SeparatedListAdapter.createItem(
                            "Pie slice gap",
                            "Size of the gap between two pie slices, default = 0"));
            linkedList6.add(
                    SeparatedListAdapter.createItem(
                            "Pie start gap",
                            "Size of the gap between pie and border, default = 0"));
            int i10 = i9 + 1;
            this.mSettingsHelper.posOffsetPieStyle = i9;
            int i11 = i10 + 1;
            this.mSettingsHelper.posPieColor = i10;
            int i12 = i11 + 1;
            this.mSettingsHelper.posPieInnerRadius = i11;
            int i13 = i12 + 1;
            this.mSettingsHelper.posPieOuterRadius = i12;
            int i14 = i13 + 1;
            this.mSettingsHelper.posPieShiftSize = i13;
            int i15 = i14 + 1;
            this.mSettingsHelper.posPieOutlineSize = i14;
            int i16 = i15 + 1;
            this.mSettingsHelper.posPieSliceGap = i15;
            int i17 = i16 + 1;
            this.mSettingsHelper.posPieStartGap = i16;
            SettingsViewHelper settingsViewHelper6 = this.mSettingsHelper;
            settingsViewHelper6.getClass();
            separatedListAdapter.addSection(
                    "Pie Style",
                    settingsViewHelper6
                    .new SettingsSimpleAdapter(
                            getActivity(),
                            linkedList6,
                            this.mSettingsHelper.posOffsetPieStyle,
                            false));
            LinkedList linkedList7 = new LinkedList();
            linkedList7.add(
                    SeparatedListAdapter.createItem(
                            "Longpress time", "Min time for longpress in ms, default = 500"));
            linkedList7.add(
                    SeparatedListAdapter.createItem(
                            "Animation time", "Time for the pie animation in ms, default = 80"));
            linkedList7.add(
                    SeparatedListAdapter.createItem(
                            "Feedback type",
                            "Configure the pie feedback for diffent actions, default = longpress"));
            linkedList7.add(
                    SeparatedListAdapter.createItem(
                            "Vibration Time", "Define the vibration time in ms, default = 30"));
            linkedList7.add(
                    SeparatedListAdapter.createItem(
                            "Multi command",
                            "Configure if actions should be fired multiple times when swiping on"
                                    + " item, default = disabled"));
            linkedList7.add(
                    SeparatedListAdapter.createItem(
                            "Expand trigger area",
                            "Configure if the action trigger area should be expanded to the full"
                                    + " screen for the outer pie ring, default = enabled"));
            int i18 = i17 + 1;
            this.mSettingsHelper.posOffsetPieBehavior = i17;
            int i19 = i18 + 1;
            this.mSettingsHelper.posPieLongpress = i18;
            int i20 = i19 + 1;
            this.mSettingsHelper.posPieAnimation = i19;
            int i21 = i20 + 1;
            this.mSettingsHelper.posPieVibrate = i20;
            int i22 = i21 + 1;
            this.mSettingsHelper.posPieVibrationTime = i21;
            int i23 = i22 + 1;
            this.mSettingsHelper.posPieMultiCommand = i22;
            int i24 = i23 + 1;
            this.mSettingsHelper.posPieExpandTriggerArea = i23;
            SettingsViewHelper settingsViewHelper7 = this.mSettingsHelper;
            settingsViewHelper7.getClass();
            separatedListAdapter.addSection(
                    "Pie Behavior",
                    settingsViewHelper7
                    .new SettingsSimpleAdapter(
                            getActivity(),
                            linkedList7,
                            this.mSettingsHelper.posOffsetPieBehavior,
                            false));
            LinkedList linkedList8 = new LinkedList();
            linkedList8.add(
                    SeparatedListAdapter.createItem(
                            "Rotate images", "Rotate icons based on pie angle"));
            linkedList8.add(
                    SeparatedListAdapter.createItem(
                            "Set nav button style",
                            "Switch between different icon sets for the nav buttons"));
            linkedList8.add(
                    SeparatedListAdapter.createItem(
                            "Show and scale app images",
                            "0 = disabled, 1 = enabled (no filter), 2 = enabled, >2 = image size in"
                                    + " pixel, default = enabled"));
            linkedList8.add(
                    SeparatedListAdapter.createItem(
                            "Show and scale user images",
                            "0 = disabled, 1 = enabled (no filter), 2 = enabled, >2 = image size in"
                                    + " pixel, default = disabled"));
            linkedList8.add(
                    SeparatedListAdapter.createItem(
                            "Set user image search path",
                            "default = /storage/emulated/0/Android/data/com.noname81.lmt/files/"
                                    + " (Leave blank to set default)"));
            int i25 = i24 + 1;
            this.mSettingsHelper.posOffsetPieIcons = i24;
            int i26 = i25 + 1;
            this.mSettingsHelper.posPieRotateImages = i25;
            int i27 = i26 + 1;
            this.mSettingsHelper.posPieNavButtonStyle = i26;
            int i28 = i27 + 1;
            this.mSettingsHelper.posPieShowScaleAppImages = i27;
            int i29 = i28 + 1;
            this.mSettingsHelper.posPieShowScaleUserImages = i28;
            int i30 = i29 + 1;
            this.mSettingsHelper.posPieUserImageSearchPath = i29;
            SettingsViewHelper settingsViewHelper8 = this.mSettingsHelper;
            settingsViewHelper8.getClass();
            separatedListAdapter.addSection(
                    "Pie Icons",
                    settingsViewHelper8
                    .new SettingsSimpleAdapter(
                            getActivity(),
                            linkedList8,
                            this.mSettingsHelper.posOffsetPieIcons,
                            false));
            LinkedList linkedList9 = new LinkedList();
            linkedList9.add(
                    SeparatedListAdapter.createItem(
                            "Pie pointer edge activation",
                            "Configure if the pie pointer feature can be activated from the edges"
                                    + " (Also consider the pie pointer action to activate the"
                                    + " feature!), default = disabled"));
            linkedList9.add(
                    SeparatedListAdapter.createItem(
                            "Pie pointer warp factor",
                            "Configure the warp factor for the pie pointer feature,\n"
                                    + "default = 300%, min = 200%, max = 1000%"));
            linkedList9.add(
                    SeparatedListAdapter.createItem("Pie pointer color", "0 = blue, default = 0"));
            linkedList9.add(
                    SeparatedListAdapter.createItem(
                            "Show status infos",
                            "Configure if status info feature should be shown, default = enabled"));
            linkedList9.add(
                    SeparatedListAdapter.createItem(
                            "Pie status info colors", "0 = white, default = 0"));
            linkedList9.add(
                    SeparatedListAdapter.createItem(
                            "Pie status info font",
                            "Configure the pie font, default = Roboto Light"));
            int i31 = i30 + 1;
            this.mSettingsHelper.posOffsetPieExtensions = i30;
            int i32 = i31 + 1;
            this.mSettingsHelper.posPiePointerFromEdges = i31;
            int i33 = i32 + 1;
            this.mSettingsHelper.posPiePointerWarpFactor = i32;
            int i34 = i33 + 1;
            this.mSettingsHelper.posPiePointerColor = i33;
            int i35 = i34 + 1;
            this.mSettingsHelper.posPieShowStatusInfos = i34;
            this.mSettingsHelper.posPieStatusInfoColor = i35;
            this.mSettingsHelper.posPieStatusInfoFont = i35 + 1;
            SettingsViewHelper settingsViewHelper9 = this.mSettingsHelper;
            settingsViewHelper9.getClass();
            separatedListAdapter.addSection(
                    "Pie Extensions",
                    settingsViewHelper9
                    .new SettingsSimpleAdapter(
                            getActivity(),
                            linkedList9,
                            this.mSettingsHelper.posOffsetPieExtensions,
                            false));
        }
        ListView listView = new ListView(getActivity());
        listView.setAdapter((ListAdapter) separatedListAdapter);
        listView.setOnItemClickListener(this.mSettingsHelper);
        listView.setDividerHeight(0);
        return listView;
    }
}
