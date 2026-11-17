package ru.whiteleaf.notes.common.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileUtilsTest {

    @Test
    fun `sanitizeFileName should remove forward slash`() {
        val input = "test/file"
        val result = FileUtils.sanitizeFileName(input)
        assertEquals("testfile", result)
    }

    @Test
    fun `sanitizeFileName should remove backslash`() {
        val input = "test\\file"
        val result = FileUtils.sanitizeFileName(input)
        assertEquals("testfile", result)
    }

    @Test
    fun `sanitizeFileName should remove colon`() {
        val input = "test:file"
        val result = FileUtils.sanitizeFileName(input)
        assertEquals("testfile", result)
    }

    @Test
    fun `sanitizeFileName should remove asterisk`() {
        val input = "test*file"
        val result = FileUtils.sanitizeFileName(input)
        assertEquals("testfile", result)
    }

    @Test
    fun `sanitizeFileName should remove question mark`() {
        val input = "test?file"
        val result = FileUtils.sanitizeFileName(input)
        assertEquals("testfile", result)
    }

    @Test
    fun `sanitizeFileName should remove double quote`() {
        val input = "test\"file"
        val result = FileUtils.sanitizeFileName(input)
        assertEquals("testfile", result)
    }

    @Test
    fun `sanitizeFileName should remove less than`() {
        val input = "test<file"
        val result = FileUtils.sanitizeFileName(input)
        assertEquals("testfile", result)
    }

    @Test
    fun `sanitizeFileName should remove greater than`() {
        val input = "test>file"
        val result = FileUtils.sanitizeFileName(input)
        assertEquals("testfile", result)
    }

    @Test
    fun `sanitizeFileName should remove pipe`() {
        val input = "test|file"
        val result = FileUtils.sanitizeFileName(input)
        assertEquals("testfile", result)
    }

    @Test
    fun `sanitizeFileName should remove multiple special characters`() {
        val input = "test/file\\name:with*all?\"of<the>pipe|chars"
        val result = FileUtils.sanitizeFileName(input)
        assertEquals("testfilenamewithallofthepipechars", result)
    }

    @Test
    fun `sanitizeFileName should keep alphanumeric characters`() {
        val input = "test-file_123ABC"
        val result = FileUtils.sanitizeFileName(input)
        assertEquals("test-file_123ABC", result)
    }

    @Test
    fun `sanitizeFileName should keep hyphens and underscores`() {
        val input = "my-note_file"
        val result = FileUtils.sanitizeFileName(input)
        assertEquals("my-note_file", result)
    }

    @Test
    fun `sanitizeFileName should keep spaces`() {
        val input = "my note file"
        val result = FileUtils.sanitizeFileName(input)
        assertEquals("my note file", result)
    }

    @Test
    fun `sanitizeFileName should handle empty string`() {
        val input = ""
        val result = FileUtils.sanitizeFileName(input)
        assertEquals("", result)
    }

    @Test
    fun `sanitizeFileName should handle string with only special chars`() {
        val input = "/\\:*?\"<>|"
        val result = FileUtils.sanitizeFileName(input)
        assertEquals("", result)
    }

    @Test
    fun `sanitizeFileName should handle null characters`() {
        val input = "test" + "\u0000" + "file"
        val result = FileUtils.sanitizeFileName(input)
        // null character is not in the replacement list, so it should remain
        assertEquals("test" + "\u0000" + "file", result)
    }

    @Test
    fun `generateNoteId should return string with prefix note_`() {
        val result = FileUtils.generateNoteId()
        assertTrue(result.startsWith(FileUtils.FILE_NAME_PREFIX))
    }

    @Test
    fun `generateNoteId should return string with numeric suffix`() {
        val result = FileUtils.generateNoteId()
        val suffix = result.substring(FileUtils.FILE_NAME_PREFIX.length)
        assertTrue(suffix.all { it.isDigit() })
    }

    @Test
    fun `generateNoteId should return valid length`() {
        val result = FileUtils.generateNoteId()
        // "note_" (5 chars) + timestamp (13 digits for millis) = 18 chars
        assertEquals(18, result.length)
    }

    @Test
    fun `generateNoteId should return unique values`() {
        val result1 = FileUtils.generateNoteId()
        Thread.sleep(1)
        val result2 = FileUtils.generateNoteId()
        assertFalse(result1 == result2)
    }

    @Test
    fun `generateNoteId should contain current timestamp`() {
        val before = System.currentTimeMillis()
        val noteId = FileUtils.generateNoteId()
        val after = System.currentTimeMillis()

        val timestamp = noteId.substring(FileUtils.FILE_NAME_PREFIX.length).toLong()
        assertTrue(timestamp >= before)
        assertTrue(timestamp <= after)
    }

    @Test
    fun `FILE_NAME_PREFIX should be note_`() {
        assertEquals("note_", FileUtils.FILE_NAME_PREFIX)
    }

    @Test
    fun `generateNoteId should generate multiple unique ids`() {
        val ids = (1..100).map {
            Thread.sleep((1..3).random().toLong())
            FileUtils.generateNoteId()
        }
        val uniqueIds = ids.distinct()
        // All 100 IDs should be unique
        assertEquals(100, uniqueIds.size)
    }

    @Test
    fun `sanitizeFileName should handle single special character`() {
        val input = "/"
        val result = FileUtils.sanitizeFileName(input)
        assertEquals("", result)
    }

    @Test
    fun `sanitizeFileName should handle mixed valid and invalid characters`() {
        val input = "note_2024-01-15_file.txt"
        val result = FileUtils.sanitizeFileName(input)
        assertEquals("note_2024-01-15_file.txt", result)
    }

    @Test
    fun `sanitizeFileName should handle unicode characters`() {
        val input = "тест_файл_тест"
        val result = FileUtils.sanitizeFileName(input)
        assertEquals("тест_файл_тест", result)
    }

    @Test
    fun `sanitizeFileName should handle emoji characters`() {
        val input = "note😀file"
        val result = FileUtils.sanitizeFileName(input)
        assertEquals("note😀file", result)
    }

    @Test
    fun `generateNoteId should produce consistent format`() {
        val ids = (1..10).map { FileUtils.generateNoteId() }
        ids.forEach { id ->
            assertTrue(id.startsWith("note_"))
            val suffix = id.substring(5)
            assertTrue(suffix.all { it.isDigit() })
            assertEquals(18, id.length)
        }
    }
}
