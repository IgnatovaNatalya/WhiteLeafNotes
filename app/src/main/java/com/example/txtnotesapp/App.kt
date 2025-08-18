package com.example.txtnotesapp

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

import com.example.txtnotesapp.di.koinModule

class App:Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@App)
            modules(koinModule)
        }
    }
}