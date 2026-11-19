package ru.whiteleaf.notes.common.utils

import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import ru.whiteleaf.notes.presentation.note_edit.adapter.TextBlockViewHolder

/**
 * Менеджер скролла для RecyclerView с поддержкой:
 * - Короткое нажатие - переход к следующему блоку (верх блока = верх экрана)
 * - Долгое нажатие - плавный переход в конец заметки
 */
object RecyclerViewScrollManager {

    private const val TAG = "RecyclerViewScrollMgr"

    /**
     * Ищем следующий блок, чей верх ещё НЕ дошёл до верха экрана
     */
    private fun findNextBlockToScroll(
        recyclerView: RecyclerView,
        scrollView: NestedScrollView
    ): Int {
        val adapter = recyclerView.adapter ?: return RecyclerView.NO_POSITION
        val itemCount = adapter.itemCount
        if (itemCount == 0) return RecyclerView.NO_POSITION

        // Текущее смещение контента NestedScrollView
        val scrollY = scrollView.scrollY

        // Верхняя граница "видимой области" в координатах content LinearLayout
        val viewportTop = scrollY

        // Небольшой допуск, чтобы не зациклиться на блоке, который уже почти вверху
        val tolerance = 4 // px

        for (i in 0 until itemCount) {
            val vh = recyclerView.findViewHolderForAdapterPosition(i) ?: continue
            val itemTopInContent = viewHolderTopInContent(recyclerView, vh)

            // Если верх блока ниже текущего верха экрана — это наш следующий блок
            if (itemTopInContent > viewportTop + tolerance) {
                return i
            }
        }
        return RecyclerView.NO_POSITION
    }

    /**
     * Возвращает верх item'а в координатах content LinearLayout
     * (того самого, что лежит внутри NestedScrollView и чей scrollY меняется).
     */
    private fun viewHolderTopInContent(
        recyclerView: RecyclerView,
        vh: RecyclerView.ViewHolder
    ): Int {
        // Позиция RecyclerView внутри LinearLayout
        val rvTopInContent = recyclerView.top
        // Позиция item'а внутри RecyclerView
        val itemTopInRv = vh.itemView.top
        return rvTopInContent + itemTopInRv
    }


    fun setupScrollDetection(
        recyclerView: RecyclerView,
        scrollView: NestedScrollView,
        button: ImageButton,
        onVisibilityChanged: ((Boolean) -> Unit)? = null
    ) {
        button.hide()

        fun checkScrollVisibility() {
            scrollView.post {
                val contentHeight = scrollView.getChildAt(0)?.height ?: 0
                val viewportHeight = scrollView.height
                val canScroll = contentHeight > viewportHeight + 1
                if (canScroll) {
                    button.show()
                    onVisibilityChanged?.invoke(true)
                } else {
                    button.hide()
                    onVisibilityChanged?.invoke(false)
                }
            }
        }

        // Слушаем скролл NestedScrollView
        scrollView.setOnScrollChangeListener { _, _, _, _, _ ->
            checkScrollVisibility()
        }

        // Слушаем скролл RecyclerView
        recyclerView.addOnScrollListener(object : OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                checkScrollVisibility()
            }
        })

        // Короткое нажатие - переход к следующему блоку
        button.setOnClickListener {
            Log.d(TAG, "=== SHORT CLICK ===")
            scrollToNextBlock(recyclerView, scrollView)
        }

        // Долгое нажатие - плавный переход в конец
        button.setOnLongClickListener {
            Log.d(TAG, "=== LONG CLICK ===")
            smoothScrollToBottom(recyclerView, scrollView)
            true
        }

        // Инициализируем состояние кнопки после layout
        recyclerView.post {
            checkScrollVisibility()
        }
    }


    /**
     * Короткое нажатие: скроллим к следующему блоку.
     */
    private fun scrollToNextBlock(
        recyclerView: RecyclerView,
        scrollView: NestedScrollView
    ) {
        val adapter = recyclerView.adapter ?: return
        if (adapter.itemCount == 0) return

        val targetPosition = findNextBlockToScroll(recyclerView, scrollView)

        if (targetPosition == RecyclerView.NO_POSITION) {
            // Дальше блоков нет — скроллим в конец
            smoothScrollToBottom(recyclerView, scrollView)
            return
        }

        scrollToBlock(recyclerView, scrollView, targetPosition)
    }

    /**
     * Скроллит NestedScrollView к верху указанного блока.
     */
    private fun scrollToBlock(
        recyclerView: RecyclerView,
        scrollView: NestedScrollView,
        targetPosition: Int
    ) {
        val vh = recyclerView.findViewHolderForAdapterPosition(targetPosition) ?: run {
            // ViewHolder не создан — придётся подождать layout
            recyclerView.post {
                scrollToBlock(recyclerView, scrollView, targetPosition)
            }
            return
        }

        val targetY = viewHolderTopInContent(recyclerView, vh)
        scrollView.smoothScrollTo(0, targetY)

        // Курсор в начало — если это текстовый блок
        if (vh is TextBlockViewHolder) {
            val editText = vh.itemView as? EditText ?: return
            editText.requestFocus()
            editText.setSelection(0)
        }
    }

    /**
     * Долгое нажатие: плавный переход в конец заметки.
     */
    private fun smoothScrollToBottom(
        recyclerView: RecyclerView,
        scrollView: NestedScrollView
    ) {
        val adapter = recyclerView.adapter ?: return
        val lastPosition = adapter.itemCount - 1
        if (lastPosition < 0) return

        val vh = recyclerView.findViewHolderForAdapterPosition(lastPosition) ?: run {
            recyclerView.post {
                smoothScrollToBottom(recyclerView, scrollView)
            }
            return
        }

        val lastBlockBottom = viewHolderTopInContent(recyclerView, vh) + vh.itemView.height
        // Ограничиваем максимумом скролла
        val maxScroll = (scrollView.getChildAt(0)?.height ?: 0) - scrollView.height
        val target = lastBlockBottom.coerceAtMost(maxScroll).coerceAtLeast(0)
        scrollView.smoothScrollTo(0, target)
    }

}

private fun ImageButton.hide() {
    this.isVisible = false
}

private fun ImageButton.show() {
    this.isVisible = true
}