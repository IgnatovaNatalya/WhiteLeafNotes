package com.example.txtnotesapp.domain.repository

import android.net.Uri
import com.example.txtnotesapp.domain.model.Note

interface NoteRepository {
    suspend fun getNotes(notebookPath: String?): List<Note>
    suspend fun getNoteById(noteId: String, notebookPath: String?): Note?
    suspend fun saveNote(note: Note)
    suspend fun deleteNote(note: Note)
    suspend fun moveNote(note: Note, targetNotebookPath: String?)
    suspend fun renameNote(note: Note, newName: String)
    suspend fun shareNote(note: Note): Uri?
}