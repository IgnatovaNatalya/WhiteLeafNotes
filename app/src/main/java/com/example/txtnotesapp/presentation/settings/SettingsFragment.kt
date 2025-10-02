package com.example.txtnotesapp.presentation.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.txtnotesapp.common.classes.BindingFragment
import com.example.txtnotesapp.databinding.FragmentSettingsBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsFragment : BindingFragment<FragmentSettingsBinding>() {

    private val viewModel: SettingsViewModel by viewModel()

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
                is ExportState.Idle -> {
                    hideProgress()
                    binding.exportStatus.text = ""
                }

                is ExportState.Loading -> {
                    showProgress()
                    binding.exportStatus.text = "Создание архива..."
                }

                is ExportState.Success -> {
                    hideProgress()
                    binding.exportStatus.text = "Экспорт завершен успешно"
                    shareExportFile(state.fileUri)
                }

                is ExportState.Error -> {
                    hideProgress()
                    binding.exportStatus.text = "Ошибка: ${state.message}"
                    Toast.makeText(requireContext(), "Ошибка экспорта", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showProgress() {
        binding.exportStatus.text = "Загрузка"
        binding.exportButton.isEnabled = false
    }

    private fun hideProgress() {
        binding.exportStatus.text = ""
        binding.exportButton.isEnabled = true
    }

    private fun shareExportFile(uri: Uri?) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "application/zip"
        }
        startActivity(Intent.createChooser(shareIntent, "Поделиться архивом"))
    }

    private fun openDirectoryPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(intent, REQUEST_CODE_DIRECTORY)
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
                viewModel.saveExportDirectory(uri.toString())
                viewModel.saveExportDirectory("notes2")
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_DIRECTORY = 1001
    }
}