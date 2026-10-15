package com.nghianguyen.gomuni

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/** Verifies the instrumentation target uses the application package. */
@RunWith(AndroidJUnit4::class)
class ApplicationContextTest {
    @Test
    fun appContext_hasApplicationPackageName() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals(BuildConfig.APPLICATION_ID, appContext.packageName)
    }
}
