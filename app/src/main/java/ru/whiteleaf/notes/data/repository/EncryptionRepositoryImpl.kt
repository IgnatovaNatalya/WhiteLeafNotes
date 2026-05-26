package ru.whiteleaf.notes.data.repository

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import ru.whiteleaf.notes.domain.repository.EncryptionRepository
import java.io.ByteArrayOutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.coroutines.resume

class EncryptionRepositoryImpl(private val keyStore: KeyStore) : EncryptionRepository {

    // Храним идентификаторы разблокированных блокнотов (для быстрой проверки,
    // но реальную проверку делает Keystore при каждой операции)
    private val unlockedNotebooks = mutableSetOf<String>()

    // Вспомогательный метод для получения SecretKey из Keystore
    private fun getSecretKey(notebookId: String): SecretKey {
        val alias = "notebook_$notebookId"
        return (keyStore.getKey(alias, null) as? SecretKey)
            ?: throw IllegalStateException("Key for notebook $notebookId not found")
    }

    // Создание нового ключа для блокнота
//    override fun createKeyForNotebook(notebookPath: String): Result<Unit> {
//        return try {
//            val alias = "notebook_$notebookPath"
//            if (keyStore.containsAlias(alias)) {
//                return Result.success(Unit) // или можно вернуть ошибку, но обычно просто игнорируем
//            }
//            val keyGenerator =
//                KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
//            val spec = KeyGenParameterSpec.Builder(
//                alias,
//                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
//            )
//                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
//                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
//                .setUserAuthenticationRequired(true)
//                .setUserAuthenticationValidityDurationSeconds(60) // ключ остаётся разблокированным 60 секунд после биометрии
//                .build()
//            keyGenerator.init(spec)
//            keyGenerator.generateKey()
//            Result.success(Unit)
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }

    override fun createKeyForNotebook(notebookPath: String) {
        try {
            val alias = "notebook_$notebookPath"
            if (keyStore.containsAlias(alias)) return
            val keyGenerator =
                KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val spec = KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(true)
                .setUserAuthenticationValidityDurationSeconds(60)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        } catch (e: Exception) {
            throw RuntimeException("Failed to create key for notebook $notebookPath", e)
        }
    }

    override fun hasKey(notebookPath: String): Boolean {
        val alias = "notebook_$notebookPath"
        return keyStore.containsAlias(alias)
    }

    override fun isUnlocked(notebookPath: String): Boolean {
        return unlockedNotebooks.contains(notebookPath)
    }

    // Асинхронная разблокировка через биометрию

    override suspend fun unlockNotebook(notebookId: String, context: Context): Boolean {
        return suspendCancellableCoroutine { continuation ->
            val key = getSecretKey(notebookId)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            // НЕ вызываем cipher.init() здесь! Это делает система после аутентификации.
            val cryptoObject = BiometricPrompt.CryptoObject(cipher)

            val executor = ContextCompat.getMainExecutor(context)
            val biometricPrompt = BiometricPrompt(
                context as FragmentActivity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        // Аутентификация успешна – ключ разблокирован системой
                        unlockedNotebooks.add(notebookId)
                        continuation.resume(true)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        // Ошибка или отмена пользователем
                        continuation.resume(false)
                    }

                    override fun onAuthenticationFailed() {
                        // Неудачная попытка (палец не распознан)
                        continuation.resume(false)
                    }
                }
            )

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Подтвердите личность")
                .setSubtitle("Для доступа к защищённому блокноту")
                .setNegativeButtonText("Отмена")
                .build()

            try {
                biometricPrompt.authenticate(promptInfo, cryptoObject)
            } catch (e: Exception) {
                // Например, если нет биометрии на устройстве
                continuation.resume(false)
            }
        }
    }

//    override suspend fun unlockNotebook(notebookId: String, context: Context): Boolean {
//        return suspendCancellableCoroutine { continuation ->
//            val key = getSecretKey(notebookId)
//            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
//            // Инициализируем Cipher в режиме DECRYPT_MODE – именно здесь система запросит биометрию
//            val cryptoObject = BiometricPrompt.CryptoObject(cipher)
//
//            val executor = ContextCompat.getMainExecutor(context)
//            val biometricPrompt = BiometricPrompt(
//                context as FragmentActivity,
//                executor,
//                object : BiometricPrompt.AuthenticationCallback() {
//                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
//                        // Аутентификация успешна – ключ разблокирован на заданное время
//                        unlockedNotebooks.add(notebookId)
//                        continuation.resume(true)
//                    }
//
//                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
//                        continuation.resume(false)
//                    }
//
//                    override fun onAuthenticationFailed() {
//                        continuation.resume(false)
//                    }
//                }
//            )
//
//            val promptInfo = BiometricPrompt.PromptInfo.Builder()
//                .setTitle("Разблокировка блокнота")
//                .setSubtitle("Подтвердите доступ с помощью отпечатка пальца")
//                .setNegativeButtonText("Отмена")
//                .build()
//
//            try {
//                // Пытаемся инициализировать Cipher – для этого потребуется биометрия
//                cipher.init(Cipher.DECRYPT_MODE, key)
//                biometricPrompt.authenticate(promptInfo, cryptoObject)
//            } catch (e: Exception) {
//                // Если ключ уже разблокирован (например, из-за предыдущей аутентификации),
//                // то исключения не будет, и мы можем считать блокнот разблокированным
//                if (e.message?.contains("user authentication required") == false) {
//                    unlockedNotebooks.add(notebookId)
//                    continuation.resume(true)
//                } else {
//                    continuation.resume(false)
//                }
//            }
//        }
//    }

    override fun lockNotebook(notebookPath: String) {
        unlockedNotebooks.remove(notebookPath)
    }

    override fun lockAllNotebooks() {
        unlockedNotebooks.clear()
    }

    // Шифрование строки -> Base64 (с сохранением IV)
    override suspend fun encryptNote(notebookPath: String, plaintext: String): String {
        val key = getSecretKey(notebookPath)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        // Для шифрования биометрия не нужна, но ключ должен быть разблокирован (если требуется)
        // Если ключ требует аутентификации и не разблокирован, вызов init выбросит исключение
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv // получаем IV, сгенерированный автоматически
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        // Склеиваем IV + шифротекст и кодируем в Base64
        val combined = ByteArrayOutputStream().apply {
            write(iv)
            write(ciphertext)
        }.toByteArray()
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    // Расшифровка строки из Base64 (ожидается IV + шифротекст)
    override suspend fun decryptNote(notebookId: String, ciphertextBase64: String): String {
        val combined = Base64.decode(ciphertextBase64, Base64.DEFAULT)
        // Первые 12 байт – IV (для GCM стандартный размер 12)
        val iv = combined.copyOfRange(0, 12)
        val ciphertext = combined.copyOfRange(12, combined.size)

        val key = getSecretKey(notebookId)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)
        val plaintext = cipher.doFinal(ciphertext)
        return String(plaintext, Charsets.UTF_8)
    }

    override fun deleteKeyForNotebook(notebookId: String) {
        val alias = "notebook_$notebookId"
        keyStore.deleteEntry(alias)
        lockNotebook(notebookId) // также очищаем флаг разблокировки
    }
}