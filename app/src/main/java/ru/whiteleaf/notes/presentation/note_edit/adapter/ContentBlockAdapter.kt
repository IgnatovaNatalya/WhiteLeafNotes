package ru.whiteleaf.notes.presentation.note_edit.adapter

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import ru.whiteleaf.notes.R
import ru.whiteleaf.notes.domain.model.ContentBlock
import ru.whiteleaf.notes.presentation.note_edit.SearchMatch
import ru.whiteleaf.notes.presentation.note_edit.SearchMatchTarget

/**
 * Основной адаптер для RecyclerView с блоками контента.
 * Использует точечные обновления вместо полной перерисовки.
 */
class ContentBlockAdapter :
    RecyclerView.Adapter<BlockViewHolder>() {

    private val items: MutableList<ContentBlock> = mutableListOf()
    private var onNoteSaved: (() -> Unit)? = null
    private var onBindCallback: ((Int, ContentBlock, (Int, String) -> Unit) -> Unit)? = null

    // Debounce handler для отложенного обновления текста
    private val debounceHandler = Handler(Looper.getMainLooper())
    private val debounceDelay = 500L // 500мс
    private val pendingUpdates = mutableMapOf<Int, String>() // position -> text

    /**
     * Устанавливает callback для сохранения всей заметки.
     * Вызывается при debounce обновлении текста.
     */
    fun setOnNoteSaved(listener: () -> Unit) {
        onNoteSaved = listener
    }

    /**
     * Устанавливает callback для bind ViewHolder.
     */
    fun setOnBindCallback(listener: (Int, ContentBlock, (Int, String) -> Unit) -> Unit) {
        onBindCallback = listener
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

        val newBlock = when (val oldBlock = items[position]) {
            is ContentBlock.Text -> oldBlock.copy(text = text)
            is ContentBlock.Image -> oldBlock.copy(caption = text)
            is ContentBlock.Table -> oldBlock
        }

        items[position] = newBlock
    }

    /**
     * Точечное обновление текста блока с перерисовкой ViewHolder.
     */
    fun updateTextAt(position: Int, text: String) {
        updateDataOnly(position, text)
        notifyItemChanged(position)
    }

    /**
     * Обновляет текст блока с debounce (задержкой).
     * Вызывается при каждом изменении текста в EditText.
     * Реальное обновление происходит через 500мс после окончания ввода.
     */
    fun updateTextWithDebounce(position: Int, text: String) {
        // Сохраняем текст для debounce
        pendingUpdates[position] = text

        // Удаляем предыдущую задачу debounce
        debounceHandler.removeCallbacksAndMessages(position)

        // Планируем обновление через 500мс
        val runnable = Runnable {
            val newText = pendingUpdates.remove(position) ?: return@Runnable

            // Обновляем данные
            updateDataOnly(position, newText)

            // Сохраняем заметку
            onNoteSaved?.invoke()
        }

        debounceHandler.postDelayed(runnable, debounceDelay)
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

        // Устанавливаем позицию в ViewHolder
        when (holder) {
            is TextBlockViewHolder -> holder.setAdapterPosition(position)
            is ImageBlockViewHolder -> holder.setAdapterPosition(position)
            is TableBlockViewHolder -> holder.setAdapterPosition(position)
        }

        // Передаём callback — debounce происходит внутри updateTextWithDebounce
        holder.bind(block) { pos, text ->
            updateTextWithDebounce(pos, text)
        }
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

    /**
     * Подсвечивает match в соответствующем ViewHolder.
     * НЕ очищает другие подсветки - вызывающий должен сам управлять очисткой.
     */
    fun highlightMatch(match: SearchMatch, color: Int, recyclerView: RecyclerView) {
        val position = when (match.target) {
            SearchMatchTarget.TITLE -> -1 // Заголовок обрабатывается отдельно в Fragment
            SearchMatchTarget.CONTENT -> findContentBlockPosition(match.start)
        }

        if (position == RecyclerView.NO_POSITION) return

        val holder = recyclerView.findViewHolderForAdapterPosition(position)
        if (holder is TextBlockViewHolder) {
            holder.highlightText(match.start, match.end, color)
        }
    }

    /**
     * Ищет позицию TextBlock в контенте по смещению.
     */
    private fun findContentBlockPosition(contentOffset: Int): Int {
        var currentOffset = 0
        for ((i, element) in items.withIndex()) {
            val block = element
            if (block is ContentBlock.Text) {
                val blockLength = block.text.length + 2 // +2 для разделителя \n\n
                if (contentOffset in currentOffset until currentOffset + block.text.length) {
                    return i
                }
                currentOffset += blockLength
            }
        }
        return RecyclerView.NO_POSITION
    }

    /**
     * Очищает подсветку во всех ViewHolder'ах.
     */
    fun clearAllHighlights(recyclerView: RecyclerView) {
        for (i in 0 until itemCount) {
            val holder = recyclerView.findViewHolderForAdapterPosition(i)
            if (holder is TextBlockViewHolder) {
                holder.clearHighlight()
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
