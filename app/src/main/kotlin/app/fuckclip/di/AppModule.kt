package app.fuckclip.di

import app.fuckclip.data.SettingsRepository
import app.fuckclip.ui.home.HomeViewModel
import app.fuckclip.ui.log.LogViewModel
import app.fuckclip.ui.settings.SettingsViewModel
import app.fuckclip.ui.theme.ThemeViewModel
import app.fuckclip.ui.whitelist.WhitelistViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin dependency injection module configuring application singletons and view models.
 *
 * 配置应用单例与 ViewModel 的 Koin 依赖注入模块。
 */
val appModule = module {
    single { SettingsRepository(get()) }

    viewModel { HomeViewModel(get()) }
    viewModel { WhitelistViewModel(get(), get()) }
    viewModel { LogViewModel(get()) }
    viewModel { SettingsViewModel(get(), get()) }
    viewModel { ThemeViewModel(get()) }
}
