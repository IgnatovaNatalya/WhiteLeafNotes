import android.content.SharedPreferences
import androidx.core.content.edit
import ru.whiteleaf.notes.domain.repository.SecurityPreferences

class SecurityPreferencesImpl(
    private val sharedPreferences: SharedPreferences
) : SecurityPreferences {

    companion object {
        private const val PREFIX_ENCRYPTED = "notebook_encrypted_"
        private const val PREFIX_UNLOCKED = "notebook_unlocked_"
    }

    override fun setNotebookEncrypted(notebookPath: String, encrypted: Boolean) {
        sharedPreferences.edit {
            putBoolean(PREFIX_ENCRYPTED + notebookPath, encrypted)
        }
    }

    override fun isNotebookEncrypted(notebookPath: String): Boolean {
        return sharedPreferences.getBoolean(PREFIX_ENCRYPTED + notebookPath, false)
    }

    override fun setNotebookUnlocked(notebookPath: String, unlocked: Boolean) {
        sharedPreferences.edit {
            putBoolean(PREFIX_UNLOCKED + notebookPath, unlocked)
            apply() // Используем apply() для немедленного сохранения
        }
    }

    override fun isNotebookUnlocked(notebookPath: String): Boolean {
        return sharedPreferences.getBoolean(PREFIX_UNLOCKED + notebookPath, false)
    }

    override fun clearUnlockedState() {
        // Находим все ключи связанные с разблокировкой и очищаем их
        val allPreferences = sharedPreferences.all
        val keysToRemove = allPreferences.keys.filter { it.startsWith(PREFIX_UNLOCKED) }

        sharedPreferences.edit {
            keysToRemove.forEach { key ->
                remove(key)
            }
        }
    }
}