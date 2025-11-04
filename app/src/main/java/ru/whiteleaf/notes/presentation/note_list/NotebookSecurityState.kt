package ru.whiteleaf.notes.presentation.note_list

data class NotebookSecurityState(
    val isEncrypted: Boolean = false,
    val isUnlocked: Boolean = true,
    val requiresAuthentication: Boolean = false
)