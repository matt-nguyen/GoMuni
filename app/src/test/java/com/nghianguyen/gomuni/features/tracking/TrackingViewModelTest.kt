package com.nghianguyen.gomuni.features.tracking

import androidx.lifecycle.ViewModelStore
import com.nghianguyen.gomuni.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.After
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TrackingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val viewModelStore = ViewModelStore()
    private val request = TrackingRequest("123", "Market", "38", "Geary", "bus1")

    @After
    fun clearViewModels() {
        viewModelStore.clear()
    }

    private fun createTrackingViewModel(): TrackingViewModel = TrackingViewModel().also {
        viewModelStore.put("tracking:${System.identityHashCode(it)}", it)
    }

    @Test
    fun `startup emits permission and launch once and allows same target again`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createTrackingViewModel()
        val effects = mutableListOf<TrackingEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effects.collect { effects.add(it) } }
        viewModel.onAction(TrackingAction.Start(request))
        viewModel.onAction(TrackingAction.Start(request.copy(stopId = "other")))
        assertEquals(listOf(TrackingEffect.RequestNotificationPermission(request)), effects)
        assertTrue(viewModel.uiState.value.isStarting)
        viewModel.onAction(TrackingAction.PermissionResolved(true))
        assertEquals(TrackingEffect.LaunchTrackingService(request), effects.last())
        viewModel.onAction(TrackingAction.LaunchResolved(true))
        assertEquals(TrackingUiState(), viewModel.uiState.value)
        viewModel.onAction(TrackingAction.Start(request))
        assertEquals(
            listOf(
                TrackingEffect.RequestNotificationPermission(request),
                TrackingEffect.LaunchTrackingService(request),
                TrackingEffect.RequestNotificationPermission(request),
            ),
            effects,
        )
    }

    @Test
    fun `no collector does not leave startup busy or replay effect`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createTrackingViewModel()
        viewModel.onAction(TrackingAction.Start(request))
        assertFalse(viewModel.uiState.value.isStarting)
        assertEquals(TrackingFailure.STARTUP_FAILED, viewModel.uiState.value.error?.reason)
        val effects = mutableListOf<TrackingEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effects.collect { effects.add(it) } }
        assertTrue(effects.isEmpty())
    }

    @Test
    fun `denied permission retains rationale and can be dismissed`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createTrackingViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effects.collect {} }
        viewModel.onAction(TrackingAction.Start(request))
        viewModel.onAction(TrackingAction.PermissionResolved(false, true))
        assertEquals(TrackingUiState(error = TrackingErrorUiState(TrackingFailure.PERMISSION_DENIED, true)), viewModel.uiState.value)
        viewModel.onAction(TrackingAction.DismissFailure)
        assertEquals(TrackingUiState(), viewModel.uiState.value)
    }

    @Test
    fun `collector restart does not replay and interrupted startup can retry`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createTrackingViewModel()
        val effects = mutableListOf<TrackingEffect>()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effects.collect { effects.add(it) } }
        viewModel.onAction(TrackingAction.Start(request))
        collector.cancelAndJoin()
        viewModel.onAction(TrackingAction.StartupInterrupted)
        assertFalse(viewModel.uiState.value.isStarting)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effects.collect { effects.add(it) } }
        assertEquals(listOf(TrackingEffect.RequestNotificationPermission(request)), effects)
        viewModel.onAction(TrackingAction.Start(request))
        assertEquals(List(2) { TrackingEffect.RequestNotificationPermission(request) }, effects)
    }

    @Test
    fun `new view model does not restore an unfinished startup`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createTrackingViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effects.collect {} }
        viewModel.onAction(TrackingAction.Start(request))
        assertTrue(viewModel.uiState.value.isStarting)

        val recreatedViewModel = createTrackingViewModel()

        assertEquals(TrackingUiState(), recreatedViewModel.uiState.value)
    }

    @Test
    fun `permission completion without collector fails instead of leaving busy`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createTrackingViewModel()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effects.collect {} }
        viewModel.onAction(TrackingAction.Start(request))
        collector.cancelAndJoin()
        viewModel.onAction(TrackingAction.PermissionResolved(true))
        assertFalse(viewModel.uiState.value.isStarting)
        assertEquals(TrackingFailure.STARTUP_FAILED, viewModel.uiState.value.error?.reason)
    }

    @Test
    fun `launch failure allows retry and inactive phase results are ignored`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createTrackingViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effects.collect {} }
        viewModel.onAction(TrackingAction.Start(request))
        viewModel.onAction(TrackingAction.LaunchResolved(true))
        assertTrue(viewModel.uiState.value.isStarting)
        viewModel.onAction(TrackingAction.StartupFailed)
        assertEquals(TrackingFailure.STARTUP_FAILED, viewModel.uiState.value.error?.reason)
        viewModel.onAction(TrackingAction.Start(request))
        viewModel.onAction(TrackingAction.PermissionResolved(true))
        viewModel.onAction(TrackingAction.LaunchResolved(false))
        assertEquals(TrackingFailure.STARTUP_FAILED, viewModel.uiState.value.error?.reason)
        assertFalse(viewModel.uiState.value.isStarting)
    }
}
