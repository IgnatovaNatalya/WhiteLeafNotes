package ru.whiteleaf.notes.domain.model

data class NoteFound(
    val query: String,
    val id: String,
    val title: String,
    val foundedInTitle: Boolean,
    val contentSearchPreview: String?,          // null если найдено в названии
    val contentPosition: Int?,         // глобальная позиция вхождения
    val modifiedAt: Long,
    val notebookPath: String?
)

fun NoteFound.toNote(): Note {
    return Note(
        id = id,
        title = title,
        content = "",
        modifiedAt = modifiedAt,
        notebookPath = notebookPath
    )
}

fun NoteFound.printDebug() {
    println("DEBUG found note id=$id, title=$title, path=$notebookPath, search preview=$contentSearchPreview")
}