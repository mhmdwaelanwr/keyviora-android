package com.mhmdwaelanwr.keyviora

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TouchActionCatalogTest {
    @Test fun actionIdsAreUniqueAndHaveReadableLabels() {
        val actions = TouchActionCatalog.actions
        assertEquals(actions.size, actions.map { it.key }.distinct().size)
        assertTrue(actions.all { it.key.isNotBlank() && it.label.isNotBlank() })
        assertTrue(actions.all { it.shortLabel.isNotBlank() && it.glyph.isNotBlank() })
    }

    @Test fun invalidSavedActionsFallBackToSafeDefaults() {
        assertEquals("MENU", TouchActionCatalog.resolve("INVALID", "MENU").key)
        assertEquals("MENU", TouchActionCatalog.resolve(null, "MISSING").key)
        assertEquals("NONE", TouchActionCatalog.resolve("NONE", "LOCK").key)
    }

    @Test fun sixQuickControlDefaultsAreValid() {
        assertEquals(6, TouchPrefs.quickKeys.size)
        assertEquals(6, TouchPrefs.quickDefaults.size)
        assertEquals(TouchPrefs.quickKeys.size, TouchPrefs.quickKeys.distinct().size)
        assertTrue(TouchPrefs.quickDefaults.all {
            TouchActionCatalog.resolve(it, "MENU").key == it
        })
    }

    @Test fun invalidQuickControlActionFallsBackToSlotDefault() {
        TouchPrefs.quickDefaults.forEach { fallback ->
            assertEquals(fallback, TouchActionCatalog.resolve("REMOVED_ACTION", fallback).key)
        }
    }

    @Test fun pinnedDefaultActionsRemainAvailable() {
        assertEquals(3, TouchPrefs.favoriteKeys.size)
        assertEquals(TouchPrefs.favoriteKeys.size, TouchPrefs.favoriteDefaults.size)
        assertTrue(TouchPrefs.favoriteDefaults.all {
            TouchActionCatalog.resolve(it, "MENU").key == it
        })
        assertFalse(TouchActionCatalog.actions.isEmpty())
    }
}
