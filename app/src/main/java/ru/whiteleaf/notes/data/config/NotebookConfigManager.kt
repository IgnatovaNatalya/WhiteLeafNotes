package ru.whiteleaf.notes.data.config

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class NotebookConfigManager(private val context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("notebook_config", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val protectedNotebooks: MutableMap<String, String> by lazy {
        loadProtectedNotebooks()
    }

    /**
     * Защищает блокнот (создает ключ и сохраняет конфиг)
     */
    fun setNotebookProtected(notebookPath: String, keyAlias: String) {
        protectedNotebooks[notebookPath] = keyAlias
        saveProtectedNotebooks()
    }

    /**
     * Снимает защиту с блокнота
     */
    fun setNotebookUnprotected(notebookPath: String) {
        protectedNotebooks.remove(notebookPath)
        saveProtectedNotebooks()
    }

    /**
     * Проверяет, защищен ли блокнот
     */
    fun isNotebookProtected(notebookPath: String): Boolean {
        return protectedNotebooks.containsKey(notebookPath)
    }

    /**
     * Возвращает алиас ключа для блокнота
     */
    fun getKeyAliasForNotebook(notebookPath: String): String? {
        return protectedNotebooks[notebookPath]
    }

    /**
     * Возвращает все защищенные блокноты
     */
    fun getProtectedNotebooks(): Map<String, String> {
        return protectedNotebooks.toMap()
    }

    /**
     * Генерирует уникальный алиас для ключа
     */
    fun generateKeyAlias(notebookPath: String): String {
        val timestamp = System.currentTimeMillis()
        return "key_${notebookPath.hashCode()}_$timestamp"
    }

    private fun loadProtectedNotebooks(): MutableMap<String, String> {
        val json = sharedPreferences.getString("protected_notebooks", "{}") ?: "{}"
        val type = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(json, type) ?: mutableMapOf()
    }

    private fun saveProtectedNotebooks() {
        val json = gson.toJson(protectedNotebooks)
        sharedPreferences.edit().putString("protected_notebooks", json).apply()
    }
}