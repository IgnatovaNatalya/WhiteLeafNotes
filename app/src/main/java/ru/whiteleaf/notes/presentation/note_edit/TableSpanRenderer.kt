package ru.whiteleaf.notes.presentation.note_edit

import android.content.Context
import android.text.SpannableString
import android.text.style.ReplacementSpan
import ru.whiteleaf.notes.common.utils.TableParser
import ru.whiteleaf.notes.common.utils.TextOrTableBlock
import ru.whiteleaf.notes.domain.model.TableBlock

/**
 * Рендер таблиц: конвертирует markdown-таблицы в ReplacementSpan
 */
object TableSpanRenderer {

    /**
     * Рендерит контент, заменяя markdown-таблицы на ReplacementSpan
     */
    fun renderTables(
        context: Context,
        content: String,
        onCellClick: ((row: Int, col: Int) -> Unit)? = null
    ): SpannableString {
        val spannable = SpannableString(content)
        val blocks = TableParser.splitContentIntoBlocks(content)

        var currentOffset = 0
        blocks.forEach { block ->
            when (block) {
                is TextOrTableBlock.TextBlock -> {
                    currentOffset += block.text.length + 2 // +2 для \n\n
                }
                is TextOrTableBlock.TableBlockData -> {
                    val tableText = block.table.toMarkdown()
                    val start = currentOffset
                    val end = start + tableText.length

                    val tableSpan = TableReplacementSpan(block.table, context, onCellClick)
                    spannable.setSpan(tableSpan, start, end, 0)

                    currentOffset += tableText.length + 2
                }
            }
        }

        return spannable
    }

    /**
     * Создаёт TableReplacementSpan для конкретной таблицы
     */
    fun createTableSpan(
        context: Context,
        table: TableBlock,
        onCellClick: ((row: Int, col: Int) -> Unit)? = null
    ): ReplacementSpan {
        return TableReplacementSpan(table, context, onCellClick)
    }
}
