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
        private const val ENCRYPTION_KEY_SUFFIX = "_enc"
        private const val ACCESS_KEY_SUFFIX = "_access"

        private const val TRANSFORMATION = "AES/CBC/PKCS7Padding"
        private const val IV_SEPARATOR = "|"
    }

    fun createNotebookKeys(notebookKeyAlias: String): Boolean {
        val encryptionKeyCreated = createEncryptionKey("$notebookKeyAlias$ENCRYPTION_KEY_SUFFIX")
        val accessKeyCreated = createAccessKey("$notebookKeyAlias$ACCESS_KEY_SUFFIX")

        return encryptionKeyCreated && accessKeyCreated
    }

    /**
     * Ключ для ШИФРОВАНИЯ (без биометрии)
     */
    private fun createEncryptionKey(keyAlias: String): Boolean = try {
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
            .setUserAuthenticationRequired(false) // Без биометрии!
            .build()

        keyGenerator.init(keySpec)
        keyGenerator.generateKey()
        true
    } catch (e: Exception) {
        println("❌ createEncryptionKey failed: ${e.message}")
        false
    }

    /**
     * Ключ для ДОСТУПА (с биометрией)
     */
    private fun createAccessKey(keyAlias: String): Boolean = try {
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
        keyGenerator.generateKey()
        true
    } catch (e: Exception) {
        println("❌ createAccessKey failed: ${e.message}")
        false
    }

    /**
     * Шифрует содержимое (использует ключ без биометрии)
     */
    fun encryptContent(plainText: String, notebookKeyAlias: String): String = try {
        val encryptionKeyAlias = "${notebookKeyAlias}_enc" // использует ключ БЕЗ биометрии
        val cipher = getCipher().apply {
            init(Cipher.ENCRYPT_MODE, getSecretKey(encryptionKeyAlias))
        }

        val encryptedBytes = cipher.doFinal(plainText.toByteArray())
        val iv = cipher.iv

        "${Base64.encodeToString(iv, Base64.NO_WRAP)}|${Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)}"
    } catch (e: Exception) {
        println("❌ encryptContent failed: ${e.message}")
        plainText
    }

    /**
     * Расшифровывает содержимое (использует ключ с биометрией)
     */
    fun decryptContent(encryptedText: String, notebookKeyAlias: String): String {
        println("🔓 decryptContent START - key: $notebookKeyAlias")
        return try {
            val accessKeyAlias = "${notebookKeyAlias}_access"
            println("🔓 Using access key: $accessKeyAlias")

            val parts = encryptedText.split("|")
            if (parts.size != 2) {
                println("❌ Invalid encrypted format")
                return encryptedText
            }

            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val encryptedBytes = Base64.decode(parts[1], Base64.NO_WRAP)
            println("🔓 IV length: ${iv.size}, encrypted bytes: ${encryptedBytes.size}")

            val cipher = getCipher().apply {
                init(Cipher.DECRYPT_MODE, getSecretKey(accessKeyAlias), IvParameterSpec(iv))
            }

            val decrypted = String(cipher.doFinal(encryptedBytes))
            println("✅ decryptContent SUCCESS: '$decrypted'")
            decrypted

        } catch (e: SecurityException) {
            println("🔓 SecurityException: ${e.message}")
            throw e // Пробрасываем для биометрии
        } catch (e: Exception) {
            println("❌ decryptContent failed: ${e.message}")
            e.printStackTrace() // ← ВАЖНО: покажет полный stacktrace
            encryptedText
        } finally {
            println("🔓 decryptContent END\n")
        }
    }

    /**
     * Получает Cipher для BiometricPrompt (для доступа)
     */
    fun getCipherForAccess(encryptedText: String, notebookKeyAlias: String): Cipher {
        val accessKeyAlias = "${notebookKeyAlias}_access"
        val parts = encryptedText.split("|")

        if (parts.size != 2) {
            throw IllegalArgumentException("Invalid encrypted format")
        }

        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val cipher = getCipher()
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(accessKeyAlias), IvParameterSpec(iv))

        return cipher
    }

    /**
     * Создает ключ для блокнота с привязкой к биометрии
     * Вызывается ТОЛЬКО при включении защиты блокнота
     */
    fun createKeyForNotebook(keyAlias: String): Boolean = try {
        if (keyStore.containsAlias(keyAlias)) {
            true // Ключ уже существует
        } else {
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
            keyGenerator.generateKey()
            true
        }
    } catch (e: Exception) {
        false
    }

    /**
     * Получает существующий ключ для операций
     * @throws SecurityException если ключ требует биометрию
     */
    private fun getSecretKey(keyAlias: String): SecretKey {
        return keyStore.getKey(keyAlias, null) as? SecretKey
            ?: throw SecurityException("Key not found: $keyAlias")
    }

    /**
     * Получает Cipher для использования с BiometricPrompt
     */
    fun getCipherForDecryption(keyAlias: String): Cipher {
        return getCipher().apply {
            init(Cipher.DECRYPT_MODE, getSecretKey(keyAlias))
        }
    }

    fun keyExists(keyAlias: String): Boolean = keyStore.containsAlias(keyAlias)

    fun deleteKey(keyAlias: String): Boolean = try {
        keyStore.deleteEntry(keyAlias)
        true
    } catch (e: Exception) {
        false
    }

    private fun getCipher(): Cipher = Cipher.getInstance(TRANSFORMATION)
}