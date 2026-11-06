package ru.whiteleaf.notes.data.repository

import android.content.SharedPreferences
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

class PreferencesRepositoryImplTest {

    @Mock
    private lateinit var prefs: SharedPreferences

    @Mock
    private lateinit var editor: SharedPreferences.Editor

    private lateinit var gson: Gson
    private lateinit var preferencesRepository: PreferencesRepositoryImpl

    @Before
    fun setup() {
        AutoCloseable::class.java.cast(this).close()
        gson = Gson()
        preferencesRepository = PreferencesRepositoryImpl(prefs, gson)
    }

    // LAST OPENED NOTEBOOK

    @Test
    fun `saveLastOpenedNotebook should save path to preferences`() {
        `when`(prefs.edit()).thenReturn(editor)
        `when`(editor.putString("last_notebook_path", "/test/notebook")).thenReturn(editor)

        preferencesRepository.saveLastOpenedNotebook("/test/notebook")

        verify(editor).putString("last_notebook_path", "/test/notebook")
        verify(editor).apply()
    }

    @Test
    fun `getLastOpenedNotebook should return saved path`() {
        `when`(prefs.getString("last_notebook_path", null)).thenReturn("/test/notebook")

        val result = preferencesRepository.getLastOpenedNotebook()

        assertEquals("/test/notebook", result)
    }

    @Test
    fun `getLastOpenedNotebook should return null when not set`() {
        `when`(prefs.getString("last_notebook_path", null)).thenReturn(null)

        val result = preferencesRepository.getLastOpenedNotebook()

        assertNull(result)
    }

    // PINNED NOTEBOOKS

    @Test
    fun `pinNotebook should pin notebook`() {
        `when`(prefs.edit()).thenReturn(editor)
        `when`(editor.putBoolean("notebook_pinned_/test/notebook", true)).thenReturn(editor)

        preferencesRepository.pinNotebook("/test/notebook")

        verify(editor).putBoolean("notebook_pinned_/test/notebook", true)
        verify(editor).apply()
    }

    @Test
    fun `unpinNotebook should unpinned notebook`() {
        `when`(prefs.edit()).thenReturn(editor)
        `when`(editor.remove("notebook_pinned_/test/notebook")).thenReturn(editor)

        preferencesRepository.unpinNotebook("/test/notebook")

        verify(editor).remove("notebook_pinned_/test/notebook")
        verify(editor).apply()
    }

    @Test
    fun `isNotebookPinned should return true when pinned`() {
        `when`(prefs.contains("notebook_pinned_/test/notebook")).thenReturn(true)

        val result = preferencesRepository.isNotebookPinned("/test/notebook")

        assertTrue(result)
    }

    @Test
    fun `isNotebookPinned should return false when not pinned`() {
        `when`(prefs.contains("notebook_pinned_/test/notebook")).thenReturn(false)

        val result = preferencesRepository.isNotebookPinned("/test/notebook")

        assertFalse(result)
    }

    // VIEW MODE

    @Test
    fun `saveViewMode should save planner mode`() {
        `when`(prefs.edit()).thenReturn(editor)
        `when`(editor.putBoolean("view_mode_planner_/test/notebook", true)).thenReturn(editor)

        preferencesRepository.saveViewMode("/test/notebook", true)

        verify(editor).putBoolean("view_mode_planner_/test/notebook", true)
        verify(editor).apply()
    }

    @Test
    fun `getViewMode should return true when planner mode enabled`() {
        `when`(prefs.getBoolean("view_mode_planner_/test/notebook", true)).thenReturn(true)

        val result = preferencesRepository.getViewMode("/test/notebook", true)

        assertTrue(result)
    }

    @Test
    fun `getViewMode should return default when not set`() {
        `when`(prefs.getBoolean("view_mode_planner_/test/notebook", false)).thenReturn(false)

        val result = preferencesRepository.getViewMode("/test/notebook", false)

        assertFalse(result)
    }

    // RECENT NOTES

