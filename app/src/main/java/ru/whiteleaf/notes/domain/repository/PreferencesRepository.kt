package ru.whiteleaf.notes.domain.repository

import ru.whiteleaf.notes.data.model.RecentNote
import ru.whiteleaf.notes.domain.model.Note

interface PreferencesRepository {
    fun saveLastOpenedNotebook(notebookPath: String)
    fun getLastOpenedNotebook(): String?

    fun saveViewMode(notebookPath: String, isPlannerMode: Boolean)
    fun getViewMode(notebookPath: String, defaultIsPlanner: Boolean = false): Boolean

    fun pinNotebook(notebookPath: String)
    fun unpinNotebook(notebookPath: String)
    fun isNotebookPinned(notebookPath: String):Boolean

    fun saveNoteScrollPosition(noteId: String, notebookPath: String, scrollY: Int)

    fun getNoteScrollPosition(
        noteId: String,
        notebookPath: String
    ): Int?   // null – нет сохранённой позиции

    fun clearNoteScrollPosition(noteId:String, notebookPath: String)
    fun updateNoteScrollPosition(oldNote:Note, newNote:Note)

    fun getRecentNotes(): List<RecentNote>
    fun saveRecentNote(note: Note)
    fun updateRecentNoteNotebookPath(
        noteId: String,
        oldNotebookPath: String?,
        newNotebookPath: String?
    ): Boolean
    fun updateRecentEntry(oldNote:Note, newNote:Note) : Boolean
    fun removeRecentNotesByNotebookPath(notebookPath: String)
    fun removeRecentNote(noteId: String, notebookPath: String?)
    fun updateNotebookPathInRecent(oldNotebookPath: String?, newNotebookPath: String?)

    fun updateNotebookPreferences(oldPath: String, newPath: String)
    fun removeNotebookPreferences(notebookPath: String)


}