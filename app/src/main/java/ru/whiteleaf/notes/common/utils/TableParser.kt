package ru.whiteleaf.notes.common.utils

import ru.whiteleaf.notes.domain.model.TableBlock
import ru.whiteleaf.notes.domain.model.TableCell

/**
 * Блок контента: либо текст, либо таблица
 */
sealed class TextOrTableBlock {
    data class TextBlock(val text: String) : TextOrTableBlock()
    data class TableBlockData(val table: TableBlock) : TextOrTableBlock()
}

/**
 * Парсер markdown-таблиц
 */
object TableParser {

    /**
     * Проверяет, является ли текст markdown-таблицей
     * Markdown таблица: строка заголовков | строка разделителей | строки данных
     */
    fun isTableBlock(text: String): Boolean {
        val lines = text.trim().lines()
        if (lines.size < 2) return false

        // Вторая строка должна быть разделителем (---|---|---)
        if (!isSeparatorLine(lines[1])) return false

        // Первая строка должна содержать |
        if (!lines[0].contains('|')) return false

        return true
    }

    /**
     * Проверяет, является ли строка разделителем таблицы
     */
    private fun isSeparatorLine(line: String): Boolean {
        val trimmed = line.trim()
        // Разделитель: ---|---|--- или -|-|- или ---|-|---
        val parts = trimmed.split('|').map { it.trim() }
        if (parts.size < 2) return false
        return parts.all { it.matches(Regex("^[-_:]+$")) }
    }

    /**
     * Парсит текст в TableBlock
     */
    fun parseTable(text: String): TableBlock? {
        val lines = text.trim().lines()
        if (lines.size < 2 || !isSeparatorLine(lines[1])) return null

        val rows = mutableListOf<List<TableCell>>()

        // Заголовочная строка
        val headerCells = parseLine(lines[0], isHeader = true)
        if (headerCells.isEmpty()) return null
        rows.add(headerCells)

        // Пропускаем разделитель (вторая строка)

        // Строки данных
        for (i in 2 until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue
            val cells = parseLine(line, isHeader = false)
            // Выравниваем количество ячеек с заголовком
            val alignedCells = alignRow(cells, headerCells.size)
            rows.add(alignedCells)
        }

        return if (rows.size >= 2) TableBlock(rows) else null
    }

    /**
     * Парсит одну строку таблицы на ячейки
     */
    private fun parseLine(line: String, isHeader: Boolean): List<TableCell> {
        return line.split('|').map { cellText ->
            TableCell(
                text = cellText.trim(),
                isHeader = isHeader
            )
        }
    }

    /**
     * Выравнивает строку по количеству ячеек
     */
    private fun alignRow(cells: List<TableCell>, targetCount: Int): List<TableCell> {
        return if (cells.size < targetCount) {
            cells.toMutableList().apply {
                while (size < targetCount) add(TableCell())
            }
        } else {
            cells
        }
    }

    /**
     * Конвертирует TableBlock обратно в markdown-строку
     */
    fun toMarkdown(table: TableBlock): String = table.toMarkdown()

    /**
     * Разделяет контент заметки на текстовые блоки и блоки таблиц
     * Таблица определяется как блок текста, который начинается с markdown-таблицы
     */
    fun splitContentIntoBlocks(content: String): List<TextOrTableBlock> {
        if (content.isEmpty()) return emptyList()

        val blocks = mutableListOf<TextOrTableBlock>()
        val lines = content.lines()
        var i = 0

        while (i < lines.size) {
            val currentLine = lines[i]

            // Проверяем, начинается ли здесь таблица
            if (i + 1 < lines.size && isTableBlock(currentLine + "\n" + lines[i + 1])) {
                // Собираем всю таблицу
                val tableLines = mutableListOf(currentLine, lines[i + 1])
                var j = i + 2

                while (j < lines.size) {
                    val nextLine = lines[j]
                    // Таблица заканчивается на пустой строке или если следующая строка не выглядит как часть таблицы
                    if (nextLine.trim().isEmpty()) {
                        // Пропускаем пустые строки между таблицами
                        j++
                        continue
                    }
                    // Проверяем, является ли строка частью таблицы (содержит | и не пустая)
                    if (nextLine.contains('|') && nextLine.trim().isNotEmpty()) {
                        tableLines.add(nextLine)
                        j++
                    } else {
                        break
                    }
                }

                val tableText = tableLines.joinToString("\n")
                val table = parseTable(tableText)
                if (table != null) {
                    blocks.add(TextOrTableBlock.TableBlockData(table))
                } else {
                    blocks.add(TextOrTableBlock.TextBlock(tableText))
                }
                i = j
            } else {
                // Собираем текстовый блок (до следующей таблицы или конца контента)
                val textLines = mutableListOf(currentLine)
                var j = i + 1

                while (j < lines.size) {
                    // Проверяем, начинается ли здесь новая таблица
                    if (j + 1 < lines.size && isTableBlock(lines[j] + "\n" + lines[j + 1])) {
                        break
                    }
                    textLines.add(lines[j])
                    j++
                }

                blocks.add(TextOrTableBlock.TextBlock(textLines.joinToString("\n")))
                i = j
            }
        }

        return blocks
    }

    /**
     * Восстанавливает контент из блоков
     */
    fun reconstructContent(blocks: List<TextOrTableBlock>): String {
        return blocks.map { block ->
            when (block) {
                is TextOrTableBlock.TextBlock -> block.text
                is TextOrTableBlock.TableBlockData -> block.table.toMarkdown()
            }
        }.joinToString("\n\n")
    }
}
