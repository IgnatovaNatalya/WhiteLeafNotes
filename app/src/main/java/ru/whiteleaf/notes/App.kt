package ru.whiteleaf.notes

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

import ru.whiteleaf.notes.di.koinModule
import ru.whiteleaf.notes.domain.repository.EncryptionRepository
import ru.whiteleaf.notes.domain.repository.SecurityPreferences

class App:Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@App)
            modules(koinModule)
        }
        // Очищаем состояния безопасности при перезапуске приложения
        clearSecurityStates()

        // Следим за жизненным циклом приложения для блокировки при уходе в фон
        setupAppLifecycleObserver()

    }

    private fun clearSecurityStates() {
        try {
            val securityPreferences: SecurityPreferences = org.koin.java.KoinJavaComponent.get(SecurityPreferences::class.java)
            val encryptionRepository: EncryptionRepository = org.koin.java.KoinJavaComponent.get(EncryptionRepository::class.java)

            securityPreferences.clearUnlockedState()
            encryptionRepository.clearAllKeys()
        } catch (e: Exception) {
            // Игнорируем ошибки, если Koin еще не инициализирован
        }
    }

    private fun setupAppLifecycleObserver() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleObserver())
    }
}