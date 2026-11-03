package ru.whiteleaf.notes.presentation.note_edit

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.TextPaint
import android.view.View
import android.widget.EditText
import ru.whiteleaf.notes.domain.model.TableBlock
import ru.whiteleaf.notes.domain.model.TableCell

/**
 * Кастомный ReplacementSpan для отрисовки таблиц в EditText
 */
class TableReplacementSpan(
    private val table: TableBlock,
    private val context: android.content.Context,
    private val onCellClick: ((row: Int, col: Int) -> Unit)? = null
) : android.text.style.ReplacementSpan() {

    companion object {
        private const val MIN_CELL_HEIGHT = 30f
        private const val CELL_PADDING = 8f
        private const val BORDER_WIDTH = 1f
    }

    private val defaultTextColor = android.util.TypedValue.applyDimension(
        android.util.TypedValue.COMPLEX_UNIT_SP, 14f, context.resources.displayMetrics
    ).toInt()

    override fun getSize(paint: Paint, text: CharSequence?, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int {
        // Вычисляем ширину таблицы
        val textPaint = TextPaint(paint)
        val colWidths = calculateColumnWidths(textPaint)
        return colWidths.sum().toInt() + BORDER_WIDTH.toInt() * (colWidths.size + 1)
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence?,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        val textPaint = TextPaint(paint)
        val colWidths = calculateColumnWidths(textPaint)
        val rowHeights = calculateRowHeights(textPaint, colWidths)

        var currentX = x
        var currentY = top.toFloat()

        for (rowIndex in table.rows.indices) {
            val row = table.rows[rowIndex]
            val cellHeight = rowHeights.getOrElse(rowIndex) { MIN_CELL_HEIGHT }
            val isHeader = row.firstOrNull()?.isHeader == true

            var rowX = currentX
            for (colIndex in row.indices) {
                val cellWidth = colWidths.getOrElse(colIndex) { 100f }
                val cell = row[colIndex]

                // Рисуем границу ячейки
                val borderPaint = Paint().apply {
                    color = Color.GRAY
                    strokeWidth = BORDER_WIDTH
                    style = Paint.Style.STROKE
                }
                canvas.drawRect(rowX, currentY, rowX + cellWidth, currentY + cellHeight, borderPaint)

                // Рисуем фон для заголовков
                if (isHeader) {
                    val bgPaint = Paint().apply {
                        color = Color.argb(60, 100, 150, 255)
                        style = Paint.Style.FILL
                    }
                    canvas.drawRect(rowX + BORDER_WIDTH, currentY + BORDER_WIDTH,
                        rowX + cellWidth - BORDER_WIDTH, currentY + cellHeight - BORDER_WIDTH, bgPaint)
                }

                // Рисуем текст ячейки
                val textPaintForCell = TextPaint(textPaint).apply {
                    color = if (isHeader) Color.BLACK else defaultTextColor
                    isFakeBoldText = isHeader
                }

                val cellText = cell.text.ifEmpty { " " }
                val fontMetrics = textPaintForCell.fontMetrics
                val textHeight = fontMetrics.descent - fontMetrics.ascent
                val textY = currentY + (cellHeight - textHeight) / 2f - fontMetrics.ascent

                canvas.drawText(cellText, rowX + CELL_PADDING, textY, textPaintForCell)

                rowX += cellWidth + BORDER_WIDTH
            }

            currentY += cellHeight + BORDER_WIDTH
        }
    }

    /**
     * Вычисляет ширину каждого столбца
     */
    private fun calculateColumnWidths(paint: Paint): List<Float> {
        val maxTextWidths = mutableListOf<Float>()
        val maxCols = table.colCount

        for (row in table.rows) {
            val limit = row.lastIndex.coerceAtMost(maxCols - 1)
            for (colIndex in 0..limit) {
                val textWidth = paint.measureText(row[colIndex].text.ifEmpty { " " })
                while (maxTextWidths.size <= colIndex) maxTextWidths.add(0f)
                maxTextWidths[colIndex] = maxOf(maxTextWidths[colIndex], textWidth)
            }
        }

        return maxTextWidths.map { it + CELL_PADDING * 2 }
    }

    /**
     * Вычисляет высоту каждой строки
     */
    private fun calculateRowHeights(paint: Paint, colWidths: List<Float>): List<Float> {
        return table.rows.map { row ->
            var maxHeight = MIN_CELL_HEIGHT.toFloat()
            for (cell in row) {
                if (cell.text.isNotEmpty()) {
                    // Простая оценка высоты текста
                    val fontMetrics = paint.fontMetrics
                    val textHeight = fontMetrics.descent - fontMetrics.ascent
                    maxHeight = maxOf(maxHeight, textHeight + CELL_PADDING * 2)
                }
            }
            maxHeight
        }
    }
}
