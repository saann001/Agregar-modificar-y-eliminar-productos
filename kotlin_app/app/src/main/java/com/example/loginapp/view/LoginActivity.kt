package com.example.loginapp.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.loginapp.R
import com.example.loginapp.controller.LoginController
import com.example.loginapp.controller.SessionController
import com.example.loginapp.databinding.ActivityLoginBinding
import com.example.loginapp.model.LoginResult
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * View de la pantalla de Login (patrón MVC).
 * Solo se encarga de capturar input y renderizar estado; toda la
 * lógica de negocio vive en [LoginController].
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var loginController: LoginController
    private lateinit var sessionController: SessionController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loginController = LoginController(applicationContext)
        sessionController = SessionController(applicationContext)

        // Auto-login: si ya existe sesión persistida, saltar directo a Home.
        if (sessionController.isAuthenticated()) {
            goToHome()
            return
        }

        binding.btnLogin.setOnClickListener { attemptLogin() }
    }

    private fun attemptLogin() {
        val username = binding.editUsername.text?.toString()?.trim().orEmpty()
        val password = binding.editPassword.text?.toString().orEmpty()

        setLoading(true)
        lifecycleScope.launch {
            val result = loginController.login(username, password)
            setLoading(false)

            when (result) {
                is LoginResult.Success -> {
                    sessionController.saveSession(result.user)
                    goToHome()
                }
                is LoginResult.InvalidCredentials ->
                    showError(getString(R.string.login_error_invalid))
                is LoginResult.NoConnection ->
                    showError(getString(R.string.login_error_no_connection))
                is LoginResult.UnknownError ->
                    showError(getString(R.string.login_error_unknown, result.detail))
            }
        }
    }

    private fun goToHome() {
        // US02 - se destruye el stack previo: Login no debe quedar
        // accesible con "Atrás" una vez autenticado.
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(getColor(R.color.error))
            .show()
    }
}
