package com.swadaya.tv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.junit.Test;

public class UpdateCheckTest {
    @Test
    public void versionsCompareNumberByNumber() {
        assertTrue(UpdateCheck.compare("1.10.0", "1.9.1") > 0);
        assertTrue(UpdateCheck.compare("1.10.1", "1.10") > 0);
        assertTrue(UpdateCheck.compare("1.9", "1.10.1") < 0);
        assertEquals(0, UpdateCheck.compare("1.10", "1.10.0"));
        assertEquals(0, UpdateCheck.compare("2.0-beta", "2.0"));
    }

    @Test
    public void releaseTagsShowTheVersionTheAppCompares() {
        assertEquals("1.1.0", UpdateCheck.versionFromTag("v1.1.0-build24"));
        assertEquals("1.1.0", UpdateCheck.versionFromTag("1.1.0"));
        assertEquals("2.0-beta", UpdateCheck.versionFromTag("v2.0-beta"));
    }

    @Test
    public void releaseTagFindsTheNewestBuildOfTheInstalledVersion() throws Exception {
        JSONArray list = new JSONArray("[" +
                "{\"tag_name\":\"v1.1.4-build32\"}," +
                "{\"tag_name\":\"v1.1.3-build31\"}," +
                "{\"tag_name\":\"v1.1.3-build30\"}," +
                "{\"tag_name\":\"v1.1.2-build28\"}]");
        assertEquals("v1.1.3-build31", UpdateCheck.tagFor(list, "1.1.3"));
        assertEquals("v1.1.2-build28", UpdateCheck.tagFor(list, "1.1.2"));
        assertNull(UpdateCheck.tagFor(list, "1.0"));
        assertNull(UpdateCheck.tagFor(new JSONArray(
                "[{\"tag_name\":\"v1.1.3-build31\",\"draft\":true}]"), "1.1.3"));
        assertNull(UpdateCheck.tagFor(new JSONArray(
                "[{\"tag_name\":\"v1.1.3-build31\",\"prerelease\":true}]"), "1.1.3"));
    }
}
