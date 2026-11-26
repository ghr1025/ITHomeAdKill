package io.github.ghsssss.ithomeadkill.settings

import org.junit.Assert.*
import org.junit.Test

class ServiceSessionsTest {
    @Test fun onlyApi102WithRemoteCapabilityIsEligible() {
        val sessions = ServiceSessions<Any>()
        val service = Any()
        sessions.bind(service, 101, 2L)
        assertNull(sessions.selected())
        sessions.bind(service, 102, 0L)
        assertNull(sessions.selected())
        sessions.bind(service, 102, 2L)
        assertSame(service, sessions.selected())
    }

    @Test fun multipleEligibleFrameworksAreNotSelectedArbitrarily() {
        val sessions = ServiceSessions<Any>()
        val first = Any()
        val second = Any()
        sessions.bind(first, 102, 2L)
        sessions.bind(second, 102, 2L)
        assertNull(sessions.selected())
        sessions.died(first)
        assertSame(second, sessions.selected())
    }

    @Test fun oldServiceDeathCannotClearAnotherSession() {
        val sessions = ServiceSessions<Any>()
        val old = Any()
        val current = Any()
        sessions.bind(old, 102, 2L)
        sessions.died(old)
        sessions.bind(current, 102, 2L)
        sessions.died(old)
        assertSame(current, sessions.selected())
    }

    @Test fun ineligibleFrameworkDoesNotMaskEligibleService() {
        val sessions = ServiceSessions<Any>()
        val active = Any()
        sessions.bind(active, 102, 2L)
        sessions.bind(Any(), 100, 2L)
        assertSame(active, sessions.selected())
    }

    @Test fun rebindingSameObjectDoesNotCreateAmbiguity() {
        val sessions = ServiceSessions<Any>()
        val active = Any()
        sessions.bind(active, 102, 2L)
        sessions.bind(active, 102, 2L)
        assertSame(active, sessions.selected())
        sessions.died(active)
        assertNull(sessions.selected())
    }
}
