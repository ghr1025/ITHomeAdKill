package io.github.ghsssss.ithomeadkill.settings

import android.util.Log
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import java.util.concurrent.CopyOnWriteArraySet

/** A single process owner; a Binder's death never clears a newer service identity. */
internal object FrameworkConnection {
    private val sessions = ServiceSessions<XposedService>()
    private val listeners = CopyOnWriteArraySet<() -> Unit>()
    private var started = false
    private val serviceListener = object : XposedServiceHelper.OnServiceListener {
        override fun onServiceBind(service: XposedService) {
            try {
                sessions.bind(service, service.apiVersion, service.frameworkProperties)
                Log.i("ITHomeAdKill", "service-bound:api=${service.apiVersion}:" +
                    "framework=${service.frameworkName}:${service.frameworkVersionCode}")
            } catch (error: RuntimeException) {
                sessions.died(service)
                Log.w("ITHomeAdKill", "service-bind-failed:${error.javaClass.simpleName}")
            }
            notifyListeners()
        }

        override fun onServiceDied(service: XposedService) {
            sessions.died(service)
            Log.i("ITHomeAdKill", "service-died")
            notifyListeners()
        }
    }

    @Synchronized fun start() {
        if (started) return
        started = true
        XposedServiceHelper.registerListener(serviceListener)
    }

    fun selected(): XposedService? = sessions.selected()

    fun subscribe(listener: () -> Unit) { listeners.add(listener) }
    fun unsubscribe(listener: () -> Unit) { listeners.remove(listener) }

    private fun notifyListeners() {
        listeners.forEach { listener ->
            try { listener() } catch (error: RuntimeException) {
                Log.w("ITHomeAdKill", "service-listener-failed:${error.javaClass.simpleName}")
            }
        }
    }
}
