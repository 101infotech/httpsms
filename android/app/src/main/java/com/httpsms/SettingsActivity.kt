package com.httpsms

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.httpsms.ui.settings.SettingsScreen
import com.httpsms.ui.settings.SettingsViewModel
import com.httpsms.ui.theme.HttpSmsTheme
import timber.log.Timber

class SettingsActivity : AppCompatActivity() {
    companion object {
        /** Which tab of the floating tab bar to open: [TAB_SIMS] or [TAB_SETTINGS]. */
        const val EXTRA_TAB = "com.httpsms.extra.TAB"
        const val TAB_SIMS = 1
        const val TAB_SETTINGS = 2
    }

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        viewModel.initialize(this)

        setContent {
            HttpSmsTheme {
                SettingsScreen(
                    viewModel = viewModel,
                    initialTab = intent.getIntExtra(EXTRA_TAB, TAB_SETTINGS),
                    onBackClick = { onBackClicked() },
                    onLogoutClick = { onLogoutClick() }
                )
            }
        }
    }

    private fun onBackClicked() {
        Timber.d("back button clicked")
        redirectToMain()
    }

    private fun redirectToMain() {
        finish()
        val switchActivityIntent = Intent(this, MainActivity::class.java)
        startActivity(switchActivityIntent)
    }

    private fun onLogoutClick() {
        Timber.d("logout button clicked")
        MaterialAlertDialogBuilder(this)
            .setTitle("Confirm")
            .setMessage("Disconnect this phone from Saubhagya SMS? It will stop sending and receiving messages.")
            .setNeutralButton("Cancel"){ _, _ -> Timber.d("logout dialog canceled") }
            .setPositiveButton("Logout"){_, _ ->
                Timber.d("logging out user")
                viewModel.logout(this) {
                    redirectToLogin()
                }
            }
            .show()
    }

    private fun redirectToLogin():Boolean {
        if (Settings.isLoggedIn(this)) {
            return false
        }
        val switchActivityIntent = Intent(this, LoginActivity::class.java)
        startActivity(switchActivityIntent)
        return true
    }
}
