# Weatherapp Android

A simple, modern application built with Kotlin, Jetpack Compose, and Clean Architecture principles.

![Build](https://github.com/vladigeras/weatherapp-android/actions/workflows/build.yml/badge.svg)
![GitHub release](https://img.shields.io/github/v/release/vladigeras/weatherapp-android)
![GitHub downloads](https://img.shields.io/github/downloads/vladigeras/weatherapp-android/total)

---

## 📱 About

Weatherapp shows current weather and forecasts for your location or a selected city. Choose **Open-Meteo** or **wttr.in** in Settings; both weather requests and city search use the selected provider.

Open-Meteo supports up to 16 forecast days with hourly data. wttr.in supports 3 days with 3-hour intervals and finds one nearby location for you to confirm. Unavailable settings are disabled, and missing weather details are hidden. Your display preferences are kept when switching providers.

The app features:

- **Kotlin** + **Coroutines** for asynchronous work
- **Jetpack Compose** for declarative UI
- **Hilt** for dependency injection
- **MVVM** architecture
- **Material Design 3** theming
- Unit tests with **JUnit**, **MockK**, and **Turbine**

## 🧩 Home Screen Widget

Quickly check the weather without opening the app:

- **Resizable:** Resize horizontally or vertically to fit your home screen
- **Live Sync:** Updates with the app when you change location, language, or weather provider
- **Info:** Shows the city, weather condition, icon, and current temperature
- **Tap to Open:** Opens the app directly to the weather screen
- **Design:** Clean white text that adapts to your launcher background

## ⚙️ Requirements

- **Minimum Android SDK**: 35 (Android 15)
- **Java**: 21

---

## 🔒 Privacy

The app contains **no advertisements or analytics**. Weather requests send location coordinates to your selected provider; city searches send the entered city name and app language. Android location services may also be used to detect your location and resolve its name. Settings and cached weather are stored on your device.

---

## 📦 Usage

1. Launch the app and choose a city or use your current location.
2. Grant location permission if you choose GPS.
3. Pull‑to‑refresh to fetch the latest data.
4. Choose a weather provider, displayed metrics, forecast range, and language in `Settings`. With wttr.in, tap **Find** and confirm the returned location.
5. **Add the Widget:** Long-press your home screen → `Widgets` → `Weatherapp` → drag to your preferred size. Tap it anytime to open the app.

---

## 🤝 Contributing

Contributions are welcome! Please ensure your code adheres to the existing style and includes relevant tests.

## 📄 License

MIT License — see the `LICENSE` file for details.
