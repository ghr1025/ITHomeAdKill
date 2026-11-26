package io.github.ghsssss.ithomeadkill

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import io.github.ghsssss.ithomeadkill.hook.HostRuntime
import io.github.ghsssss.ithomeadkill.hook.ModernHookFlow
import io.github.ghsssss.ithomeadkill.hook.ModernHookGate
import io.github.ghsssss.ithomeadkill.hook.ModernHooks
import io.github.ghsssss.ithomeadkill.hook.entryName
import io.github.ghsssss.ithomeadkill.hook.modernModuleLogger
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface

/** Public no-argument modern entry. No framework calls occur during construction. */
class MainHook : XposedModule() {
    private val gate = ModernHookGate()
    private val moduleLog = modernModuleLogger(this)
    private var processName: String? = null
    private var systemServer = false
    private var runtimeApi = 0
    private var hooks: ModernHooks? = null

    override fun onModuleLoaded(param: XposedModuleInterface.ModuleLoadedParam) {
        processName = param.processName
        systemServer = param.isSystemServer
        ModernHookFlow.safely({
            runtimeApi = apiVersion
            val remote = frameworkProperties and XposedInterface.PROP_CAP_REMOTE != 0L
            moduleLog("module-loaded:process=$processName systemServer=$systemServer api=$runtimeApi " +
                "framework=$frameworkName version=$frameworkVersion code=$frameworkVersionCode remote=$remote")
        }, { error -> moduleLog("module-runtime-info-failed:${error.javaClass.simpleName}") })
    }

    override fun onPackageReady(param: XposedModuleInterface.PackageReadyParam) {
        if (runtimeApi < XposedInterface.API_102) return
        if (!gate.enter(param.packageName, param.isFirstPackage, processName, systemServer)) return
        val installed = ModernHooks(this, moduleLog).also { hooks = it }
        val runtime = HostRuntime(this, installed, moduleLog, param.classLoader)
        val main = Handler(Looper.getMainLooper())
        fun post(name: String, action: () -> Unit) {
            main.post {
                ModernHookFlow.safely(action, { error ->
                    moduleLog("lifecycle-failed:$name:${error.javaClass.simpleName}")
                })
            }
        }

        installed.observe(Application::class.java, "attach", Context::class.java, after = { chain ->
            val context = chain.args[0] as? Context ?: return@observe
            post("attach") { runtime.attach(context) }
        })
        for (name in listOf("onResume", "onPause", "onDestroy")) {
            installed.observe(Activity::class.java, name, after = { chain ->
                val activity = chain.thisObject as? Activity ?: return@observe
                when (name) {
                    "onResume" -> post("resume") { runtime.resumed(activity) }
                    "onPause" -> runtime.paused(activity)
                    "onDestroy" -> runtime.destroyed(activity)
                }
            })
        }
        installed.observe(Activity::class.java, "dispatchTouchEvent", MotionEvent::class.java, before = { chain ->
            (chain.args[0] as? MotionEvent)?.let { runtime.touched(it.actionMasked) }
        })
        installed.observe(TextView::class.java, "setText", CharSequence::class.java, TextView.BufferType::class.java, after = { chain ->
            val view = chain.thisObject as? TextView ?: return@observe
            if (entryName(view) in setOf("tv_small_tags", "tv_title", "tv_ad")) runtime.viewChanged(view)
        })
        installed.observe(View::class.java, "setVisibility", Int::class.javaPrimitiveType!!, arguments = { chain, copy ->
            val view = chain.thisObject as? View ?: return@observe
            copy[0] = runtime.desiredVisibility(view, copy[0] as Int)
        })
        installed.observe(View::class.java, "setMinimumHeight", Int::class.javaPrimitiveType!!, arguments = { chain, copy ->
            val view = chain.thisObject as? View ?: return@observe
            copy[0] = runtime.desiredMinimumHeight(view, copy[0] as Int)
        })
        installed.observe(View::class.java, "setLayoutParams", ViewGroup.LayoutParams::class.java, before = { chain ->
            val view = chain.thisObject as? View ?: return@observe
            val params = chain.args[0] as? ViewGroup.LayoutParams ?: return@observe
            runtime.desiredLayout(view, params)
        })
        installed.observe(WebView::class.java, "setWebViewClient", WebViewClient::class.java, after = { chain ->
            chain.args[0]?.let(runtime::webClient)
        })
        moduleLog("hooks-installed:main-process count=${installed.size} api=$runtimeApi")
    }
}
