package com.example.txtnotesapp.presentation.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

        viewModel.currentPath.observe(viewLifecycleOwner) { path ->
            binding.folderPath.text = path ?: "Папка по умолчанию"
        }

        binding.settingsFolder.setOnClickListener {
            openDirectoryPicker()
        }

        binding.resetFolderButton.setOnClickListener {
            viewModel.resetToDefault()
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
                viewModel.saveCustomDirectory(uri.toString())
                viewModel.saveCustomDirectory("notes2")
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_DIRECTORY = 1001
    }
}