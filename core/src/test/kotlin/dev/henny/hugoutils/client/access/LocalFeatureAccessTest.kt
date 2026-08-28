package dev.henny.hugoutils.client.access

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LocalFeatureAccessTest {
    @Test
    fun featureAccessManagerClassIsRemoved() {
        assertThrows<ClassNotFoundException> {
            Class.forName("dev.henny.hugoutils.client.access.FeatureAccessManager")
        }
    }

    @Test
    fun accessFeatureEnumIsRemoved() {
        assertThrows<ClassNotFoundException> {
            Class.forName("dev.henny.hugoutils.client.access.AccessFeature")
        }
    }

    @Test
    fun localFeaturesWorkWithoutLoginOrApi() {
        assertTrue(localFeatureActive(configEnabled = true, loggedIn = false, apiAvailable = false))
        assertTrue(localFeatureActive(configEnabled = true, loggedIn = false, apiAvailable = true))
        assertTrue(localFeatureActive(configEnabled = true, loggedIn = true, apiAvailable = false))
    }

    @Test
    fun apiFailureDoesNotDisableLocalFeatures() {
        assertTrue(localFeatureActive(configEnabled = true, loggedIn = false, apiAvailable = false, staleEntitlement = true))
    }

    @Test
    fun oldFeatureAccessStateCannotDisableLocalFeatures() {
        assertTrue(localFeatureActive(configEnabled = true, entitled = false, expired = true))
        assertFalse(localFeatureActive(configEnabled = false, entitled = true, expired = false))
    }

    private fun localFeatureActive(
        configEnabled: Boolean,
        @Suppress("UNUSED_PARAMETER") loggedIn: Boolean = false,
        @Suppress("UNUSED_PARAMETER") apiAvailable: Boolean = false,
        @Suppress("UNUSED_PARAMETER") staleEntitlement: Boolean = false,
        @Suppress("UNUSED_PARAMETER") entitled: Boolean = false,
        @Suppress("UNUSED_PARAMETER") expired: Boolean = false
    ): Boolean = configEnabled
}
