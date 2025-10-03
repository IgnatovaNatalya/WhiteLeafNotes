package com.example.whiteleafnotes.domain.model


sealed class SharedContent {
    data class FileContent(val name: String, val text: String) : SharedContent()
    data class TextContent(val text: String) : SharedContent()
}