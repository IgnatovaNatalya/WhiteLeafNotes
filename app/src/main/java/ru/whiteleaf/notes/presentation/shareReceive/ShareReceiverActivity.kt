package ru.whiteleaf.notes.presentation.shareReceive


import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import ru.whiteleaf.notes.R
import ru.whiteleaf.notes.databinding.ActivityShareRecieverBinding
import ru.whiteleaf.notes.domain.model.SharedContent
import ru.whiteleaf.notes.domain.model.onError
import ru.whiteleaf.notes.domain.model.onSuccess
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.whiteleaf.notes.common.utils.ContextMenuHelper
import ru.whiteleaf.notes.common.utils.DateHelper
import kotlin.getValue
import kotlin.time.ExperimentalTime

class ShareReceiverActivity : AppCompatActivity() {
    private lateinit var binding: ActivityShareRecieverBinding
    private val viewModel: ShareReceiverViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        binding = ActivityShareRecieverBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.share_receiver_main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        viewModel.processIntent(intent)

        setupObservers()
        setupNavigationListener()
        setupOptionsMenu()
    }


    private fun setupNavigationListener() {
        binding.shareReceiverToolbar.setNavigationOnClickListener { saveNote() }
    }

    private fun saveNote() {
        viewModel.insertNote(
            binding.noteEditContainer.noteTitle.text.toString(),
            binding.noteEditContainer.noteText.text.toString()
        )
    }

    @OptIn(ExperimentalTime::class)
    private fun formatDate(timestamp: Long): String {
        val date = Instant.fromEpochMilliseconds(timestamp)
            .toLocalDateTime(TimeZone.currentSystemDefault())

        return "${date.day} ${DateHelper.getMonthName(date.month)} ${date.year}"
    }

    private fun setupObservers() {

        viewModel.contentState.observe(this) { result ->
            result.onSuccess { content ->
                when (content) {
                    is SharedContent.FileContent -> showContent(content.name, content.text)
                    is SharedContent.TextContent -> showContent(content.text)
                }
            }.onError { message ->
                showError(message)
            }
        }

        viewModel.isSaved.observe(this) { isSaved ->
            if (isSaved) {
                Toast.makeText(this, "Заметка сохранена", Toast.LENGTH_SHORT).show()
//                Snackbar.make(binding.shareReceiverMain, "Заметка сохранена", Snackbar.LENGTH_LONG)
//                    .setAction("Открыть в приложении") {
//                        viewModel.recentNoteId.value?.let { noteId ->
//                            openSpecificNote(noteId)
//                        }
//                    }
//                    .show()

                finish()
            }
        }
        viewModel.message.observe(this) { message ->
            message?.let {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                viewModel.clearMessage()
            }
        }
    }

    private fun setupOptionsMenu() {
        val optionsButton = binding.btnShareReceiverOptions

        optionsButton.setOnClickListener {
            ContextMenuHelper.showPopupMenu(
                context = this,
                anchorView = optionsButton,
                items = ContextMenuHelper.getOptionsMenuShareReceiver(optionsButton.context),
                onItemSelected = { itemId ->
                    when (itemId) {
                        R.id.options_save_note -> saveNote()
                        R.id.options_append_note -> appendNote()
                        R.id.options_cancel -> finish()
                    }
                }
            )
        }
    }

    private fun appendNote() {
        Toast.makeText(this, "Добавление к существующей заметке пока не реализовано", Toast.LENGTH_SHORT).show()
    }

    private fun showContent(sharedTitle: String?, sharedText: String) {
        if (!sharedTitle.isNullOrEmpty()) binding.noteEditContainer.noteTitle.setText(sharedTitle)
        binding.noteEditContainer.noteDate.text = formatDate(System.currentTimeMillis())
        binding.noteEditContainer.noteText.setText(sharedText)
    }

    private fun showContent(sharedText: String) = showContent(null, sharedText)

    private fun showError(error: String) {
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
    }

}
