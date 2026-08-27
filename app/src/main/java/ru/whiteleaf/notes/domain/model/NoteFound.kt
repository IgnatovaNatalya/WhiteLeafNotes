package ru.whiteleaf.notes.domain.model

data class NoteFound(
    val id: String,
    val title: String,
    val foundedInTitle: Boolean,
    val contentSearchPreview: String?,          // null если найдено в названии
    val contentPreviewOffset: Int?,      // начало фрагмента в полном тексте
    val contentPosition: Int?,         // глобальная позиция вхождения
    val modifiedAt: Long,
    val notebookPath: String?
)