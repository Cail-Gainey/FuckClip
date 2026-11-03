package app.fuckclip

import android.app.Application
import android.content.Context
import app.fuckclip.di.appModule
import app.fuckclip.i18n.AppLanguage
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

/**
 * Main application class initializing dependency injection and localization configuration.
 *
 * 初始化依赖注入与本地化配置的主 Application 类。
 */
class FuckClipApp : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLanguage.localizedContext(base))
    }

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@FuckClipApp)
            modules(appModule)
        }
    }
}
