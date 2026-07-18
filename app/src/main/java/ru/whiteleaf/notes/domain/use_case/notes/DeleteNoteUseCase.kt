package ru.whiteleaf.notes.domain.use_case.notes

import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.repository.NotesRepository
import ru.whiteleaf.notes.domain.repository.PreferencesRepository

class DeleteNoteUseCase(private val repository: NotesRepository, private val preferencesRepository: PreferencesRepository)  {
    suspend operator fun invoke(note: Note) {
        preferencesRepository.removeRecentNote(note.id, note.notebookPath)
        repository.deleteNote(note)
    }
}