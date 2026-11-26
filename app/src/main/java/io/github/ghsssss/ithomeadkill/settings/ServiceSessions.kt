package io.github.ghsssss.ithomeadkill.settings

import java.util.IdentityHashMap

/** Binder service objects are session identities, never compared by display names. */
internal class ServiceSessions<T : Any> {
    private val eligible = IdentityHashMap<T, Boolean>()

    @Synchronized fun bind(service: T, api: Int, properties: Long) {
        eligible[service] = api >= 102 && properties and 2L != 0L
    }

    @Synchronized fun died(service: T) { eligible.remove(service) }

    @Synchronized fun selected(): T? = eligible.keys.filter { eligible[it] == true }.singleOrNull()
}
