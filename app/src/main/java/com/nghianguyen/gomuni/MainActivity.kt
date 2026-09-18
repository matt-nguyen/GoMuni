package com.nghianguyen.gomuni

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.nghianguyen.gomuni.tracking.BusTrackingService
import com.nghianguyen.gomuni.ui.TransitLinesKey
import com.nghianguyen.gomuni.ui.TransitStopsKey
import com.nghianguyen.gomuni.ui.lines.TransitLinesScreen
import com.nghianguyen.gomuni.ui.lines.TransitLinesViewModel
import com.nghianguyen.gomuni.ui.stops.StopsScreen
import com.nghianguyen.gomuni.ui.stops.StopsViewModel
import com.nghianguyen.gomuni.ui.theme.GoMuniTheme
import com.nghianguyen.gomuni.ui.theme.EditorialTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var transitLinesViewModel: TransitLinesViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GoMuniTheme {
                val backstack = rememberNavBackStack(TransitLinesKey)
                val context = LocalContext.current

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = EditorialTheme.colors.editorialBlack
                ) { innerPadding ->
                    NavDisplay(
                        backStack = backstack,
                        modifier = Modifier.padding(innerPadding),
                        entryProvider = entryProvider<NavKey> {
                            entry<TransitLinesKey> {
                                val viewModel: TransitLinesViewModel = hiltViewModel()
                                transitLinesViewModel = viewModel
                                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                                LaunchedEffect(Unit) {
                                    viewModel.errorEvents.collect { message ->
                                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                    }
                                }
                                
                                LaunchedEffect(intent) {
                                    handleIntent(intent)
                                }

                                TransitLinesScreen(
                                    uiState = uiState,
                                    onRefresh = { viewModel.refreshTransitLines(operatorId = "SF") },
                                    onLineSelected = { lineId ->
                                        viewModel.onLineSelected(lineId)
                                    },
                                    onSortOptionSelected = { viewModel.onSortOptionSelected(it) },
                                    onStopSelected = { viewModel.onStopSelected(it) },
                                    onRefreshArrivals = { viewModel.refreshBusArrivals(it) }
                                )
                            }
                            entry<TransitStopsKey> { key ->
                                val viewModel: StopsViewModel = hiltViewModel { factory: StopsViewModel.Factory ->
                                    factory.create(lineId = key.lineId, operatorId = key.operatorId)
                                }
                                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                                LaunchedEffect(key.lineId) {
                                    viewModel.errorEvents.collect { message ->
                                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                    }
                                }

                                StopsScreen(
                                    lineId = key.lineId,
                                    uiState = uiState,
                                    onRefresh = { viewModel.refreshStops() },
                                    onBack = {
                                        if (backstack.size > 1) {
                                            backstack.removeAt(backstack.size - 1)
                                        }
                                    }
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val stopId = intent?.getStringExtra(BusTrackingService.EXTRA_STOP_ID)
        val lineId = intent?.getStringExtra(BusTrackingService.EXTRA_LINE_REF)
        
        if (stopId != null && lineId != null) {
            transitLinesViewModel?.onStopSelectedById(lineId, stopId)
        }
    }
}
