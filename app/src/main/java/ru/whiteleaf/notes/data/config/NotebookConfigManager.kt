package ru.whiteleaf.notes.data.config

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.core.content.edit

class NotebookConfigManager(
private val sharedPreferences: SharedPreferences,
private val gson: Gson = Gson() // значение по умолчанию
) {

    private val protectedNotebooks: MutableMap<String, String> by lazy {
        val json = sharedPreferences.getString("protected_notebooks", "{}") ?: "{}"
        val type = object : TypeToken<Map<String, String>>() {}.type
        gson.fromJson(json, type) ?: mutableMapOf()
    }

    fun setNotebookProtected(notebookPath: String, keyAlias: String) {
        protectedNotebooks[notebookPath] = keyAlias
        saveProtectedNotebooks()
    }

    fun setNotebookUnprotected(notebookPath: String) {
        protectedNotebooks.remove(notebookPath)
        saveProtectedNotebooks()
    }

    fun isNotebookProtected(notebookPath: String): Boolean =
        protectedNotebooks.containsKey(notebookPath)

    fun getKeyAliasForNotebook(notebookPath: String): String? =
        protectedNotebooks[notebookPath]

    fun getAllProtectedNotebooks(): Map<String, String> =
        protectedNotebooks.toMap()

    fun generateKeyAlias(notebookPath: String): String =
        "key_${notebookPath.hashCode()}_${System.currentTimeMillis()}"

    private fun saveProtectedNotebooks() {
        sharedPreferences.edit {
            putString("protected_notebooks", gson.toJson(protectedNotebooks))
        }
    }
}