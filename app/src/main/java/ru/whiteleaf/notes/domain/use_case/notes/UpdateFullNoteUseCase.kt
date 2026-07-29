package ru.whiteleaf.notes.domain.use_case.notes

import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.repository.NotesRepository
import ru.whiteleaf.notes.domain.repository.PreferencesRepository

class UpdateFullNoteUseCase(
    private val repository: NotesRepository,
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(note: Note, newTitle: String, newContent: String): Note {

        val renamedNote = repository.renameNote(note, newTitle)
        val updatedNote = if (newTitle != note.title) renamedNote.copy(content = note.content) else renamedNote
        repository.saveNote(updatedNote)

        preferencesRepository.updateRecentEntry(note, updatedNote)

        return updatedNote
    }
}