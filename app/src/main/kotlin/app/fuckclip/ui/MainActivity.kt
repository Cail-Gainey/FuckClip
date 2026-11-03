package app.fuckclip.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.fuckclip.data.SettingsRepository
import app.fuckclip.i18n.AppLanguage
import app.fuckclip.ui.navigation.MainNavHost
import app.fuckclip.ui.settings.SettingsViewModel
import app.fuckclip.ui.theme.FuckClipTheme
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Single activity entry hosting the Jetpack Compose user interface.
 *
 * 承载 Jetpack Compose 用户界面的单 Activity 入口。
 */
class MainActivity : AppCompatActivity() {

    private val settingsRepository: SettingsRepository by inject()
    private val settingsViewModel: SettingsViewModel by viewModel()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.localizedContext(newBase))
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        recreate()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        settingsViewModel.checkOnLaunch()

        setContent {
            val colorMode by settingsRepository.themeColorMode.collectAsStateWithLifecycle()
            val keyColor by settingsRepository.themeKeyColor.collectAsStateWithLifecycle()
            val paletteStyle by settingsRepository.themePaletteStyle.collectAsStateWithLifecycle()

            FuckClipTheme(
                colorMode = colorMode,
                keyColor = keyColor,
                paletteStyle = paletteStyle
            ) {
                MainNavHost()
            }
        }
    }
}
