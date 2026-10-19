package com.example.arts

import com.example.arts.utils.TimeFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Tests reading the website's ISO timestamps and showing them as "5 min ago". */
class TimeFormatTest {

    private val stamp = "2026-10-09T05:26:43.578Z"
    private val base = TimeFormat.parse(stamp)!!

    @Test
    fun parsesTimestampsWithAndWithoutMilliseconds() {
        assertNotNull(TimeFormat.parse(stamp))
        assertNotNull(TimeFormat.parse("2026-06-22T10:10:00Z"))
    }

    @Test
    fun badTimestampsGiveNull() {
        assertNull(TimeFormat.parse("garbage"))
        assertNull(TimeFormat.parse(""))
        assertNull(TimeFormat.parse(null))
    }

    @Test
    fun underAMinuteIsJustNow() = assertEquals("Just now", TimeFormat.ago(stamp, base + 30_000))

    @Test
    fun showsMinutes() = assertEquals("12 min ago", TimeFormat.ago(stamp, base + 12 * 60_000))

    @Test
    fun showsHours() = assertEquals("3 hr ago", TimeFormat.ago(stamp, base + 3 * 3_600_000))

    @Test
    fun showsDays() {
        assertEquals("1 day ago", TimeFormat.ago(stamp, base + 86_400_000))
        assertEquals("4 days ago", TimeFormat.ago(stamp, base + 4 * 86_400_000))
    }

    @Test
    fun aBlankTimestampGivesBlankText() = assertEquals("", TimeFormat.ago(""))

    @Test
    fun nowIsoCanBeReadBackAgain() = assertNotNull(TimeFormat.parse(TimeFormat.nowIso()))
}
