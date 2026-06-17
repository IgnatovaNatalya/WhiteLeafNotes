package ru.whiteleaf.notes.data.repository

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.UserNotAuthenticatedException
import android.util.Base64
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import ru.whiteleaf.notes.domain.repository.EncryptionRepository
import ru.whiteleaf.notes.domain.repository.AuthenticationRequiredException
import java.io.ByteArrayOutputStream
import java.security.InvalidKeyException
import java.security.KeyStore
import java.util.concurrent.atomic.AtomicBoolean
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.coroutines.resume

const val NOTEBOOK_KEY_ALIAS_PREFIX = "notebook_"
const val NOTEBOOK_KEY_AUTH_DURATION = 10

class EncryptionRepositoryImpl(private val keyStore: KeyStore) : EncryptionRepository {

    // Храним идентификаторы разблокированных блокнотов (для быстрой проверки,
    // но реальную проверку делает Keystore при каждой операции)
    private val unlockedNotebooks = mutableSetOf<String>()

    // Вспомогательный метод для получения SecretKey из Keystore
    private fun getSecretKey(notebookPath: String): SecretKey {

        //val alias = "notebook_$notebookPath"
        val alias = NOTEBOOK_KEY_ALIAS_PREFIX + notebookPath

        return (keyStore.getKey(alias, null) as? SecretKey)
            ?: throw IllegalStateException("Key for notebook $notebookPath not found")
    }

    override fun createKeyForNotebook(notebookPath: String) {
        try {

            val alias = NOTEBOOK_KEY_ALIAS_PREFIX + notebookPath

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
                .setUserAuthenticationValidityDurationSeconds(NOTEBOOK_KEY_AUTH_DURATION) // 0 — значит, каждый раз нужна свежая биометрия
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
            println("DEBUG: EncryptionRepo: created key $alias")
        } catch (e: Exception) {
            println("DEBUG: EncryptionRepo: Failed to create key for notebook $notebookPath")
            throw RuntimeException("Failed to create key for notebook $notebookPath", e)
        }
    }

    override fun hasKey(notebookPath: String): Boolean {
//        val alias = "notebook_$notebookPath"
        val alias = NOTEBOOK_KEY_ALIAS_PREFIX + notebookPath
        return keyStore.containsAlias(alias)
    }

    override fun isUnlocked(notebookPath: String): Boolean {
        return unlockedNotebooks.contains(notebookPath)
    }

    override suspend fun unlockNotebook(
        notebookPath: String,
        context: Context,
        reason: String
    ): Boolean {
        return suspendCancellableCoroutine { continuation ->

            println("DEBUG: EncryptionRepo: all keys:  ${getAllKeyAliases()}")

            val activity = context as? FragmentActivity
            if (activity == null) {
                Log.e("DEBUG: EncryptionRepo", "Context is not FragmentActivity")
                continuation.resume(false)
                return@suspendCancellableCoroutine
            }

            // Проверка наличия биометрии
            val biometricManager = BiometricManager.from(activity)
            val canAuth =
                biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
                Log.e("DEBUG: EncryptionRepo", "Biometric not available: $canAuth")
                continuation.resume(false)
                return@suspendCancellableCoroutine
            }

            val executor = ContextCompat.getMainExecutor(activity)

            val isResumed = AtomicBoolean(false)

            val biometricPrompt = BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        if (isResumed.compareAndSet(false, true)) {
                            unlockedNotebooks.add(notebookPath)
                            continuation.resume(true)
                        }
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (isResumed.compareAndSet(false, true)) {
                            Log.e("DEBUG: Biometric", "Error $errorCode: $errString")
                            continuation.resume(false)
                        }
                    }

                    override fun onAuthenticationFailed() {
                        if (isResumed.compareAndSet(false, true)) {
                            Log.e("DEBUG: Biometric", "Неверный отпечаток")
                            continuation.resume(false)
                        }
                    }
                }
            )

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Записная книжка защищена")
                .setSubtitle("$reason подтвердите личность")
                .setNegativeButtonText("Отмена")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build()

            try {
                biometricPrompt.authenticate(promptInfo)
            } catch (e: Exception) {
                Log.e("Biometric", "Error starting biometric", e)
                continuation.resume(false)
            }
        }
    }

    override fun lockNotebook(notebookPath: String) {
        unlockedNotebooks.remove(notebookPath)
    }

    override fun lockAllNotebooks() {
        unlockedNotebooks.clear()
    }


    override suspend fun encryptNote(notebookPath: String, plaintext: String): String {

        // Шифрование строки -> Base64 (с сохранением IV)
        val key = getSecretKey(notebookPath)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")

        // Для шифрования биометрия не нужна, но ключ должен быть разблокирован (если требуется)
        // Если ключ требует аутентификации и не разблокирован, вызов init выбросит исключение

        try {
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv // получаем IV, сгенерированный автоматически
            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            // Склеиваем IV + шифротекст и кодируем в Base64
            val combined = ByteArrayOutputStream().apply {
                write(iv)
                write(ciphertext)
            }.toByteArray()

            return Base64.encodeToString(combined, Base64.NO_WRAP)

        } catch (e: InvalidKeyException) {
            // Ключ не разблокирован (истекло время действия биометрии или не было аутентификации)
            throw AuthenticationRequiredException("Key is locked for encrypting $notebookPath").apply {
                initCause(e)
            }

        } catch (e: UserNotAuthenticatedException) {
            throw AuthenticationRequiredException("User not authenticated").apply { initCause(e) }

        } catch (e: Exception) {
            // Другие ошибки пробрасываем как есть
            throw e
        }
    }


    override suspend fun decryptNote(notebookPath: String, ciphertext: String): String {
        // Расшифровка строки из Base64 (ожидается IV + шифротекст)
        val combined = Base64.decode(ciphertext, Base64.DEFAULT)
        // Первые 12 байт – IV (для GCM стандартный размер 12)
        val iv = combined.copyOfRange(0, 12)
        val ciphertext = combined.copyOfRange(12, combined.size)

        val key = getSecretKey(notebookPath)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, iv)

        try {
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)
            val plaintext = cipher.doFinal(ciphertext)
            return String(plaintext, Charsets.UTF_8)

        } catch (e: InvalidKeyException) {
            // Ключ не разблокирован (истекло время действия биометрии или не было аутентификации)
            throw AuthenticationRequiredException("Key is locked for decrypting $notebookPath").apply {
                initCause(e)
            }
        } catch (e: Exception) {
            // Другие ошибки (повреждённые данные, неправильный IV и т.п.) пробрасываем как есть
            throw e
        }
    }

    override fun deleteKeyForNotebook(notebookPath: String) {
        //val alias = "notebook_$notebookPath"
        val alias = NOTEBOOK_KEY_ALIAS_PREFIX + notebookPath
        keyStore.deleteEntry(alias)
        lockNotebook(notebookPath) // также очищаем флаг разблокировки
    }

    /**
     * Возвращает список псевдонимов всех ключей, созданных приложением в Keystore.
     */
    fun getAllKeyAliases(): List<String> {
        return try {
            keyStore.aliases().toList().filter { alias ->
                alias.startsWith(NOTEBOOK_KEY_ALIAS_PREFIX ) && keyStore.isKeyEntry(alias)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}