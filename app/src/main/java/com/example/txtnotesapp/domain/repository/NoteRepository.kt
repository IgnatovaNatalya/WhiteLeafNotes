package com.example.txtnotesapp.domain.repository

import com.example.txtnotesapp.domain.model.Note

interface NoteRepository {
    suspend fun getNotes(notebookPath: String?): List<Note>
}