package com.example.txtnotesapp.domain.model


sealed class SharedContent {
    data class FileContent(val name: String, val text: String) : SharedContent()
    data class TextContent(val name: String, val text: String) : SharedContent()
}