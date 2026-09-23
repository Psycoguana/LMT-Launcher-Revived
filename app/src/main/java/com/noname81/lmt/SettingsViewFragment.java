package com.noname81.lmt;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.noname81.lmt.SettingsViewHelper.SettingsSimpleAdapter;

import java.util.LinkedList;

/**
 * The main settings tab.
 *
 * <p>Rows are built into sections and each is assigned a position index, which {@link
 * SettingsViewHelper} switches on to handle taps. The visible set depends on the feature mode, so
 * the indices are assigned as rows are added rather than fixed.
 */
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
        this.mSettingsHelper = new SettingsViewHelper(getActivity(), this);
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
        int pos = 4;
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
            pos = 20;
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
            this.mSettingsHelper.posOffsetPieActivation = pos++;
            this.mSettingsHelper.posPiePos = pos++;
            this.mSettingsHelper.posPieAreaX = pos++;
            this.mSettingsHelper.posPieAreaY = pos++;
            this.mSettingsHelper.posPieAreaGravity = pos++;
            this.mSettingsHelper.posPieAreaBehindKeyboard = pos++;
            if (Build.VERSION.SDK_INT < 26) {
                this.mSettingsHelper.posPieOnLockScreen = pos++;
            }
            this.mSettingsHelper.posAddBlacklistPie = pos++;
            this.mSettingsHelper.posClearBlacklistPie = pos++;
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
            this.mSettingsHelper.posOffsetPieStyle = pos++;
            this.mSettingsHelper.posPieColor = pos++;
            this.mSettingsHelper.posPieInnerRadius = pos++;
            this.mSettingsHelper.posPieOuterRadius = pos++;
            this.mSettingsHelper.posPieShiftSize = pos++;
            this.mSettingsHelper.posPieOutlineSize = pos++;
            this.mSettingsHelper.posPieSliceGap = pos++;
            this.mSettingsHelper.posPieStartGap = pos++;
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
            this.mSettingsHelper.posOffsetPieBehavior = pos++;
            this.mSettingsHelper.posPieLongpress = pos++;
            this.mSettingsHelper.posPieAnimation = pos++;
            this.mSettingsHelper.posPieVibrate = pos++;
            this.mSettingsHelper.posPieVibrationTime = pos++;
            this.mSettingsHelper.posPieMultiCommand = pos++;
            this.mSettingsHelper.posPieExpandTriggerArea = pos++;
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
            this.mSettingsHelper.posOffsetPieIcons = pos++;
            this.mSettingsHelper.posPieRotateImages = pos++;
            this.mSettingsHelper.posPieNavButtonStyle = pos++;
            this.mSettingsHelper.posPieShowScaleAppImages = pos++;
            this.mSettingsHelper.posPieShowScaleUserImages = pos++;
            this.mSettingsHelper.posPieUserImageSearchPath = pos++;
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
            this.mSettingsHelper.posOffsetPieExtensions = pos++;
            this.mSettingsHelper.posPiePointerFromEdges = pos++;
            this.mSettingsHelper.posPiePointerWarpFactor = pos++;
            this.mSettingsHelper.posPiePointerColor = pos++;
            this.mSettingsHelper.posPieShowStatusInfos = pos++;
            this.mSettingsHelper.posPieStatusInfoColor = pos++;
            this.mSettingsHelper.posPieStatusInfoFont = pos++;
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
        LinkedList linkedList10 = new LinkedList();
        linkedList10.add(
                SeparatedListAdapter.createItem(
                        "Export settings", "Save every setting and pie icon to a zip file"));
        linkedList10.add(
                SeparatedListAdapter.createItem(
                        "Import settings", "Restore every setting and pie icon from a zip file"));
        this.mSettingsHelper.posOffsetBackup = pos++;
        this.mSettingsHelper.posExportSettings = pos++;
        this.mSettingsHelper.posImportSettings = pos++;
        SettingsViewHelper settingsViewHelper10 = this.mSettingsHelper;
        settingsViewHelper10.getClass();
        separatedListAdapter.addSection(
                "Backup",
                settingsViewHelper10
                .new SettingsSimpleAdapter(
                        getActivity(),
                        linkedList10,
                        this.mSettingsHelper.posOffsetBackup,
                        false));
        ListView listView = new ListView(getActivity());
        listView.setAdapter((ListAdapter) separatedListAdapter);
        listView.setOnItemClickListener(this.mSettingsHelper);
        listView.setDividerHeight(0);
        return listView;
    }

    @Override // android.app.Fragment
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || data == null) {
            return;
        }
        if (requestCode == SettingsViewHelper.REQUEST_EXPORT_SETTINGS) {
            this.mSettingsHelper.handleExportSettingsPicked(data.getData());
        } else if (requestCode == SettingsViewHelper.REQUEST_IMPORT_SETTINGS) {
            this.mSettingsHelper.handleImportSettingsPicked(data.getData());
        }
    }
}
