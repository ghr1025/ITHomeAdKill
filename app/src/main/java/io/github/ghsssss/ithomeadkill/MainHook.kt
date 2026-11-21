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
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import io.github.ghsssss.ithomeadkill.hook.HostRuntime
import io.github.ghsssss.ithomeadkill.hook.entryName
import io.github.ghsssss.ithomeadkill.hook.moduleLog

/** Package/process-scoped hooks. No host data or network calls are modified. */
class MainHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(param: XC_LoadPackage.LoadPackageParam) {
        if (param.packageName != "com.ruanmei.ithome" || param.processName != param.packageName) return
        val runtime = HostRuntime()
        val main = Handler(Looper.getMainLooper())
        fun install(name: String, block: () -> Unit) {
            runCatching(block).onFailure { moduleLog("hook-install-failed:$name:${it.javaClass.simpleName}") }
        }
        install("Application.attach") {
            XposedHelpers.findAndHookMethod(Application::class.java, "attach", Context::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val context = param.args[0] as? Context ?: return
                        main.post { runtime.attach(context) }
                    }
                })
        }
        for (name in listOf("onResume", "onPause", "onDestroy")) install("Activity.$name") {
            XposedBridge.hookAllMethods(Activity::class.java, name, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val activity = param.thisObject as? Activity ?: return
                    when (name) {
                        "onResume" -> main.post { runtime.resumed(activity) }
                        "onPause" -> runtime.paused(activity)
                        "onDestroy" -> runtime.destroyed(activity)
                    }
                }
            })
        }
        install("Activity.dispatchTouchEvent") {
            XposedHelpers.findAndHookMethod(Activity::class.java, "dispatchTouchEvent", MotionEvent::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        (param.args[0] as? MotionEvent)?.let { runtime.touched(it.actionMasked) }
                    }
                })
        }
        install("TextView.setText") {
            XposedHelpers.findAndHookMethod(TextView::class.java, "setText", CharSequence::class.java, TextView.BufferType::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val view = param.thisObject as? TextView ?: return
                        if (entryName(view) in setOf("tv_small_tags", "tv_title", "tv_ad")) runtime.viewChanged(view)
                    }
                })
        }
        install("View.setVisibility") {
            XposedHelpers.findAndHookMethod(View::class.java, "setVisibility", Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val view = param.thisObject as? View ?: return
                        param.args[0] = runtime.desiredVisibility(view, param.args[0] as Int)
                    }
                })
        }
        install("View.setMinimumHeight") {
            XposedHelpers.findAndHookMethod(View::class.java, "setMinimumHeight", Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val view = param.thisObject as? View ?: return
                        param.args[0] = runtime.desiredMinimumHeight(view, param.args[0] as Int)
                    }
                })
        }
        install("View.setLayoutParams") {
            XposedHelpers.findAndHookMethod(View::class.java, "setLayoutParams", ViewGroup.LayoutParams::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val view = param.thisObject as? View ?: return
                        val params = param.args[0] as? ViewGroup.LayoutParams ?: return
                        runtime.desiredLayout(view, params)
                    }
                })
        }
        install("WebView.setWebViewClient") {
            XposedHelpers.findAndHookMethod(WebView::class.java, "setWebViewClient", WebViewClient::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        param.args[0]?.let(runtime::webClient)
                    }
                })
        }
        moduleLog("hooks-installed:main-process")
    }
}
