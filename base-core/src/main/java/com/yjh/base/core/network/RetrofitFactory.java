package com.yjh.base.core.network;

import androidx.annotation.NonNull;
import com.yjh.base.utils.config.AppConfig;
import com.yjh.base.utils.util.LogUtils;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Created by jiahui on 2025/12/26
 */
public class RetrofitFactory {

    private static final String TAG = "RetrofitFactory";

    private static final int DEFAULT_TIMEOUT = 10;

    public static OkHttpClient.Builder getBaseOkHttpBuilder() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        builder.connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
        builder.readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
        builder.writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS);

        // 直接读取 AppConfig.DEBUG_ENABLE
        if (AppConfig.DEBUG_ENABLE) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
                @Override
                public void log(@NonNull String message) {
                    LogUtils.debug(TAG, message);
                }
            });
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            builder.addInterceptor(loggingInterceptor);
        }

        return builder;
    }
}