package com.printxpress.app.data.local;

import org.junit.Test;
import java.util.Date;
import java.util.TimeZone;
import static org.junit.Assert.*;

public class ConvertersTest {

    @Test
    public void testNullTimestampAndDateHandling() {
        assertNull(Converters.fromTimestamp(null));
        assertNull(Converters.dateToTimestamp(null));
        assertEquals("", Converters.formatUtcIso(null));
        assertNull(Converters.parseUtcIso(null));
        assertNull(Converters.parseUtcIso(""));
        assertNull(Converters.parseUtcIso("invalid-date"));
    }

    @Test
    public void testRoundTripUtcEpochConversion() {
        long epochMs = 1790935200000L; // Fixed UTC epoch timestamp
        Date date = Converters.fromTimestamp(epochMs);
        assertNotNull(date);
        assertEquals(epochMs, date.getTime());

        Long convertedBack = Converters.dateToTimestamp(date);
        assertNotNull(convertedBack);
        assertEquals(Long.valueOf(epochMs), convertedBack);
    }

    @Test
    public void testUtcIsoFormattingAndParsingAcrossTimeZones() {
        TimeZone originalTz = TimeZone.getDefault();
        try {
            long epochMs = 1790935200123L;
            TimeZone.setDefault(TimeZone.getTimeZone("GMT+05:30"));
            Date dateColombo = Converters.fromTimestamp(epochMs);
            String isoColombo = Converters.formatUtcIso(dateColombo);

            TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
            Date dateNewYork = Converters.fromTimestamp(epochMs);
            String isoNewYork = Converters.formatUtcIso(dateNewYork);

            assertEquals("UTC ISO string must be identical regardless of system timezone", isoColombo, isoNewYork);
            assertTrue(isoColombo.endsWith("Z"));

            Date parsed = Converters.parseUtcIso(isoColombo);
            assertNotNull(parsed);
            assertEquals(epochMs, parsed.getTime());
        } finally {
            TimeZone.setDefault(originalTz);
        }
    }
}
