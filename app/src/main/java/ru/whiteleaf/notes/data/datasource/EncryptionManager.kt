package ru.whiteleaf.notes.data.datasource

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

class EncryptionManager() {

    private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    companion object {
        private const val TRANSFORMATION = "AES/CBC/PKCS7Padding"
        private const val IV_SEPARATOR = "|"
    }

    /**
     * Создает или получает существующий ключ
     */
    fun getOrCreateKey(keyAlias: String): SecretKey {
        return (keyStore.getKey(keyAlias, null) as? SecretKey) ?: createKey(keyAlias)
    }

    /**
     * Создает ключ для блокнота с привязкой к биометрии
     */
    private fun createKey(keyAlias: String): SecretKey {
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
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
            .setUserAuthenticationValidityDurationSeconds(-1)
            .build()

        keyGenerator.init(keySpec)
        return keyGenerator.generateKey()
    }

    /**
     * Удаляет ключ блокнота
     */
    fun deleteKey(keyAlias: String): Boolean = try {
        keyStore.deleteEntry(keyAlias)
        true
    } catch (e: Exception) {
        false
    }

    /**
     * Шифрует содержимое заметки
     */
    fun encryptContent(plainText: String, keyAlias: String): String = try {
        val cipher = getCipher().apply {
            init(Cipher.ENCRYPT_MODE, getOrCreateKey(keyAlias))
        }

        val encryptedBytes = cipher.doFinal(plainText.toByteArray())
        val iv = cipher.iv

        "${Base64.encodeToString(iv, Base64.NO_WRAP)}$IV_SEPARATOR${Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)}"
    } catch (e: Exception) {
        plainText // fallback
    }

    /**
     * Расшифровывает содержимое заметки
     */
    fun decryptContent(encryptedText: String, keyAlias: String): String {
        return try {
            val parts = encryptedText.split(IV_SEPARATOR)
            if (parts.size != 2) return encryptedText

            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val encryptedBytes = Base64.decode(parts[1], Base64.NO_WRAP)

            val cipher = getCipher().apply {
                init(Cipher.DECRYPT_MODE, getOrCreateKey(keyAlias), IvParameterSpec(iv))
            }

            String(cipher.doFinal(encryptedBytes))
        } catch (e: Exception) {
            encryptedText // fallback
        }
    }

    /**
     * Получает Cipher для использования с BiometricPrompt
     */
    fun getCipherForDecryption(keyAlias: String): Cipher {
        return getCipher().apply {
            init(Cipher.DECRYPT_MODE, getOrCreateKey(keyAlias))
        }
    }

    /**
     * Проверяет, существует ли ключ
     */
    fun keyExists(keyAlias: String): Boolean = keyStore.containsAlias(keyAlias)

    private fun getCipher(): Cipher = Cipher.getInstance(TRANSFORMATION)
}