package io.github.ghsssss.ithomeadkill.hook

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModernHookScopeTest {
    private val host = "com.ruanmei.ithome"

    @Test fun initializesOnlyOnceInTheFirstHostMainPackage() {
        val gate = ModernHookGate()
        assertTrue(gate.enter(host, true, host, false))
        assertFalse(gate.enter(host, true, host, false))
    }

    @Test fun rejectedPackagesAndProcessesDoNotConsumeInitialization() {
        val gate = ModernHookGate()
        assertFalse(gate.enter("other", true, host, false))
        assertFalse(gate.enter(host, false, host, false))
        assertFalse(gate.enter(host, true, "$host:remote", false))
        assertFalse(gate.enter(host, true, null, false))
        assertFalse(gate.enter(host, true, host, true))
        assertTrue(gate.enter(host, true, host, false))
    }
}
