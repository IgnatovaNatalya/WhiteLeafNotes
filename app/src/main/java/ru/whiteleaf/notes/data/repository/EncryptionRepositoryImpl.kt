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
    private val keyGenerator =
        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")

    // Кэш для временного хранения ключей в памяти
    private val unlockedKeys = mutableMapOf<String, SecretKey>()
    private val noteContentCache = mutableMapOf<String, Pair<String, String>>()

    companion object {
        private fun getKeyAlias(notebookPath: String): String {
            return "notebook_key_${notebookPath.hashCode()}"
        }
    }

    override suspend fun encryptNotebook(notebookPath: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                println("🔐 Удаляем старые ключи: $notebookPath")
                clearProblematicKeys(notebookPath)

                println("🔐 Шифруем блокнот: $notebookPath")

                // Создаем ОДИН ключ без аутентификации
                val key = generateKeyWithoutAuth(notebookPath)
                println("✅ Ключ создан")

                // Получаем все заметки ДО шифрования
                val notes = notesRepository.getNotes(notebookPath)
                println("📝 Найдено заметок: ${notes.size}")

                // Шифруем все заметки в блокноте
                notes.forEachIndexed { index, note ->
                    println("🔒 Шифруем заметку ${index + 1}/${notes.size}: ${note.title}")

                    val encryptedContent = encryptData(note.content, key)
//                    val encryptedTitle =
//                        if (note.title.isNotEmpty() && note.title != "[ENCRYPTED]") {
//                            encryptData(note.title, key)
//                        } else {
//                            "[ENCRYPTED]"
//                        }

                    // Сохраняем зашифрованную заметку
                    val encryptedNote = note.copy(
                        content = "ENCRYPTED:$encryptedContent"
                    )
                    notesRepository.saveNote(encryptedNote)
                    println("✅ Заметка зашифрована и сохранена")
                }

                // Сохраняем ключ в памяти
                unlockedKeys[notebookPath] = key
                println("🎯 Шифрование завершено успешно")
                Result.success(Unit)
            } catch (e: Exception) {
                println("❌ Ошибка шифрования: ${e.message}")
                e.printStackTrace()
                Result.failure(e)
            }
        }

    override fun debugKeyStoreState(notebookPath: String) {
        try {
            val keyAlias = "notebook_key_${notebookPath.hashCode()}"
            println("🔍 ДЕБАГ KEYSTORE ДЛЯ: $notebookPath")
            println("🔑 ALIAS: $keyAlias")

            val aliases = keyStore.aliases().toList()
            println("📋 ВСЕ ALIASES В KEYSTORE: $aliases")
            println("🔍 НАШ ALIAS СУЩЕСТВУЕТ: ${keyStore.containsAlias(keyAlias)}")

            if (keyStore.containsAlias(keyAlias)) {
                val key = (keyStore.getEntry(keyAlias, null) as KeyStore.SecretKeyEntry).secretKey
                println("✅ КЛЮЧ ДОСТУПЕН: ${key.algorithm}")
            } else {
                println("❌ КЛЮЧ НЕ ДОСТУПЕН")
            }
        } catch (e: Exception) {
            println("❌ ОШИБКА ДЕБАГА KEYSTORE: ${e.message}")
        }
    }

    private fun generateKeyWithoutAuth(notebookPath: String): SecretKey {
        val keyAlias = getKeyAlias(notebookPath) //"notebook_key_${notebookPath.hashCode()}"

        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(false) // БЕЗ аутентификации
            .setInvalidatedByBiometricEnrollment(false)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }


    private fun decryptData(encryptedData: String, key: SecretKey): String {
        return try {
            println("🔓 Дешифруем данные: ${encryptedData.take(20)}...")

            val decoded = Base64.decode(encryptedData, Base64.DEFAULT)
            println("✅ Данные декодированы из Base64, размер: ${decoded.size} байт")

            val iv = decoded.copyOfRange(0, 12) // GCM IV size
            val encrypted = decoded.copyOfRange(12, decoded.size)
            println("✅ IV извлечен, размер зашифрованных данных: ${encrypted.size} байт")

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            val decrypted = cipher.doFinal(encrypted)
            println("✅ Данные расшифрованы, размер: ${decrypted.size} байт")

            String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            println("❌ Ошибка в decryptData: ${e.message}")
            throw e
        }
    }

    override fun debugKeyInfo(notebookPath: String?) {
        val key = unlockedKeys[notebookPath ?: "default"]
        if (key == null) {
            println("❌ Ключ не найден для блокнота: $notebookPath")
            println("📋 Доступные ключи: ${unlockedKeys.keys}")
        } else {
            println("✅ Ключ найден: ${key.algorithm}, ${key.format}")
        }
    }

    private fun getKeyForNotebook(notebookPath: String): SecretKey {
        val keyAlias = getKeyAlias(notebookPath) //"notebook_key_${notebookPath.hashCode()}"
        println("🔑 ПОЛУЧАЕМ КЛЮЧ С ALIAS: $keyAlias")
        //return (keyStore.getEntry(keyAlias, null) as KeyStore.SecretKeyEntry).secretKey
        try {
            // Проверим существует ли alias
            val aliases = keyStore.aliases().toList()
            println("📋 Все aliases в KeyStore: $aliases")
            println("🔍 Наш alias существует: ${keyStore.containsAlias(keyAlias)}")

            if (!keyStore.containsAlias(keyAlias)) {
                println("❌ ALIAS НЕ НАЙДЕН В KEYSTORE!")
                throw IllegalStateException("Ключ не найден в KeyStore")
            }

            val key = (keyStore.getEntry(keyAlias, null) as KeyStore.SecretKeyEntry).secretKey
            println("✅ КЛЮЧ УСПЕШНО ПОЛУЧЕН ИЗ KEYSTORE")
            return key
        } catch (e: Exception) {
            println("❌ ОШИБКА ПОЛУЧЕНИЯ КЛЮЧА: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    fun clearProblematicKeys(notebookPath: String) {
        try {
            val tempAlias = "temp_key_${notebookPath.hashCode()}"
            val permAlias = "perm_key_${notebookPath.hashCode()}"

            keyStore.deleteEntry(tempAlias)
            keyStore.deleteEntry(permAlias)
            println("🗑️ Старые ключи удалены")
        } catch (e: Exception) {
            println("⚠️ Не удалось удалить старые ключи: ${e.message}")
        }
    }

    override suspend fun decryptNotebook(notebookPath: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                println("🔓 ДЕШИФРУЕМ БЛОКНОТ: $notebookPath")
                val key = getKeyForNotebook(notebookPath)
                println("✅ Ключ получен из KeyStore")


                // Получаем все заметки
                val notes = notesRepository.getNotes(notebookPath)

                // Декриптуем заметки в памяти (не сохраняем в файл)
                notes.forEach { note ->
                    if (note.content.startsWith("ENCRYPTED:") && note.title.startsWith("ENCRYPTED:")) {
                        val decryptedContent =
                            decryptData(note.content.removePrefix("ENCRYPTED:"), key)
//                        val decryptedTitle = if (note.title != "ENCRYPTED:[ENCRYPTED]") {
//                            decryptData(note.title.removePrefix("ENCRYPTED:"), key)
//                        } else {
//                            "" // Заголовок будет восстановлен из контента
//                        }

                        // Кэшируем декриптованный контент
                        noteContentCache[note.id] = decryptedContent to note.id
                    }
                }

                // Сохраняем ключ в памяти
                unlockedKeys[notebookPath] = key
                println("✅ Ключ сохранен в памяти. Теперь unlockedKeys: ${unlockedKeys.keys}")

                Result.success(Unit)
            } catch (e: Exception) {
                println("❌ КРИТИЧЕСКАЯ ОШИБКА в decryptNotebook: ${e.message}")
                e.printStackTrace()
                Result.failure(e)
            }
        }

    override suspend fun encryptNote(noteId: String, notebookPath: String?): Result<Unit> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val note = notesRepository.getNotes(notebookPath).find { it.id == noteId }
                    ?: return@withContext Result.failure(IllegalArgumentException("Заметка не найдена"))

                val key = unlockedKeys[notebookPath ?: "default"]
                    ?: return@withContext Result.failure(IllegalStateException("Блокнот не разблокирован"))

                val encryptedContent = encryptData(note.content, key)
//                val encryptedTitle = if (note.title.isNotEmpty()) {
//                    encryptData(note.title, key)
//                } else {
//                    "[ENCRYPTED]"
//                }

                // Сохраняем зашифрованную заметку
                val encryptedNote = note.copy(
                    content = "ENCRYPTED:$encryptedContent"
                )
                notesRepository.saveNote(encryptedNote)

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun decryptNote(noteId: String, notebookPath: String?): Result<Unit> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                println("🔓 Начало дешифровки заметки: $noteId")

                val note = notesRepository.getNotes(notebookPath).find { it.id == noteId }
                    ?: return@withContext Result.failure(IllegalArgumentException("Заметка не найдена"))

                println("📄 Заметка найдена: ${note.title}")
                println("🔍 Контент начинается с: ${note.content.take(20)}...")

                // Если заметка не зашифрована, ничего не делаем
                if (!note.content.startsWith("ENCRYPTED:")) {
                    println("⚠️ Заметка не зашифрована, пропускаем дешифровку")
                    noteContentCache[noteId] = note.content to note.id//note.title
                    return@withContext Result.success(Unit)
                }

                val key = unlockedKeys[notebookPath ?: "default"]
                    ?: return@withContext Result.failure(IllegalStateException("Блокнот не разблокирован"))

                println("✅ Ключ найден, начинаем дешифровку...")

                val encryptedContent = note.content.removePrefix("ENCRYPTED:")
                val decryptedContent = decryptData(encryptedContent, key)

                println("✅ Контент расшифрован: ${decryptedContent.take(20)}...")

                // Обрабатываем заголовок
//                val decryptedTitle =
//                    if (note.title.startsWith("ENCRYPTED:") && note.title != "ENCRYPTED:[ENCRYPTED]") {
//                        decryptData(note.title.removePrefix("ENCRYPTED:"), key)
//                    } else {
//                        "" // Заголовок будет восстановлен из контента
//                    }

                println("✅ Заголовок расшифрован: ${note.title}")

                // Кэшируем декриптованный контент
                noteContentCache[noteId] = decryptedContent to note.id //decryptedTitle

                println("🎯 Дешифровка завершена успешно")
                Result.success(Unit)
            } catch (e: Exception) {
                println("❌ Ошибка дешифровки: ${e.message}")
                e.printStackTrace()
                Result.failure(e)
            }
        }


    // Добавляем методы для работы с кэшем
    override fun getDecryptedContent(noteId: String): String? {
        return noteContentCache[noteId]?.first
    }

