#!/usr/bin/env python3
"""Sync football data from TheSportsDB (primary) with football-data.org fallback."""
import json, os, time, urllib.request, urllib.error
from datetime import datetime, timezone

KEY = "123"
BASE = "https://www.thesportsdb.com/api/v1/json/" + KEY
LEAGUES = [
    ("EPL", "4328", "English Premier League"),
    ("LL", "4335", "Spanish La Liga"),
    ("SA", "4332", "Italian Serie A"),
    ("BL", "4331", "German Bundesliga"),
    ("L1", "4334", "French Ligue 1"),
    ("IDN", "4790", "Indonesian Super League"),
    ("UCL", "4480", "UEFA Champions League"),
]
OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "data", "football")
SEASON = "2025-2026"
FD_KEY = os.environ.get("FOOTBALL_DATA_API_KEY", "")

def get(url, headers=None):
    try:
        req = urllib.request.Request(url, headers=headers or {})
        with urllib.request.urlopen(req, timeout=30) as r:
            return json.loads(r.read().decode())
    except Exception as e:
        print("ERR", url, e)
        return None

def to_millis(date_str, time_str):
    if not date_str:
        return 0
    try:
        t = time_str or "00:00:00"
        if len(t) == 5:
            t += ":00"
        dt = datetime.strptime(date_str + " " + t, "%Y-%m-%d %H:%M:%S").replace(tzinfo=timezone.utc)
        return int(dt.timestamp() * 1000)
    except Exception:
        return 0

def state_from_status(s):
    s = (s or "").lower()
    if "finish" in s or s == "ft":
        return 2
    if "live" in s or "in play" in s or "half" in s:
        return 1
    if "postpon" in s or "cancel" in s or "suspend" in s:
        return 3
    return 0

def fetch_thesportsdb(code, lid):
    table = []
    data = get(BASE + "/lookuptable.php?l=%s&s=%s" % (lid, SEASON))
    if data and data.get("table"):
        for r in data["table"]:
            table.append({
                "n": r.get("strTeam", ""),
                "g": r.get("strGroup", "") or "",
                "r": int(r.get("intRank", 0) or 0),
                "p": int(r.get("intPlayed", 0) or 0),
                "w": int(r.get("intWin", 0) or 0),
                "d": int(r.get("intDraw", 0) or 0),
                "l": int(r.get("intLoss", 0) or 0),
                "gd": int(r.get("intGoalDifference", 0) or 0),
                "pt": int(r.get("intPoints", 0) or 0),
                "q": 0,
                "tid": int(r.get("idTeam", 0) or 0),
            })
    time.sleep(1)
    matches = []
    ev = get(BASE + "/eventsseason.php?id=%s&s=%s" % (lid, SEASON))
    if ev and ev.get("events"):
        for e in ev["events"]:
            hs = e.get("intHomeScore")
            as_ = e.get("intAwayScore")
            matches.append({
                "id": e.get("idEvent", ""),
                "h": e.get("strHomeTeam", ""),
                "a": e.get("strAwayTeam", ""),
                "t": to_millis(e.get("dateEvent"), e.get("strTime")),
                "s": state_from_status(e.get("strStatus")),
                "hs": "" if hs is None else str(hs),
                "as": "" if as_ is None else str(as_),
                "dt": e.get("strStatus", "") or "",
                "hid": int(e.get("idHomeTeam", 0) or 0),
                "aid": int(e.get("idAwayTeam", 0) or 0),
            })
    if not table and not matches:
        return None
    return {"v": 2, "season": SEASON, "source": "TheSportsDB", "fetchedAt": int(time.time()*1000),
            "table": table, "matches": matches}

def fetch_football_data(code, lid):
    # football-data.org fallback: only if API key present
    if not FD_KEY:
        return None
    # Map our codes to football-data competition codes
    fd_map = {"EPL": "PL", "LL": "PD", "SA": "SA", "BL": "BL1", "L1": "FL1", "UCL": "CL"}
    comp = fd_map.get(code)
    if not comp:
        return None
    headers = {"X-Auth-Token": FD_KEY}
    table = []
    data = get("https://api.football-data.org/v4/competitions/%s/standings" % comp, headers)
    if data:
        for st in data.get("standings", []):
            for r in st.get("table", []):
                t = r.get("team", {})
                table.append({
                    "n": t.get("shortName") or t.get("name", ""),
                    "g": "",
                    "r": r.get("position", 0),
                    "p": r.get("playedGames", 0),
                    "w": r.get("won", 0),
                    "d": r.get("draw", 0),
                    "l": r.get("lost", 0),
                    "gd": r.get("goalDifference", 0),
                    "pt": r.get("points", 0),
                    "q": 0,
                    "tid": t.get("id", 0),
                })
    matches = []
    data = get("https://api.football-data.org/v4/competitions/%s/matches" % comp, headers)
    if data:
        for m in data.get("matches", []):
            hs = m.get("score", {}).get("fullTime", {}).get("home")
            as_ = m.get("score", {}).get("fullTime", {}).get("away")
            status = m.get("status", "")
            st = 2 if status == "FINISHED" else (1 if status in ("IN_PLAY", "PAUSED", "LIVE") else (3 if status in ("POSTPONED", "CANCELLED") else 0))
            if st in (0, 3):
                hs = ""; as_ = ""
            matches.append({
                "id": str(m.get("id", "")),
                "h": m.get("homeTeam", {}).get("shortName") or m.get("homeTeam", {}).get("name", ""),
                "a": m.get("awayTeam", {}).get("shortName") or m.get("awayTeam", {}).get("name", ""),
                "t": to_millis(m.get("utcDate", "")[:10], m.get("utcDate", "")[11:19]),
                "s": st,
                "hs": "" if hs is None else str(hs),
                "as": "" if as_ is None else str(as_),
                "dt": status,
                "hid": m.get("homeTeam", {}).get("id", 0),
                "aid": m.get("awayTeam", {}).get("id", 0),
            })
    if not table and not matches:
        return None
    return {"v": 2, "season": SEASON, "source": "football-data.org", "fetchedAt": int(time.time()*1000),
            "table": table, "matches": matches}

os.makedirs(OUT, exist_ok=True)
for code, lid, name in LEAGUES:
    print("Fetching", code, name)
    d = fetch_thesportsdb(code, lid)
    if d is None:
        print("  TheSportsDB failed, trying football-data.org fallback")
        d = fetch_football_data(code, lid)
    if d is None:
        print("  all sources failed, keeping existing file")
        continue
    path = os.path.join(OUT, code + ".json")
    with open(path, "w") as f:
        json.dump(d, f)
    print("  wrote", path, "rows=", len(d["table"]), "matches=", len(d["matches"]), "source=", d["source"])
