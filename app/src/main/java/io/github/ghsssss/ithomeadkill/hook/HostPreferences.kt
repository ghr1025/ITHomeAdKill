package io.github.ghsssss.ithomeadkill.hook

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import io.github.ghsssss.ithomeadkill.settings.ModuleSettings
import io.github.ghsssss.ithomeadkill.settings.SettingsContract
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

internal fun moduleLog(message: String) {
    Log.i("ITHomeAdKill", message)
    XposedBridge.log("[ITHomeAdKill] $message")
}

/** Only this worker touches module preferences. View hooks read an immutable snapshot. */
internal class HostPreferences(private val onChanged: () -> Unit) {
    @Volatile var snapshot: ModuleSettings? = null
        private set
    private val worker = Executors.newSingleThreadExecutor { task ->
        Thread(task, "ithome-settings-reader").apply { isDaemon = true }
    }
    private val loading = AtomicBoolean()
    private val main = Handler(Looper.getMainLooper())
    private var lastRequested = Long.MIN_VALUE
    private var lastStatus = ""

    fun reload(force: Boolean = false) {
        val now = SystemClock.uptimeMillis()
        if (!force && lastRequested != Long.MIN_VALUE && now - lastRequested < 2_000) return
        if (!loading.compareAndSet(false, true)) return
        lastRequested = now
        worker.execute {
            var decoded: ModuleSettings? = null
            var status = "unavailable"
            try {
                val preferences = XSharedPreferences(SettingsContract.MODULE_PACKAGE, SettingsContract.PREFS_NAME)
                val values = preferences.all
                decoded = ModuleSettings.fromPreferences(values)
                status = if (decoded == null) "unavailable:keys=${values.size}"
                    else "ready:${decoded.revision}:schema=${values[SettingsContract.SCHEMA_KEY]}"
            } catch (error: Throwable) {
                status = "read-failed:${error.javaClass.simpleName}"
            }
            val result = decoded
            val resultStatus = status
            main.post {
                val changed = snapshot != result
                snapshot = result
                if (lastStatus != resultStatus) {
                    lastStatus = resultStatus
                    moduleLog("settings=$resultStatus")
                }
                loading.set(false)
                if (changed) onChanged()
            }
        }
    }
}
