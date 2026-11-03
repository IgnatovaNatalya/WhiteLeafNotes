package ru.whiteleaf.notes.common

object AppConstants {
    const val DEFAULT_DIR = "WhiteLeafNotes"
    const val EXPORT_ZIP_PREFIX = "White_leaf_export"
    const val WHITE_LEAF_PREFS = "White_leaf_notes_prefs"

    // Image markers
    const val IMAGE_DIR = "images"
    const val IMAGE_MARKER_PREFIX = "[IMG:"
    const val IMAGE_MARKER_SUFFIX = "]"
    const val IMAGE_MARKER_PATTERN = "\\[IMG:([^\\]]+)\\]"
}