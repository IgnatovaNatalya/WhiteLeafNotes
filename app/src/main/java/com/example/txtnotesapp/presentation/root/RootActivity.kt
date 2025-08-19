package com.example.txtnotesapp.presentation.root

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.txtnotesapp.R
import com.example.txtnotesapp.databinding.ActivityRootBinding
import com.example.txtnotesapp.presentation.note_list.NoteListFragmentDirections
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class RootActivity  : AppCompatActivity() {
    private lateinit var binding: ActivityRootBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Установка темы перед setContentView
       setTheme(R.style.Theme_TxtNotesApp)

        binding = ActivityRootBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupNavigation()
        setupDrawer()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Настройка AppBarConfiguration с верхними уровнями навигации
        drawerLayout = binding.drawerLayout
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.noteListFragment), // Фрагменты верхнего уровня
            drawerLayout
        )

        // Связывание NavController с AppBar
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Связывание NavController с NavigationView
        binding.navView.setupWithNavController(navController)
    }

    private fun setupDrawer() {
        // Обработка кликов по элементам бокового меню
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_all_notes -> {
                    // Переход к списку всех заметок (корневая папка)
                    val action = NoteListFragmentDirections
                        .actionGlobalNoteListFragment(null)
                    navController.navigate(action)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.menu_create_notebook -> {
                    // Создание новой записной книжки
                    showCreateNotebookDialog()
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                else -> {
                    // Для других элементов используем стандартное поведение
                    NavigationUI.onNavDestinationSelected(menuItem, navController)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
            }
        }

        // Загрузка списка записных книжек в меню
        loadNotebooksIntoMenu()
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun loadNotebooksIntoMenu() {
        // Загрузка списка записных книжек в боковое меню
        // Это упрощенная реализация, в реальном приложении нужно использовать ViewModel
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val notebooksDir = File(filesDir, "notes")
                if (notebooksDir.exists() && notebooksDir.isDirectory) {
                    val notebooks = notebooksDir.listFiles()?.filter { it.isDirectory } ?: emptyList()

                    withContext(Dispatchers.Main) {
                        val menu = binding.navView.menu
                        val notebooksMenu = menu.findItem(R.id.menu_notebooks).subMenu
                        notebooksMenu?.clear()

                        notebooks.sortedByDescending { it.lastModified() }.forEach { notebook ->
                            notebooksMenu?.add(
                                R.id.menu_group_notebooks,
                                Menu.NONE,
                                Menu.NONE,
                                notebook.name
                            )?.setOnMenuItemClickListener {
                                val action = NoteListFragmentDirections
                                    .actionGlobalNoteListFragment(notebook.name)
                                navController.navigate(action)
                                drawerLayout.closeDrawer(GravityCompat.START)
                                true
                            }
                        }

                        // Если записных книжек нет, показываем соответствующий текст
                        if (notebooks.isEmpty()) {
                            val noNotebooksItem = notebooksMenu?.add(
                                R.id.menu_group_notebooks,
                                Menu.NONE,
                                Menu.NONE,
                                "Записных книжек пока нет"
                            )
                            noNotebooksItem?.isEnabled = false
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("RootActivity", "Ошибка загрузки записных книжек", e)
            }
        }
    }

    private fun showCreateNotebookDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_notebook, null)
        val editText = dialogView.findViewById<EditText>(R.id.notebook_name)

        AlertDialog.Builder(this)
            .setTitle("Создать записную книжку")
            .setView(dialogView)
            .setPositiveButton("Создать") { dialog, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotEmpty()) {
                    createNotebook(name)
                } else {
                    Toast.makeText(this, "Введите название книжки", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun createNotebook(name: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val notebooksDir = File(filesDir, "notes")
                val newNotebookDir = File(notebooksDir, name)
                if (!newNotebookDir.exists()) {
                    newNotebookDir.mkdirs()

                    // Обновляем меню
                    withContext(Dispatchers.Main) {
                        loadNotebooksIntoMenu()
                        Toast.makeText(
                            this@RootActivity,
                            "Записная книжка создана",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@RootActivity,
                            "Записная книжка с таким именем уже существует",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@RootActivity,
                        "Ошибка создания записной книжки",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    drawerLayout.openDrawer(GravityCompat.START)
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}