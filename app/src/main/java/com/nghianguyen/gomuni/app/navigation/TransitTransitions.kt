package com.nghianguyen.gomuni.app.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith

/** Slides only the navigation panel; the shared map and toolbar remain outside this scope. */
internal fun AnimatedContentTransitionScope<*>.transitSlideTransition(forward: Boolean): ContentTransform {
    val direction = if (forward) AnimatedContentTransitionScope.SlideDirection.Left
        else AnimatedContentTransitionScope.SlideDirection.Right
    return slideIntoContainer(direction, animationSpec = tween(300, easing = FastOutSlowInEasing)) togetherWith
        slideOutOfContainer(direction, animationSpec = tween(300, easing = FastOutSlowInEasing))
}
