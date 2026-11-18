package ru.whiteleaf.notes.presentation.note_edit.adapter

import android.view.View
import android.widget.EditText
import android.widget.ImageView
import ru.whiteleaf.notes.R
import ru.whiteleaf.notes.domain.model.ContentBlock

/**
 * ViewHolder для блока картинки.
 */
class ImageBlockViewHolder(itemView: View) : BlockViewHolder(itemView) {

    private val imageView: ImageView = itemView.findViewById(R.id.item_image_block_image)
    private val captionEdit: EditText = itemView.findViewById(R.id.item_image_block_caption)

    override fun bind(block: Any, onTextChanged: ((String) -> Unit)?) {
        val imageBlock = block as? ContentBlock.Image
            ?: return

        // Загружаем изображение по пути
        imageBlock.imagePath.let { path ->
            if (path.isNotEmpty()) {
                imageView.visibility = View.VISIBLE
                // TODO: Загрузить изображение через Glide/Picasso
                // Glide.with(itemView.context).load(path).into(imageView)
            } else {
                imageView.visibility = View.GONE
            }
        }

        // Устанавливаем подпись
        captionEdit.setText(imageBlock.caption)
        
        // Обновляем подпись при изменении
        captionEdit.onTextChanged { caption ->
            onTextChanged?.invoke(caption)
        }
    }

    override fun getContent(): String {
        return captionEdit.text?.toString() ?: ""
    }

    private fun EditText.onTextChanged(listener: (String) -> Unit) {
        addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                listener(s?.toString() ?: "")
            }
        })
    }

    override fun toString(): String {
        return "ImageBlockViewHolder(caption='${captionEdit.text}')"
    }
}