package ru.whiteleaf.notes.presentation.note_edit.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import ru.whiteleaf.notes.R
import ru.whiteleaf.notes.domain.model.ContentBlock

/**
 * Основной адаптер для RecyclerView с блоками контента.
 * Использует точечные обновления вместо полной перерисовки.
 */
class ContentBlockAdapter :
    RecyclerView.Adapter<BlockViewHolder>() {

    private val items: MutableList<ContentBlock> = mutableListOf()
    private var onTextChanged: ((Int, String) -> Unit)? = null

    fun setOnTextChanged(listener: (Int, String) -> Unit) {
        onTextChanged = listener
    }

    /**
     * Обновляет список блоков с помощью DiffUtil.
     */
    fun submitList(newList: List<ContentBlock>) {
        val oldList = items.toList()
        val diffCallback = BlockDiffCallback(oldList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        
        items.clear()
        items.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    /**
     * Обновляет данные блока без перерисовки ViewHolder.
     * Используется в onPause для сбора актуального текста.
     */
    fun updateDataOnly(position: Int, text: String) {
        if (position >= items.size) return
        
        val oldBlock = items[position]
        val newBlock = when (oldBlock) {
            is ContentBlock.Text -> oldBlock.copy(text = text)
            is ContentBlock.Image -> oldBlock.copy(caption = text)
            is ContentBlock.Table -> oldBlock
        }
        
        items[position] = newBlock
    }

    /**
     * Точечное обновление текста блока с перерисовкой ViewHolder.
     * Используется при потере фокуса EditText.
     */
    fun updateTextAt(position: Int, text: String) {
        updateDataOnly(position, text)
        notifyItemChanged(position)
    }

    val currentList: List<ContentBlock>
        get() = items.toList()

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
                val editText = view.findViewById<EditText>(R.id.item_text_block_edit_text)
                TextBlockViewHolder(editText)
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
                val editText = view.findViewById<EditText>(R.id.item_text_block_edit_text)
                TextBlockViewHolder(editText)
            }
        }
    }

    override fun onBindViewHolder(holder: BlockViewHolder, position: Int) {
        val block = items[position]
        holder.bind(block, null) // onTextChanged обрабатывается через updateTextAt
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

/**
 * DiffUtil callback для вычисления разницы между списками блоков.
 */
class BlockDiffCallback(
    private val oldList: List<ContentBlock>,
    private val newList: List<ContentBlock>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition]::class == newList[newItemPosition]::class &&
               oldList[oldItemPosition] == newList[newItemPosition]
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}