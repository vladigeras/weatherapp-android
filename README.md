# Weatherapp Android

Weather for your location or a city of your choice, with hourly and daily forecasts and a home screen widget.

Requires Android 15 or newer. [Download the APK](https://github.com/vladigeras/weatherapp-android/releases).

## Getting started

1. Find a city and select it, or allow location access. Approximate location works too.
2. Pull down to refresh the weather. On the location screen, **Refresh** saves your new coordinates.
3. Choose your provider, weather details, forecast length, and language in **Settings**, then tap **Save**.
4. To add the widget, hold your home screen → **Widgets** → **Weatherapp**. Tap the widget to open the app.

## Weather providers

| Provider | Daily forecast | Hourly interval |
| --- | --- | --- |
| Open-Meteo | Up to 16 days | 1 hour |
| wttr.in | Up to 3 days | 3 hours |
| 7Timer | Up to 7 days | 3 hours |

Available details depend on your provider. Unsupported options are disabled in settings, and missing values stay hidden. If wttr.in cannot provide hourly weather, the app shows a notice.

7Timer uses the nearest forecast for the main weather card. Its times and daily boundaries follow your phone's time zone.

## Privacy

No ads or analytics. Your weather provider receives coordinates. Android services search for cities and can detect your location and its name. Settings and cached weather stay on your device.

Licensed under [MIT](LICENSE).
