package ru.whiteleaf.notes.domain.model

import org.junit.Assert.*
import org.junit.Test

class NotebookTest {

    @Test
    fun `Notebook should be created with required fields`() {
        val notebook = Notebook(
            path = "/test/notebook",
            createdAt = 1234567890L
        )

        assertEquals("/test/notebook", notebook.path)
        assertEquals(1234567890L, notebook.createdAt)
    }

    @Test
    fun `Notebook should have default noteCount of 0`() {
        val notebook = Notebook(
            path = "/test",
            createdAt = System.currentTimeMillis()
        )

        assertEquals(0, notebook.noteCount)
    }

    @Test
    fun `Notebook should have default isEncrypted as false`() {
        val notebook = Notebook(
            path = "/test",
            createdAt = System.currentTimeMillis()
        )

        assertFalse(notebook.isEncrypted)
    }

    @Test
    fun `Notebook should have default isUnlocked as false`() {
        val notebook = Notebook(
            path = "/test",
            createdAt = System.currentTimeMillis()
        )

        assertFalse(notebook.isUnlocked)
    }

    @Test
    fun `Notebook should have default isLastOpened as false`() {
        val notebook = Notebook(
            path = "/test",
            createdAt = System.currentTimeMillis()
        )

        assertFalse(notebook.isLastOpened)
    }

    @Test
    fun `Notebook should have default isPinned as false`() {
        val notebook = Notebook(
            path = "/test",
            createdAt = System.currentTimeMillis()
        )

        assertFalse(notebook.isPinned)
    }

    @Test
    fun `Notebook should have default modifiedAt as 0`() {
        val notebook = Notebook(
            path = "/test",
            createdAt = System.currentTimeMillis()
        )

        assertEquals(0L, notebook.modifiedAt)
    }

    @Test
    fun `Notebook should allow setting all properties`() {
        val notebook = Notebook(
            path = "/test",
            createdAt = 1000L,
            noteCount = 5,
            isEncrypted = true,
            isUnlocked = true,
            isLastOpened = true,
            isPinned = true,
            modifiedAt = 2000L
        )

        assertEquals("/test", notebook.path)
        assertEquals(1000L, notebook.createdAt)
        assertEquals(5, notebook.noteCount)
        assertTrue(notebook.isEncrypted)
        assertTrue(notebook.isUnlocked)
        assertTrue(notebook.isLastOpened)
        assertTrue(notebook.isPinned)
        assertEquals(2000L, notebook.modifiedAt)
    }

    @Test
    fun `Notebook should preserve russian path`() {
        val notebook = Notebook(
            path = "/рабочие/заметки",
            createdAt = System.currentTimeMillis()
        )

        assertEquals("/рабочие/заметки", notebook.path)
    }

    @Test
    fun `Notebook should handle empty path`() {
        val notebook = Notebook(
            path = "",
            createdAt = System.currentTimeMillis()
        )

        assertEquals("", notebook.path)
    }

    @Test
    fun `Notebook should handle special characters in path`() {
        val notebook = Notebook(
            path = "/test-notebook_2024/Jan 15",
            createdAt = System.currentTimeMillis()
        )

        assertEquals("/test-notebook_2024/Jan 15", notebook.path)
    }

    @Test
    fun `Notebook should handle zero createdAt`() {
        val notebook = Notebook(
            path = "/test",
            createdAt = 0L
        )

        assertEquals(0L, notebook.createdAt)
    }

    @Test
    fun `Notebook should handle negative createdAt`() {
        val notebook = Notebook(
            path = "/test",
            createdAt = -1000L
        )

        assertEquals(-1000L, notebook.createdAt)
    }

    @Test
    fun `Notebook should handle very large noteCount`() {
        val notebook = Notebook(
            path = "/test",
            createdAt = System.currentTimeMillis(),
            noteCount = Int.MAX_VALUE
        )

        assertEquals(Int.MAX_VALUE, notebook.noteCount)
    }

    @Test
    fun `Notebook should handle long path`() {
        val longPath = "/a".repeat(10000)
        val notebook = Notebook(
            path = longPath,
            createdAt = System.currentTimeMillis()
        )

        assertEquals(longPath, notebook.path)
    }

    @Test
    fun `Notebook properties should be immutable`() {
        val notebook = Notebook(
            path = "/test",
            createdAt = 1000L
        )

        assertEquals("/test", notebook.path)
        assertEquals(1000L, notebook.createdAt)
    }

    @Test
    fun `Notebook should distinguish encrypted and unlocked states`() {
        val encryptedButLocked = Notebook(
            path = "/test",
            createdAt = System.currentTimeMillis(),
            isEncrypted = true,
            isUnlocked = false
        )

        val encryptedAndUnlocked = Notebook(
            path = "/test",
            createdAt = System.currentTimeMillis(),
            isEncrypted = true,
            isUnlocked = true
        )

        assertTrue(encryptedButLocked.isEncrypted)
        assertFalse(encryptedButLocked.isUnlocked)
        assertTrue(encryptedAndUnlocked.isEncrypted)
        assertTrue(encryptedAndUnlocked.isUnlocked)
    }

    @Test
    fun `Notebook should handle multiple flags simultaneously`() {
        val notebook = Notebook(
            path = "/test",
            createdAt = System.currentTimeMillis(),
            noteCount = 10,
            isEncrypted = true,
            isUnlocked = true,
            isLastOpened = true,
            isPinned = true
        )

        assertEquals(10, notebook.noteCount)
        assertTrue(notebook.isEncrypted)
        assertTrue(notebook.isUnlocked)
        assertTrue(notebook.isLastOpened)
        assertTrue(notebook.isPinned)
    }

    @Test
    fun `Notebook should handle pinned but not encrypted`() {
        val pinnedNotEncrypted = Notebook(
            path = "/test",
            createdAt = System.currentTimeMillis(),
            isPinned = true,
            isEncrypted = false
        )

        assertTrue(pinnedNotEncrypted.isPinned)
        assertFalse(pinnedNotEncrypted.isEncrypted)
    }

    @Test
    fun `Notebook should handle last opened without encryption`() {
        val lastOpenedNotEncrypted = Notebook(
            path = "/test",
            createdAt = System.currentTimeMillis(),
            isLastOpened = true,
            isEncrypted = false
        )

        assertTrue(lastOpenedNotEncrypted.isLastOpened)
        assertFalse(lastOpenedNotEncrypted.isEncrypted)
    }
}
