package ru.whiteleaf.notes.domain.use_case.notes

import ru.whiteleaf.notes.domain.model.Note

class UpdateFullNoteUseCase(
    private val renameNoteUseCase: RenameNoteUseCase,
    private val saveNoteContentUseCase: SaveNoteContentUseCase
) {
    suspend operator fun invoke(note: Note, newTitle: String, newContent: String): Note {

        val updContentNote = note.copy(content = newContent)
        saveNoteContentUseCase(updContentNote)

        val updatedNote = if (newTitle != note.title) renameNoteUseCase(
            updContentNote,
            newTitle
        ) else updContentNote

        return updatedNote
    }
}