package com.example.whiteleafnotes

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

import com.example.whiteleafnotes.di.koinModule

class App:Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@App)
            modules(koinModule)
        }
    }
}