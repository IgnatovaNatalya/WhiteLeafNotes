package com.example.whiteleafnotes.domain.repository

import android.net.Uri
import com.example.whiteleafnotes.domain.model.Note
import com.example.whiteleafnotes.domain.model.Notebook

interface ExternalRepository {
    suspend fun createExportZip(
        notes: List<Note>,
        notebooks: List<Notebook>,
        password: String?
    ): Uri
}