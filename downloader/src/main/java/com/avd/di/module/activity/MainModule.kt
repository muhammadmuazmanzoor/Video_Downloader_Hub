package com.avd.di.module.activity

import com.avd.ui.main.help.HelpFragment
import com.avd.ui.main.history.HistoryFragment
import com.avd.ui.main.link.LinkFragment
import com.avd.ui.main.progress.ProgressFragment
import com.avd.ui.main.proxies.ProxiesFragment
import com.avd.ui.main.settings.SettingsFragmentDownloader
import com.avd.ui.main.video.VideoFragment
import com.avd.util.fragment.FragmentFactory
import com.avd.util.fragment.FragmentFactoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped
import dagger.hilt.android.scopes.FragmentScoped

@Module
@InstallIn(ActivityComponent::class) // Hilt's replacement for custom scopes
abstract class MainModule {

    @FragmentScoped
    @Binds
    abstract fun bindProxiesFragment(proxiesFragment: ProxiesFragment): ProxiesFragment

    @FragmentScoped
    @Binds
    abstract fun bindHistoryFragment(historyFragment: HistoryFragment): HistoryFragment

    @FragmentScoped
    @Binds
    abstract fun bindHelpFragment(helpFragment: HelpFragment): HelpFragment

    @FragmentScoped
    @Binds
    abstract fun bindProgressFragment(progressFragment: ProgressFragment): ProgressFragment

    @FragmentScoped
    @Binds
    abstract fun bindVideoFragment(videoFragment: VideoFragment): VideoFragment

    @FragmentScoped
    @Binds
    abstract fun bindSettingsFragment(settingsFragment: SettingsFragmentDownloader): SettingsFragmentDownloader

    @FragmentScoped
    @Binds
    abstract fun bindLinkFragment(linkFragment: LinkFragment): LinkFragment

    @ActivityScoped
    @Binds
    abstract fun bindFragmentFactory(fragmentFactory: FragmentFactoryImpl): FragmentFactory
}
