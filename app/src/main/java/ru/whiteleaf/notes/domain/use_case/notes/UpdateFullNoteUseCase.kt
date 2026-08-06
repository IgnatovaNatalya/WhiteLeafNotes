package ru.whiteleaf.notes.domain.use_case.notes

import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.repository.NotesRepository
import ru.whiteleaf.notes.domain.repository.PreferencesRepository

class UpdateFullNoteUseCase(
    private val repository: NotesRepository,
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(note: Note, newTitle: String, newContent: String): Note {

        println("DEBUG: UpdateFullNoteUseCase: oldNote:${note.printDebugIdTitlePath()}, new title=$newTitle, content=${newContent.take(10)}")
//        println("DEBUG: newTitle hex=${newTitle.map { it.code.toString(16) }}___")
//        println("DEBUG: note.title hex=${note.title.map { it.code.toString(16) }}___")

        val renamedNote =
            if (newTitle != note.title) repository.renameNote(note, newTitle) else note

        val updatedNote =
            if (newContent != note.content) renamedNote.copy(content = newContent) else renamedNote

        repository.saveNote(updatedNote)

        preferencesRepository.updateRecentEntry(note, updatedNote)

        return updatedNote
    }
}