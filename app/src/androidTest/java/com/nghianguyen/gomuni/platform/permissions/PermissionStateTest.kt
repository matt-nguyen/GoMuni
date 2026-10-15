package com.nghianguyen.gomuni.platform.permissions

import android.Manifest
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class PermissionStateTest {
    @get:Rule val compose = createComposeRule()

    @Test fun repeatedDenialsCompleteEvenWhenStatusDoesNotChange() {
        lateinit var permission: PermissionState
        val results = mutableListOf<PermissionStatus>()
        compose.setContent {
            // An undeclared permission is denied without opening a system dialog.
            permission = rememberPermissionState("com.nghianguyen.gomuni.TEST_PERMISSION", results::add)
        }
        compose.runOnIdle { permission.launchPermissionRequest() }
        compose.waitUntil { results.size == 1 }
        compose.runOnIdle { permission.launchPermissionRequest() }
        compose.waitUntil { results.size == 2 }
        compose.runOnIdle {
            assertEquals(listOf(PermissionStatus.Denied(false), PermissionStatus.Denied(false)), results)
            assertFalse(permission.isRequestInProgress)
        }
    }

    @Test fun alreadyGrantedRequestsCompleteEveryTimeAndUseLatestCallback() {
        lateinit var permission: PermissionState
        val generation = mutableStateOf(0)
        val results = mutableListOf<Pair<Int, PermissionStatus>>()
        compose.setContent {
            val current = generation.value
            permission = rememberPermissionState(Manifest.permission.INTERNET) {
                results.add(current to it)
            }
        }
        compose.runOnIdle {
            assertEquals(PermissionStatus.Granted, permission.status)
            permission.launchPermissionRequest()
            permission.launchPermissionRequest()
            assertFalse(permission.isRequestInProgress)
            generation.value = 1
        }
        compose.runOnIdle {
            permission.launchPermissionRequest()
            assertEquals(listOf(0, 0, 1), results.map { it.first })
            assertTrue(results.all { it.second == PermissionStatus.Granted })
        }
    }
}
