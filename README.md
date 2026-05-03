# Weatherapp Android

A simple, modern application built with Kotlin, Jetpack Compose, and Clean Architecture principles.

![Build](https://github.com/vladigeras/weatherapp-android/actions/workflows/build.yml/badge.svg)
![GitHub release](https://img.shields.io/github/v/release/vladigeras/weatherapp-android)
![GitHub downloads](https://img.shields.io/github/downloads/vladigeras/weatherapp-android/total)

---

## 📱 About

Weatherapp provides current weather information for any location using API. The app features:

- **Kotlin** + **Coroutines** for asynchronous work
- **Jetpack Compose** for declarative UI
- **Hilt** for dependency injection
- **MVVM** architecture with a single‑activity setup
- **Material Design 3** theming
- Unit tests with **JUnit**, **MockK**, and **Turbine**

## 🧩 Home Screen Widget

Quickly check the weather without opening the app:
- **Resizable:** Supports sizes from `3x2` to `5x2` (horizontal & vertical)
- **Live Sync:** Instantly updates when you change location or language in the app
- **Info:** Shows city name, weather icon, current temperature, and "Feels like"
- **Tap to Open:** Opens the app directly to the weather screen
- **Design:** Clean white text that adapts to your launcher background

## ⚙️ Requirements

- **Minimum Android SDK**: 35 (Android 15)
- **Java**: 21

---

## 🔒 Privacy

The app contains **no advertisements**, **does not collect** any personal or usage data, and **does not transmit** any information from your device. It works entirely offline except for fetching weather data from the API, which only receives the location coordinates you explicitly provide. We never sell, share, or monetize your data.

---

## 📦 Usage

1. Launch the app – it will request location permission.
2. Grant permission to see weather for your current location, or use the search bar to look up any city worldwide.
3. Pull‑to‑refresh to fetch the latest data.
4. Customize displayed metrics (humidity, wind, forecast, etc.) and language in `Settings`.
5. **Add the Widget:** Long-press your home screen → `Widgets` → `Weatherapp` → drag to your preferred size. Tap it anytime to open the app.

---

## 🤝 Contributing

Contributions are welcome! Please ensure your code adheres to the existing style and includes relevant tests.

## 📄 License

MIT License — see the `LICENSE` file for details.