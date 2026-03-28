package com.foodrecommend.letmecook.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SceneModeResolverTest {

    @Test
    void shouldNormalizeSupportedModes() {
        assertEquals("family", SceneModeResolver.normalizeMode(" Family "));
        assertEquals("fitness", SceneModeResolver.normalizeMode("FITNESS"));
        assertEquals("quick", SceneModeResolver.normalizeMode("quick"));
        assertEquals("party", SceneModeResolver.normalizeMode("party"));
    }

    @Test
    void shouldRejectUnsupportedModes() {
        assertNull(SceneModeResolver.normalizeMode(null));
        assertNull(SceneModeResolver.normalizeMode(" "));
        assertNull(SceneModeResolver.normalizeMode("solo"));
    }

    @Test
    void shouldResolveSceneCodesFromModes() {
        assertEquals("family", SceneModeResolver.resolveSceneCode("family"));
        assertEquals("diet", SceneModeResolver.resolveSceneCode("fitness"));
        assertEquals("quick", SceneModeResolver.resolveSceneCode(" quick "));
        assertEquals("banquet", SceneModeResolver.resolveSceneCode("PARTY"));
        assertNull(SceneModeResolver.resolveSceneCode("solo"));
    }
}
