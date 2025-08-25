package com.example.txtnotesapp.presentation.root

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.txtnotesapp.R
import com.example.txtnotesapp.databinding.ActivityRootBinding
import com.example.txtnotesapp.presentation.note_list.NoteListFragmentDirections
import com.example.txtnotesapp.utils.PermissionUtils
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class RootActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRootBinding
    private lateinit var appBarConfiguration: AppBarConfiguration

    private lateinit var navController: NavController
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

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

        // Установка темы перед setContentView
        //setTheme(R.style.Theme_WhiteList)

        binding = ActivityRootBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            //v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }
        //window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN //вообще убирает статус бар вместе с иконками

        setupToolbar()
        setupNavigation()
        setupDrawer()
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
                    // На стартовом экране скрываем Drawer
                    setDrawerEnabled(false)
                    supportActionBar?.title = "Мои заметки"
                }

                R.id.noteListFragment -> {
                    // На главном экране показываем drawer ,  гамбургер и заголовок "Заметки"
                    setDrawerEnabled(true)
                    supportActionBar?.title = "Заметки"
                    supportActionBar?.setDisplayHomeAsUpEnabled(false)
                    binding.toolbar.navigationIcon = ContextCompat.getDrawable(
                        this,
                        R.drawable.ic_menu
                    )

                }

                R.id.noteEditFragment -> {
                    // На экране редактирования скрываем Drawer и показываем стрелку "назад"
                    setDrawerEnabled(false)
                    supportActionBar?.title = ""
                    supportActionBar?.setDisplayHomeAsUpEnabled(true)
                    binding.toolbar.navigationIcon = ContextCompat.getDrawable(
                        this,
                        R.drawable.ic_arrow_back
                    )
                }
            }
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
    }

    private fun setupNavigation() {

        drawerLayout = binding.drawerLayout
        navController = findNavController(R.id.nav_host_fragment)
        val navView: NavigationView = binding.navView

//        val navHostFragment = supportFragmentManager
//            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
//        navController = navHostFragment.navController

        // Настройка AppBarConfiguration с верхними уровнями навигации

        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.noteListFragment),
            drawerLayout
        )

        // Связывание NavController с AppBar
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Связывание NavController с NavigationView
        navView.setupWithNavController(navController)
    }



    private fun setupDrawer() {
        // Настраиваем поведение DrawerLayout для перекрытия AppBar
        drawerLayout.setStatusBarBackgroundColor(Color.TRANSPARENT)
        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
//                // Анимация затемнения основного контента
//                val scale = 1 - slideOffset * 0.1f
//                binding.toolbar.alpha = 1 - slideOffset * 0.5f
            }

            override fun onDrawerOpened(drawerView: View) {
                // Скрываем кнопки в AppBar при открытии меню
                supportActionBar?.setDisplayShowHomeEnabled(false)
                supportActionBar?.setDisplayHomeAsUpEnabled(false)
                //hideSystemUI()
            }

            override fun onDrawerClosed(drawerView: View) {
                // Восстанавливаем кнопки в AppBar при закрытии меню
                supportActionBar?.setDisplayShowHomeEnabled(true)
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
                binding.toolbar.alpha = 1f
                //showSystemUI()
            }

            override fun onDrawerStateChanged(newState: Int) {
                // Не используется
            }
        })

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

                R.id.menu_create_other_notebook -> {
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
        //return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp()

        if (navController.currentDestination?.id == R.id.noteEditFragment) { // На экране редактирования

            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            }

            //navController.navigateUp() || super.onSupportNavigateUp()
            navController.navigate(R.id.noteListFragment) // все равно открвается меню
            return true
        }

        if (navController.currentDestination?.id == R.id.noteListFragment) {// На главном экране
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
            return true
        }
       return true
    }

    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun loadNotebooksIntoMenu() {
        // Загрузка списка записных книжек в боковое меню
        // todo  переделать с ViewModel
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val notebooksDir = File(filesDir, "notes")
                if (notebooksDir.exists() && notebooksDir.isDirectory) {
                    val notebooks =
                        notebooksDir.listFiles()?.filter { it.isDirectory } ?: emptyList()

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