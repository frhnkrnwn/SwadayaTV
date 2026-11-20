package com.swadaya.tv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LiveChannelsTest {
    @Test
    public void listsIndonesianHttpsStreams() {
        assertTrue(LiveChannels.ALL.size() >= 30);
        Set<String> slugs = new HashSet<>();
        for (LiveChannels.Channel c : LiveChannels.ALL) {
            assertTrue(c.slug, c.slug.matches("[a-z0-9-]+"));
            assertTrue(c.name, !c.name.isEmpty());
            assertTrue(c.url, c.url.startsWith("https://"));
            assertTrue(c.url, c.url.contains("tvri.go.id") || c.url.contains("medcom.id"));
            assertTrue(c.logoUrl, c.logoUrl.startsWith("https://"));
            assertTrue("duplicate slug " + c.slug, slugs.add(c.slug));
        }
        assertEquals("TVRI Nasional", LiveChannels.ALL.get(0).name);
        assertEquals("Metro TV", LiveChannels.ALL.get(1).name);
    }

    @Test
    public void recognisesOnlyItsOwnPages() {
        LiveChannels.Channel first = LiveChannels.ALL.get(0);
        assertEquals(first, LiveChannels.byPage(first.page()));
        assertTrue(LiveChannels.isChannelPage(first.page()));
        assertNull(LiveChannels.byPage("https://rtmklik.rtm.gov.my/live/tv/tv1"));
        assertFalse(LiveChannels.isChannelPage("https://example.com/live.m3u8"));
    }

    @Test
    public void nowAndUpcoming() {
        List<LiveChannels.Programme> list = java.util.Arrays.asList(
                new LiveChannels.Programme(1_000, 2_000, "Satu", "Pertama"),
                new LiveChannels.Programme(2_000, 3_000, "Dua", "Kedua"));
        LiveChannels.Programme now = LiveChannels.now(list, 1_500);
        assertEquals("Satu", now.title);
        assertEquals(0.5f, now.progress(1_500), 0.001f);
        assertEquals("Dua", LiveChannels.upcoming(list, 1_500, 2).get(0).title);
        assertNull(LiveChannels.now(list, 4_000));
        assertNull(LiveChannels.now(null, 1_500));
    }

    @Test
    public void thereIsNoFreeGuideSoTheMapIsEmpty() {
        assertTrue(LiveChannels.load().isEmpty());
    }
}