//    override fun getDecryptedTitle(noteId: String): String? {
//        return noteContentCache[noteId]?.second
//    }

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
        val isUnlocked =  unlockedKeys.containsKey(notebookPath)
        println("🔍 Проверка ключа в памяти для $notebookPath: $isUnlocked")
        println("📋 Все ключи в памяти: ${unlockedKeys.keys}")
        return isUnlocked
    }

    override fun lockNotebook(notebookPath: String) {
        println("🔒 Блокируем записную книжку в репозитории: $notebookPath")
        println("📋 Ключи до блокировки: ${unlockedKeys.keys}")
        unlockedKeys.remove(notebookPath)

        // Очищаем кэш контента для этого блокнота
        noteContentCache.keys.removeAll { key ->
            key.startsWith("$notebookPath/") || key.contains(notebookPath)
        }
        println("📋 Ключи после блокировки: ${unlockedKeys.keys}")
        println("✅ Записная книжка заблокирована в репозитории")

    }

    override fun clearNotebookKeys(notebookPath: String) {
        try {
            println("🗑️ Очищаем ключи для блокнота: $notebookPath")

            val keyAlias = "notebook_key_${notebookPath.hashCode()}"
            val tempAlias = "temp_key_${notebookPath.hashCode()}"
            val permAlias = "perm_key_${notebookPath.hashCode()}"

            listOf(keyAlias, tempAlias, permAlias).forEach { alias ->
                if (keyStore.containsAlias(alias)) {
                    keyStore.deleteEntry(alias)
                    println("✅ Удален ключ: $alias")
                }
            }

            // Удаляем из памяти
            unlockedKeys.remove(notebookPath)
            noteContentCache.keys.removeAll { it.contains(notebookPath) }

        } catch (e: Exception) {
            println("❌ Ошибка очистки ключей записной книжки: ${e.message}")
        }
    }

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
}