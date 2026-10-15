package com.nghianguyen.gomuni.features.transit.map

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

/**
 * Remembers the transit viewport and applies destination-driven camera changes.
 * @param data Visible stops and vehicles used for fitting the camera.
 * @param routeId Selected route, or null for the San Francisco overview.
 * @param selectedStopId Stop to focus when arrivals are visible.
 * @param focusArrivals Whether the camera should include arrivals for the selected stop.
 * @param cameraKey Full owner identity, including operator, used to detect route changes.
 */
@Composable
internal fun rememberTransitMapViewport(
    data: TransitMapData,
    routeId: String?,
    selectedStopId: String?,
    focusArrivals: Boolean,
    cameraKey: Any? = routeId,
): com.mapbox.maps.extension.compose.animation.viewport.MapViewportState {
    val viewport = rememberMapViewportState {
        setCameraOptions { center(Point.fromLngLat(-122.4194, 37.7749)); zoom(11.0) }
    }
    val focusedStop = if (focusArrivals) data.stops.find { it.id == selectedStopId } else null
    val bounds = if (focusedStop != null) {
        listOf(Point.fromLngLat(focusedStop.longitude, focusedStop.latitude)) +
            data.vehicles.map { Point.fromLngLat(it.longitude, it.latitude) }
    } else data.stops.map { Point.fromLngLat(it.longitude, it.latitude) }
    // Fit on selection/visibility changes or the first available route snapshot only.
    // Prediction updates and manual gestures retain the current camera, including follow mode.
    LaunchedEffect(cameraKey, focusedStop?.id, focusArrivals, bounds.isNotEmpty()) {
        if (routeId == null) {
            viewport.setCameraOptions { center(Point.fromLngLat(-122.4194, 37.7749)); zoom(11.0) }
        } else if (bounds.isNotEmpty()) {
            viewport.flyTo(
                viewport.cameraForCoordinates(bounds, coordinatesPadding = EdgeInsets(100.0, 100.0, 100.0, 100.0)),
                MapAnimationOptions.mapAnimationOptions { duration(1000) },
            )
        }
    }
    return viewport
}

@Composable
internal fun TransitMapRoute(
    data: TransitMapData,
    selectedStopId: String?,
    focusArrivals: Boolean,
    locationPermissionGranted: Boolean,
    routeColor: Color,
    viewport: com.mapbox.maps.extension.compose.animation.viewport.MapViewportState,
    onFollowLocation: () -> Unit,
    onStopSelected: (TransitStop) -> Unit,
    modifier: Modifier,
) {
    val colors = EditorialTheme.colors
    val stopIcon = rememberIconImage(key = "stop-marker", painter = painterResource(R.drawable.baseline_adjust_24))
    val busIcon = rememberIconImage(key = "bus-marker", painter = painterResource(R.drawable.bus_icon))
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
                    val selected = stop.id == selectedStopId
                    PointAnnotation(point = Point.fromLngLat(stop.longitude, stop.latitude)) {
                        interactionsState.onClicked { onStopSelected(stop); true }
                        iconImage = stopIcon
                        iconColor = if (selected) colors.signalChartreuse else routeColor
                        iconOpacity = if (focusArrivals && !selected) 0.3 else 1.0
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
            onClick = onFollowLocation,
            modifier = Modifier.align(Alignment.BottomEnd).padding(EditorialTheme.spacing.spaceMd),
        ) {
            Icon(painterResource(R.drawable.current_location_icon), stringResource(R.string.my_location), tint = colors.editorialBlack)
        }
    }
}
