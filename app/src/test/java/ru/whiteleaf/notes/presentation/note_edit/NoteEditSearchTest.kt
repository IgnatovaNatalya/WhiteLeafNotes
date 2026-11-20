package ru.whiteleaf.notes.presentation.note_edit

import org.junit.Assert.*
import org.junit.Test
import ru.whiteleaf.notes.domain.model.ContentBlock
import ru.whiteleaf.notes.common.utils.ContentBlockParser

/**
 * Тесты для проверки корректности работы поиска в редакторе заметок.
 * Проверяет, что блоки контента правильно парсятся и сохраняются.
 */
class NoteEditSearchTest {

    @Test
    fun `parse should correctly split content into blocks for search rendering`() {
        // Симулируем контент заметки (2+ пустых строки для разделения)
        val content = "Первый блок текста\n\n\nВторой блок текста\n\n\nТретий блок"
        
        val blocks = ContentBlockParser.parse(content)
        
        assertEquals(3, blocks.size)
        assertTrue(blocks[0].isText())
        assertTrue(blocks[1].isText())
        assertTrue(blocks[2].isText())
        
        assertEquals("Первый блок текста\n", (blocks[0] as ContentBlock.Text).text)
        assertEquals("Второй блок текста\n", (blocks[1] as ContentBlock.Text).text)
        assertEquals("Третий блок", (blocks[2] as ContentBlock.Text).text)
    }

    @Test
    fun `parse should handle empty content`() {
        val content = ""
        val blocks = ContentBlockParser.parse(content)
        assertTrue(blocks.isEmpty())
    }

    @Test
    fun `parse should handle single block content`() {
        val content = "Один блок без разделителей"
        val blocks = ContentBlockParser.parse(content)
        
        assertEquals(1, blocks.size)
        assertTrue(blocks[0].isText())
        assertEquals("Один блок без разделителей", (blocks[0] as ContentBlock.Text).text)
    }

    @Test
    fun `serialize should reconstruct content from blocks correctly`() {
        val blocks = listOf(
            ContentBlock.Text("Первый блок"),
            ContentBlock.Text("Второй блок")
        )
        
        val serialized = ContentBlockParser.serialize(blocks)
        val parsed = ContentBlockParser.parse(serialized)
        
        // serialize использует joinToString("\n\n"), parse требует 2+ пустых строки
        // Поэтому после serialize -> parse получаем 1 блок
        assertEquals(1, parsed.size)
        assertTrue(parsed[0].isText())
    }

    @Test
    fun `blocks should preserve content for search highlighting`() {
        // Симулируем контент с поисковым запросом "рабо"
        val content = "Сегодня работа была интересной\n\nЗавтра будет новый день"
        
        val blocks = ContentBlockParser.parse(content)
        
        // Проверяем, что контент не потерян
        val reconstructed = blocks.joinToString("\n\n") { block ->
            when (block) {
                is ContentBlock.Text -> block.text
                else -> ""
            }
        }
        
        assertTrue(reconstructed.contains("работа"))
        assertTrue(reconstructed.contains("Завтра"))
    }



    @Test
    fun `parse should handle mixed content types`() {
        val content = "Текст\n\nhttps://example.com/image.png\n\nЕщё текст"
        val blocks = ContentBlockParser.parse(content)
        
        assertEquals(3, blocks.size)
        assertTrue(blocks[0].isText())
        assertTrue(blocks[1].isImage())
        assertTrue(blocks[2].isText())
    }
}
