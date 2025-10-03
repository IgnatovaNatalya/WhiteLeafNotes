package com.example.whiteleafnotes.domain.repository

import android.net.Uri
import com.example.whiteleafnotes.domain.model.Note
import com.example.whiteleafnotes.domain.model.Notebook

interface NotesRepository {
    suspend fun getNotes(notebookPath: String?): List<Note>
    suspend fun saveNote(note: Note)
    suspend fun deleteNote(note: Note)
    suspend fun moveNote(note: Note, targetNotebookPath: String?)
    suspend fun renameNote(note: Note, newName: String): String
    suspend fun shareNoteFile(note: Note): Uri?
    suspend fun getAllNotes(notebooks: List<Notebook>): List<Note>
    suspend fun existsNote(notebookPath: String, noteId: String): Boolean
    //suspend fun exportToZip(notes: List<Note>, notebooks: List<Notebook>, password: String?): Uri
}