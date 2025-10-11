package ru.whiteleaf.notes.presentation.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import ru.whiteleaf.notes.common.classes.BindingFragment
import ru.whiteleaf.notes.databinding.FragmentSettingsBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsFragment : BindingFragment<FragmentSettingsBinding>() {

    private val viewModel: SettingsViewModel by viewModel()

    private val importLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { viewModel.importNotesFromZip(it) }
        }

    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSettingsBinding {
        return FragmentSettingsBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupObservers()
    }

    private fun setupUI() {

        binding.importButton.setOnClickListener {
            openFilePicker()
        }
//        binding.usePassword.setOnCheckedChangeListener { _, isChecked ->
//            binding.exportPassword.visibility = if (isChecked) View.VISIBLE else View.GONE
//        }

        binding.exportButton.setOnClickListener {
//            val password = if (binding.usePassword.isChecked) {
//                binding.exportPassword.text.toString().takeIf { it.isNotBlank() }
//            } else {
//                null
//            }
//            viewModel.exportNotes(password)
            viewModel.exportNotes(null)
        }
    }

    private fun setupObservers() {

        viewModel.exportPath.observe(viewLifecycleOwner) { path ->
            binding.folderPath.text = path
        }

        viewModel.exportState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ExportState.Idle -> renderExportIdle()
                is ExportState.Loading -> renderExportLoading()
                is ExportState.Success -> renderExportSuccess(state.fileUri)
                is ExportState.Error -> renderExportError(state.message)
            }
        }
        viewModel.importState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ImportState.Error -> renderImportStateError(state.message)
                ImportState.Idle -> renderImportStateIdle()
                ImportState.Loading -> renderImportStateLoading()
                ImportState.Success -> renderImportStateSuccess()
            }
        }
    }

    private fun openFilePicker() {
        try {
            importLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed"))
        } catch (_: Exception) {
            renderImportStateError("Не удалось открыть файловый менеджер")
        }
    }

    private fun renderImportStateSuccess() {
        binding.importButton.isEnabled = true
        binding.importStatus.text = "Импорт выполнен успешно"
    }

    private fun renderImportStateLoading() {
        binding.importButton.isEnabled = false
        binding.importStatus.text = "Распаковка архива..."
    }

    private fun renderImportStateIdle() {
        binding.importButton.isEnabled = true
        binding.importStatus.text = ""
    }

    private fun renderImportStateError(message: String) {
        binding.importButton.isEnabled = true
        binding.importStatus.text = "Ошибка: $message"
    }

    private fun renderExportIdle() {
        binding.exportStatus.text = ""
        binding.exportButton.isEnabled = true
    }

    private fun renderExportLoading() {
        binding.exportButton.isEnabled = false
        binding.exportStatus.text = "Создание архива..."
    }

    private fun renderExportSuccess(fileUri: Uri?) {
        binding.exportButton.isEnabled = true
        binding.exportStatus.text = "Экспорт завершен успешно"

        if (binding.shareZip.isChecked == true) shareExportFile(fileUri)
    }

    private fun renderExportError(message: String) {
        binding.exportButton.isEnabled = true
        binding.exportStatus.text = "Ошибка: $message"
    }

    private fun shareExportFile(uri: Uri?) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "application/zip"
        }
        startActivity(Intent.createChooser(shareIntent, "Поделиться архивом"))
    }

}