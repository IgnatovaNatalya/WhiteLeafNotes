package ru.whiteleaf.notes.domain.repository

import android.content.Context
import androidx.fragment.app.FragmentActivity

interface BiometricRepository {
    suspend fun authenticate(activity: FragmentActivity): Result<Unit>
    fun isBiometricAvailable(): Boolean
    fun hasBiometricSetUp(): Boolean
}