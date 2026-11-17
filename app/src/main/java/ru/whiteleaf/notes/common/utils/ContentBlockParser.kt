package ru.whiteleaf.notes.common.utils

import ru.whiteleaf.notes.domain.model.ContentBlock

/**
 * Парсер, который преобразует обычный текст заметки в список ContentBlock.
 * 
 * Правила парсинга:
 * - Пустые строки → игнорируются
 * - Строки, начинающиеся с http:// или https:// → ImageBlock
 * - Строки, содержащие разделитель (,;|) → TableBlock (попытка распарсить таблицу)
 * - Остальное → TextBlock
 */
object ContentBlockParser {

    /**
     * Парсит текст заметки в список блоков.
     */
    fun parse(content: String): List<ContentBlock> {
        if (content.isBlank()) return emptyList()
        
        val lines = content.lines()
        val blocks = mutableListOf<ContentBlock>()
        val textBuffer = mutableListOf<String>()
        var order = 0
        var i = 0

        fun flushTextBuffer() {
            if (textBuffer.isNotEmpty()) {
                blocks.add(ContentBlock.text(textBuffer.joinToString("\n"), order++))
                textBuffer.clear()
            }
        }

        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()
            
            when {
                trimmed.isEmpty() -> {
                    flushTextBuffer()
                    i++
                }
                isImageUrl(trimmed) -> {
                    flushTextBuffer()
                    blocks.add(ContentBlock.image(trimmed, order = order++))
                    i++
                }
                isTableLine(trimmed) -> {
                    flushTextBuffer()
                    // Собираем все строки таблицы
                    val tableLines = mutableListOf<String>()
                    while (i < lines.size) {
                        val currentLine = lines[i].trim()
                        if (currentLine.isEmpty()) break
                        if (currentLine.contains(",") || currentLine.contains(";") || currentLine.contains("|")) {
                            tableLines.add(lines[i])
                            i++
                        } else {
                            break
                        }
                    }
                    if (tableLines.isNotEmpty()) {
                        val table = parseTable(tableLines)
                        if (table != null) {
                            blocks.add(table)
                        }
                    }
                }
                else -> {
                    textBuffer.add(line)
                    i++
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
     * Проверяет, содержит ли строка признаки таблицы (разделители ячеек).
     */
    private fun isTableLine(line: String): Boolean {
        val trimmed = line.trim()
        return trimmed.contains(",") || trimmed.contains(";") || trimmed.contains("|")
    }

    /**
     * Парсит строки таблицы в ContentBlock.Table.
     */
    private fun parseTable(lines: List<String>): ContentBlock.Table? {
        if (lines.isEmpty()) return null
        
        val rows = lines.map { line ->
            val trimmed = line.trim()
            when {
                trimmed.contains("|") -> trimmed.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.map { it.trim() }
                trimmed.contains(",") -> trimmed.split(",").map { it.trim() }
                trimmed.contains(";") -> trimmed.split(";").map { it.trim() }
                else -> listOf(trimmed)
            }
        }
        
        return ContentBlock.Table(rows)
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