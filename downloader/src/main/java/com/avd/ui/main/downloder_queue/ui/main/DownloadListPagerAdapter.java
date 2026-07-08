/*
 * Copyright (C) 2019-2021 Tachibana General Laboratories, LLC
 * Copyright (C) 2019-2021 Yaroslav Pronin <proninyaroslav@mail.ru>
 *
 * This file is part of Download Navi.
 *
 * Download Navi is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Download Navi is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Download Navi.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.avd.ui.main.downloder_queue.ui.main;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.avd.ui.main.progress.ProgressFragment;
import com.avd.ui.main.video.VideoFragment;

public class DownloadListPagerAdapter extends FragmentStateAdapter {
    @ViewPager2.OffscreenPageLimit
    public static final int NUM_FRAGMENTS = 2;
    public static final int QUEUED_FRAG_POS = 0;
    public static final int COMPLETED_FRAG_POS = 1;

    private final boolean showLatestCompletedDownloadsFirst;

    public DownloadListPagerAdapter(@NonNull Fragment fragment) {
        this(fragment, false);
    }

    public DownloadListPagerAdapter(@NonNull Fragment fragment, boolean showLatestCompletedDownloadsFirst) {
        super(fragment);
        this.showLatestCompletedDownloadsFirst = showLatestCompletedDownloadsFirst;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        /* Stubs */
        switch (position) {
            case QUEUED_FRAG_POS:
                return ProgressFragment.Companion.newInstance();
            case COMPLETED_FRAG_POS:
                return VideoFragment.Companion.newInstance(showLatestCompletedDownloadsFirst);
            default:
                return new Fragment();
        }
    }

    @Override
    public int getItemCount() {
        return NUM_FRAGMENTS;
    }
}
