#!/usr/bin/env python3
"""Sync football data from TheSportsDB (primary) with football-data.org fallback."""
import json, os, urllib.request, urllib.error

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

def get(url):
    try:
        with urllib.request.urlopen(url, timeout=30) as r:
            return json.loads(r.read().decode())
    except Exception as e:
        print("ERR", url, e)
        return None

def fetch_league(code, lid):
    table = []
    data = get(BASE + "/lookuptable.php?l=%s&s=%s" % (lid, SEASON))
    if data and data.get("table"):
        for r in data["table"]:
            table.append({
                "n": r.get("strTeam", ""),
                "g": "",
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
    import time
    time.sleep(1)
    matches = []
    ev = get(BASE + "/eventsseason.php?id=%s&s=%s" % (lid, SEASON))
    if ev and ev.get("events"):
        for e in ev["events"]:
            matches.append({
                "id": e.get("idEvent", ""),
                "h": e.get("strHomeTeam", ""),
                "a": e.get("strAwayTeam", ""),
                "t": 0,
                "s": 0,
                "hs": e.get("intHomeScore") or "",
                "as": e.get("intAwayScore") or "",
                "dt": e.get("strStatus", "") or "",
                "hid": int(e.get("idHomeTeam", 0) or 0),
                "aid": int(e.get("idAwayTeam", 0) or 0),
            })
    return {"v": 2, "season": SEASON, "source": "TheSportsDB", "fetchedAt": int(__import__("time").time()*1000), "table": table, "matches": matches}

os.makedirs(OUT, exist_ok=True)
for code, lid, name in LEAGUES:
    print("Fetching", code, name)
    d = fetch_league(code, lid)
    path = os.path.join(OUT, code + ".json")
    with open(path, "w") as f:
        json.dump(d, f)
    print("  wrote", path, "rows=", len(d["table"]), "matches=", len(d["matches"]))