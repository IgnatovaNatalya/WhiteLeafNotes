package ru.whiteleaf.notes.common.utils

import ru.whiteleaf.notes.domain.model.ContentBlock

/**
 * Парсер, который преобразует обычный текст заметки в список ContentBlock.
 * 
 * Правила парсинга:
 * - 2+ пустых строки подряд → разделитель блоков
 * - Строки, начинающиеся с http:// или https:// → ImageBlock
 * - Остальное → TextBlock (включая строки с разделителями | , ;)
 * 
 * Таблицы создаются явно через меню вставки блока,
 * а не автоматически из текста.
 */
object ContentBlockParser {

    /**
     * Парсит текст заметки в список блоков.
     * Блок заканчивается при встрече 2+ пустых строк подряд.
     */
    fun parse(content: String): List<ContentBlock> {
        if (content.isBlank()) return emptyList()
        
        val lines = content.lines()
        val blocks = mutableListOf<ContentBlock>()
        val textBuffer = mutableListOf<String>()
        var emptyCount = 0
        var order = 0

        fun flushTextBuffer() {
            if (textBuffer.isNotEmpty()) {
                blocks.add(ContentBlock.text(textBuffer.joinToString("\n"), order++))
                textBuffer.clear()
            }
        }

        for (line in lines) {
            val trimmed = line.trim()
            
            when {
                trimmed.isEmpty() -> {
                    emptyCount++
                    if (emptyCount >= 2) {
                        // 2+ пустых строки подряд → конец блока
                        flushTextBuffer()
                        emptyCount = 0
                    }
                    // 1 пустая строка → продолжение блока, добавляем как пробел
                    else {
                        textBuffer.add("")
                    }
                }
                isImageUrl(trimmed) -> {
                    flushTextBuffer()
                    emptyCount = 0
                    blocks.add(ContentBlock.image(trimmed, order = order++))
                }
                else -> {
                    emptyCount = 0
                    textBuffer.add(line)
                }
            }
        }
        
        flushTextBuffer()
        return blocks
    }

    /**
     * Конвертирует список блоков обратно в обычный текст.
     */
    fun serialize(blocks: List<ContentBlock>): String {
        return blocks.joinToString("\n\n") { block ->
            when (block) {
                is ContentBlock.Text -> block.text
                is ContentBlock.Image -> block.imagePath
                is ContentBlock.Table -> serializeTable(block)
            }
        }
    }

    /**
     * Проверяет, является ли строка URL изображения.
     */
    private fun isImageUrl(line: String): Boolean {
        return line.startsWith("http://") || line.startsWith("https://") ||
               line.startsWith("file://") || line.startsWith("content://")
    }

    /**
     * Конвертирует таблицу обратно в строковое представление.
     */
    private fun serializeTable(table: ContentBlock.Table): String {
        return table.rows.joinToString("\n") { row ->
            row.joinToString("|")
        }
    }
}