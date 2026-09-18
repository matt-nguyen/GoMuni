# GoMuni

GoMuni is an Android app for browsing San Francisco Muni bus routes and getting real-time arrival estimates at bus stops.


## Features
- Browse San Francisco Muni bus routes
- View the stops for a selected bus route on a map
- View real-time bus stop ETAs
- See incoming buses on the map when available
- Track a bus stop and receive push notifications with updated ETA information
- Manually refresh a tracked stop's ETA

## Tech Stack
- **Kotlin** — Application language
- **Jetpack Compose** — UI
- **Hilt** — Dependency injection
- **Retrofit** — Network/API communication
- **Room** — Local data persistence
- **Mapbox** — Maps and map visualization

## Requirements
- Android Studio
- Android SDK 36
- Minimum Android version: **Android 8.0 (API 26)**
- A 511 SF Bay API key ([https://511.org/open-data/token](https://511.org/open-data/token))
- A Mapbox access token ([https://www.mapbox.com/mobile-maps-sdk](https://www.mapbox.com/mobile-maps-sdk))

## Configuration
GoMuni requires API credentials that should **not** be committed to the repository.

Add the following values to your local **local.properties** file:
```
transit_api_key=<511 api key here>
MAPBOX_DOWNLOADS_TOKEN=<Mapbox access token here>
```
Replace the placeholder values with your own credentials.

Make sure local.properties remains uncommitted and is included in your **.gitignore**.

## Building
1. Clone the repository:
```
git clone https://github.com/matt-nguyen/GoMuni.git
cd GoMuni
```
2. Add your API credentials to **local.properties** as described above.

3. Open the project in Android Studio.

4. Sync the Gradle project.

5. Build and run the app on an Android device or emulator running Android 8.0 (API 26) or newer.

## Data
GoMuni uses the [**511 SF Bay API**](https://511.org/open-data/transit) to retrieve transit information, including Muni routes, stops, and arrival estimates.
