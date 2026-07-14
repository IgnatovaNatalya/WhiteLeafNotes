package ru.whiteleaf.notes.data.model

data class RecentNote(
    val id: String,
    val title: String,
    val modifiedAt: Long, // Timestamp
    val notebookPath: String
)