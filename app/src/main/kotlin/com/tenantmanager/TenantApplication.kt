package com.tenantmanager

import android.app.Application
import com.tenantmanager.di.AppContainer
import com.tenantmanager.di.DefaultAppContainer

class TenantApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
