package com.example.txtnotesapp.domain.repository

import android.net.Uri
import com.example.txtnotesapp.domain.model.Note
import com.example.txtnotesapp.domain.model.Notebook

interface NoteRepository {
    suspend fun getNotes(notebookPath: String?): List<Note>
    suspend fun getNoteByTitle(noteTitle: String, notebookPath: String?): Note?
    suspend fun saveNote(note: Note)
    suspend fun deleteNote(note: Note)
    suspend fun moveNote(note: Note, targetNotebookPath: String?)
    suspend fun renameNote(note: Note, newName: String)
    suspend fun shareNote(note: Note): Uri?
    suspend fun exportToZip(notes: List<Note>, notebooks: List<Notebook>,password: String?): Uri
}