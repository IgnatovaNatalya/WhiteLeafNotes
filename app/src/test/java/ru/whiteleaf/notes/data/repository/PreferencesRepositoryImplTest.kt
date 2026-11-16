package ru.whiteleaf.notes.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import ru.whiteleaf.notes.domain.model.Note
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PreferencesRepositoryImplTest {

    private lateinit var gson: Gson
    private lateinit var prefs: SharedPreferences
    private lateinit var preferencesRepository: PreferencesRepositoryImpl

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        prefs = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()  // чистое состояние перед каждым тестом
        gson = Gson()
        preferencesRepository = PreferencesRepositoryImpl(prefs, gson)
    }

    // LAST OPENED NOTEBOOK

    @Test
    fun `saveLastOpenedNotebook should save path to preferences`() {
        preferencesRepository.saveLastOpenedNotebook("test")
        assertEquals("test", prefs.getString("last_notebook_path", null))
    }

    @Test
    fun `getLastOpenedNotebook should return saved path`() {
        preferencesRepository.saveLastOpenedNotebook("test")
        val result = preferencesRepository.getLastOpenedNotebook()
        assertEquals("test", result)
    }

    @Test
    fun `getLastOpenedNotebook should return null when not set`() {
        val result = preferencesRepository.getLastOpenedNotebook()
        assertNull(result)
    }

    // PINNED NOTEBOOKS

    @Test
    fun `pinNotebook should pin notebook`() {
        preferencesRepository.pinNotebook("test")
        assertTrue(prefs.getBoolean("notebook_pinned_test", false))
    }

    @Test
    fun `unpinNotebook should unpinned notebook`() {
        preferencesRepository.unpinNotebook("test")
        assertFalse(prefs.getBoolean("notebook_pinned_test", false))
    }

    @Test
    fun `isNotebookPinned should return true when pinned`() {
        preferencesRepository.pinNotebook("test")
        val result = preferencesRepository.isNotebookPinned("test")
        assertTrue(result)
    }

    @Test
    fun `isNotebookPinned should return false when not pinned`() {
        val result = preferencesRepository.isNotebookPinned("test")
        assertFalse(result)
    }

    // VIEW MODE

    @Test
    fun `saveViewMode should save planner mode`() {
        preferencesRepository.saveViewMode("test", true)
        assertTrue(prefs.getBoolean("view_mode_planner_test", false))
    }

    @Test
    fun `getViewMode should return true when planner mode enabled`() {
        val result = preferencesRepository.getViewMode("test", true)
        assertTrue(result)
    }

    @Test
    fun `getViewMode should return default when not set`() {
        val result = preferencesRepository.getViewMode("test", false)
        assertFalse(result)
    }

    // RECENT NOTES

    @Test
    fun `saveRecentNote should save note to recent list`() {
        val note = Note(
            id = "note_123",
            title = "Test Title",
            content = "Test Content",
            modifiedAt = System.currentTimeMillis(),
            notebookPath = "test"
        )

        preferencesRepository.saveRecentNote(note)

        val recent = preferencesRepository.getRecentNotes()
        assertEquals(1, recent.size)

        val saved = recent[0]
        assertEquals("note_123", saved.id)
        assertEquals("Test Title", saved.recentTitle)
        assertEquals("test", saved.notebookPath)
    }

    @Test
    fun `getRecentNotes should return empty list when no recent notes`() {
        val result = preferencesRepository.getRecentNotes()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `removeRecentNote should remove note from recent list`() {
        // Given: в prefs есть список с двумя заметками
        val note1 = Note("note_123", "Test", "Content", 1000L, "test")
        val note2 = Note("note_456", "Other", "Content", 2000L, "test")
        preferencesRepository.saveRecentNote(note1)
        preferencesRepository.saveRecentNote(note2)

        // When: удаляем первую
        preferencesRepository.removeRecentNote("note_123", "test")

        // Then: в списке осталась только вторая
        val recent = preferencesRepository.getRecentNotes()
        assertEquals(1, recent.size)
        assertEquals("note_456", recent[0].id)
        assertNull(recent.find { it.id == "note_123" })
    }

    @Test
    fun `updateRecentEntry should return true when note exists`() {
        val oldNote = Note(
            id = "note_123",
            title = "Old Title",
            content = "Content",
            modifiedAt = 1000L
        )
        val newNote = Note(
            id = "note_123",
            title = "New Title",
            content = "Content",
            modifiedAt = 1000L
        )
        preferencesRepository.saveRecentNote(oldNote)
        val result = preferencesRepository.updateRecentEntry(oldNote, newNote)

        assertTrue(result)
    }

    // SCROLL POSITION

    @Test
    fun `saveNoteScrollPosition should save scroll position`() {
        preferencesRepository.saveNoteScrollPosition("note_123", "test", 100)
        assertEquals(100, prefs.getInt("note_scroll_pos_test_note_123", -1))
    }

    @Test
    fun `saveNoteScrollPosition should not save when scroll is 0 and key not exists`() {
        preferencesRepository.saveNoteScrollPosition("note_123", "test", 0)

        // Ключ не должен появиться в SharedPreferences
        assertFalse(
            "Key should not be created when scroll is 0 and key doesn't exist",
            prefs.contains("note_scroll_pos_test_note_123")
        )
    }

    @Test
    fun `getNoteScrollPosition should return saved position`() {
        preferencesRepository.saveNoteScrollPosition("note_123", "test", 150)
        val result = preferencesRepository.getNoteScrollPosition("note_123", "test")
        assertEquals(150, result)
    }

    @Test
    fun `getNoteScrollPosition should return null when not set`() {
        val result = preferencesRepository.getNoteScrollPosition("note_123", "test")
        assertNull(result)
    }

    @Test
    fun `clearNoteScrollPosition should remove scroll position`() {
        preferencesRepository.saveNoteScrollPosition("note_123", "test", 150)
        preferencesRepository.clearNoteScrollPosition("note_123", "test")

        val result = preferencesRepository.getNoteScrollPosition("note_123", "test")
        assertNull(result)
    }

    // UPDATE NOTEBOOK PREFERENCES

    @Test
    fun `updateNotebookPreferences should update pinned key`() {
        // Given: старый путь закреплён в реальных SharedPreferences
        prefs.edit()
            .putBoolean("notebook_pinned_old path", true)
            .commit()

        // When: обновляем путь
        preferencesRepository.updateNotebookPreferences("old path", "new path")

        // Then: старый ключ удалён, новый — установлен
        assertFalse(
            "Old pinned key should be removed",
            prefs.contains("notebook_pinned_old path")
        )
        assertTrue(
            "New pinned key should be set to true",
            prefs.getBoolean("notebook_pinned_new path", false)
        )
    }

    @Test
    fun `updateNotebookPreferences should update view mode key`() {

        prefs.edit()
            .putBoolean("view_mode_planner_old path", true)
            .commit()

        preferencesRepository.updateNotebookPreferences("old path", "new path")

        assertFalse( prefs.contains("view_mode_planner_old path"))
        assertTrue(   prefs.getBoolean("view_mode_planner_new path", false))
    }

    @Test
    fun `removeNotebookPreferences should remove all notebook preferences`() {
        // Given: три ключа, относящихся к блокноту "test"
        prefs.edit()
            .putBoolean("notebook_pinned_test", true)
            .putBoolean("view_mode_planner_test", false)
            .putInt("note_scroll_pos_test_note_123", 100)
            .commit()

        // When: удаляем все preferences блокнота "test"
        preferencesRepository.removeNotebookPreferences("test")

        // Then: все три ключа удалены
        assertFalse(prefs.contains("notebook_pinned_test"))
        assertFalse(prefs.contains("view_mode_planner_test") )
        assertFalse(  prefs.contains("note_scroll_pos_test_note_123") )
    }

}
