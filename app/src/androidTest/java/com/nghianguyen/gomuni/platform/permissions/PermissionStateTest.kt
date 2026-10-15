package com.nghianguyen.gomuni.platform.permissions

import android.Manifest
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class PermissionStateTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun permissionRequest_whenRepeatedlyDenied_completesEachRequest() {
        lateinit var permission: PermissionState
        val results = mutableListOf<PermissionStatus>()
        composeRule.setContent {
            // An undeclared permission is denied without opening a system dialog.
            permission = rememberPermissionState("com.nghianguyen.gomuni.TEST_PERMISSION", results::add)
        }
        composeRule.runOnIdle { permission.launchPermissionRequest() }
        composeRule.waitUntil { results.size == 1 }
        composeRule.runOnIdle { permission.launchPermissionRequest() }
        composeRule.waitUntil { results.size == 2 }
        composeRule.runOnIdle {
            assertEquals(listOf(PermissionStatus.Denied(false), PermissionStatus.Denied(false)), results)
            assertFalse(permission.isRequestInProgress)
        }
    }

    @Test
    fun permissionRequest_whenAlreadyGranted_completesEachRequestWithLatestCallback() {
        lateinit var permission: PermissionState
        val generation = mutableStateOf(0)
        val results = mutableListOf<Pair<Int, PermissionStatus>>()
        composeRule.setContent {
            val current = generation.value
            permission = rememberPermissionState(Manifest.permission.INTERNET) {
                results.add(current to it)
            }
        }
        composeRule.runOnIdle {
            assertEquals(PermissionStatus.Granted, permission.status)
            permission.launchPermissionRequest()
            permission.launchPermissionRequest()
            assertFalse(permission.isRequestInProgress)
            generation.value = 1
        }
        composeRule.runOnIdle {
            permission.launchPermissionRequest()
            assertEquals(listOf(0, 0, 1), results.map { it.first })
            assertTrue(results.all { it.second == PermissionStatus.Granted })
        }
    }
}
