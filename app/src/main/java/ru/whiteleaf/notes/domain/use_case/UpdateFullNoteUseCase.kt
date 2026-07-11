package ru.whiteleaf.notes.domain.use_case

import ru.whiteleaf.notes.domain.model.Note

class UpdateFullNoteUseCase(
    private val renameNoteUseCase: RenameNoteUseCase,
    private val saveNoteContentUseCase: SaveNoteContentUseCase
) {
    suspend operator fun invoke(note: Note, newTitle: String, newContent: String): Note {

        val newId = if (newTitle.isNotEmpty() && newTitle != note.title) {
            renameNoteUseCase(note, newTitle)
        } else {
            note.id
        }

        val updatedNote = note.copy(id = newId, title = newTitle, content = newContent)
        saveNoteContentUseCase(updatedNote)
        return updatedNote
    }
}