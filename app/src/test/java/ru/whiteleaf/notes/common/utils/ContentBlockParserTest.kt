package ru.whiteleaf.notes.common.utils

import org.junit.Assert.*
import org.junit.Test
import ru.whiteleaf.notes.domain.model.ContentBlock

class ContentBlockParserTest {

    // === parse() - пустой контент ===

    @Test
    fun `parse should return empty list for empty string`() {
        val result = ContentBlockParser.parse("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parse should return empty list for blank string`() {
        val result = ContentBlockParser.parse("   \n  \n  ")
        assertTrue(result.isEmpty())
    }

    // === parse() - текстовые блоки ===

    @Test
    fun `parse should create Text block for plain text`() {
        val result = ContentBlockParser.parse("Hello world")
        assertEquals(1, result.size)
        assertTrue(result[0].isText())
        assertEquals("Hello world", (result[0] as ContentBlock.Text).text)
    }

    @Test
    fun `parse should join multiple lines into one Text block`() {
        val content = "Line 1\nLine 2\nLine 3"
        val result = ContentBlockParser.parse(content)
        assertEquals(1, result.size)
        assertEquals("Line 1\nLine 2\nLine 3", (result[0] as ContentBlock.Text).text)
    }

    @Test
    fun `parse should split text by empty lines`() {
        val content = "Paragraph 1\n\nParagraph 2"
        val result = ContentBlockParser.parse(content)
        assertEquals(2, result.size)
        assertTrue(result[0].isText())
        assertTrue(result[1].isText())
        assertEquals("Paragraph 1", (result[0] as ContentBlock.Text).text)
        assertEquals("Paragraph 2", (result[1] as ContentBlock.Text).text)
    }

    @Test
    fun `parse should handle multiple empty lines between paragraphs`() {
        val content = "Para 1\n\n\n\nPara 2"
        val result = ContentBlockParser.parse(content)
        assertEquals(2, result.size)
    }

    // === parse() - URL как картинки ===

    @Test
    fun `parse should create Image block for http URL`() {
        val result = ContentBlockParser.parse("https://example.com/image.png")
        assertEquals(1, result.size)
        assertTrue(result[0].isImage())
        assertEquals("https://example.com/image.png", (result[0] as ContentBlock.Image).imagePath)
    }

    @Test
    fun `parse should create Image block for https URL`() {
        val result = ContentBlockParser.parse("https://example.com/photo.jpg")
        assertEquals(1, result.size)
        assertTrue(result[0].isImage())
    }

    @Test
    fun `parse should create Image block for file URL`() {
        val result = ContentBlockParser.parse("file:///storage/emulated/0/image.png")
        assertEquals(1, result.size)
        assertTrue(result[0].isImage())
        assertEquals("file:///storage/emulated/0/image.png", (result[0] as ContentBlock.Image).imagePath)
    }

    @Test
    fun `parse should separate text and image`() {
        val content = "Some text\nhttps://example.com/image.png"
        val result = ContentBlockParser.parse(content)
        assertEquals(2, result.size)
        assertTrue(result[0].isText())
        assertTrue(result[1].isImage())
    }

    // === parse() - таблицы ===

    @Test
    fun `parse should create Table block for comma-separated values`() {
        val result = ContentBlockParser.parse("Name,Age\nAlice,30\nBob,25")
        assertEquals(1, result.size)
        assertTrue(result[0].isTable())
        val table = result[0] as ContentBlock.Table
        assertEquals(3, table.rows.size)
        assertEquals(2, table.columnCount)
    }

    @Test
    fun `parse should create Table block for pipe-separated values`() {
        val result = ContentBlockParser.parse("Name|Age\nAlice|30")
        assertEquals(1, result.size)
        assertTrue(result[0].isTable())
    }

    @Test
    fun `parse should create Table block for semicolon-separated values`() {
        val result = ContentBlockParser.parse("Name;Age\nAlice;30")
        assertEquals(1, result.size)
        assertTrue(result[0].isTable())
    }

    @Test
    fun `parse should extract headers from first table row`() {
        val result = ContentBlockParser.parse("Name,Age\nAlice,30")
        val table = result[0] as ContentBlock.Table
        assertEquals(listOf("Name", "Age"), table.headers)
    }

    @Test
    fun `parse should separate text and table`() {
        val content = "Before table\nName,Age\nAlice,30\nAfter table"
        val result = ContentBlockParser.parse(content)
        assertEquals(3, result.size)
        assertTrue(result[0].isText())
        assertTrue(result[1].isTable())
        assertTrue(result[2].isText())
    }

    // === serialize() ===

    @Test
    fun `serialize should convert Text block to string`() {
        val blocks = listOf(ContentBlock.Text("Hello"))
        val result = ContentBlockParser.serialize(blocks)
        assertEquals("Hello", result)
    }

    @Test
    fun `serialize should convert Image block to URL string`() {
        val blocks = listOf(ContentBlock.Image("https://example.com/img.png"))
        val result = ContentBlockParser.serialize(blocks)
        assertEquals("https://example.com/img.png", result)
    }

    @Test
    fun `serialize should convert Table block to pipe-separated string`() {
        val table = ContentBlock.Table(listOf(
            listOf("Name", "Age"),
            listOf("Alice", "30")
        ))
        val blocks = listOf(table)
        val result = ContentBlockParser.serialize(blocks)
        assertTrue(result.contains("Name|Age"))
        assertTrue(result.contains("Alice|30"))
    }

    @Test
    fun `serialize should join multiple blocks with newlines`() {
        val blocks = listOf(
            ContentBlock.Text("Hello"),
            ContentBlock.Image("https://example.com/img.png")
        )
        val result = ContentBlockParser.serialize(blocks)
        val lines = result.split("\n\n")
        assertEquals(2, lines.size)
    }

    @Test
    fun `serialize should return empty string for empty list`() {
        val result = ContentBlockParser.serialize(emptyList())
        assertEquals("", result)
    }

    // === Round-trip ===

    @Test
    fun `parse and serialize should be consistent for text`() {
        val original = "Hello world"
        val blocks = ContentBlockParser.parse(original)
        val restored = ContentBlockParser.serialize(blocks)
        assertEquals(original, restored)
    }

    @Test
    fun `parse and serialize should be consistent for image`() {
        val original = "https://example.com/image.png"
        val blocks = ContentBlockParser.parse(original)
        val restored = ContentBlockParser.serialize(blocks)
        assertEquals(original, restored)
    }

    // === Order field ===

    @Test
    fun `parse should assign increasing order to blocks`() {
        val content = "Para 1\n\nPara 2\n\nPara 3"
        val result = ContentBlockParser.parse(content)
        assertEquals(0, (result[0] as ContentBlock.Text).order)
        assertEquals(1, (result[1] as ContentBlock.Text).order)
        assertEquals(2, (result[2] as ContentBlock.Text).order)
    }

    // === Edge cases ===

    @Test
    fun `parse should handle single line`() {
        val result = ContentBlockParser.parse("Single line")
        assertEquals(1, result.size)
        assertTrue(result[0].isText())
    }

    @Test
    fun `parse should handle trailing newlines`() {
        val content = "Text\n\n"
        val result = ContentBlockParser.parse(content)
        assertEquals(1, result.size)
        assertTrue(result[0].isText())
    }

    @Test
    fun `parse should trim table cell values`() {
        val result = ContentBlockParser.parse("Name , Age \nAlice , 30")
        val table = result[0] as ContentBlock.Table
        assertEquals("Name", table.headers[0])
        assertEquals("Age", table.headers[1])
    }
}