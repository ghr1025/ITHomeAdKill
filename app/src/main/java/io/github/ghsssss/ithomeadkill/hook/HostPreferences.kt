package io.github.ghsssss.ithomeadkill.hook

import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import io.github.ghsssss.ithomeadkill.settings.ModuleSettings
import io.github.ghsssss.ithomeadkill.settings.SettingsContract
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import java.util.concurrent.Executors

/** Creating the logger is inert; the attached module API is used only when logging. */
internal fun modernModuleLogger(module: XposedModule): (String) -> Unit = { message ->
    ModernHookFlow.safely({ module.log(Log.INFO, "ITHomeAdKill", message) })
}

/** Only this worker obtains/reads remote preferences. View hooks read a nullable snapshot. */
internal class HostPreferences(
    private val module: XposedModule,
    private val moduleLog: (String) -> Unit,
    private val onChanged: () -> Unit,
) {
    @Volatile var snapshot: ModuleSettings? = null
        private set
    private val worker = Executors.newSingleThreadExecutor { task ->
        Thread(task, "ithome-settings-reader").apply { isDaemon = true }
    }
    private val main = Handler(Looper.getMainLooper())
    private var lastRequested = Long.MIN_VALUE
    private var lastStatus = ""
    private var loading = false
    private var pending = false
    @Volatile private var closed = false
    // Worker-owned. Both this reference and the listener are retained strongly.
    private var remotePreferences: SharedPreferences? = null
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> reload(true) }

    @Synchronized fun reload(force: Boolean = false) {
        if (closed) return
        if (loading) {
            if (force) pending = true
            return
        }
        val now = SystemClock.uptimeMillis()
        if (!force && lastRequested != Long.MIN_VALUE && now - lastRequested < 2_000) return
        loading = true
        lastRequested = now
        worker.execute {
            var decoded: ModuleSettings? = null
            val status: String
            try {
                status = when {
                    module.apiVersion < XposedInterface.API_102 -> "unsupported:api"
                    module.frameworkProperties and XposedInterface.PROP_CAP_REMOTE == 0L -> "unsupported:remote"
                    else -> {
                        val preferences = module.getRemotePreferences(SettingsContract.PREFS_NAME)
                        if (remotePreferences !== preferences) {
                            releaseRemote()
                            preferences.registerOnSharedPreferenceChangeListener(prefsListener)
                            remotePreferences = preferences
                        }
                        val values = preferences.all
                        decoded = ModuleSettings.fromPreferences(values)
                        if (decoded == null) "unavailable:keys=${values.size}"
                        else "ready:${decoded.revision}:schema=${values[SettingsContract.SCHEMA_KEY]}"
                    }
                }
            } catch (error: Throwable) {
                releaseRemote()
                publish(null, "read-failed:${error.javaClass.simpleName}")
                return@execute
            }
            publish(decoded, status)
        }
    }

    private fun publish(result: ModuleSettings?, status: String) {
        main.post {
            try {
                if (!closed) {
                    val changed = snapshot != result
                    snapshot = result
                    if (lastStatus != status) {
                        lastStatus = status
                        ModernHookFlow.safely({ moduleLog("settings=$status") })
                    }
                    if (changed) ModernHookFlow.safely(onChanged, { error ->
                        moduleLog("settings-observer-failed:${error.javaClass.simpleName}")
                    })
                }
            } finally {
                finished()
            }
        }
    }

    @Synchronized private fun finished() {
        loading = false
        if (pending && !closed) {
            pending = false
            reload(true)
        }
    }

    /** Worker serialization prevents unregister from racing an in-flight read/register. */
    @Synchronized fun close() {
        if (closed) return
        closed = true
        pending = false
        main.post {
            val changed = snapshot != null
            snapshot = null
            if (changed) ModernHookFlow.safely(onChanged)
        }
        worker.execute(::releaseRemote)
        worker.shutdown()
    }

    private fun releaseRemote() {
        val previous = remotePreferences
        remotePreferences = null
        if (previous != null) ModernHookFlow.safely({ previous.unregisterOnSharedPreferenceChangeListener(prefsListener) })
    }
}
