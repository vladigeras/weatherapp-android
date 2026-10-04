# Weatherapp Android

Weather for your location or a city of your choice.

![Build](https://github.com/vladigeras/weatherapp-android/actions/workflows/build.yml/badge.svg)
![GitHub release](https://img.shields.io/github/v/release/vladigeras/weatherapp-android)
![GitHub downloads](https://img.shields.io/github/downloads/vladigeras/weatherapp-android/total)

## 📱 About

Current weather, hourly and daily forecasts, and a choice of language and weather details.

- **Open-Meteo:** forecasts for up to 16 days.
- **wttr.in:** uses `wttr.is`, with 3 forecast days and 3-hour intervals. If hourly data is unavailable, the app shows a notice and keeps the current and daily weather.

## 🧩 Home Screen Widget

Shows the city, weather, and temperature. Resizes to fit your home screen and updates with the app. Tap it to open the weather screen.

## ⚙️ Requirements

Android 15 or newer. Download the APK from [Releases](https://github.com/vladigeras/weatherapp-android/releases).

To build from source, open the project in Android Studio or run:

```sh
./gradlew assembleDebug
```

## 🔒 Privacy

**No ads or analytics.** Your selected provider receives coordinates for weather requests, or the city name and language for searches. Android services can detect your location and its name. Settings and cached weather stay on your device.

## 📦 Usage

1. Choose a city or allow location access. Approximate location works too. With wttr.in, tap **Find** and confirm the suggested place.
2. Pull down to refresh the weather. **Refresh** on the location screen saves your new coordinates.
3. In **Settings**, choose the provider, forecast, weather details, and language. Tap **Save** to apply changes.
4. To add the widget, hold your home screen → **Widgets** → **Weatherapp**.

## 🤝 Contributing

Contributions are welcome! Follow the project style and include tests for changed behavior.

## 📄 License

MIT — see [LICENSE](LICENSE).
