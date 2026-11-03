package ru.whiteleaf.notes.presentation.note_edit

import android.app.Dialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import ru.whiteleaf.notes.R
import ru.whiteleaf.notes.common.utils.ImageStorageUtils

/**
 * Диалог для просмотра изображения на весь экран
 */
class ImagePreviewDialogFragment : DialogFragment() {

    interface Listener {
        fun onDismissed()
    }

    private lateinit var listener: Listener
    private var filename: String = ""
    private var notebookPath: String? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_image_preview, null)
        val imageView = view.findViewById<ImageView>(R.id.dialog_image_view)

        filename = arguments?.getString(ARG_FILENAME, "") ?: ""
        notebookPath = arguments?.getString(ARG_NOTEBOOK_PATH)

        // Загружаем изображение
        val imageFile = ImageStorageUtils.getImageFile(
            requireContext(),
            ImageStorageUtils.extractNoteIdFromFilename(filename),
            filename,
            notebookPath
        )

        if (imageFile.exists()) {
            val options = BitmapFactory.Options().apply {
                inSampleSize = 2
            }
            val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath, options)
            imageView.setImageBitmap(bitmap)
        } else {
            imageView.visibility = android.view.View.GONE
        }

        builder.setView(view)
            .setOnCancelListener { listener.onDismissed() }

        return builder.create()
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    companion object {
        private const val ARG_FILENAME = "filename"
        private const val ARG_NOTEBOOK_PATH = "notebook_path"

        fun newInstance(filename: String, notebookPath: String?): ImagePreviewDialogFragment {
            return ImagePreviewDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_FILENAME, filename)
                    putString(ARG_NOTEBOOK_PATH, notebookPath)
                }
            }
        }
    }
}
