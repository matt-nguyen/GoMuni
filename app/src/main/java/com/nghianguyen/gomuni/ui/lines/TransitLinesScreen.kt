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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import com.nghianguyen.gomuni.domain.model.TransitLine
import com.nghianguyen.gomuni.domain.model.TransitStop
import com.nghianguyen.gomuni.tracking.BusTrackingService
import com.nghianguyen.gomuni.ui.components.EditorialCard
import com.nghianguyen.gomuni.ui.components.EditorialDivider
import com.nghianguyen.gomuni.ui.components.EditorialFAB
import com.nghianguyen.gomuni.ui.components.RouteBadge
import com.nghianguyen.gomuni.ui.theme.EditorialColors
import com.nghianguyen.gomuni.ui.theme.EditorialTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransitLinesScreen(
    uiState: TransitLinesUiState,
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

    val isRouteSelected = !uiState.selectedLineId.isNullOrEmpty()
    val isStopSelected = uiState.selectedStop != null

    val selectedLine = remember(uiState.lines, uiState.selectedLineId) {
        if (isRouteSelected) uiState.lines.find { it.id == uiState.selectedLineId } else null
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

    LaunchedEffect(uiState.stops, uiState.selectedStop) {
        if (uiState.stops.isNotEmpty() && !isStopSelected) {
            val points = uiState.stops.map { Point.fromLngLat(it.longitude, it.latitude) }
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

    LaunchedEffect(uiState.selectedStop, uiState.busArrivals) {
        uiState.selectedStop?.let { stop ->
            val points = mutableListOf(Point.fromLngLat(stop.longitude, stop.latitude))
            val busPoints = uiState.busArrivals
                .filter { it.latitude != 0.0 && it.longitude != 0.0 && it.lineRef == uiState.selectedLineId }
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
    var showBottomSheet by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.selectedStop) {
        showBottomSheet = uiState.selectedStop != null
    }

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
                                    if (uiState.sortOption == TransitLineSortOption.ID) {
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
                                    if (uiState.sortOption == TransitLineSortOption.NAME) {
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


                    for (stop in uiState.stops) {
                        key(stop.id) {
                            val isSelected = uiState.selectedStop?.id == stop.id
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

                    val activeBuses = uiState.busArrivals.filter { it.latitude != 0.0 && it.longitude != 0.0 && it.lineRef == uiState.selectedLineId }
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
                if (uiState.isLoading && uiState.lines.isEmpty()) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = editorialColors.signalChartreuse
                    )
                } else if (uiState.errorMessage != null && uiState.lines.isEmpty()) {
                    Text(
                        text = "ERROR: ${uiState.errorMessage.uppercase()}",
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
                                lines = uiState.lines,
                                selectedLineId = uiState.selectedLineId,
                                onLineClick = { id -> onLineSelected(id) }
                            )
                        } else {
                            TransitStopList(
                                stops = uiState.stops,
                                selectedStopId = uiState.selectedStop?.id,
                                onStopClick = { stop -> onStopSelected(stop) }
                            )
                        }
                    }
                }
            }
        }

        if (showBottomSheet && uiState.selectedStop != null) {
            val selectedStop = uiState.selectedStop
            if (selectedStop != null) {
                val (selectedRouteArrivals, otherRouteArrivals) = remember(uiState.busArrivals, uiState.selectedLineId) {
                    uiState.busArrivals.partition { it.lineRef == uiState.selectedLineId }
                }
                ModalBottomSheet(
                    onDismissRequest = { 
                        showBottomSheet = false
                        onStopSelected(null)
                    },
                    sheetState = sheetState,
                    shape = RectangleShape,
                    containerColor = editorialColors.surfaceCard,
                    dragHandle = { BottomSheetDefaults.DragHandle(color = editorialColors.editorialPaper) }
                ) {
                    StopArrivalsContent(
                        stop = selectedStop,
                        selectedRouteArrivals = selectedRouteArrivals,
                        otherRouteArrivals = otherRouteArrivals,
                        isLoading = uiState.isArrivalsLoading,
                        lines = uiState.lines,
                        arrivalsLastUpdatedMillis = uiState.arrivalsLastUpdatedMillis,
                        onRefresh = { onRefreshArrivals(selectedStop.id) },
                        onTrackArrival = { arrival -> startTracking(arrival, selectedStop) }
                    )
                }
            }
        }
    }
}

@Composable
fun TransitStopList(
    stops: List<TransitStop>,
    selectedStopId: String?,
    onStopClick: (TransitStop) -> Unit,
    modifier: Modifier = Modifier
) {
    val editorialColors = EditorialTheme.colors
    LazyColumn(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = EditorialTheme.spacing.gutterMobile, vertical = EditorialTheme.spacing.spaceSm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Stops",
                    style = EditorialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = editorialColors.editorialPaper
                )
            }
            EditorialDivider()
        }
        items(stops, key = { it.id }) { stop ->
            val isSelected = stop.id == selectedStopId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isSelected) editorialColors.surfaceCard else MaterialTheme.colorScheme.surface)
                    .clickable { onStopClick(stop) }
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(4.dp)
                        .background(if (isSelected) editorialColors.electricAlert else Color.Transparent)
                )
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = EditorialTheme.spacing.gutterMobile, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stop.name.toTitleCase(),
                            style = EditorialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = editorialColors.editorialPaper
                        )
                        Text(
                            text = "Stop ID: ${stop.id}",
                            style = EditorialTheme.typography.labelSmall,
                            color = editorialColors.onSurfaceVariant
                        )
                    }
                    
                    if (stop.stopType != null) {
                        Box(
                            modifier = Modifier
                                .background(editorialColors.surfaceContainerHigh)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = stop.stopType.toTitleCase(),
                                style = EditorialTheme.typography.labelSmall,
                                color = editorialColors.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            EditorialDivider()
        }
    }
}

