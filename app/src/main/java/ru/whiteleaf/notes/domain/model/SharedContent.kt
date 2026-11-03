package ru.whiteleaf.notes.domain.model

import android.net.Uri

sealed class SharedContent {
    data class FileContent(val name: String, val text: String) : SharedContent()
    data class TextContent(val text: String) : SharedContent()
    data class ImageContent(val uri: Uri, val name: String) : SharedContent()
}
