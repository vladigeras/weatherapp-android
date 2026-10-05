import argparse
import json
from datetime import datetime, timedelta, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse, unquote
from pathlib import Path
import tempfile
from zoneinfo import ZoneInfo


def location(query):
    western = "york" in query.lower() or "74" in query
    return (40.7128, -74.0060, "America/New_York") if western else (55.7558, 37.6173, "Europe/Moscow")


def open_meteo(params):
    latitude, longitude, zone = location(params.get("longitude", [""])[0])
    now = datetime.now(ZoneInfo(zone))
    days = [now.date() + timedelta(days=i) for i in range(int(params.get("forecast_days", ["7"])[0]))]
    hours = [now.replace(minute=0, second=0, microsecond=0) + timedelta(hours=i) for i in range(int(params.get("forecast_hours", ["12"])[0]))]
    return {
        "latitude": latitude, "longitude": longitude, "generationtime_ms": 0.1,
        "utc_offset_seconds": int(now.utcoffset().total_seconds()), "timezone": zone, "elevation": 100.0,
        "current": {"temperature_2m": 18.0, "apparent_temperature": 17.0, "windspeed_10m": 12.0, "weathercode": 2, "is_day": 1},
        "current_units": {"temperature_2m": "°C"},
        "hourly": {"time": [h.isoformat(timespec="minutes")[:16] for h in hours], "temperature_2m": [18.0] * len(hours), "relativehumidity_2m": [65] * len(hours), "windspeed_10m": [12.0] * len(hours)},
        "daily": {
            "time": [str(d) for d in days], "weathercode": [2] * len(days), "temperature_2m_min": [10.0] * len(days), "temperature_2m_max": [20.0] * len(days),
            "precipitation_sum": [2.0] * len(days), "sunrise": [f"{d}T06:30" for d in days], "sunset": [f"{d}T18:30" for d in days],
            "windspeed_10m_max": [18.0] * len(days), "winddirection_10m_dominant": [180] * len(days), "uv_index_max": [3.0] * len(days)
        }
    }


def wttr(query, include_hourly):
    zone = location(query)[-1]
    now = datetime.now(ZoneInfo(zone))
    days = []
    for i in range(3):
        day = {"date": str(now.date() + timedelta(days=i)), "mintempC": "11", "maxtempC": "24", "uvIndex": "8", "astronomy": [{"sunrise": "06:30 AM", "sunset": "06:30 PM"}]}
        if include_hourly:
            day["hourly"] = [{"time": str(h * 100), "tempC": "23", "weatherCode": "116", "humidity": "70", "windspeedKmph": "9", "precipMM": "2", "uvIndex": "5"} for h in range(0, 24, 3)]
        days.append(day)
    return {
        "current_condition": [{"temp_C": "27", "FeelsLikeC": "26", "humidity": "70", "windspeedKmph": "9", "weatherCode": "113", "observation_time": "12:00 PM"}],
        "weather": days
    }


def seven_timer(params):
    zone = ZoneInfo(location(params.get("lon", [""])[0])[-1])
    now = datetime.now(timezone.utc)
    init = now.replace(hour=now.hour // 6 * 6, minute=0, second=0, microsecond=0)
    points = []
    for hour in range(3, 193, 3):
        local = (init + timedelta(hours=hour)).astimezone(zone)
        suffix = "day" if 6 <= local.hour < 18 else "night"
        points.append({"timepoint": hour, "temp2m": 11 + hour // 3 % 4, "rh2m": "72%", "weather": "pcloudy" + suffix,
                       "prec_type": "none", "prec_amount": 0, "wind10m": {"direction": "N", "speed": 2}})
    return {"product": "civil", "init": init.strftime("%Y%m%d%H"), "dataseries": points}


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        request = urlparse(self.path)
        params = parse_qs(request.query)
        print(json.dumps({"path": request.path, "params": params}), flush=True)
        failure = Path(tempfile.gettempdir(), "weather_mock_failure")
        source = "wttr" if request.path.startswith("/wttr/") else "7timer" if request.path == "/7timer/forecast" else "open-meteo"
        if failure.exists() and failure.read_text().strip() == source:
            self.send_error(503)
            return
        content_type = "application/json"
        if request.path.startswith("/wttr/"):
            query = unquote(request.path.removeprefix("/wttr/"))
            content_type = "text/plain"
            if params.get("format") == ["%Z"]:
                body = location(query)[-1]
            else:
                body = wttr(query, params.get("format") == ["j1"])
        elif request.path == "/open-meteo/forecast":
            body = open_meteo(params)
        elif request.path == "/7timer/forecast":
            body = seven_timer(params)
        else:
            self.send_error(404)
            return
        payload = (body if isinstance(body, str) else json.dumps(body)).encode()
        self.send_response(200)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(payload)))
        self.end_headers()
        self.wfile.write(payload)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--port", type=int, default=8765)
    args = parser.parse_args()
    ThreadingHTTPServer(("127.0.0.1", args.port), Handler).serve_forever()
