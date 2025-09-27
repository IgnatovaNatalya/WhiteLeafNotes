package com.example.txtnotesapp.presentation.shareReceive


import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.txtnotesapp.R
import com.example.txtnotesapp.databinding.ActivityShareRecieverBinding
import com.example.txtnotesapp.domain.model.SharedContent
import com.example.txtnotesapp.domain.model.onError
import com.example.txtnotesapp.domain.model.onSuccess
import kotlin.time.Instant
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.androidx.viewmodel.ext.android.viewModel
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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        viewModel.processIntent(intent)

        setupObservers()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.shareReceiverToolbar.setOnClickListener {
            viewModel.insertNote(
                binding.noteEditContainer.noteTitle.text.toString(),
                binding.noteEditContainer.noteText.text.toString()
            )
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun formatDate(timestamp: Long): String {
        val date = Instant.fromEpochMilliseconds(timestamp)
            .toLocalDateTime(TimeZone.currentSystemDefault())

        return "${date.day} ${getMonthName(date.month)} ${date.year}"
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

    private fun showContent(sharedTitle: String?, sharedText: String) {
        if (!sharedTitle.isNullOrEmpty()) binding.noteEditContainer.noteTitle.setText(sharedTitle)
        binding.noteEditContainer.noteDate.text = formatDate(System.currentTimeMillis())
        binding.noteEditContainer.noteText.setText(sharedText)
    }

    private fun showContent(sharedText: String) = showContent(null, sharedText)

    private fun showError(error: String) {
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
    }

    private fun getMonthName(month: Month): String { //todo
        return when (month) {
            Month.JANUARY -> "января"
            Month.FEBRUARY -> "февраля"
            Month.MARCH -> "марта"
            Month.APRIL -> "апреля"
            Month.MAY -> "мая"
            Month.JUNE -> "июня"
            Month.JULY -> "июля"
            Month.AUGUST -> "августа"
            Month.SEPTEMBER -> "сентября"
            Month.OCTOBER -> "октября"
            Month.NOVEMBER -> "ноября"
            Month.DECEMBER -> "декабря"
        }
    }
}
