package ru.whiteleaf.notes.data.datasource

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

class EncryptionManager(private val context: Context) {

    private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    companion object {
        private const val TRANSFORMATION = "AES/CBC/PKCS7Padding"
        private const val KEY_SIZE = 256
        private const val IV_SEPARATOR = "|"
    }

    /**
     * Создает ключ для блокнота с привязкой к биометрии
     */
    fun createKeyForNotebook(keyAlias: String): Boolean {
        return try {
            if (keyStore.containsAlias(keyAlias)) {
                true // Ключ уже существует
            }

            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
            )

            val keySpec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .setKeySize(KEY_SIZE)
                .setUserAuthenticationRequired(true) // Требует биометрию
                .setInvalidatedByBiometricEnrollment(true)
                .setUserAuthenticationValidityDurationSeconds(-1) // Каждый раз требовать аутентификацию
                .build()

            keyGenerator.init(keySpec)
            keyGenerator.generateKey()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Удаляет ключ блокнота
     */
    fun deleteKey(keyAlias: String): Boolean {
        return try {
            if (keyStore.containsAlias(keyAlias)) {
                keyStore.deleteEntry(keyAlias)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Шифрует содержимое заметки
     * Формат: base64(IV) + SEPARATOR + base64(encrypted_data)
     */
    fun encryptContent(plainText: String, keyAlias: String): String {
        return try {
            val cipher = getCipher()
            val secretKey = getSecretKey(keyAlias)

            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray())
            val iv = cipher.iv

            // Сохраняем IV вместе с данными
            val result = Base64.encodeToString(iv, Base64.NO_WRAP) +
                    IV_SEPARATOR +
                    Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
            result
        } catch (e: Exception) {
            // Если шифрование не удалось, возвращаем оригинальный текст
            plainText
        }
    }

    /**
     * Расшифровывает содержимое заметки
     * Требует биометрическую аутентификацию через getCipherForDecryption
     */
    fun decryptContent(encryptedText: String, keyAlias: String): String {
        return try {
            val parts = encryptedText.split(IV_SEPARATOR)
            if (parts.size != 2) {
                return encryptedText // Не зашифрованный формат
            }

            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val encryptedBytes = Base64.decode(parts[1], Base64.NO_WRAP)

            val cipher = getCipher()
            val secretKey = getSecretKey(keyAlias)

            cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes)
        } catch (e: Exception) {
            // Если расшифровка не удалась, возвращаем зашифрованный текст
            encryptedText
        }
    }

    /**
     * Получает Cipher для использования с BiometricPrompt
     * Этот метод требует биометрическую аутентификацию
     */
    fun getCipherForDecryption(keyAlias: String): Cipher {
        val cipher = getCipher()
        val secretKey = getSecretKey(keyAlias)
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        return cipher
    }

    /**
     * Проверяет, существует ли ключ для данного алиаса
     */
    fun keyExists(keyAlias: String): Boolean {
        return try {
            keyStore.containsAlias(keyAlias)
        } catch (e: Exception) {
            false
        }
    }

    private fun getCipher(): Cipher {
        return Cipher.getInstance(TRANSFORMATION)
    }

    private fun getSecretKey(keyAlias: String): SecretKey {
        return keyStore.getKey(keyAlias, null) as? SecretKey
            ?: throw SecurityException("Ключ не найден или недоступен: $keyAlias")
    }
}