package com.example.txtnotesapp.domain.repository

import android.net.Uri
import com.example.txtnotesapp.domain.model.Note
import com.example.txtnotesapp.domain.model.Notebook

interface ExternalRepository {
    suspend fun createExportZip(
        notes: List<Note>,
        notebooks: List<Notebook>,
        password: String?
    ): Uri
}