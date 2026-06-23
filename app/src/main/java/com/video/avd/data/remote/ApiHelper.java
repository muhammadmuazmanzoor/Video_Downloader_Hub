package com.video.avd.data.remote;


import com.video.avd.utils.chromecast.constent.CastConstant;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class ApiHelper implements ApiHelperInterface {
    private static ApiHelper mInstance;
    private ApiHelper() {
    }
    public static ApiHelper getInstance() {
        if (mInstance == null) {
            return new ApiHelper();
        }
        return mInstance;
    }
    private OkHttpClient getOkHttpRequest() {
        return new OkHttpClient().newBuilder()
                .connectTimeout(CastConstant.CONNECT_TIMEOUT_NETWORK, TimeUnit.SECONDS)
                .readTimeout(CastConstant.CONNECT_TIMEOUT_NETWORK, TimeUnit.SECONDS)
                .writeTimeout(CastConstant.CONNECT_TIMEOUT_NETWORK, TimeUnit.SECONDS)
                .build();
    }
}
