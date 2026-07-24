package ru.whiteleaf.notes.domain.repository

import ru.whiteleaf.notes.data.model.RecentNote
import ru.whiteleaf.notes.domain.model.Note

interface PreferencesRepository {
    fun saveLastOpenedNotebook(notebookPath: String)
    fun getLastOpenedNotebook(): String?

    fun saveViewMode(notebookPath: String, isPlannerMode: Boolean)
    fun getViewMode(notebookPath: String, defaultIsPlanner: Boolean = false): Boolean

    fun saveNoteScrollPosition(noteId: String, notebookPath: String, scrollY: Int)

    fun getNoteScrollPosition(
        noteId: String,
        notebookPath: String
    ): Int?   // null – нет сохранённой позиции

    fun getRecentNotes(): List<RecentNote>
    fun saveRecentNote(note: Note)
    fun updateRecentNoteNotebookPath(
        noteId: String,
        oldNotebookPath: String?,
        newNotebookPath: String?
    ): Boolean

    fun removeRecentNotesByNotebookPath(notebookPath: String)
    fun removeRecentNote(noteId: String, notebookPath: String?)

    fun updateRecentEntry(oldNote:Note, newNote:Note) : Boolean
}