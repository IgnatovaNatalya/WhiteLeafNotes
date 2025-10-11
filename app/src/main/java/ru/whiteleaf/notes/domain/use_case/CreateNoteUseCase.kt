package ru.whiteleaf.notes.domain.use_case

import ru.whiteleaf.notes.common.utils.FileUtils.generateNoteId
import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.repository.NotesRepository

class CreateNoteUseCase(private val repository: NotesRepository) {
    suspend operator fun invoke(notebookPath: String? = null): Note {

        val note = Note(
            id = generateNoteId(),
            title = "",
            content = "",
            modifiedAt = System.currentTimeMillis(),
            notebookPath = notebookPath
        )

        repository.saveNote(note)
        return note
    }
}