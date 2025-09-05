package com.example.txtnotesapp.data

import android.content.Context
import android.content.SharedPreferences
import com.example.txtnotesapp.domain.repository.PreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.content.edit

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

    override suspend fun getNotesDirectoryPath(): String? {
        return withContext(Dispatchers.IO) {
            prefs.getString(KEY_NOTES_DIRECTORY, null)
        }
    }

    override suspend fun saveNotesDirectoryPath(path: String) {
        withContext(Dispatchers.IO) {
            prefs.edit { putString(KEY_NOTES_DIRECTORY, path) }
        }
    }
}