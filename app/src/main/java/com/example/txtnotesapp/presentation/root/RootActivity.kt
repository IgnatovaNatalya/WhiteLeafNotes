package com.example.txtnotesapp.presentation.root

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.txtnotesapp.R
import com.example.txtnotesapp.databinding.ActivityRootBinding
import com.example.txtnotesapp.domain.model.Note
import com.example.txtnotesapp.domain.model.Notebook
import com.example.txtnotesapp.presentation.note_list.NoteListFragmentDirections
import com.example.txtnotesapp.utils.PermissionUtils
import org.koin.androidx.viewmodel.ext.android.viewModel

class RootActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRootBinding
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navController: NavController
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var drawerMenuAdapter: DrawerMenuAdapter

    private val menuViewModel: DrawerMenuViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Проверка разрешений перед установкой контента
        if (!PermissionUtils.checkStoragePermission(this)) {
            PermissionUtils.requestStoragePermission(this)
            // Можно показать загрузочный экран или сообщение
        } else {
            initializeApp()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PermissionUtils.STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeApp()
            } else {
                // Разрешение не получено, показываем сообщение
                Toast.makeText(
                    this,
                    "Для работы приложения необходимо разрешение на доступ к хранилищу",
                    Toast.LENGTH_LONG
                ).show()
                // Можно предложить повторить запрос или закрыть приложение
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PermissionUtils.STORAGE_PERMISSION_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    initializeApp()
                } else {
                    Toast.makeText(
                        this,
                        "Для работы приложения необходимо разрешение на доступ к хранилищу",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun initializeApp() {

        enableEdgeToEdge()

        binding = ActivityRootBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            //v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }
        setupToolbar()
        setupNavigation()
        setupDrawerMenu()
        setupObservers()
        setupNavigationListener()
    }

    fun setDrawerEnabled(enabled: Boolean) {
        if (enabled) {
            binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            binding.toolbar.navigationIcon = ContextCompat.getDrawable(this, R.drawable.ic_menu)
        } else {
            binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
            binding.toolbar.navigationIcon = null
        }
    }

    private fun setupNavigationListener() {
        // Слушатель изменений навигации для управления кнопкой в AppBar
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.startFragment -> {
                    setDrawerEnabled(false)
                    //supportActionBar?.title = "Заметки и записные книжки"
                    supportActionBar?.hide()
                }

                R.id.noteListFragment -> {
                    setDrawerEnabled(true)
                    supportActionBar?.title = "Заметки"
                    supportActionBar?.setDisplayHomeAsUpEnabled(false)
                    binding.toolbar.navigationIcon = ContextCompat.getDrawable(
                        this,
                        R.drawable.ic_menu
                    )
                    supportActionBar?.show()
                }

                R.id.noteEditFragment -> {
                    setDrawerEnabled(false)
                    supportActionBar?.title = ""
                    supportActionBar?.setDisplayHomeAsUpEnabled(true)
                    binding.toolbar.navigationIcon = ContextCompat.getDrawable(
                        this,
                        R.drawable.ic_arrow_back
                    )
                    supportActionBar?.show()
                }
            }
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)//
        supportActionBar?.setHomeButtonEnabled(true)//
    }

    private fun setupNavigation() {

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        navController = navHostFragment.navController
        drawerLayout = binding.drawerLayout

        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.startFragment),
            drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    private fun setupDrawerMenu() {
        drawerMenuAdapter = DrawerMenuAdapter(
            onNotebookClicked = { notebook ->
                navigateToNotebook(notebook)
                drawerLayout.closeDrawer(GravityCompat.START)
            },
            onNoteClicked = { note ->
                navigateToNote(note)
                drawerLayout.closeDrawer(GravityCompat.START)
            },
            onCreateNotebook = {
                showCreateNotebookDialog()
            },
            onCreateNote = {
                menuViewModel.createNewNote()
            }
        )

        binding.navView.getHeaderView(0).findViewById<RecyclerView>(R.id.drawer_menu_recyclerView)
            .apply {
                adapter = drawerMenuAdapter
                layoutManager = LinearLayoutManager(this@RootActivity)
                addItemDecoration(
                    DividerItemDecoration(
                        this@RootActivity,
                        DividerItemDecoration.VERTICAL
                    )
                )
            }

        // Загрузка данных для меню
        menuViewModel.loadMenuData()
    }

    private fun setupObservers() {
        menuViewModel.menuItems.observe(this) { items ->
            drawerMenuAdapter.submitList(items)
        }

        menuViewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                menuViewModel.clearError()
            }
        }

        menuViewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                // Показать индикатор загрузки, если нужно
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.noteListFragment, R.id.noteEditFragment -> {
                    menuViewModel.loadMenuData()
                }
            }
        }
    }

    private fun navigateToNotebook(notebook: Notebook) {
        val action = NoteListFragmentDirections.actionGlobalNoteListFragment(notebook.path)
        navController.navigate(action)
    }

    private fun navigateToNote(note: Note) {
        val action = NoteListFragmentDirections.actionGlobalNoteEditFragment(
            noteTitle = note.title,
            notebookPath = null
        )
        navController.navigate(action)
    }

    override fun onSupportNavigateUp(): Boolean {
        return if (navController.currentDestination?.id == R.id.startFragment) {
            // На стартовом экране - открытие/закрытие Drawer
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
            true
        } else {
            // На других экранах - навигация назад
            navController.navigateUp() || super.onSupportNavigateUp()
        }
    }

    @Deprecated("This method has been deprecated in favor of using the\n{@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun showCreateNotebookDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_notebook, null)
        val editText = dialogView.findViewById<EditText>(R.id.notebook_name)

        AlertDialog.Builder(this)
            .setTitle("Создать записную книжку")
            .setView(dialogView)
            .setPositiveButton("Создать") { dialog, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotEmpty()) {
                    menuViewModel.createNewNotebook(name)
                } else {
                    Toast.makeText(this, "Введите название книжки", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
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