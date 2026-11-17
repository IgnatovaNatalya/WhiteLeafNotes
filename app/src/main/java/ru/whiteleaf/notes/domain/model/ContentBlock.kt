package ru.whiteleaf.notes.domain.model

/**
 * Унифицированный блок контента заметки.
 * Поддерживает 3 типа: текстовый абзац, картинка, таблица.
 */
sealed class ContentBlock {

    /** Текстовый абзац */
    data class Text(
        val text: String,
        val order: Int = 0
    ) : ContentBlock()

    /** Картинка с опциональной подписью */
    data class Image(
        val imagePath: String,
        val caption: String = "",
        val order: Int = 0
    ) : ContentBlock()

    /** Таблица, где первая строка — заголовки колонок */
    data class Table(
        val rows: List<List<String>>,
        val order: Int = 0
    ) : ContentBlock() {
        val headers: List<String>
            get() = rows.firstOrNull() ?: emptyList()
        
        val dataRows: List<List<String>>
            get() = if (rows.size > 1) rows.drop(1) else emptyList()
        
        val columnCount: Int
            get() = headers.size
        
        val rowCount: Int
            get() = if (rows.isEmpty()) 0 else rows.size - 1 // минус заголовок
        
        init {
            require(rows.isEmpty() || rows.all { it.size == columnCount }) {
                "Все строки таблицы должны иметь одинаковое количество ячеек: $columnCount"
            }
        }
    }

    /** Является ли блок текстовым */
    fun isText(): Boolean = this is Text

    /** Является ли блок картинкой */
    fun isImage(): Boolean = this is Image

    /** Является ли блок таблицей */
    fun isTable(): Boolean = this is Table

    companion object {
        /** Создать текстовый блок по умолчанию */
        fun text(text: String = "", order: Int = 0): Text = Text(text, order)
        
        /** Создать блок картинки */
        fun image(imagePath: String, caption: String = "", order: Int = 0): Image =
            Image(imagePath, caption, order)
        
        /** Создать таблицу по умолчанию (пустая) */
        fun table(order: Int = 0): Table = Table(emptyList(), order)
    }
}