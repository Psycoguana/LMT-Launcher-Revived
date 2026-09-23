package com.noname81.lmt;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.noname81.lmt.SettingsViewHelper.SettingsSimpleAdapter;

import java.util.LinkedList;

public class SettingsViewActivity extends Activity {
    SettingsViewHelper mSettingsHelper;

    @Override // android.app.Activity
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mSettingsHelper = new SettingsViewHelper(this);
        SeparatedListAdapter separatedListAdapter = new SeparatedListAdapter(this);
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
                        this, linkedList, this.mSettingsHelper.posOffsetTouchservice, false));
        LinkedList linkedList2 = new LinkedList();
        linkedList2.add(
                SeparatedListAdapter.createItem(
                        "Activation area position", "Activation area position, default = all"));
        linkedList2.add(
                SeparatedListAdapter.createItem(
                        "Activation area thickness",
                        "Activation area thickness in pixel, default = 50"));
        linkedList2.add(
                SeparatedListAdapter.createItem(
                        "Activation area length",
                        "Activation area length in pixel, default = 600"));
        linkedList2.add(
                SeparatedListAdapter.createItem(
                        "Activation area gravity", "Configure the gravity, default = center"));
        linkedList2.add(
                SeparatedListAdapter.createItem(
                        "Activation area behind keyboard",
                        "Configure if the activation area should be placed behind the keyboard,"
                                + " default = deactivated"));
        if (Build.VERSION.SDK_INT < 26) {
            linkedList2.add(
                    SeparatedListAdapter.createItem(
                            "Activation area on lockscreen",
                            "Configure if pie should be active on lockscreen, default ="
                                    + " deactivated"));
        }
        linkedList2.add(
                SeparatedListAdapter.createItem("Add to pie blacklist", "Exclude an app from pie"));
        linkedList2.add(
                SeparatedListAdapter.createItem("Clear pie blacklist", "Clear all excluded apps"));
        this.mSettingsHelper.posOffsetPieActivation = 4;
        this.mSettingsHelper.posPiePos = 5;
        this.mSettingsHelper.posPieAreaX = 6;
        this.mSettingsHelper.posPieAreaY = 7;
        this.mSettingsHelper.posPieAreaGravity = 8;
        this.mSettingsHelper.posPieAreaBehindKeyboard = 9;
        int pos = 10;
        if (Build.VERSION.SDK_INT < 26) {
            this.mSettingsHelper.posPieOnLockScreen = 10;
            pos = 11;
        }
        this.mSettingsHelper.posAddBlacklistPie = pos++;
        this.mSettingsHelper.posClearBlacklistPie = pos++;
        SettingsViewHelper settingsViewHelper2 = this.mSettingsHelper;
        settingsViewHelper2.getClass();
        separatedListAdapter.addSection(
                "Pie Activation",
                settingsViewHelper2
                .new SettingsSimpleAdapter(
                        this, linkedList2, this.mSettingsHelper.posOffsetPieActivation, false));
        LinkedList linkedList3 = new LinkedList();
        linkedList3.add(
                SeparatedListAdapter.createItem(
                        "Pie colors",
                        "0 = blue, 1 = red, 2 = grey, 3 = none, 4 = white, 5 = magenta/yellow, 6 ="
                                + " green/outline, 7 = red/outline, default = 0"));
        linkedList3.add(
                SeparatedListAdapter.createItem("Pie inner radius", "Radius in dip, default = 60"));
        linkedList3.add(
                SeparatedListAdapter.createItem("Pie outer radius", "Radius in dip, default = 80"));
        linkedList3.add(
                SeparatedListAdapter.createItem(
                        "Pie shift size", "Size of the pie shift effect in pixel, default = 0"));
        linkedList3.add(
                SeparatedListAdapter.createItem(
                        "Pie outline size", "Size of the outline in pixel, default = 3"));
        linkedList3.add(
                SeparatedListAdapter.createItem(
                        "Pie slice gap", "Size of the gap between two pie slices, default = 0"));
        linkedList3.add(
                SeparatedListAdapter.createItem(
                        "Pie start gap", "Size of the gap between pie and border, default = 0"));
        this.mSettingsHelper.posOffsetPieStyle = pos++;
        this.mSettingsHelper.posPieColor = pos++;
        this.mSettingsHelper.posPieInnerRadius = pos++;
        this.mSettingsHelper.posPieOuterRadius = pos++;
        this.mSettingsHelper.posPieShiftSize = pos++;
        this.mSettingsHelper.posPieOutlineSize = pos++;
        this.mSettingsHelper.posPieSliceGap = pos++;
        this.mSettingsHelper.posPieStartGap = pos++;
        SettingsViewHelper settingsViewHelper3 = this.mSettingsHelper;
        settingsViewHelper3.getClass();
        separatedListAdapter.addSection(
                "Pie Style",
                settingsViewHelper3
                .new SettingsSimpleAdapter(
                        this, linkedList3, this.mSettingsHelper.posOffsetPieStyle, false));
        LinkedList linkedList4 = new LinkedList();
        linkedList4.add(
                SeparatedListAdapter.createItem(
                        "Longpress time", "Min time for longpress in ms, default = 500"));
        linkedList4.add(
                SeparatedListAdapter.createItem(
                        "Animation time", "Time for the pie animation in ms, default = 80"));
        linkedList4.add(
                SeparatedListAdapter.createItem(
                        "Feedback type",
                        "Configure the pie feedback for diffent actions, default = longpress"));
        linkedList4.add(
                SeparatedListAdapter.createItem(
                        "Vibration Time", "Define the vibration time in ms, default = 30"));
        linkedList4.add(
                SeparatedListAdapter.createItem(
                        "Multi command",
                        "Configure if actions should be fired multiple times when swiping on item,"
                                + " default = disabled"));
        linkedList4.add(
                SeparatedListAdapter.createItem(
                        "Expand trigger area",
                        "Configure if the action trigger area should be expanded to the full screen"
                                + " for the outer pie ring, default = enabled"));
        this.mSettingsHelper.posOffsetPieBehavior = pos++;
        this.mSettingsHelper.posPieLongpress = pos++;
        this.mSettingsHelper.posPieAnimation = pos++;
        this.mSettingsHelper.posPieVibrate = pos++;
        this.mSettingsHelper.posPieVibrationTime = pos++;
        this.mSettingsHelper.posPieMultiCommand = pos++;
        this.mSettingsHelper.posPieExpandTriggerArea = pos++;
        SettingsViewHelper settingsViewHelper4 = this.mSettingsHelper;
        settingsViewHelper4.getClass();
        separatedListAdapter.addSection(
                "Pie Behavior",
                settingsViewHelper4
                .new SettingsSimpleAdapter(
                        this, linkedList4, this.mSettingsHelper.posOffsetPieBehavior, false));
        LinkedList linkedList5 = new LinkedList();
        linkedList5.add(
                SeparatedListAdapter.createItem(
                        "Rotate images", "Rotate icons based on pie angle"));
        linkedList5.add(
                SeparatedListAdapter.createItem(
                        "Set nav button style",
                        "Switch between different icon sets for the nav buttons"));
        linkedList5.add(
                SeparatedListAdapter.createItem(
                        "Show and scale app images",
                        "0 = disabled, 1 = enabled (no filter), 2 = enabled, >2 = image size in"
                                + " pixel, default = enabled"));
        linkedList5.add(
                SeparatedListAdapter.createItem(
                        "Show and scale user images",
                        "0 = disabled, 1 = enabled (no filter), 2 = enabled, >2 = image size in"
                                + " pixel, default = disabled"));
        linkedList5.add(
                SeparatedListAdapter.createItem(
                        "Set user image search path",
                        "default = /storage/emulated/0/Android/data/com.noname81.lmt/files/ (Leave"
                                + " blank to set default)"));
        this.mSettingsHelper.posOffsetPieIcons = pos++;
        this.mSettingsHelper.posPieRotateImages = pos++;
        this.mSettingsHelper.posPieNavButtonStyle = pos++;
        this.mSettingsHelper.posPieShowScaleAppImages = pos++;
        this.mSettingsHelper.posPieShowScaleUserImages = pos++;
        this.mSettingsHelper.posPieUserImageSearchPath = pos++;
        SettingsViewHelper settingsViewHelper5 = this.mSettingsHelper;
        settingsViewHelper5.getClass();
        separatedListAdapter.addSection(
                "Pie Icons",
                settingsViewHelper5
                .new SettingsSimpleAdapter(
                        this, linkedList5, this.mSettingsHelper.posOffsetPieIcons, false));
        LinkedList linkedList6 = new LinkedList();
        linkedList6.add(
                SeparatedListAdapter.createItem(
                        "Pie pointer edge activation",
                        "Configure if the pie pointer feature can be activated from the edges,"
                                + " default = disabled"));
        linkedList6.add(
                SeparatedListAdapter.createItem(
                        "Pie pointer warp factor",
                        "Configure the warp factor for the pie pointer feature,\n"
                                + "default = 300%, min = 200%, max = 1000%"));
        linkedList6.add(
                SeparatedListAdapter.createItem("Pie pointer color", "0 = blue, default = 0"));
        linkedList6.add(
                SeparatedListAdapter.createItem(
                        "Show status infos",
                        "Configure if status info feature should be shown, default = enabled"));
        linkedList6.add(
                SeparatedListAdapter.createItem(
                        "Pie status info colors", "0 = white, default = 0"));
        linkedList6.add(
                SeparatedListAdapter.createItem(
                        "Pie status info font", "Configure the pie font, default = Roboto Light"));
        this.mSettingsHelper.posOffsetPieExtensions = pos++;
        this.mSettingsHelper.posPiePointerFromEdges = pos++;
        this.mSettingsHelper.posPiePointerWarpFactor = pos++;
        this.mSettingsHelper.posPiePointerColor = pos++;
        this.mSettingsHelper.posPieShowStatusInfos = pos++;
        this.mSettingsHelper.posPieStatusInfoColor = pos++;
        this.mSettingsHelper.posPieStatusInfoFont = pos++;
        SettingsViewHelper settingsViewHelper6 = this.mSettingsHelper;
        settingsViewHelper6.getClass();
        separatedListAdapter.addSection(
                "Pie Extensions",
                settingsViewHelper6
                .new SettingsSimpleAdapter(
                        this, linkedList6, this.mSettingsHelper.posOffsetPieExtensions, false));
        ListView listView = new ListView(this);
        listView.setAdapter((ListAdapter) separatedListAdapter);
        listView.setOnItemClickListener(this.mSettingsHelper);
        listView.setDividerHeight(0);
        setContentView(listView);
    }
}
