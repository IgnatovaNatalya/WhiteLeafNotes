package com.example.txtnotesapp.domain.use_case

import com.example.txtnotesapp.domain.repository.PreferencesRepository

class GetExportDirectoryUseCase (
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(): String? {
        return preferencesRepository.getExportDirectoryPath()
    }
}