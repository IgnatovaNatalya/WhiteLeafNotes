package ru.whiteleaf.notes.domain.model

import org.junit.Assert.*
import org.junit.Test

class ContentBlockTest {

    // === Text Block ===

    @Test
    fun `Text block should store text and order`() {
        val block = ContentBlock.Text("Hello", order = 0)
        assertEquals("Hello", block.text)
        assertEquals(0, block.order)
    }

    @Test
    fun `Text block should default order to 0`() {
        val block = ContentBlock.Text("Test")
        assertEquals(0, block.order)
    }

    @Test
    fun `Text block should be identified as text`() {
        val block: ContentBlock = ContentBlock.Text("Test")
        assertTrue(block.isText())
        assertFalse(block.isImage())
        assertFalse(block.isTable())
    }

    @Test
    fun `ContentBlock text should create Text block`() {
        val block = ContentBlock.text("Hello", order = 1)
        assertTrue(block.isText())
        assertEquals("Hello", block.text)
        assertEquals(1, block.order)
    }

    // === Image Block ===

    @Test
    fun `Image block should store imagePath and caption`() {
        val block = ContentBlock.Image("/path/to/image.png", "Caption", order = 1)
        assertEquals("/path/to/image.png", block.imagePath)
        assertEquals("Caption", block.caption)
        assertEquals(1, block.order)
    }

    @Test
    fun `Image block should default caption to empty string`() {
        val block = ContentBlock.Image("/path/to/image.png")
        assertEquals("", block.caption)
    }

    @Test
    fun `Image block should be identified as image`() {
        val block: ContentBlock = ContentBlock.Image("/path/to/image.png")
        assertFalse(block.isText())
        assertTrue(block.isImage())
        assertFalse(block.isTable())
    }

    @Test
    fun `ContentBlock image should create Image block`() {
        val block = ContentBlock.image("/img.png", "My image", order = 2)
        assertTrue(block.isImage())
        assertEquals("/img.png", block.imagePath)
        assertEquals("My image", block.caption)
    }

    // === Table Block ===

    @Test
    fun `Table block should store rows`() {
        val rows = listOf(
            listOf("Name", "Age"),
            listOf("Alice", "30"),
            listOf("Bob", "25")
        )
        val block = ContentBlock.Table(rows, order = 0)
        assertEquals(3, block.rows.size)
        assertEquals(2, block.columnCount)
        assertEquals(2, block.rowCount)
    }

    @Test
    fun `Table block should extract headers from first row`() {
        val rows = listOf(
            listOf("Name", "Age"),
            listOf("Alice", "30")
        )
        val block = ContentBlock.Table(rows)
        assertEquals(listOf("Name", "Age"), block.headers)
    }

    @Test
    fun `Table block should extract data rows excluding header`() {
        val rows = listOf(
            listOf("Name", "Age"),
            listOf("Alice", "30"),
            listOf("Bob", "25")
        )
        val block = ContentBlock.Table(rows)
        assertEquals(2, block.dataRows.size)
        assertEquals(listOf("Alice", "30"), block.dataRows[0])
    }

    @Test
    fun `Table block should return empty data rows when no data`() {
        val block = ContentBlock.Table(listOf(listOf("Name", "Age")))
        assertTrue(block.dataRows.isEmpty())
        assertEquals(0, block.rowCount)
    }

    @Test
    fun `Table block should be identified as table`() {
        val block: ContentBlock = ContentBlock.Table(listOf(listOf("A", "B")))
        assertFalse(block.isText())
        assertFalse(block.isImage())
        assertTrue(block.isTable())
    }

    @Test
    fun `ContentBlock table should create empty table`() {
        val block = ContentBlock.table(order = 0)
        assertTrue(block.isTable())
        assertTrue(block.rows.isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Table block should throw when rows have different column counts`() {
        val rows = listOf(
            listOf("A", "B"),
            listOf("C") // Only 1 column, should throw
        )
        ContentBlock.Table(rows)
    }

    @Test
    fun `Table block should allow empty rows`() {
        val block = ContentBlock.Table(emptyList())
        assertTrue(block.headers.isEmpty())
        assertEquals(0, block.columnCount)
        assertEquals(0, block.rowCount)
    }

    // === Sealed class behavior ===

    @Test
    fun `Different block types should not be equal`() {
        val text = ContentBlock.Text("Test")
        val image = ContentBlock.Image("/path.png")
        val table = ContentBlock.Table(listOf(listOf("A")))

        assertNotEquals(text, image)
        assertNotEquals(text, table)
        assertNotEquals(image, table)
    }

    @Test
    fun `Same block types with same data should be equal`() {
        val text1 = ContentBlock.Text("Test", 0)
        val text2 = ContentBlock.Text("Test", 0)
        assertEquals(text1, text2)

        val img1 = ContentBlock.Image("/path.png", "caption", 1)
        val img2 = ContentBlock.Image("/path.png", "caption", 1)
        assertEquals(img1, img2)
    }

    @Test
    fun `Text block should store order field`() {
        val block = ContentBlock.Text("Test", order = 5)
        assertEquals(5, block.order)
    }

    @Test
    fun `Image block should store order field`() {
        val block = ContentBlock.Image("/path.png", order = 3)
        assertEquals(3, block.order)
    }

    @Test
    fun `Table block should store order field`() {
        val block = ContentBlock.Table(listOf(listOf("A", "B")), order = 7)
        assertEquals(7, block.order)
    }
}