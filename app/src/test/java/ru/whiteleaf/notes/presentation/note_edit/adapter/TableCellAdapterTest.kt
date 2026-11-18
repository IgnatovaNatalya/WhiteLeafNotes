package ru.whiteleaf.notes.presentation.note_edit.adapter

import org.junit.Assert.*
import org.junit.Test

class TableCellAdapterTest {

    @Test
    fun `TableCellAdapter should return correct row count`() {
        val rows = listOf(
            listOf("A", "B"),
            listOf("C", "D")
        )
        val adapter = TableCellAdapter(rows, 2)
        
        assertEquals(4, adapter.itemCount)
    }

    @Test
    fun `TableCellAdapter should return correct column count`() {
        val rows = listOf(
            listOf("A", "B", "C")
        )
        val adapter = TableCellAdapter(rows, 3)
        
        assertEquals(3, adapter.itemCount)
    }

    @Test
    fun `TableCellAdapter should return correct rows`() {
        val rows = listOf(
            listOf("Name", "Age"),
            listOf("Alice", "30")
        )
        val adapter = TableCellAdapter(rows, 2)
        
        assertEquals(rows, adapter.getRows())
    }

    @Test
    fun `TableCellAdapter should handle empty rows`() {
        val adapter = TableCellAdapter(emptyList(), 0)
        
        assertEquals(0, adapter.itemCount)
        assertTrue(adapter.getRows().isEmpty())
    }

    @Test
    fun `TableCellAdapter should handle single cell`() {
        val rows = listOf(listOf("Single"))
        val adapter = TableCellAdapter(rows, 1)
        
        assertEquals(1, adapter.itemCount)
        assertEquals(listOf(listOf("Single")), adapter.getRows())
    }

    @Test
    fun `TableCellAdapter should handle single row multiple columns`() {
        val rows = listOf(listOf("A", "B", "C", "D"))
        val adapter = TableCellAdapter(rows, 4)
        
        assertEquals(4, adapter.itemCount)
    }
}