package com.example.txtnotesapp.presentation.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import com.example.txtnotesapp.common.classes.BindingFragment
import com.example.txtnotesapp.databinding.FragmentSettingsBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsFragment : BindingFragment<FragmentSettingsBinding>() {

    private val viewModel: SettingsViewModel by viewModel()

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
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
        } catch (e: Exception) {
            renderImportStateError("Не удалось открыть файловый менеджер")
        }
    }

    private fun openDirectoryPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(intent, REQUEST_CODE_DIRECTORY)
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
        //todo сделать галочку нужен ли эккспорт
        shareExportFile(fileUri)
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

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_DIRECTORY && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                // Сохраняем разрешения на доступ к URI
                requireContext().contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                viewModel.saveDirectory(uri.toString())
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_DIRECTORY = 1001
    }
}