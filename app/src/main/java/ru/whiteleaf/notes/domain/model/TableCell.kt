package ru.whiteleaf.notes.domain.model

/**
 * Ячейка таблицы
 */
data class TableCell(
    val text: String = "",
    val isHeader: Boolean = false
) {
    fun isEmpty(): Boolean = text.trim().isEmpty()
}

/**
 * Блок таблицы (список строк ячеек)
 */
data class TableBlock(
    val rows: List<List<TableCell>>
) {
    val rowCount: Int get() = rows.size
    val colCount: Int get() = rows.firstOrNull()?.size ?: 0

    /**
     * Конвертирует таблицу в markdown-строку
     */
    fun toMarkdown(): String {
        if (rows.isEmpty()) return ""

        val lines = mutableListOf<String>()

        // Заголовочная строка
        lines.add(rows[0].joinToString(" | ") { it.text.trim() })

        // Разделитель
        lines.add(rows[0].joinToString(" | ") {
            if (it.isHeader) "---" else "-"
        })

        // Данные
        rows.drop(1).forEach { row ->
            lines.add(row.joinToString(" | ") { it.text.trim() })
        }

        return lines.joinToString("\n")
    }

    /**
     * Конвертирует таблицу в plain text (markdown)
     */
    fun toPlainText(): String = toMarkdown()

    /**
     * Получает ячейку по строке и столбцу
     */
    fun getCell(row: Int, col: Int): TableCell? {
        return if (row in rows.indices && col in rows[row].indices) {
            rows[row][col]
        } else null
    }

    /**
     * Устанавливает ячейку по строке и столбцу
     */
    fun setCell(row: Int, col: Int, text: String, isHeader: Boolean = false): TableBlock {
        if (row !in rows.indices || col !in rows[row].indices) return this

        val newRows = rows.toMutableList()
        val newRow = newRows[row].toMutableList()
        newRow[col] = TableCell(text, isHeader)
        newRows[row] = newRow
        return TableBlock(newRows)
    }

    /**
     * Добавляет строку (по умолчанию в конец)
     */
    fun addRow(rowIndex: Int = -1, isHeader: Boolean = false): TableBlock {
        val cols = colCount.coerceAtLeast(1)
        val newRow = List(cols) { TableCell("", isHeader = false) }

        val newRows = rows.toMutableList()
        if (rowIndex < 0) {
            newRows.add(newRow)
        } else {
            newRows.add(rowIndex.coerceIn(newRows.indices), newRow)
        }
        return TableBlock(newRows)
    }

    /**
     * Удаляет строку
     */
    fun deleteRow(rowIndex: Int): TableBlock {
        if (rowIndex !in rows.indices || rows.size <= 2) return this
        return TableBlock(rows.toMutableList().apply { removeAt(rowIndex) })
    }

    /**
     * Добавляет столбец (по умолчанию в конец)
     */
    fun addColumn(colIndex: Int = -1): TableBlock {
        val newRows = rows.map { row ->
            val newRow = row.toMutableList()
            if (colIndex < 0) {
                newRow.add(TableCell())
            } else {
                newRow.add(colIndex.coerceIn(row.indices), TableCell())
            }
            newRow
        }
        return TableBlock(newRows)
    }

    /**
     * Удаляет столбец
     */
    fun deleteColumn(colIndex: Int): TableBlock {
        if (colIndex !in (0 until colCount) || colCount <= 1) return this
        val newRows = rows.map { row ->
            row.toMutableList().apply { removeAt(colIndex) }
        }
        return TableBlock(newRows)
    }
}
