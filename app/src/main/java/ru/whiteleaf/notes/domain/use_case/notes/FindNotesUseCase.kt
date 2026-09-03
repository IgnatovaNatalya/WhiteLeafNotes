package ru.whiteleaf.notes.domain.use_case.notes

import ru.whiteleaf.notes.domain.model.NoteFound
import ru.whiteleaf.notes.domain.model.Notebook
import ru.whiteleaf.notes.domain.repository.NotesRepository

class FindNotesUseCase(private val repository: NotesRepository) {
    suspend operator fun invoke(
    notebookPath: String? = null,
    query: String,
    notebooks: List<Notebook> = emptyList()
    ): List<NoteFound> {
        return repository.findNotes(notebookPath, query, notebooks)
    }
}