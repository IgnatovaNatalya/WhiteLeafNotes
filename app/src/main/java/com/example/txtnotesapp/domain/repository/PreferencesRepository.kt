package com.example.txtnotesapp.domain.repository

interface PreferencesRepository {
    suspend fun getExportDirectoryPath(): String?
    suspend fun saveExportDirectoryPath(path: String)
}