@Composable
fun StopArrivalsContent(
    stop: TransitStop,
    selectedRouteArrivals: List<BusArrival>,
    otherRouteArrivals: List<BusArrival>,
    isLoading: Boolean,
    lines: List<TransitLine>,
    arrivalsLastUpdatedMillis: Long?,
    onRefresh: () -> Unit,
    onTrackArrival: (BusArrival) -> Unit
) {
    val editorialColors = EditorialTheme.colors
    var lastUpdatedText by remember { mutableStateOf("") }

    LaunchedEffect(arrivalsLastUpdatedMillis) {
        while (isActive) {
            if (arrivalsLastUpdatedMillis == null) {
                lastUpdatedText = ""
            } else {
                val elapsedMinutes = (System.currentTimeMillis() - arrivalsLastUpdatedMillis) / 60000
                lastUpdatedText = if (elapsedMinutes < 1) {
                    "Updated just now"
                } else {
                    "Updated $elapsedMinutes min ago"
                }
            }
            delay(60000L)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(EditorialTheme.spacing.spaceMd)
            .padding(bottom = EditorialTheme.spacing.spaceXl)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stop.name.toTitleCase(),
                    style = EditorialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = editorialColors.editorialPaper
                )
                Text(
                    text = "Stop ID: ${stop.id}",
                    style = EditorialTheme.typography.labelSmall,
                    color = editorialColors.onSurfaceVariant
                )
                if (lastUpdatedText.isNotEmpty()) {
                    Text(
                        text = lastUpdatedText,
                        style = EditorialTheme.typography.labelSmall,
                        color = editorialColors.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onRefresh, enabled = !isLoading) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = editorialColors.signalChartreuse,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = editorialColors.editorialPaper
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(EditorialTheme.spacing.spaceMd))
        EditorialDivider()
        
        if (selectedRouteArrivals.isEmpty() && otherRouteArrivals.isEmpty() && !isLoading) {
            Text(
                text = "NO UPCOMING ARRIVALS FOUND.",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 32.dp),
                style = EditorialTheme.typography.bodyLarge,
                color = editorialColors.onSurfaceVariant
            )
        } else {
            LazyColumn {
                item { EditorialDivider() }
                items(selectedRouteArrivals) { arrival ->
                    val line = lines.find { it.id == arrival.lineRef }
                    val routeColor = line?.let { getRouteColor(it, editorialColors) } ?: editorialColors.routeBus
                    ArrivalItem(
                        arrival = arrival,
                        routeColor = routeColor,
                        onTrackClick = { onTrackArrival(arrival) }
                    )
                }
                items(otherRouteArrivals) { arrival ->
                    val line = lines.find { it.id == arrival.lineRef }
                    val routeColor = line?.let { getRouteColor(it, editorialColors) } ?: editorialColors.routeBus
                    ArrivalItem(
                        arrival = arrival,
                        routeColor = routeColor,
                        onTrackClick = { onTrackArrival(arrival) },
                        modifier = Modifier.alpha(0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun ArrivalItem(
    arrival: BusArrival,
    routeColor: Color,
    onTrackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val editorialColors = EditorialTheme.colors
    EditorialCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RouteBadge(
                    routeNumber = arrival.lineRef,
                    backgroundColor = routeColor,
                    contentColor = editorialColors.editorialPaper
                )
                Spacer(modifier = Modifier.width(EditorialTheme.spacing.spaceMd))
                Column {
                    Text(
                        text = arrival.lineName.toTitleCase(),
                        style = EditorialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = editorialColors.editorialPaper
                    )
                    Text(
                        text = "Vehicle: ${arrival.vehicleRef ?: "N/A"}",
                        style = EditorialTheme.typography.labelSmall,
                        color = editorialColors.onSurfaceVariant
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onTrackClick) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Track Bus",
                        tint = editorialColors.editorialPaper
                    )
                }
                Spacer(modifier = Modifier.width(EditorialTheme.spacing.spaceSm))
                Column(horizontalAlignment = Alignment.End) {
                    val timeStr = formatArrivalTime(arrival.expectedArrival ?: arrival.aimedArrival ?: "")
                    Text(
                        text = timeStr,
                        style = EditorialTheme.typography.titleLarge,
                        color = editorialColors.signalChartreuse,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (timeStr.contains(":")) "TIME" else "MIN",
                        style = EditorialTheme.typography.labelSmall,
                        color = editorialColors.signalChartreuse
                    )
                }
            }
        }
    }
}

private fun formatArrivalTime(isoTime: String): String {
    if (isoTime.isEmpty()) return "N/A"
    return try {
        val instant = Instant.parse(isoTime)
        val now = Instant.now()
        val duration = java.time.Duration.between(now, instant)
        val minutes = max(0, duration.toMinutes())
        
        if (minutes in 0..60) {
            java.lang.String.format(java.util.Locale.US, "%02d", minutes)
        } else {
            val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
                .withZone(ZoneId.systemDefault())
            formatter.format(instant)
        }
    } catch (e: Exception) {
        isoTime.split("T").lastOrNull()?.take(5) ?: isoTime
    }
}

@Composable
fun TransitLineList(
    lines: List<TransitLine>,
    selectedLineId: String?,
    onLineClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val editorialColors = EditorialTheme.colors
    var activeFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Local", "Rapid", "Express", "Owl")

    val filteredLines = remember(lines, activeFilter) {
        if (activeFilter == "All") lines
        else {
            lines.filter { line ->
                val id = line.id.uppercase()
                when (activeFilter) {
                    "Local" -> id.all { it.isDigit() } || (id.all { it.isLetter() } && !id.endsWith("OWL"))
                    "Rapid" -> id.endsWith("R")
                    "Express" -> id.endsWith("X") || id.endsWith("AX") || id.endsWith("BX")
                    "Owl" -> id.endsWith("OWL")
                    else -> true
                }
            }
        }
    }

    LazyColumn(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(editorialColors.surfaceContainerLow)
                    .padding(horizontal = EditorialTheme.spacing.gutterMobile, vertical = EditorialTheme.spacing.spaceXs)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(EditorialTheme.spacing.space2xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                filters.forEach { filter ->
                    val isActive = filter == activeFilter
                    Box(
                        modifier = Modifier
                            .background(
                                if (isActive) editorialColors.editorialPaper else editorialColors.surfaceCard
                            )
                            .clickable { activeFilter = filter }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = filter.uppercase(),
                            style = EditorialTheme.typography.labelLarge,
                            color = if (isActive) editorialColors.editorialBlack else editorialColors.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            EditorialDivider()
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = EditorialTheme.spacing.gutterMobile, vertical = EditorialTheme.spacing.spaceSm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Muni Routes",
                    style = EditorialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = editorialColors.editorialPaper
                )
            }
            EditorialDivider()
        }

        items(filteredLines, key = { it.id }) { line ->
            val isSelected = line.id == selectedLineId
            val routeColor = getRouteColor(line, editorialColors)
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isSelected) editorialColors.surfaceCard else MaterialTheme.colorScheme.surface)
                    .clickable { onLineClick(line.id) }
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(4.dp)
                        .background(if (isSelected) editorialColors.electricAlert else Color.Transparent)
                )

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = EditorialTheme.spacing.gutterMobile, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(EditorialTheme.spacing.spaceSm)
                    ) {
                        RouteBadge(
                            routeNumber = line.id,
                            backgroundColor = routeColor,
                            contentColor = editorialColors.editorialPaper
                        )
                        Text(
                            text = line.name.toTitleCase(),
                            style = EditorialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = editorialColors.editorialPaper,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(editorialColors.surfaceContainerHigh)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = line.transportMode.toTitleCase(),
                            style = EditorialTheme.typography.labelSmall,
                            color = editorialColors.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            EditorialDivider()
        }
    }
}

fun getRouteColor(line: TransitLine, colors: EditorialColors): Color {
    val id = line.id.uppercase()
    return when {
        id.endsWith("R") -> colors.routeRapid
        id.endsWith("X") || id.endsWith("AX") || id.endsWith("BX") -> colors.routeExpress
        id.endsWith("OWL") -> colors.routeLateNight
        id.all { it.isLetter() } -> colors.routeLightRail
        id.all { it.isDigit() } -> colors.routeBus
        else -> colors.routeBus
    }
}

private fun String.toTitleCase(): String {
    return this.lowercase().split(" ").joinToString(" ") { 
        it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(Locale.US) else char.toString() } 
    }
}
