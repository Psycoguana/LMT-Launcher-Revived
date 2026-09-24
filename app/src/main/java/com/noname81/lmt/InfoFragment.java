package com.noname81.lmt;

import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

/** About tab, and the hidden entry point to the debug menu. */
public class InfoFragment extends Fragment {

    @Override // androidx.fragment.app.Fragment
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        setUserVisibleHint(true);
    }

    @Override // androidx.fragment.app.Fragment
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        Spanned spannedFromHtml;
        super.onCreateView(layoutInflater, viewGroup, bundle);
        TextView textView = new TextView(getActivity());
        textView.setGravity(17);
        textView.setLinksClickable(true);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        String html =
                "LMT "
                        + SettingsValues.getInstance(getActivity()).getVersion()
                        + " by noname81<br><br>This version of LMT will never expire!<br><br><a"
                        + " href='http://forum.xda-developers.com/showthread.php?t=1330150'>Visit"
                        + " the thread at XDA developers!</a><br><br><a"
                        + " href='https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=9C5JKBHDM6QSE'>If"
                        + " you like my work, please consider a donation!</a><br><br>Runtime"
                        + " Permissions:<br>Root: "
                        + RootContext.getInstance(getActivity()).isRootAvailable(false)
                        + "<br>Accessibility: "
                        + AccessibilityHandler.isAccessibilityAvailable(getActivity(), false)
                        + "<br>DrawOverApps: "
                        + PermissionChecker.getInstance()
                                .hasDrawOverAppsPermission(getActivity(), false)
                        + "<br>ExternalStorageRead: "
                        + PermissionChecker.getInstance()
                                .hasExternalStorageReadPermission(getActivity(), false)
                        + "<br>ExternalStorageWrite: "
                        + PermissionChecker.getInstance()
                                .hasExternalStorageWritePermission(getActivity(), false)
                        + "<br>PhoneCalls: "
                        + PermissionChecker.getInstance()
                                .hasPhoneCallPermission(getActivity(), false)
                        + "<br>UsageStats: "
                        + PermissionChecker.getInstance()
                                .hasUsageStatsPermission(getActivity(), false)
                        + "<br><br>";
        if (Build.VERSION.SDK_INT >= 24) {
            spannedFromHtml = Html.fromHtml(html, 0);
        } else {
            spannedFromHtml = Html.fromHtml(html);
        }
        textView.setText(spannedFromHtml);
        addGestureListener(textView);
        FrameLayout frameLayout = new FrameLayout(getActivity());
        frameLayout.addView(textView);
        return frameLayout;
    }

    /**
     * Five double-taps anywhere on the Info tab opens the hidden debug menu, which toggles the
     * per-component logging that {@code DebugHelper} drives.
     */
    private void addGestureListener(View view) {
        final GestureDetector gestureDetector =
                new GestureDetector(
                        getActivity(),
                        new GestureDetector.SimpleOnGestureListener() { // from class:
                            // com.noname81.lmt.InfoFragment.1GestureListener
                            private int count = 0;

                            @Override // android.view.GestureDetector.SimpleOnGestureListener,
                            // android.view.GestureDetector.OnDoubleTapListener
                            public boolean onDoubleTap(MotionEvent event) {
                                int taps = this.count + 1;
                                this.count = taps;
                                if (taps < 5) { // five double-taps reveals it
                                    return false;
                                }
                                DebugHelper.getInstance()
                                        .showDebugMenu(InfoFragment.this.getActivity());
                                return false;
                            }
                        });
        view.setOnTouchListener(
                new View.OnTouchListener() { // from class: com.noname81.lmt.InfoFragment.1
                    @Override // android.view.View.OnTouchListener
                    public boolean onTouch(View touched, MotionEvent event) {
                        gestureDetector.onTouchEvent(event);
                        return false;
                    }
                });
    }
}
