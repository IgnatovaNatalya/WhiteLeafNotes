package ru.whiteleaf.notes.presentation.note_edit.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ru.whiteleaf.notes.R
import ru.whiteleaf.notes.domain.model.ContentBlock

/**
 * Основной адаптер для RecyclerView с блоками контента.
 * Использует обычный RecyclerView.Adapter вместо ListAdapter,
 * чтобы избежать зависимости от Android Looper в unit-тестах.
 */
class ContentBlockAdapter(
    private var items: MutableList<ContentBlock> = mutableListOf()
) : RecyclerView.Adapter<BlockViewHolder>() {

    private var onTextChanged: ((Int, String) -> Unit)? = null

    fun setOnTextChanged(listener: (Int, String) -> Unit) {
        onTextChanged = listener
    }

    fun submitList(newList: List<ContentBlock>) {
        items = newList.toMutableList()
        notifyDataSetChanged()
    }

    fun getItem(position: Int): ContentBlock = items[position]

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ContentBlock.Text -> VIEW_TYPE_TEXT
            is ContentBlock.Image -> VIEW_TYPE_IMAGE
            is ContentBlock.Table -> VIEW_TYPE_TABLE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlockViewHolder {
        return when (viewType) {
            VIEW_TYPE_TEXT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_text_block, parent, false)
                TextBlockViewHolder(view)
            }
            VIEW_TYPE_IMAGE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_image_block, parent, false)
                ImageBlockViewHolder(view)
            }
            VIEW_TYPE_TABLE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_table_block, parent, false)
                TableBlockViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_text_block, parent, false)
                TextBlockViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: BlockViewHolder, position: Int) {
        val block = items[position]
        holder.bind(block, onTextChanged?.let { { text: String -> it(position, text) } })
    }

    override fun getItemCount(): Int = items.size

    /**
     * Получить контент блока по позиции.
     */
    fun getContentAt(position: Int): String {
        return items[position].let { block ->
            when (block) {
                is ContentBlock.Text -> block.text
                is ContentBlock.Image -> block.caption
                is ContentBlock.Table -> block.rows.joinToString("\n") { it.joinToString("|") }
            }
        }
    }

    companion object {
        private const val VIEW_TYPE_TEXT = 0
        private const val VIEW_TYPE_IMAGE = 1
        private const val VIEW_TYPE_TABLE = 2
    }
}