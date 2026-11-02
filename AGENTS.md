## Overview

This is an Android application where users in San Francisco can look up bus stops to track incoming Muni buses.

## Tech Stack
- **Architecture**: MVVM / Clean Architecture
- **UI:** Jetpack Compose, Material 3 components and adaptive layouts for different screen sizes
- **Dependency Injection:** Hilt
- **Navigation:** Jetpack Navigation 3 for Compose
- **Data:** The data layer is implemented using the repository pattern.
    - **Local Data:** Room and DataStore
    - **Remote Data:** Retrofit and OkHttp

## Documentation

Before planning, changing, or debugging code, read `docs/README.md`
and follow its guidance to select relevant documents.
Read those documents alongside the affected implementation.
For Compose changes, read [UI architecture](docs/ui-architecture.md) for responsibilities
and [Compose conventions](docs/compose.md) for implementation mechanics.

## Kotlin Style

Use imports instead of fully qualified names in Kotlin declarations and expressions.
Use import aliases to resolve name conflicts.

## Build Verification

For Kotlin or Gradle Kotlin changes, run:

```powershell
.\gradlew.bat spotlessApply
.\gradlew.bat spotlessCheck :app:detektDebug
```

## UI Design

The application's visual design is defined in `DESIGN.md`.

When modifying Compose UI:
- Use `DESIGN.md` as the source of truth for design tokens and visual conventions.
- Use the PNG files un `screenshots/` as visual references for the intended UI states.
- Reuse the project's design-system tokens and components rather than introducing one-off styling.
- Preserve existing application behavior, state management, and data flow unless the design explicitly requires a UI-state change.
