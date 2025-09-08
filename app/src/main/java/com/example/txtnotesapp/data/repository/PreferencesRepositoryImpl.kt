package com.example.txtnotesapp.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.txtnotesapp.domain.repository.PreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PreferencesRepositoryImpl(
    private val context: Context,
) : PreferencesRepository {

    companion object {
        private const val PREFS_NAME = "txt_notes_app_prefs"
        private const val KEY_NOTES_DIRECTORY = "txt_notes_directory"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override suspend fun getExportDirectoryPath(): String? {
        return withContext(Dispatchers.IO) {
            prefs.getString(KEY_NOTES_DIRECTORY, null)
        }
    }

    override suspend fun saveExportDirectoryPath(path: String) {
        withContext(Dispatchers.IO) {
            prefs.edit { putString(KEY_NOTES_DIRECTORY, path) }
        }
    }
}