package ru.whiteleaf.notes.data.model

data class RecentNote(
    val id: String,
    val title: String,
    val notebookPath: String?,
    val recentDate: Long, // Timestamp
)