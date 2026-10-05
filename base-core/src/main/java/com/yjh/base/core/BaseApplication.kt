package com.yjh.base.core

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context

/**
 * Created by youjiahui on 2026/10/2
 */
class BaseApplication : Application() {
    companion object{
        
        @SuppressLint("StaticFieldLeak")
        private lateinit var context: Context
        
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: BaseApplication
        
        fun getAppContext(): Context=context
        
        fun getInstance(): BaseApplication=instance
        
    }

    override fun onCreate() {
        super.onCreate()
        context=applicationContext
        instance=this
    }
}