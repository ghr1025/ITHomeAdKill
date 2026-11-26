package io.github.ghsssss.ithomeadkill.hook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ModernHookFlowTest {
    @Test fun replacementArgumentsUseACopyOfTheImmutableList() {
        val original: List<Any?> = java.util.Collections.unmodifiableList(listOf(7, "value"))
        val replacement = ModernHookFlow.prepareArguments(original, { it[0] = 0 })
        assertEquals(listOf(7, "value"), original)
        assertEquals(listOf(0, "value"), replacement?.toList())
    }

    @Test fun failedArgumentPreparationFallsBackWithoutMutatingOriginal() {
        val original: List<Any?> = listOf(7)
        val failure = IllegalArgumentException("arguments")
        val failures = mutableListOf<Throwable>()
        val replacement = ModernHookFlow.prepareArguments(original, { copy ->
            copy[0] = 0
            throw failure
        }, { failures += it })
        assertEquals(null, replacement)
        assertEquals(listOf(7), original)
        assertEquals(listOf(failure), failures)
    }

    @Test fun brokenFailureReporterCannotMaskTheHostException() {
        val hostFailure = AssertionError("host")
        var calls = 0
        val escaped = runCatching {
            ModernHookFlow.observe(
                proceed = { calls++; throw hostFailure },
                before = { error("module-before") },
                after = { error("module-after") },
                onFailure = { error("module-logger") },
            )
        }.exceptionOrNull()
        assertSame(hostFailure, escaped)
        assertEquals(1, calls)
    }

    @Test fun hostExceptionIsPropagatedIdenticallyAndAfterCleanupStillRuns() {
        val hostFailure = UnsupportedOperationException("host")
        val moduleFailure = IllegalStateException("cleanup")
        val failures = mutableListOf<Throwable>()
        var calls = 0
        var cleanups = 0
        val escaped = runCatching {
            ModernHookFlow.observe(
                proceed = { calls++; throw hostFailure },
                after = { cleanups++; throw moduleFailure },
                onFailure = { failures += it },
            )
        }.exceptionOrNull()
        assertSame(hostFailure, escaped)
        assertEquals(1, calls)
        assertEquals(1, cleanups)
        assertEquals(listOf(moduleFailure), failures)
    }

    @Test fun failingAfterObservationPreservesTheHostResult() {
        val failure = IllegalArgumentException("module-after")
        val failures = mutableListOf<Throwable>()
        val expected = Any()
        var calls = 0
        var actual: Any? = null
        val escaped = runCatching {
            actual = ModernHookFlow.observe(
                proceed = { calls++; expected },
                after = { throw failure },
                onFailure = { failures += it },
            )
        }.exceptionOrNull()
        assertEquals(null, escaped)
        assertSame(expected, actual)
        assertEquals(1, calls)
        assertEquals(listOf(failure), failures)
    }

    @Test fun failingBeforeObservationDoesNotSkipOrRepeatTheHost() {
        val failure = IllegalStateException("module-before")
        val failures = mutableListOf<Throwable>()
        val expected = Any()
        var calls = 0
        var actual: Any? = null
        val escaped = runCatching {
            actual = ModernHookFlow.observe(
                proceed = { calls++; expected },
                before = { throw failure },
                onFailure = { failures += it },
            )
        }.exceptionOrNull()
        assertEquals(null, escaped)
        assertSame(expected, actual)
        assertEquals(1, calls)
        assertEquals(listOf(failure), failures)
    }

    @Test fun observationsSurroundExactlyOneCallAndPreserveItsResult() {
        val events = mutableListOf<String>()
        val expected = Any()
        var calls = 0
        val actual = ModernHookFlow.observe(
            proceed = { calls++; events += "host"; expected },
            before = { events += "before" },
            after = { events += "after" },
        )
        assertSame(expected, actual)
        assertEquals(1, calls)
        assertEquals(listOf("before", "host", "after"), events)
    }
}
