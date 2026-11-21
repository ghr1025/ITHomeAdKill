package io.github.ghsssss.ithomeadkill.settings

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import java.util.concurrent.Executors

/** UI 只持有本地保存状态；此类不探测或声称宿主已激活。 */
class SettingsRepository(context: Context) {
    private val applicationContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val lifecycleLock = Any()
    private var closed = false
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "ITHomeSettings")
    }
    private val mutableState = mutableStateOf(SettingsState())
    val state: State<SettingsState> = mutableState

    // 首次打开、加载与写入都发生在同一个 IO 队列，不在主线程等待 SharedPreferences。
    private val preferences by lazy(LazyThreadSafetyMode.NONE) {
        // LSPosed's official legacy shared-preference extension owns the path
        // and permissions. Without it, keep the settings UI usable privately;
        // the host then fails open instead of performing chmod or custom IPC.
        @Suppress("DEPRECATION")
        try {
            applicationContext.getSharedPreferences(SettingsContract.PREFS_NAME, Context.MODE_WORLD_READABLE)
        } catch (_: SecurityException) {
            applicationContext.getSharedPreferences(SettingsContract.PREFS_NAME, Context.MODE_PRIVATE)
        }
    }
    private val store = SettingsStore(
        state = sharedStoreState,
        read = { preferences.all },
        commit = { values ->
            val editor = preferences.edit().clear()
            values.forEach { (key, value) ->
                when (value) {
                    is Boolean -> editor.putBoolean(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is String -> editor.putString(key, value)
                    else -> error("Unsupported settings value for $key")
                }
            }
            editor.commit()
        },
    )

    init {
        reload()
    }

    fun setToggle(toggle: Toggle, value: Boolean) {
        enqueue { store.setToggle(toggle, value) }
    }

    fun setRefreshMinutes(minutes: Int) {
        enqueue { store.setRefreshMinutes(minutes) }
    }

    fun reload() {
        enqueue { store.reload() }
    }

    fun close() {
        synchronized(lifecycleLock) {
            if (closed) return
            closed = true
            mainHandler.removeCallbacksAndMessages(null)
            // 不 interrupt 已接受的写入，也不遗弃队列；只停止新的任务与 UI 发布。
            executor.shutdown()
        }
    }

    private fun enqueue(operation: () -> SettingsState) {
        synchronized(lifecycleLock) {
            if (closed) return
            executor.execute {
                publish(SettingsState(store.settings, "正在读取或保存本机设置", busy = true))
                publish(operation())
            }
        }
    }

    private companion object {
        // 本进程只有这一个设置文件；页面重建不能丢弃尚未确认的提交。
        val sharedStoreState = SettingsStoreState()
    }

    private fun publish(next: SettingsState) {
        synchronized(lifecycleLock) {
            if (closed) return
            mainHandler.post {
                synchronized(lifecycleLock) {
                    // 处理已经 post、随后 close 的竞态，销毁后不再变更 Compose State。
                    if (!closed) mutableState.value = next
                }
            }
        }
    }
}
