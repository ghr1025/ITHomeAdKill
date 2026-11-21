package io.github.ghsssss.ithomeadkill.hook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RefreshScheduleTest {
    @Test
    fun negativeUpdateTimeIsRejectedBeforeChangingState() {
        listOf(-1L, Long.MIN_VALUE).forEach { now ->
            val schedule = RefreshSchedule()
            assertThrows(IllegalArgumentException::class.java) {
                schedule.update(0, active = false, column = null, nowMillis = now)
            }
            schedule.update(1, active = true, column = "news", nowMillis = 1_000L)

            assertThrows(IllegalArgumentException::class.java) {
                schedule.update(5, active = true, column = "tech", nowMillis = now)
            }
            assertThrows(IllegalArgumentException::class.java) {
                schedule.update(0, active = false, column = null, nowMillis = now)
            }
            assertEquals(
                61_000L,
                schedule.update(1, active = true, column = "news", nowMillis = 50_000L),
            )
            assertTrue(schedule.due(61_000L))
        }
    }

    @Test
    fun negativeDueTimeIsRejectedEvenWhenStopped() {
        listOf(-1L, Long.MIN_VALUE).forEach { now ->
            val schedule = RefreshSchedule()
            assertThrows(IllegalArgumentException::class.java) { schedule.due(now) }
            schedule.update(1, active = true, column = "news", nowMillis = 0L)

            assertThrows(IllegalArgumentException::class.java) { schedule.due(now) }
            assertTrue(schedule.due(60_000L))
        }
    }

    @Test
    fun negativeConsumedTimeIsRejectedBeforeChangingState() {
        listOf(-1L, Long.MIN_VALUE).forEach { now ->
            val schedule = RefreshSchedule()
            assertThrows(IllegalArgumentException::class.java) { schedule.consumed(now) }
            schedule.update(1, active = true, column = "news", nowMillis = 1_000L)

            assertThrows(IllegalArgumentException::class.java) { schedule.consumed(now) }
            assertEquals(
                61_000L,
                schedule.update(1, active = true, column = "news", nowMillis = 50_000L),
            )
            assertTrue(schedule.due(61_000L))
        }
    }

    @Test
    fun initialDeadlinesSaturateWithoutWrappingAndFiringEarly() {
        val intervals = listOf(1 to 60_000L, 5 to 300_000L, 10 to 600_000L)
        intervals.forEach { (minutes, interval) ->
            val starts = listOf(
                Long.MAX_VALUE - interval,
                Long.MAX_VALUE - interval + 1L,
                Long.MAX_VALUE - 1L,
                Long.MAX_VALUE,
            )
            starts.forEach { now ->
                val schedule = RefreshSchedule()

                assertEquals(
                    "minutes=$minutes, now=$now",
                    Long.MAX_VALUE,
                    schedule.update(minutes, active = true, column = "news", nowMillis = now),
                )
                assertFalse(schedule.due(Long.MAX_VALUE - 1L))
                assertTrue(schedule.due(Long.MAX_VALUE))
            }
        }
    }

    @Test
    fun consumedDeadlinesAlsoSaturateWithoutWrapping() {
        val intervals = listOf(1 to 60_000L, 5 to 300_000L, 10 to 600_000L)
        intervals.forEach { (minutes, interval) ->
            val schedule = RefreshSchedule()
            schedule.update(minutes, active = true, column = "news", nowMillis = 0L)
            val now = Long.MAX_VALUE - interval + 1L
            assertTrue(schedule.due(now))

            assertEquals(Long.MAX_VALUE, schedule.consumed(now))
            assertFalse(schedule.due(now))
            assertFalse(schedule.due(Long.MAX_VALUE - 1L))
            assertTrue(schedule.due(Long.MAX_VALUE))
            assertEquals(Long.MAX_VALUE, schedule.consumed(Long.MAX_VALUE))
        }
    }

    @Test
    fun missingVisibleColumnStopsUntilAColumnTokenIsAvailable() {
        val schedule = RefreshSchedule()
        assertNull(schedule.update(1, active = true, column = null, nowMillis = 0L))
        schedule.update(1, active = true, column = "news", nowMillis = 0L)

        assertNull(schedule.update(1, active = true, column = null, nowMillis = 50_000L))
        assertFalse(schedule.due(60_000L))
        assertNull(schedule.consumed(60_000L))
        assertEquals(
            360_000L,
            schedule.update(1, active = true, column = "news", nowMillis = 300_000L),
        )
        assertFalse(schedule.due(300_000L))
    }

    @Test
    fun lateConsumptionStartsOneFullIntervalWithoutCatchingUpMissedTicks() {
        val intervals = listOf(1 to 60_000L, 5 to 300_000L, 10 to 600_000L)
        intervals.forEach { (minutes, interval) ->
            val schedule = RefreshSchedule()
            schedule.update(minutes, active = true, column = "news", nowMillis = 1_000L)
            val lateNow = 1_000L + interval * 7L + 123L
            val next = lateNow + interval
            assertTrue(schedule.due(lateNow))

            assertEquals(next, schedule.consumed(lateNow))
            assertFalse(schedule.due(lateNow))
            assertFalse(schedule.due(next - 1L))
            assertEquals(
                next,
                schedule.update(minutes, active = true, column = "news", nowMillis = lateNow + 1L),
            )
            assertTrue(schedule.due(next))
            assertEquals(next + interval, schedule.consumed(next))
            assertFalse(schedule.due(next))
        }
    }

    @Test
    fun stopIsIdempotentAndReactivationCannotInheritADeadline() {
        val schedule = RefreshSchedule()
        schedule.stop()
        schedule.update(10, active = true, column = "news", nowMillis = 0L)

        schedule.stop()
        schedule.stop()

        assertFalse(schedule.due(Long.MAX_VALUE))
        assertNull(schedule.consumed(1_000_000L))
        assertEquals(
            1_600_000L,
            schedule.update(10, active = true, column = "news", nowMillis = 1_000_000L),
        )
        assertFalse(schedule.due(1_000_000L))
    }

    @Test
    fun changingMinutesStartsTheNewFullIntervalInEitherDirection() {
        val intervals = listOf(1 to 60_000L, 5 to 300_000L, 10 to 600_000L)
        intervals.forEach { (oldMinutes, _) ->
            intervals.filter { it.first != oldMinutes }.forEach { (minutes, interval) ->
                val schedule = RefreshSchedule()
                schedule.update(oldMinutes, active = true, column = "news", nowMillis = 0L)
                val expected = 20_000L + interval

                assertEquals(
                    "$oldMinutes -> $minutes",
                    expected,
                    schedule.update(minutes, active = true, column = "news", nowMillis = 20_000L),
                )
                assertEquals(
                    expected,
                    schedule.update(minutes, active = true, column = "news", nowMillis = 20_001L),
                )
                assertFalse(schedule.due(expected - 1L))
                assertTrue(schedule.due(expected))
            }
        }
    }

    @Test
    fun changingColumnStartsAFullIntervalEvenWhenReturningToAnOldColumn() {
        val schedule = RefreshSchedule()
        schedule.update(1, active = true, column = "news", nowMillis = 0L)

        assertEquals(90_000L, schedule.update(1, active = true, column = "tech", nowMillis = 30_000L))
        assertFalse(schedule.due(60_000L))
        assertFalse(schedule.due(89_999L))
        assertTrue(schedule.due(90_000L))
        assertEquals(150_000L, schedule.update(1, active = true, column = "news", nowMillis = 90_000L))
        assertFalse(schedule.due(90_000L))
    }

    @Test
    fun sameStateDoesNotPostponeEvenAnOverdueDeadline() {
        val schedule = RefreshSchedule()
        val token = String(charArrayOf('n', 'e', 'w', 's'))
        assertEquals(301_000L, schedule.update(5, active = true, column = token, nowMillis = 1_000L))

        listOf(1_000L, 10_000L, 300_000L, 301_000L, 900_000L).forEach { now ->
            assertEquals(
                301_000L,
                schedule.update(5, active = true, column = "news", nowMillis = now),
            )
        }
        assertTrue(schedule.due(900_000L))
    }

    @Test
    fun backgroundDropsDeadlineAndForegroundStartsAFullInterval() {
        val schedule = RefreshSchedule()
        schedule.update(1, active = true, column = "news", nowMillis = 0L)

        assertNull(schedule.update(1, active = false, column = "news", nowMillis = 50_000L))
        assertFalse(schedule.due(60_000L))
        assertNull(schedule.consumed(1_000_000L))
        assertNull(schedule.update(1, active = false, column = "news", nowMillis = 1_000_000L))
        assertFalse(schedule.due(1_000_000L))
        assertEquals(
            1_060_000L,
            schedule.update(1, active = true, column = "news", nowMillis = 1_000_000L),
        )
        assertFalse(schedule.due(1_059_999L))
        assertTrue(schedule.due(1_060_000L))
    }

    @Test
    fun zeroOrInvalidMinutesImmediatelyDisableExistingDeadline() {
        listOf(0, -1, 2, 3, 4, 6, 9, 11, 60, Int.MIN_VALUE, Int.MAX_VALUE).forEach { minutes ->
            val schedule = RefreshSchedule()
            assertNull(schedule.update(minutes, active = true, column = "news", nowMillis = 0L))
            schedule.update(1, active = true, column = "news", nowMillis = 0L)

            assertNull(schedule.update(minutes, active = true, column = "news", nowMillis = 61_000L))
            assertFalse(schedule.due(Long.MAX_VALUE))
            assertNull(schedule.consumed(61_000L))
            assertEquals(
                1_300_000L,
                schedule.update(5, active = true, column = "news", nowMillis = 1_000_000L),
            )
            assertFalse(schedule.due(1_000_000L))
        }
    }

    @Test
    fun dueBecomesTrueAtDeadlineWithoutConsumingIt() {
        val schedule = RefreshSchedule()
        assertFalse(schedule.due(0L))
        assertNull(schedule.consumed(0L))
        schedule.update(1, active = true, column = "news", nowMillis = 1_000L)

        assertFalse(schedule.due(1_000L))
        assertFalse(schedule.due(60_999L))
        assertTrue(schedule.due(61_000L))
        assertTrue(schedule.due(61_000L))
        assertTrue(schedule.due(90_000L))
    }

    @Test
    fun enabledOptionsStartOneFullIntervalFromNow() {
        val intervals = listOf(1 to 60_000L, 5 to 300_000L, 10 to 600_000L)

        intervals.forEach { (minutes, interval) ->
            val schedule = RefreshSchedule()

            assertEquals(
                "minutes=$minutes",
                1_000L + interval,
                schedule.update(minutes, active = true, column = "news", nowMillis = 1_000L),
            )
        }
    }
}
