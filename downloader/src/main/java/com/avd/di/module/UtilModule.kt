package com.avd.di.module

import android.app.DownloadManager
import android.content.Context
import com.avd.util.FileUtil
import com.avd.util.IntentUtil
import com.avd.util.NotificationsHelper
import com.avd.util.SystemUtil
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // Install this module in the SingletonComponent
object UtilModule {

    @Singleton
    @Provides
    fun bindDownloadManager(@ApplicationContext context: Context): DownloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

//    @Singleton
//    @Provides
//    fun bindFileUtil(): FileUtil = FileUtil()

    @Singleton
    @Provides
    fun bindSystemUtil(): SystemUtil = SystemUtil()

    @Singleton
    @Provides
    fun bindIntentUtil(fileUtil: FileUtil): IntentUtil = IntentUtil(fileUtil)

    @Singleton
    @Provides
    fun provideNotificationsHelper(@ApplicationContext context: Context): NotificationsHelper {
        return NotificationsHelper(context)
    }

}
