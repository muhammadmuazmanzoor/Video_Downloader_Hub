package com.avd.di.qualifier

import javax.inject.Qualifier

@Qualifier
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
annotation class RemoteData


@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BaseUrl1

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BaseUrl2

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SocialDownloaderBaseUrl