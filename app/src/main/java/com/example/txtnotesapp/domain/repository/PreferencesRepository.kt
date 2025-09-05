package com.example.txtnotesapp.domain.repository

interface PreferencesRepository {
    suspend fun getNotesDirectoryPath(): String?
    suspend fun saveNotesDirectoryPath(path: String)
}