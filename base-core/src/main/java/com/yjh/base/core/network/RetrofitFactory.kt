package com.yjh.base.core.network

import com.yjh.base.utils.config.AppConfig
import com.yjh.base.utils.util.LogUtils
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * Created by youjiahui on 2026/10/3
 */
object RetrofitFactory {

    private const val TAG = "RetrofitFactory"
    private const val DEFAULT_TIMEOUT = 10L

    fun getBaseOkHttpBuilder(): OkHttpClient.Builder {

        val builder = OkHttpClient.Builder().apply {
            connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
        }

        if (AppConfig.DEBUG_ENABLE) {
            val loggingInterceptor = HttpLoggingInterceptor { message ->
                LogUtils.debug(TAG, message)
            }.apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(loggingInterceptor)
        }

        return builder

    }

}