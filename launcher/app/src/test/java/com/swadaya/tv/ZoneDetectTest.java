package com.swadaya.tv;

import org.junit.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ZoneDetectTest {
    @Test public void townsLandInTheirCity() {
        assertEquals("Jakarta", ZoneDetect.nearest(-6.17638, 106.82671));   // Gambir
        assertEquals("Bandung", ZoneDetect.nearest(-6.9175, 107.6191));     // Bandung
        assertEquals("Bandung", ZoneDetect.nearest(-6.8121, 107.6174));     // Lembang, near Bandung
        assertEquals("Surabaya", ZoneDetect.nearest(-7.2575, 112.7521));
        assertEquals("Medan", ZoneDetect.nearest(3.5952, 98.6722));
        assertEquals("Makassar", ZoneDetect.nearest(-5.1477, 119.4327));
        assertEquals("Jayapura", ZoneDetect.nearest(-2.5337, 140.7181));
        assertEquals("Denpasar", ZoneDetect.nearest(-8.6705, 115.2126));
        assertEquals("Banda Aceh", ZoneDetect.nearest(5.5483, 95.3238));
        assertEquals("Balikpapan", ZoneDetect.nearest(-1.2379, 116.8529));
    }

    @Test public void notNearIndonesiaIsNoGuess() {
        assertNull(ZoneDetect.nearest(51.5, -0.12));     // London
        assertNull(ZoneDetect.nearest(13.75, 100.50));   // Bangkok
        assertNull(ZoneDetect.nearest(3.139, 101.687));  // Kuala Lumpur
        assertNull(ZoneDetect.nearest(-33.87, 151.21));  // Sydney
    }

    /** Every city in places.json is in the detector's table, and resolves to itself. */
    @Test public void everyPlaceResolvesToItself() throws Exception {
        List<Weather.Place> places;
        try (InputStream in = ZoneDetectTest.class.getClassLoader().getResourceAsStream("weather-places.json")) {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] b = new byte[8192];
            for (int n; (n = in.read(b)) > 0; ) out.write(b, 0, n);
            places = Weather.parsePlaces(new String(out.toByteArray(), StandardCharsets.UTF_8));
        }
        assertTrue(places.size() >= 50);
        for (Weather.Place p : places) {
            assertEquals(p.name, ZoneDetect.nearest(p.lat, p.lon));
        }
    }
}
