package ru.whiteleaf.notes.presentation.note_edit

data class NotebookSecurityState(
    val isEncrypted: Boolean = false,
    val isUnlocked: Boolean = true,
    val requiresAuthentication: Boolean = false
) {
    val shouldEncrypt: Boolean get() = isEncrypted && isUnlocked
    val shouldShowEncrypted: Boolean get() = isEncrypted && !isUnlocked
}