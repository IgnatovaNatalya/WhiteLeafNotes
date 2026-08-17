package ru.whiteleaf.notes.domain.model

data class Notebook(
    val path: String, // Путь папки в корневой папке заметок
    val createdAt: Long,
    val noteCount: Int = 0,
    val isEncrypted: Boolean = false,
    val isUnlocked: Boolean = false,
    val isLastOpened: Boolean = false,
    val isPinned: Boolean = false
)