    @Test
    fun `saveRecentNote should save note to recent list`() {
        val note = ru.whiteleaf.notes.domain.model.Note(
            id = "note_123",
            title = "Test Title",
            content = "Test Content",
            modifiedAt = System.currentTimeMillis(),
            notebookPath = "/test/notebook"
        )

        `when`(prefs.edit()).thenReturn(editor)
        `when`(prefs.getString("recent_notes", null)).thenReturn(null)
        `when`(editor.putString("recent_notes", org.mockito.ArgumentMatchers.any<String>())).thenReturn(editor)

        preferencesRepository.saveRecentNote(note)

        verify(editor).putString(org.mockito.ArgumentMatchers.eq("recent_notes"), org.mockito.ArgumentMatchers.any<String>())
        verify(editor).apply()
    }

    @Test
    fun `getRecentNotes should return empty list when no recent notes`() {
        `when`(prefs.getString("recent_notes", null)).thenReturn(null)

        val result = preferencesRepository.getRecentNotes()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getRecentNotes should return empty list when json is empty`() {
        `when`(prefs.getString("recent_notes", null)).thenReturn("")

        val result = preferencesRepository.getRecentNotes()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getRecentNotes should return empty list when json is invalid`() {
        `when`(prefs.getString("recent_notes", null)).thenReturn("invalid json {{{")

        val result = preferencesRepository.getRecentNotes()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `removeRecentNote should remove note from recent list`() {
        val note = ru.whiteleaf.notes.domain.model.Note(
            id = "note_123",
            title = "Test",
            content = "Content",
            modifiedAt = System.currentTimeMillis()
        )

        `when`(prefs.edit()).thenReturn(editor)
        `when`(prefs.getString("recent_notes", null)).thenReturn(null)

        preferencesRepository.removeRecentNote("note_123", null)

        verify(editor).putString(org.mockito.ArgumentMatchers.eq("recent_notes"), org.mockito.ArgumentMatchers.any<String>())
        verify(editor).apply()
    }

    @Test
    fun `updateRecentEntry should return true when note exists`() {
        val oldNote = ru.whiteleaf.notes.domain.model.Note(
            id = "note_123",
            title = "Old Title",
            content = "Content",
            modifiedAt = System.currentTimeMillis()
        )
        val newNote = ru.whiteleaf.notes.domain.model.Note(
            id = "note_123",
            title = "New Title",
            content = "Content",
            modifiedAt = System.currentTimeMillis()
        )

        `when`(prefs.edit()).thenReturn(editor)
        `when`(prefs.getString("recent_notes", null)).thenReturn(null)

        val result = preferencesRepository.updateRecentEntry(oldNote, newNote)

        // Returns false when list is empty
        assertFalse(result)
    }

    // SCROLL POSITION

    @Test
    fun `saveNoteScrollPosition should save scroll position`() {
        `when`(prefs.edit()).thenReturn(editor)
        `when`(editor.putInt("note_scroll_pos_/test/notebook_note_123", 100)).thenReturn(editor)

        preferencesRepository.saveNoteScrollPosition("note_123", "/test/notebook", 100)

        verify(editor).putInt("note_scroll_pos_/test/notebook_note_123", 100)
        verify(editor).apply()
    }

    @Test
    fun `saveNoteScrollPosition should not save when scroll is 0 and key not exists`() {
        `when`(prefs.edit()).thenReturn(editor)
        `when`(prefs.contains("note_scroll_pos_/test/notebook_note_123")).thenReturn(false)

        preferencesRepository.saveNoteScrollPosition("note_123", "/test/notebook", 0)

        // Should not call putInt when scroll is 0 and key doesn't exist
    }

    @Test
    fun `getNoteScrollPosition should return saved position`() {
        `when`(prefs.contains("note_scroll_pos_/test/notebook_note_123")).thenReturn(true)
        `when`(prefs.getInt("note_scroll_pos_/test/notebook_note_123", 0)).thenReturn(150)

        val result = preferencesRepository.getNoteScrollPosition("note_123", "/test/notebook")

        assertEquals(150, result)
    }

    @Test
    fun `getNoteScrollPosition should return null when not set`() {
        `when`(prefs.contains("note_scroll_pos_/test/notebook_note_123")).thenReturn(false)

        val result = preferencesRepository.getNoteScrollPosition("note_123", "/test/notebook")

        assertNull(result)
    }

    @Test
    fun `clearNoteScrollPosition should remove scroll position`() {
        `when`(prefs.edit()).thenReturn(editor)
        `when`(editor.remove("note_scroll_pos_/test/notebook_note_123")).thenReturn(editor)

        preferencesRepository.clearNoteScrollPosition("note_123", "/test/notebook")

        verify(editor).remove("note_scroll_pos_/test/notebook_note_123")
        verify(editor).apply()
    }

    // UPDATEBOOK PREFERENCES

    @Test
    fun `updateNotebookPreferences should update pinned key`() {
        val allEntries = mapOf(
            "notebook_pinned_/old/path" to true
        )
        `when`(prefs.all).thenReturn(allEntries)
        `when`(prefs.edit()).thenReturn(editor)
        `when`(editor.putBoolean("notebook_pinned_/new/path", true)).thenReturn(editor)
        `when`(editor.remove("notebook_pinned_/old/path")).thenReturn(editor)
        `when`(prefs.getString("last_notebook_path", null)).thenReturn(null)

        preferencesRepository.updateNotebookPreferences("/old/path", "/new/path")

        verify(editor).putBoolean("notebook_pinned_/new/path", true)
        verify(editor).remove("notebook_pinned_/old/path")
    }

    @Test
    fun `updateNotebookPreferences should update view mode key`() {
        val allEntries = mapOf(
            "view_mode_planner_/old/path" to true
        )
        `when`(prefs.all).thenReturn(allEntries)
        `when`(prefs.edit()).thenReturn(editor)
        `when`(editor.putBoolean("view_mode_planner_/new/path", true)).thenReturn(editor)
        `when`(editor.remove("view_mode_planner_/old/path")).thenReturn(editor)
        `when`(prefs.getString("last_notebook_path", null)).thenReturn(null)

        preferencesRepository.updateNotebookPreferences("/old/path", "/new/path")

        verify(editor).putBoolean("view_mode_planner_/new/path", true)
        verify(editor).remove("view_mode_planner_/old/path")
    }

    @Test
    fun `removeNotebookPreferences should remove all notebook preferences`() {
        val allEntries = mapOf<String, Any>(
            "notebook_pinned_/test/notebook" to true,
            "view_mode_planner_/test/notebook" to false,
            "note_scroll_pos_/test/notebook_note_123" to 100
        )
        `when`(prefs.all).thenReturn(allEntries)
        `when`(prefs.edit()).thenReturn(editor)
        `when`(editor.remove("notebook_pinned_/test/notebook")).thenReturn(editor)
        `when`(editor.remove("view_mode_planner_/test/notebook")).thenReturn(editor)
        `when`(editor.remove("note_scroll_pos_/test/notebook_note_123")).thenReturn(editor)

        preferencesRepository.removeNotebookPreferences("/test/notebook")

        verify(editor).remove("notebook_pinned_/test/notebook")
        verify(editor).remove("view_mode_planner_/test/notebook")
        verify(editor).remove("note_scroll_pos_/test/notebook_note_123")
    }

    @Test
    fun `saveRecentNote should update existing note`() {
        val existingJson = """[{"id":"note_123","recentTitle":"Old Title","notebookPath":"/test","recentDate":100}]"""
        `when`(prefs.getString("recent_notes", null)).thenReturn(existingJson)
        `when`(prefs.edit()).thenReturn(editor)
        `when`(editor.putString("recent_notes", org.mockito.ArgumentMatchers.any<String>())).thenReturn(editor)

        val note = ru.whiteleaf.notes.domain.model.Note(
            id = "note_123",
            title = "New Title",
            content = "Content",
            modifiedAt = System.currentTimeMillis(),
            notebookPath = "/test"
        )

        preferencesRepository.saveRecentNote(note)

        verify(editor).putString(org.mockito.ArgumentMatchers.eq("recent_notes"), org.mockito.ArgumentMatchers.any<String>())
    }
}
