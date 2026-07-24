package ru.whiteleaf.notes.data.model

import ru.whiteleaf.notes.domain.model.Note

data class RecentNote(
    val id: String,
    val recentTitle: String,
    val notebookPath: String?,
    val recentDate: Long = 0
) {
    fun printDebug(): String {
        return "id=$id, recentTitle=$recentTitle, path=$notebookPath"
    }

    companion object {
        fun fromNote(note: Note): RecentNote {
            return RecentNote(
                id = note.id,
                recentTitle = if (note.title != "") note.title.trimStart('-') else note.content.take(
                    40
                ),
                notebookPath = note.notebookPath ?: "",
                recentDate = System.currentTimeMillis()
            )
        }
    }
}