package com.example.txtnotesapp.domain.model

data class ContextMenuItem(
    val id: Int,
    val title: String,
    val iconRes: Int? = null,
    val isEnabled: Boolean = true
)