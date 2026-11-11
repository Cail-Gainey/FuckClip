package app.fuckclip.hook

import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface

/**
 * Entry point for the FuckClip Xposed module loaded within system_server.
 *
 * 在 system_server 进程中加载的 FuckClip Xposed 模块入口。
 */
class FuckClipModule : XposedModule() {

    private val clipboardHook = ClipboardHook(this)

    override fun onModuleLoaded(param: XposedModuleInterface.ModuleLoadedParam) {
        super.onModuleLoaded(param)
        log(Log.INFO, TAG, "Module loaded: process=${param.processName}, isSystemServer=${param.isSystemServer}")
    }

    override fun onSystemServerStarting(param: XposedModuleInterface.SystemServerStartingParam) {
        super.onSystemServerStarting(param)
        log(Log.INFO, TAG, "System server starting, installing hooks")
        clipboardHook.install(param.classLoader)
    }

    override fun onHotReloading(param: XposedModuleInterface.HotReloadingParam): Boolean {
        return true
    }

    override fun onHotReloaded(param: XposedModuleInterface.HotReloadedParam) {
        super.onHotReloaded(param)
        log(Log.INFO, TAG, "Hot reloaded, resetting and reinstalling hooks")
        if (param.isSystemServer) {
            clipboardHook.reset()
            clipboardHook.install()
        }
    }

    private companion object {
        const val TAG = "FuckClipModule"
    }
}
