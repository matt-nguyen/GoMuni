package com.nghianguyen.gomuni.ui.lines

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import com.nghianguyen.gomuni.domain.model.TransitStop
import com.nghianguyen.gomuni.ui.components.EditorialFAB
import com.nghianguyen.gomuni.ui.theme.EditorialTheme

@Composable
fun TransitMap(
    routeState: RouteStopsUiState?,
    arrivalsState: StopArrivalsUiState?,
    arrivalSheetVisible: Boolean,
    locationPermissionGranted: Boolean,
    routeColor: Color,
    onStopSelected: (TransitStop) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = EditorialTheme.colors
    val viewport = rememberMapViewportState {
        setCameraOptions { center(Point.fromLngLat(-122.4194, 37.7749)); zoom(11.0) }
    }
    val stopIcon = rememberIconImage(key = "stop-marker", painter = painterResource(R.drawable.baseline_adjust_24))
    val busIcon = rememberIconImage(key = "bus-marker", painter = painterResource(R.drawable.bus_icon))
    val data = remember(routeState?.stops, routeState?.selectedStopId, routeState?.route, arrivalsState) {
        TransitMapData.from(routeState, arrivalsState)
    }
    val focusedStop = if (arrivalSheetVisible) data.stops.find { it.id == routeState?.selectedStopId } else null
    val bounds = if (focusedStop != null) {
        listOf(Point.fromLngLat(focusedStop.longitude, focusedStop.latitude)) +
            data.vehicles.map { Point.fromLngLat(it.longitude, it.latitude) }
    } else data.stops.map { Point.fromLngLat(it.longitude, it.latitude) }
    // Fit on selection/visibility changes or the first available route snapshot only.
    // Prediction updates and manual gestures retain the current camera, including follow mode.
    LaunchedEffect(routeState?.route, focusedStop?.id, arrivalSheetVisible, bounds.isNotEmpty()) {
        if (routeState == null) {
            viewport.setCameraOptions { center(Point.fromLngLat(-122.4194, 37.7749)); zoom(11.0) }
        } else if (bounds.isNotEmpty()) {
            viewport.flyTo(
                viewport.cameraForCoordinates(bounds, coordinatesPadding = EdgeInsets(100.0, 100.0, 100.0, 100.0)),
                MapAnimationOptions.mapAnimationOptions { duration(1000) },
            )
        }
    }
    Box(modifier) {
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = viewport,
            style = {
                MapboxStandardStyle(standardStyleState = rememberStandardStyleState {
                    configurationsState.apply {
                        lightPreset = LightPresetValue.NIGHT
                        showPointOfInterestLabels = BooleanValue(false)
                    }
                })
            },
        ) {
            MapEffect(locationPermissionGranted) { mapView ->
                mapView.location.updateSettings {
                    enabled = locationPermissionGranted
                    pulsingEnabled = true
                    locationPuck = LocationPuck2D(topImage = ImageHolder.from(R.drawable.current_location_icon))
                }
            }
            data.stops.forEach { stop ->
                key(stop.id) {
                    val selected = stop.id == routeState?.selectedStopId
                    PointAnnotation(point = Point.fromLngLat(stop.longitude, stop.latitude)) {
                        interactionsState.onClicked { onStopSelected(stop); true }
                        iconImage = stopIcon
                        iconColor = if (selected) colors.signalChartreuse else routeColor
                        iconOpacity = if (arrivalSheetVisible && !selected) 0.3 else 1.0
                    }
                }
            }
            data.vehicles.forEach { bus ->
                key("vehicle:${vehicleKey(bus)}") {
                    PointAnnotation(point = Point.fromLngLat(bus.longitude, bus.latitude)) {
                        iconImage = busIcon
                        iconColor = colors.signalChartreuse
                    }
                }
            }
        }
        if (locationPermissionGranted) EditorialFAB(
            onClick = { viewport.transitionToFollowPuckState() },
            modifier = Modifier.align(Alignment.BottomEnd).padding(EditorialTheme.spacing.spaceMd),
        ) {
            Icon(painterResource(R.drawable.current_location_icon), stringResource(R.string.my_location), tint = colors.editorialBlack)
        }
    }
}
