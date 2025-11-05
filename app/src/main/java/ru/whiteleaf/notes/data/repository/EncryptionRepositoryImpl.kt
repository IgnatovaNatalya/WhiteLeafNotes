package ru.whiteleaf.notes.data.repository

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.whiteleaf.notes.domain.repository.EncryptionRepository
import ru.whiteleaf.notes.domain.repository.NotesRepository
import ru.whiteleaf.notes.domain.repository.SecurityPreferences

class EncryptionRepositoryImpl(
    private val context: Context,
    private val notesRepository: NotesRepository,
    private val securityPreferences: SecurityPreferences
) : EncryptionRepository {

    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    private val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")

    // Кэш для временного хранения ключей в памяти
    private val unlockedKeys = mutableMapOf<String, SecretKey>()
    private val noteContentCache = mutableMapOf<String, Pair<String, String>>()


    override suspend fun encryptNotebook(notebookPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            println("🔐 Начало шифрования блокнота: $notebookPath")

            // Создаем временный ключ БЕЗ аутентификации для шифрования
            val tempKey = generateTemporaryKey(notebookPath)
            println("✅ Временный ключ создан")

            // Получаем все заметки ДО шифрования
            val notes = notesRepository.getNotes(notebookPath)
            println("📝 Найдено заметок: ${notes.size}")

            // Шифруем все заметки временным ключом
            notes.forEachIndexed { index, note ->
                println("🔒 Шифруем заметку ${index + 1}/${notes.size}: ${note.title}")

                val encryptedContent = encryptDataWithKey(note.content, tempKey)
                val encryptedTitle = if (note.title.isNotEmpty() && note.title != "[ENCRYPTED]") {
                    encryptDataWithKey(note.title, tempKey)
                } else {
                    "[ENCRYPTED]"
                }

                // Сохраняем зашифрованную заметку
                val encryptedNote = note.copy(
                    title = "ENCRYPTED:$encryptedTitle",
                    content = "ENCRYPTED:$encryptedContent"
                )
                notesRepository.saveNote(encryptedNote)
                println("✅ Заметка зашифрована и сохранена")
            }

            // Теперь создаем постоянный ключ С аутентификацией
            val permanentKey = generatePermanentKey(notebookPath)
            println("✅ Постоянный ключ создан")

            // Сохраняем постоянный ключ в памяти
            unlockedKeys[notebookPath] = permanentKey

            // Удаляем временный ключ
            deleteTemporaryKey(notebookPath)

            println("🎯 Шифрование завершено успешно")
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ Ошибка шифрования: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun generateTemporaryKey(notebookPath: String): SecretKey {
        val keyAlias = "temp_key_${notebookPath.hashCode()}"

        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(false) // Без аутентификации
            .setInvalidatedByBiometricEnrollment(false)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }

    private fun generatePermanentKey(notebookPath: String): SecretKey {
        val keyAlias = "perm_key_${notebookPath.hashCode()}"

        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(true) // С аутентификацией для доступа
            .setInvalidatedByBiometricEnrollment(true)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }

    private fun deleteTemporaryKey(notebookPath: String) {
        try {
            val keyAlias = "temp_key_${notebookPath.hashCode()}"
            keyStore.deleteEntry(keyAlias)
        } catch (e: Exception) {
            // Игнорируем ошибки удаления
        }
    }

    private fun encryptDataWithKey(data: String, key: SecretKey): String {
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(iv + encrypted, Base64.DEFAULT)
        } catch (e: Exception) {
            println("❌ Ошибка encryptDataWithKey: ${e.message}")
            throw e
        }
    }

    // Обновите также decryptData для использования правильного ключа
    private fun decryptData(encryptedData: String, key: SecretKey): String {
        val decoded = Base64.decode(encryptedData, Base64.DEFAULT)
        val iv = decoded.copyOfRange(0, 12) // GCM IV size
        val encrypted = decoded.copyOfRange(12, decoded.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        val decrypted = cipher.doFinal(encrypted)
        return String(decrypted, Charsets.UTF_8)
    }

    // Обновите getKeyForNotebook для использования постоянного ключа
    private fun getKeyForNotebook(notebookPath: String): SecretKey {
        val keyAlias = "perm_key_${notebookPath.hashCode()}"
        return (keyStore.getEntry(keyAlias, null) as KeyStore.SecretKeyEntry).secretKey
    }

//    override suspend fun encryptNotebook(notebookPath: String): Result<Unit> = withContext(Dispatchers.IO) {
//        return@withContext try {
//            println("🔐 Начало шифрования блокнота: $notebookPath")
//
//            // Генерируем ключ для блокнота
//            val key = generateKeyForNotebook(notebookPath)
//            println("✅ Ключ сгенерирован")
//
//            // Получаем все заметки ДО шифрования
//            val notes = notesRepository.getNotes(notebookPath)
//            println("📝 Найдено заметок: ${notes.size}")
//
//            if (notes.isEmpty()) {
//                println("⚠️ В блокноте нет заметок для шифрования")
//            }
//
//            // Шифруем все заметки в блокноте
//            notes.forEachIndexed { index, note ->
//                println("🔒 Шифруем заметку ${index + 1}/${notes.size}: ${note.title}")
//
//                val encryptedContent = encryptData(note.content, key)
//                val encryptedTitle = if (note.title.isNotEmpty() && note.title != "[ENCRYPTED]") {
//                    encryptData(note.title, key)
//                } else {
//                    "[ENCRYPTED]"
//                }
//
//                // Сохраняем зашифрованную заметку
//                val encryptedNote = note.copy(
//                    title = "ENCRYPTED:$encryptedTitle",
//                    content = "ENCRYPTED:$encryptedContent"
//                )
//                notesRepository.saveNote(encryptedNote)
//                println("✅ Заметка зашифрована и сохранена")
//            }
//
//            // Сохраняем ключ в памяти как разблокированный
//            unlockedKeys[notebookPath] = key
//            println("🎯 Шифрование завершено успешно")
//
//            Result.success(Unit)
//        } catch (e: Exception) {
//            println("❌ Ошибка шифрования: ${e.message}")
//            e.printStackTrace()
//            Result.failure(e)
//        }
//    }

    override suspend fun decryptNotebook(notebookPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val key = getKeyForNotebook(notebookPath)

            // Получаем все заметки
            val notes = notesRepository.getNotes(notebookPath)

            // Декриптуем заметки в памяти (не сохраняем в файл)
            notes.forEach { note ->
                if (note.content.startsWith("ENCRYPTED:") && note.title.startsWith("ENCRYPTED:")) {
                    val decryptedContent = decryptData(note.content.removePrefix("ENCRYPTED:"), key)
                    val decryptedTitle = if (note.title != "ENCRYPTED:[ENCRYPTED]") {
                        decryptData(note.title.removePrefix("ENCRYPTED:"), key)
                    } else {
                        "" // Заголовок будет восстановлен из контента
                    }

                    // Кэшируем декриптованный контент
                    noteContentCache[note.id] = decryptedContent to decryptedTitle
                }
            }

            // Сохраняем ключ в памяти
            unlockedKeys[notebookPath] = key

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Упрощаем методы encryptNote/decryptNote
    override suspend fun encryptNote(noteId: String, notebookPath: String?): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val note = notesRepository.getNotes(notebookPath).find { it.id == noteId }
                ?: return@withContext Result.failure(IllegalArgumentException("Заметка не найдена"))

            val key = unlockedKeys[notebookPath ?: "default"]
                ?: return@withContext Result.failure(IllegalStateException("Блокнот не разблокирован"))

            val encryptedContent = encryptData(note.content, key)
            val encryptedTitle = if (note.title.isNotEmpty()) {
                encryptData(note.title, key)
            } else {
                "[ENCRYPTED]"
            }

            // Сохраняем зашифрованную заметку
            val encryptedNote = note.copy(
                title = "ENCRYPTED:$encryptedTitle",
                content = "ENCRYPTED:$encryptedContent"
            )
            notesRepository.saveNote(encryptedNote)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun decryptNote(noteId: String, notebookPath: String?): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val note = notesRepository.getNotes(notebookPath).find { it.id == noteId }
                ?: return@withContext Result.failure(IllegalArgumentException("Заметка не найдена"))

            // Если заметка не зашифрована, ничего не делаем
            if (!note.content.startsWith("ENCRYPTED:")) {
                noteContentCache[noteId] = note.content to note.title
                return@withContext Result.success(Unit)
            }

            val key = unlockedKeys[notebookPath ?: "default"]
                ?: return@withContext Result.failure(IllegalStateException("Блокнот не разблокирован"))

            val decryptedContent = decryptData(note.content.removePrefix("ENCRYPTED:"), key)
            val decryptedTitle = if (note.title.startsWith("ENCRYPTED:") && note.title != "ENCRYPTED:[ENCRYPTED]") {
                decryptData(note.title.removePrefix("ENCRYPTED:"), key)
            } else {
                "" // Заголовок будет восстановлен из контента
            }

            // Кэшируем декриптованный контент
            noteContentCache[noteId] = decryptedContent to decryptedTitle

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Добавляем методы для работы с кэшем
    override fun getDecryptedContent(noteId: String): String? {
        return noteContentCache[noteId]?.first
    }

    override fun getDecryptedTitle(noteId: String): String? {
        return noteContentCache[noteId]?.second
    }

    override fun cacheDecryptedContent(noteId: String, content: String, title: String) {
        noteContentCache[noteId] = content to title
    }

    override fun removeFromCache(noteId: String) {
        noteContentCache.remove(noteId)
    }

    override fun clearAllKeys() {
        unlockedKeys.clear()
        noteContentCache.clear()
        securityPreferences.clearUnlockedState()
    }


    override fun isNotebookUnlocked(notebookPath: String): Boolean {
        return unlockedKeys.containsKey(notebookPath)
    }

    override fun lockNotebook(notebookPath: String) {
        unlockedKeys.remove(notebookPath)
        // Очищаем кэш контента для этого блокнота
        noteContentCache.keys.removeAll { key ->
            key.startsWith("$notebookPath/")
        }
    }

    // Вспомогательные методы
    private fun generateKeyForNotebook(notebookPath: String): SecretKey {
        return try {
            val keyAlias = "notebook_key_${notebookPath.hashCode()}"
            println("🔑 Генерируем ключ с alias: $keyAlias")

            // Сначала удаляем старый ключ если существует
            try {
                keyStore.deleteEntry(keyAlias)
                println("🗑️ Старый ключ удален")
            } catch (e: Exception) {
                // Игнорируем если ключа не существует
            }

            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(false) // ВРЕМЕННО отключаем для шифрования
                .setInvalidatedByBiometricEnrollment(false)
                .build()

            keyGenerator.init(keyGenParameterSpec)
            val key = keyGenerator.generateKey()
            println("✅ Новый ключ сгенерирован")
            key
        } catch (e: Exception) {
            println("❌ Ошибка генерации ключа: ${e.message}")
            throw e
        }
    }
//
//    private fun getKeyForNotebook(notebookPath: String): SecretKey {
//        val keyAlias = "notebook_key_${notebookPath.hashCode()}"
//        return (keyStore.getEntry(keyAlias, null) as KeyStore.SecretKeyEntry).secretKey
//    }

    private fun encryptData(data: String, key: SecretKey): String {
        return try {
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(iv + encrypted, Base64.DEFAULT)
        } catch (e: Exception) {
            println("❌ Ошибка encryptData: ${e.message}")
            throw e
        }
    }

//    private fun decryptData(encryptedData: String, key: SecretKey): String {
//        val decoded = Base64.decode(encryptedData, Base64.DEFAULT)
//        val iv = decoded.copyOfRange(0, 12) // GCM IV size
//        val encrypted = decoded.copyOfRange(12, decoded.size)
//
//        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
//        val decrypted = cipher.doFinal(encrypted)
//        return String(decrypted, Charsets.UTF_8)
//    }



}