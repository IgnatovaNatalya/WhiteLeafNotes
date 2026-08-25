package ru.whiteleaf.notes.di


import android.content.ContentResolver
import android.content.Context
import com.google.gson.Gson
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import ru.whiteleaf.notes.common.AppConstants.WHITE_LEAF_PREFS
import java.security.KeyStore

val appModule = module {

    // App
    single<ContentResolver> { androidContext().contentResolver }

    //prefs
    single {
        androidContext().getSharedPreferences(
            WHITE_LEAF_PREFS,
            Context.MODE_PRIVATE
        )
    }

    //keystore
    single {
        KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }
    }

    //Gson
    single<Gson> { Gson() }

}

