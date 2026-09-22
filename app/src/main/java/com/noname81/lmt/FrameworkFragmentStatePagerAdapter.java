package com.noname81.lmt;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;

import androidx.viewpager.widget.PagerAdapter;

import java.util.ArrayList;

/**
 * A {@link PagerAdapter} backed by framework ({@code android.app}) fragments, saving and restoring
 * each page's state as it scrolls out of and back into range.
 *
 * <p>LMT's activities use framework fragments, not AndroidX ones, so the ordinary {@code
 * androidx.fragment} pager adapters do not apply. The original APK pulled this in from {@code
 * android.support.v13.app.FragmentStatePagerAdapter}; that class now only exists in the
 * long-deprecated {@code androidx.legacy:legacy-support-v13} artifact, so it is reproduced here
 * instead of taking on that dependency for one class. Behaviour mirrors the support-library
 * original.
 */
public abstract class FrameworkFragmentStatePagerAdapter extends PagerAdapter {

    private final FragmentManager mFragmentManager;
    private FragmentTransaction mCurTransaction;

    private final ArrayList<Fragment.SavedState> mSavedState = new ArrayList<>();
    private final ArrayList<Fragment> mFragments = new ArrayList<>();
    private Fragment mCurrentPrimaryItem;

    public FrameworkFragmentStatePagerAdapter(FragmentManager fm) {
        mFragmentManager = fm;
    }

    /** Returns the fragment associated with the given position. */
    public abstract Fragment getItem(int position);

    @Override
    public void startUpdate(ViewGroup container) {
        if (container.getId() == View.NO_ID) {
            throw new IllegalStateException(
                    "ViewPager with adapter " + this + " requires a view id");
        }
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        // If a fragment for this position already exists, reuse it as-is.
        if (mFragments.size() > position) {
            Fragment existing = mFragments.get(position);
            if (existing != null) {
                return existing;
            }
        }

        if (mCurTransaction == null) {
            mCurTransaction = mFragmentManager.beginTransaction();
        }

        Fragment fragment = getItem(position);
        if (mSavedState.size() > position) {
            Fragment.SavedState state = mSavedState.get(position);
            if (state != null) {
                fragment.setInitialSavedState(state);
            }
        }
        while (mFragments.size() <= position) {
            mFragments.add(null);
        }
        fragment.setMenuVisibility(false);
        fragment.setUserVisibleHint(false);
        mFragments.set(position, fragment);
        mCurTransaction.add(container.getId(), fragment);
        return fragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        Fragment fragment = (Fragment) object;

        if (mCurTransaction == null) {
            mCurTransaction = mFragmentManager.beginTransaction();
        }
        while (mSavedState.size() <= position) {
            mSavedState.add(null);
        }
        mSavedState.set(
                position,
                fragment.isAdded() ? mFragmentManager.saveFragmentInstanceState(fragment) : null);
        mFragments.set(position, null);
        mCurTransaction.remove(fragment);
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        Fragment fragment = (Fragment) object;
        if (fragment != mCurrentPrimaryItem) {
            if (mCurrentPrimaryItem != null) {
                mCurrentPrimaryItem.setMenuVisibility(false);
                mCurrentPrimaryItem.setUserVisibleHint(false);
            }
            if (fragment != null) {
                fragment.setMenuVisibility(true);
                fragment.setUserVisibleHint(true);
            }
            mCurrentPrimaryItem = fragment;
        }
    }

    @Override
    public void finishUpdate(ViewGroup container) {
        if (mCurTransaction != null) {
            mCurTransaction.commitAllowingStateLoss();
            mCurTransaction = null;
            mFragmentManager.executePendingTransactions();
        }
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return ((Fragment) object).getView() == view;
    }

    @Override
    public Parcelable saveState() {
        Bundle state = null;
        if (!mSavedState.isEmpty()) {
            state = new Bundle();
            Fragment.SavedState[] states = new Fragment.SavedState[mSavedState.size()];
            mSavedState.toArray(states);
            state.putParcelableArray("states", states);
        }
        for (int i = 0; i < mFragments.size(); i++) {
            Fragment fragment = mFragments.get(i);
            if (fragment != null && fragment.isAdded()) {
                if (state == null) {
                    state = new Bundle();
                }
                mFragmentManager.putFragment(state, "f" + i, fragment);
            }
        }
        return state;
    }

    @Override
    public void restoreState(Parcelable state, ClassLoader loader) {
        if (state == null) {
            return;
        }
        Bundle bundle = (Bundle) state;
        bundle.setClassLoader(loader);
        mSavedState.clear();
        mFragments.clear();

        Parcelable[] states = bundle.getParcelableArray("states");
        if (states != null) {
            for (Parcelable parcelable : states) {
                mSavedState.add((Fragment.SavedState) parcelable);
            }
        }
        for (String key : bundle.keySet()) {
            if (!key.startsWith("f")) {
                continue;
            }
            int index = Integer.parseInt(key.substring(1));
            Fragment fragment = mFragmentManager.getFragment(bundle, key);
            if (fragment == null) {
                continue;
            }
            while (mFragments.size() <= index) {
                mFragments.add(null);
            }
            fragment.setMenuVisibility(false);
            mFragments.set(index, fragment);
        }
    }
}
