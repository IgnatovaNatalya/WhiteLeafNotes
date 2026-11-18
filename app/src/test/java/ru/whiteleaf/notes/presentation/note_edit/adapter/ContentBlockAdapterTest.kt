package ru.whiteleaf.notes.presentation.note_edit.adapter

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.whiteleaf.notes.domain.model.ContentBlock

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class ContentBlockAdapterTest {

    @Test
    fun `adapter should return correct view type for Text block`() {
        val adapter = ContentBlockAdapter()
        val textBlock = ContentBlock.Text("Test")
        adapter.submitList(listOf(textBlock))
        
        assertEquals(0, adapter.getItemViewType(0))
    }

    @Test
    fun `adapter should return correct view type for Image block`() {
        val adapter = ContentBlockAdapter()
        val imageBlock = ContentBlock.Image("/path/to/image.png")
        adapter.submitList(listOf(imageBlock))
        
        assertEquals(1, adapter.getItemViewType(0))
    }

    @Test
    fun `adapter should return correct view type for Table block`() {
        val adapter = ContentBlockAdapter()
        val tableBlock = ContentBlock.Table(listOf(listOf("A", "B")))
        adapter.submitList(listOf(tableBlock))
        
        assertEquals(2, adapter.getItemViewType(0))
    }

    @Test
    fun `adapter should return correct item count`() {
        val adapter = ContentBlockAdapter()
        val blocks = listOf(
            ContentBlock.Text("Text 1"),
            ContentBlock.Image("/path.png"),
            ContentBlock.Table(listOf(listOf("A", "B")))
        )
        adapter.submitList(blocks)
        
        assertEquals(3, adapter.itemCount)
    }

    @Test
    fun `adapter should return correct content for Text block`() {
        val adapter = ContentBlockAdapter()
        adapter.submitList(listOf(ContentBlock.Text("Hello world")))
        
        assertEquals("Hello world", adapter.getContentAt(0))
    }

    @Test
    fun `adapter should return correct content for Image block`() {
        val adapter = ContentBlockAdapter()
        adapter.submitList(listOf(ContentBlock.Image("/path.png", "My caption")))
        
        assertEquals("My caption", adapter.getContentAt(0))
    }

    @Test
    fun `adapter should return correct content for Table block`() {
        val adapter = ContentBlockAdapter()
        val table = ContentBlock.Table(listOf(
            listOf("Name", "Age"),
            listOf("Alice", "30")
        ))
        adapter.submitList(listOf(table))
        
        val content = adapter.getContentAt(0)
        assertTrue(content.contains("Name|Age"))
        assertTrue(content.contains("Alice|30"))
    }

    @Test
    fun `adapter should handle empty list`() {
        val adapter = ContentBlockAdapter()
        adapter.submitList(emptyList())
        
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `adapter should handle mixed block types`() {
        val adapter = ContentBlockAdapter()
        val blocks = listOf(
            ContentBlock.Text("First"),
            ContentBlock.Text("Second"),
            ContentBlock.Image("/img.png"),
            ContentBlock.Table(listOf(listOf("A"))),
            ContentBlock.Text("Third")
        )
        adapter.submitList(blocks)
        
        assertEquals(5, adapter.itemCount)
        assertEquals(0, adapter.getItemViewType(0))
        assertEquals(0, adapter.getItemViewType(1))
        assertEquals(1, adapter.getItemViewType(2))
        assertEquals(2, adapter.getItemViewType(3))
        assertEquals(0, adapter.getItemViewType(4))
    }

    @Test
    fun `adapter should update list correctly`() {
        val adapter = ContentBlockAdapter()
        adapter.submitList(listOf(ContentBlock.Text("Old")))
        
        assertEquals(1, adapter.itemCount)
        assertEquals("Old", adapter.getContentAt(0))
        
        adapter.submitList(listOf(ContentBlock.Text("New")))
        
        assertEquals(1, adapter.itemCount)
        assertEquals("New", adapter.getContentAt(0))
    }

    @Test
    fun `adapter should handle null text in Text block`() {
        val adapter = ContentBlockAdapter()
        adapter.submitList(listOf(ContentBlock.Text("")))
        
        assertEquals("", adapter.getContentAt(0))
    }

    @Test
    fun `adapter should return correct item at position`() {
        val adapter = ContentBlockAdapter()
        val blocks = listOf(
            ContentBlock.Text("First"),
            ContentBlock.Image("/path.png"),
            ContentBlock.Table(listOf(listOf("A")))
        )
        adapter.submitList(blocks)
        
        assertTrue(adapter.getItem(0).isText())
        assertTrue(adapter.getItem(1).isImage())
        assertTrue(adapter.getItem(2).isTable())
    }
}