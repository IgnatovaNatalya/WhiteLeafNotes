package ru.whiteleaf.notes.domain.repository

import android.net.Uri
import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.model.Notebook

interface NotesRepository {
    suspend fun getNotesList(notebookPath: String?): List<Note>
    suspend fun getNote(noteId:String, notebookPath:String?): Note
    suspend fun saveNote(note: Note)
    suspend fun deleteNote(note: Note)
    suspend fun moveNote(note: Note, targetNotebookPath: String?)
    suspend fun renameNote(note: Note, newName: String): Note
    suspend fun shareNoteFile(note: Note): Uri?
    suspend fun getAllNotes(notebooks: List<Notebook>): List<Note>
    suspend fun existsNote(notebookPath: String?, noteId: String): Boolean

    suspend fun updateNoteDate(note: Note, newTimestamp: Long)
    suspend fun moveNoteToMonth(note: Note, year: Int, month: Int)

    suspend fun encryptAllNotes(notebookPath: String)
    suspend fun decryptAllNotes(notebookPath: String)

    suspend fun getRecentNoteTitle(notebookPath: String?, noteId: String): String
}