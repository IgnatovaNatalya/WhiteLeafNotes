package ru.whiteleaf.notes.presentation.note_edit.adapter

import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ru.whiteleaf.notes.R
import ru.whiteleaf.notes.domain.model.ContentBlock

/**
 * ViewHolder для блока таблицы.
 */
class TableBlockViewHolder(itemView: View) : BlockViewHolder(itemView) {

    private val tableRecyclerView: RecyclerView = itemView.findViewById(R.id.item_table_block_recycler)
    private var cellAdapter: TableCellAdapter? = null

    init {
        tableRecyclerView.layoutManager = GridLayoutManager(itemView.context, 1)
        cellAdapter = TableCellAdapter(emptyList(), 1)
        tableRecyclerView.adapter = cellAdapter
    }

    override fun bind(block: Any, onTextChanged: ((String) -> Unit)?) {
        val tableBlock = block as? ContentBlock.Table ?: return
        
        val rows = tableBlock.rows
        if (rows.isEmpty()) return

        val columnCount = rows.first().size
        cellAdapter = TableCellAdapter(rows, columnCount)
        tableRecyclerView.adapter = cellAdapter
        
        // Обновляем данные при изменении ячеек
        cellAdapter?.setOnDataChanged { newRows ->
            onTextChanged?.invoke(newRows.joinToString("\n") { it.joinToString("|") })
        }
    }

    override fun getContent(): String {
        val rows = cellAdapter?.getRows() ?: emptyList()
        return rows.joinToString("\n") { it.joinToString("|") }
    }

    override fun toString(): String {
        val rows = cellAdapter?.getRows() ?: emptyList()
        return "TableBlockViewHolder(rows=${rows.size})"
    }
}

/**
 * Адаптер для ячеек таблицы.
 */
class TableCellAdapter(
    private var rows: List<List<String>>,
    private val columnCount: Int
) : RecyclerView.Adapter<TableCellAdapter.CellViewHolder>() {

    private var onDataChanged: ((List<List<String>>) -> Unit)? = null

    fun setOnDataChanged(listener: (List<List<String>>) -> Unit) {
        onDataChanged = listener
    }

    fun getRows(): List<List<String>> = rows

    inner class CellViewHolder(itemView: EditText) : RecyclerView.ViewHolder(itemView) {
        fun bind(value: String, row: Int, col: Int) {
            (itemView as EditText).setText(value)
            (itemView as EditText).setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    // При потере фокуса сохраняем значение
                    val newValue = (itemView as? EditText)?.text?.toString() ?: ""
                    updateCell(row, col, newValue)
                }
            }
        }

        private fun updateCell(row: Int, col: Int, value: String) {
            if (row < rows.size && col < rows[row].size) {
                val newRows = rows.toMutableList()
                newRows[row] = newRows[row].toMutableList().apply {
                    this[col] = value
                }
                rows = newRows
                onDataChanged?.invoke(rows)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CellViewHolder {
        val editText = EditText(parent.context).apply {
            gravity = android.view.Gravity.CENTER
            setPadding(8, 8, 8, 8)
            textSize = 14f
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_NEXT
        }
        return CellViewHolder(editText)
    }

    override fun onBindViewHolder(holder: CellViewHolder, position: Int) {
        val row = position / columnCount
        val col = position % columnCount
        
        if (row < rows.size && col < rows[row].size) {
            holder.bind(rows[row][col], row, col)
        }
    }

    override fun getItemCount(): Int {
        return rows.sumOf { it.size }
    }
}