package com.example.txtnotesapp.domain.use_case

import com.example.txtnotesapp.domain.repository.PreferencesRepository

class SaveNotesDirectoryUseCase (
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(path: String) {
        preferencesRepository.saveNotesDirectoryPath(path)
    }
}