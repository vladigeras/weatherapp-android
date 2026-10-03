import argparse
import json
from datetime import datetime, timedelta
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse
from zoneinfo import ZoneInfo


def location(query):
    western = "york" in query.lower() or "74" in query
    return ("New York", 40.7128, -74.0060, "United States", "New York", "America/New_York") if western else ("Moscow", 55.7558, 37.6173, "Russia", "Moscow City", "Europe/Moscow")


def open_meteo(params):
    city, latitude, longitude, country, region, zone = location(params.get("longitude", [""])[0])
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


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        request = urlparse(self.path)
        params = parse_qs(request.query)
        print(json.dumps({"path": request.path, "params": params}), flush=True)
        if request.path == "/open-meteo/search":
            city, latitude, longitude, country, region, zone = location(params.get("name", [""])[0])
            body = {"results": [{"id": 1, "name": city, "latitude": latitude, "longitude": longitude, "country": country, "admin1": region}]}
        elif request.path == "/open-meteo/forecast":
            body = open_meteo(params)
        else:
            self.send_error(404)
            return
        payload = json.dumps(body).encode()
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(payload)))
        self.end_headers()
        self.wfile.write(payload)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--port", type=int, default=8765)
    args = parser.parse_args()
    ThreadingHTTPServer(("127.0.0.1", args.port), Handler).serve_forever()
