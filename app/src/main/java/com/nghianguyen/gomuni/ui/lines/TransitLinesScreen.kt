package com.nghianguyen.gomuni.ui.lines

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.ImageHolder
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.compose.annotation.rememberIconImage
import com.mapbox.maps.extension.compose.style.BooleanValue
import com.mapbox.maps.extension.compose.style.standard.LightPresetValue
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardStyle
import com.mapbox.maps.extension.compose.style.standard.rememberStandardStyleState
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.locationcomponent.location
import com.nghianguyen.gomuni.R
import com.nghianguyen.gomuni.domain.model.BusArrival
import com.nghianguyen.gomuni.domain.model.TransitStop
import com.nghianguyen.gomuni.tracking.BusTrackingService
import com.nghianguyen.gomuni.ui.components.EditorialFAB
import com.nghianguyen.gomuni.ui.theme.EditorialColors
import com.nghianguyen.gomuni.ui.theme.EditorialTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransitLinesScreen(
    uiState: TransitExplorerUiState,
    onRefresh: () -> Unit,
    onLineSelected: (String) -> Unit,
    onSortOptionSelected: (TransitLineSortOption) -> Unit,
    onStopSelected: (TransitStop?) -> Unit,
    onRefreshArrivals: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var locationPermissionGranted by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        locationPermissionGranted = permissions.values.any { it }
    }

    var showMenu by remember { mutableStateOf(false) }
    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(-122.4194, 37.7749))
            zoom(11.0)
        }
    }

    val stopIcon = rememberIconImage(key = "stop-marker", painter = painterResource(id = R.drawable.baseline_adjust_24))
    val busIcon = rememberIconImage(key = "bus-marker", painter = painterResource(id = R.drawable.bus_icon))

    val selectedLineId = uiState.routeStopsState?.route?.lineId
    val selectedStop = uiState.routeStopsState?.selectedStop
    val stops = uiState.routeStopsState?.stops.orEmpty()
    val predictions = uiState.arrivalsState?.predictions.orEmpty()
    val isRouteSelected = !selectedLineId.isNullOrEmpty()
    val isStopSelected = uiState.arrivalSheetVisible

    val selectedLine = remember(uiState.routesState.lines, selectedLineId) {
        if (isRouteSelected) uiState.routesState.lines.find { it.id == selectedLineId } else null
    }
    
    val editorialColors = EditorialTheme.colors
    val activeRouteColor = remember(selectedLine) {
        selectedLine?.let { getRouteColor(it, editorialColors) } ?: editorialColors.muniRed
    }

    BackHandler(enabled = isStopSelected || isRouteSelected) {
        if (isStopSelected) {
            onStopSelected(null)
        } else {
            onLineSelected("")
        }
    }

    LaunchedEffect(Unit) {
        onRefresh()
        if (!locationPermissionGranted) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(stops, selectedStop) {
        if (stops.isNotEmpty() && !isStopSelected) {
            val points = stops.map { Point.fromLngLat(it.longitude, it.latitude) }
            val cameraOptions = mapViewportState.cameraForCoordinates(
                coordinates = points,
                coordinatesPadding = EdgeInsets(100.0, 100.0, 100.0, 100.0)
            )
            mapViewportState.flyTo(
                cameraOptions = cameraOptions,
                animationOptions = MapAnimationOptions.mapAnimationOptions { duration(1000) }
            )
        }
    }

    LaunchedEffect(selectedStop, predictions) {
        selectedStop?.let { stop ->
            val points = mutableListOf(Point.fromLngLat(stop.longitude, stop.latitude))
            val busPoints = predictions
                .filter { it.latitude != 0.0 && it.longitude != 0.0 && it.lineRef == selectedLineId }
                .map { Point.fromLngLat(it.longitude, it.latitude) }
            points.addAll(busPoints)
            
            val cameraOptions = mapViewportState.cameraForCoordinates(
                coordinates = points,
                coordinatesPadding = EdgeInsets(200.0, 100.0, 400.0, 100.0)
            )
            mapViewportState.flyTo(
                cameraOptions = cameraOptions,
                animationOptions = MapAnimationOptions.mapAnimationOptions { duration(1000) }
            )
        }
    }

    val sheetState = rememberModalBottomSheetState()


    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Handle granted/denied if needed
    }

    fun startTracking(arrival: BusArrival, stop: TransitStop) {
        val intent = Intent(context, BusTrackingService::class.java).apply {
            action = BusTrackingService.ACTION_START
            putExtra(BusTrackingService.EXTRA_STOP_ID, stop.id)
            putExtra(BusTrackingService.EXTRA_STOP_NAME, stop.name)
            putExtra(BusTrackingService.EXTRA_LINE_REF, arrival.lineRef)
            putExtra(BusTrackingService.EXTRA_LINE_NAME, arrival.lineName)
            putExtra(BusTrackingService.EXTRA_VEHICLE_REF, arrival.vehicleRef)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                context.startForegroundService(intent)
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            context.startForegroundService(intent)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (selectedLine != null) "${selectedLine.id} ${selectedLine.name.toTitleCase()}" else "Muni",
                        style = EditorialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    if (isRouteSelected || isStopSelected) {
                        IconButton(onClick = { 
                            if (isStopSelected) {
                                onStopSelected(null)
                            } else {
                                onLineSelected("")
                            }
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = editorialColors.editorialPaper
                            )
                        }
                    }
                },
                actions = {
                    if (!isRouteSelected) {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Sort Options",
                                tint = editorialColors.editorialPaper
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(editorialColors.surfaceCard)
                        ) {
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        "Sort by ID",
                                        style = EditorialTheme.typography.labelLarge
                                    ) 
                                },
                                onClick = {
                                    onSortOptionSelected(TransitLineSortOption.ID)
                                    showMenu = false
                                },
                                trailingIcon = {
                                    if (uiState.routesState.sortOption == TransitLineSortOption.ID) {
                                        RadioButton(
                                            selected = true,
                                            onClick = null,
                                            colors = RadioButtonDefaults.colors(selectedColor = editorialColors.signalChartreuse)
                                        )
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        "Sort by Name",
                                        style = EditorialTheme.typography.labelLarge
                                    ) 
                                },
                                onClick = {
                                    onSortOptionSelected(TransitLineSortOption.NAME)
                                    showMenu = false
                                },
                                trailingIcon = {
                                    if (uiState.routesState.sortOption == TransitLineSortOption.NAME) {
                                        RadioButton(
                                            selected = true,
                                            onClick = null,
                                            colors = RadioButtonDefaults.colors(selectedColor = editorialColors.signalChartreuse)
                                        )
                                    }
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isRouteSelected) activeRouteColor else editorialColors.surfaceContainerLowest.copy(alpha = 0.9f),
                    titleContentColor = editorialColors.editorialPaper
                )
            )
        },
        containerColor = editorialColors.editorialBlack,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .weight(0.4f)) {
                MapboxMap(
                    modifier = Modifier.fillMaxSize(),
                    mapViewportState = mapViewportState,
                    style = {
                        MapboxStandardStyle(
                            standardStyleState = rememberStandardStyleState {
                                configurationsState.apply {
                                    lightPreset = LightPresetValue.DAY
                                    showPointOfInterestLabels = BooleanValue(false)
                                }
                            }
                        )
                    }
                ) {
                    MapEffect(locationPermissionGranted) { mapView ->
                        mapView.location.updateSettings {
                            enabled = locationPermissionGranted
                            pulsingEnabled = true
                            locationPuck = LocationPuck2D(
                                topImage = ImageHolder.from(R.drawable.current_location_icon)
                            )
                        }
                    }


                    for (stop in stops) {
                        key(stop.id) {
                            val isSelected = selectedStop?.id == stop.id
                            PointAnnotation(
                                point = Point.fromLngLat(stop.longitude, stop.latitude),
                            ) {
                                interactionsState.onClicked {
                                    onStopSelected(stop)
                                    true
                                }
                                iconImage = stopIcon
                                iconColor = if (isSelected) editorialColors.signalChartreuse else activeRouteColor
                                iconOpacity = if (isStopSelected && !isSelected) 0.3 else 1.0
                            }
                        }
                    }

                    val activeBuses = predictions.filter { it.latitude != 0.0 && it.longitude != 0.0 && it.lineRef == selectedLineId }
                    for (bus in activeBuses) {
                        key(bus.vehicleRef ?: bus.hashCode()) {
                            PointAnnotation(
                                point = Point.fromLngLat(bus.longitude, bus.latitude),
                            ) {
                                iconImage = busIcon
                                iconColor = editorialColors.signalChartreuse
                            }
                        }
                    }
                }

                if (locationPermissionGranted) {
                    EditorialFAB(
                        onClick = {
                            mapViewportState.transitionToFollowPuckState()
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(EditorialTheme.spacing.spaceMd)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.current_location_icon),
                            contentDescription = "My Location",
                            tint = editorialColors.editorialBlack
                        )
                    }
                }
            }
            Box(modifier = Modifier
                .fillMaxWidth()
                .weight(0.6f)) {
                if (uiState.routesState.isRefreshing && uiState.routesState.lines.isEmpty()) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = editorialColors.signalChartreuse
                    )
                } else if (uiState.routesState.error != null && uiState.routesState.lines.isEmpty()) {
                    Text(
                        text = "ERROR: ${uiState.routesState.error?.uppercase()}",
                        style = EditorialTheme.typography.bodyLarge,
                        color = EditorialTheme.colors.electricAlert,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(EditorialTheme.spacing.spaceMd)
                    )
                } else {
                    Crossfade(targetState = isRouteSelected, label = "ListTransition") { routeActive ->
                        if (!routeActive) {
                            TransitLineList(
                                lines = uiState.routesState.lines,
                                selectedLineId = selectedLineId,
                                onLineClick = { id -> onLineSelected(id) }
                            )
                        } else {
                            if (uiState.routeStopsState?.isRefreshing == true && stops.isEmpty()) {
                                CircularProgressIndicator(color = editorialColors.signalChartreuse)
                            } else if (stops.isEmpty()) {
                                Column(modifier = Modifier.padding(EditorialTheme.spacing.spaceMd)) {
                                    Text(
                                        text = uiState.routeStopsState?.error ?: "No stops available for this route.",
                                        color = editorialColors.editorialPaper,
                                    )
                                    TextButton(onClick = { selectedLineId?.let(onLineSelected) }) { Text("RETRY") }
                                }
                            } else TransitStopList(
                                stops = stops,
                                selectedStopId = selectedStop?.id,
                                onStopClick = { stop -> onStopSelected(stop) }
                            )
                        }
                    }
                }
            }
        }

        if (uiState.arrivalSheetVisible && selectedStop != null) {

            if (selectedStop != null) {
                val (selectedRouteArrivals, otherRouteArrivals) = remember(predictions, selectedLineId) {
                    predictions.partition { it.lineRef == selectedLineId }
                }
                ModalBottomSheet(
                    onDismissRequest = { 

                        onStopSelected(null)
                    },
                    sheetState = sheetState,
                    shape = RectangleShape,
                    containerColor = editorialColors.surfaceCard,
                    dragHandle = { BottomSheetDefaults.DragHandle(color = editorialColors.editorialPaper) }
                ) {
                    uiState.arrivalsState?.error?.let { error ->
                        Text(
                            text = error,
                            color = editorialColors.electricAlert,
                            modifier = Modifier.padding(EditorialTheme.spacing.spaceMd),
                        )
                    }
                    StopArrivalsContent(
                        stop = selectedStop,
                        selectedRouteArrivals = selectedRouteArrivals,
                        otherRouteArrivals = otherRouteArrivals,
                        isLoading = uiState.arrivalsState?.isRefreshing == true,
                        lines = uiState.routesState.lines,
                        arrivalsLastUpdatedMillis = uiState.arrivalsState?.fetchedAtMillis,
                        onRefresh = { onRefreshArrivals(selectedStop.id) },
                        onTrackArrival = { arrival -> startTracking(arrival, selectedStop) }
                    )
                }
            }
        }
    }
}
