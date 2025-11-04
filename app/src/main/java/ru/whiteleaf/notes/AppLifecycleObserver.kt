package ru.whiteleaf.notes

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import org.koin.java.KoinJavaComponent
import ru.whiteleaf.notes.domain.repository.EncryptionRepository
import ru.whiteleaf.notes.domain.repository.SecurityPreferences

class AppLifecycleObserver : DefaultLifecycleObserver {

    override fun onStop(owner: LifecycleOwner) {
        // При уходе приложения в фон блокируем все защищенные блокноты
        val encryptionRepository: EncryptionRepository = KoinJavaComponent.get(EncryptionRepository::class.java)
        val securityPreferences: SecurityPreferences = KoinJavaComponent.get(SecurityPreferences::class.java)

        // Очищаем все ключи из памяти, но сохраняем состояние в SharedPreferences
        encryptionRepository.clearAllKeys()
    }

    override fun onStart(owner: LifecycleOwner) {
        // При возвращении приложения из фона - все блокноты остаются заблокированными
        // Пользователь должен будет пройти аутентификацию заново
    }
}