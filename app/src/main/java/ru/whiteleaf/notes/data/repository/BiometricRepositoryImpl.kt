import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import ru.whiteleaf.notes.domain.repository.BiometricRepository
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import java.util.concurrent.CancellationException
import android.os.Build
import androidx.annotation.RequiresApi

class BiometricRepositoryImpl(
    private val context: Context
) : BiometricRepository {

    @RequiresApi(Build.VERSION_CODES.P)
    override suspend fun authenticate(activity: FragmentActivity): Result<Unit> = suspendCoroutine { continuation ->
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Аутентификация")
            .setSubtitle("Используйте отпечаток пальца для разблокировки")
            .setNegativeButtonText("Отмена")
            .setConfirmationRequired(false)
            .build()

        var authenticationCompleted = false

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (!authenticationCompleted) {
                    authenticationCompleted = true
                    when (errorCode) {
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON ->
                            continuation.resume(Result.failure(CancellationException("Аутентификация отменена")))
                        else ->
                            continuation.resume(Result.failure(Exception("Ошибка аутентификации: $errString")))
                    }
                }
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                if (!authenticationCompleted) {
                    authenticationCompleted = true
                    continuation.resume(Result.success(Unit))
                }
            }

            override fun onAuthenticationFailed() {
                // Не завершаем здесь - ждем успеха или ошибки
            }
        }

        try {
            val activity = context as? FragmentActivity
            if (activity == null) {
                continuation.resume(Result.failure(IllegalStateException("Context is not a FragmentActivity")))
                return@suspendCoroutine
            }

            val executor = ContextCompat.getMainExecutor(context)
            val prompt = BiometricPrompt(activity, executor, callback)
            prompt.authenticate(promptInfo)
        } catch (e: Exception) {
            if (!authenticationCompleted) {
                authenticationCompleted = true
                continuation.resume(Result.failure(e))
            }
        }
    }

    override fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }

    override fun hasBiometricSetUp(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> false
            else -> false
        }
    }
}