#!/usr/bin/env python3
"""
Sinkronisasi data bola SwadayaTV (dijalankan oleh GitHub Actions, bukan di TV).

Sumber: football-data.org (butuh token, disimpan sebagai GitHub Secret).
Hasil : data/football/<kode>.json   -> klasemen + jadwal/hasil (format cache v2 aplikasi)
        data/football/teams.json    -> peta ID klub -> nama, logo (PNG diutamakan)
Kunci API TIDAK pernah ditulis ke file hasil. Bila satu liga gagal, file lamanya dibiarkan.
"""
import json, os, sys, time, urllib.request, urllib.parse, urllib.error
from datetime import datetime, timezone

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "data", "football")
FD = "https://api.football-data.org/v4"
TSDB = "https://www.thesportsdb.com/api/v1/json/123"

# kode aplikasi -> kode football-data.org (id liga FotMob dipakai app sebagai kunci pilihan)
COMPETITIONS = {"EPL": "PL", "LL": "PD", "SA": "SA", "BL": "BL1", "L1": "FL1", "UCL": "CL"}

def s32(argb):
    argb &= 0xFFFFFFFF
    return argb - (1 << 32) if argb >= 1 << 31 else argb

HIJAU, BIRU, ORANYE, MERAH = s32(0xFF14E8A0), s32(0xFF4DA3FF), s32(0xFFFFB02E), s32(0xFFFF5252)

def http_json(url, headers=None, tries=3):
    for i in range(tries):
        try:
            req = urllib.request.Request(url, headers=dict({"User-Agent": "SwadayaTV-sync/1.0"}, **(headers or {})))
            with urllib.request.urlopen(req, timeout=30) as r:
                return json.loads(r.read().decode("utf-8"))
        except urllib.error.HTTPError as e:
            if e.code == 429 and i < tries - 1:
                time.sleep(65); continue
            print(f"  HTTP {e.code} {url.split('?')[0]}", file=sys.stderr); return None
        except Exception as e:
            print(f"  gagal {type(e).__name__} {url.split('?')[0]}", file=sys.stderr)
            time.sleep(3)
    return None

def parse_time(iso):
    try:
        return int(datetime.fromisoformat(iso.replace("Z", "+00:00")).timestamp() * 1000)
    except Exception:
        return 0

def nama(t):
    return (t.get("shortName") or t.get("name") or "").strip()

def qual_color(code, rank, total):
    if code in ("EPL", "LL", "SA", "BL", "L1"):
        if rank <= 4: return HIJAU
        if rank in (5, 6): return BIRU
        if rank > total - 3: return MERAH
        return 0
    if code == "UCL":
        if rank <= 8: return HIJAU
        if rank <= 24: return BIRU
        return MERAH
    return 0

def convert_table(code, js, teams):
    out = []
    for st in js.get("standings", []):
        if st.get("type") != "TOTAL": continue
        grp = st.get("group") or ""
        grp = grp.replace("_", " ").title() if grp and grp.upper() != "REGULAR_SEASON" else ""
        rows = st.get("table", [])
        for r in rows:
            t = r.get("team", {})
            register_team(teams, t)
            out.append({"n": nama(t), "g": grp, "r": r.get("position", 0), "p": r.get("playedGames", 0),
                        "w": r.get("won", 0), "d": r.get("draw", 0), "l": r.get("lost", 0),
                        "gd": r.get("goalDifference", 0), "pt": r.get("points", 0),
                        "q": qual_color(code, r.get("position", 0), len(rows)) if not grp else 0,
                        "tid": t.get("id", 0)})
    return out

def convert_matches(js, teams):
    out = []
    for m in js.get("matches", []):
        h, a = m.get("homeTeam", {}), m.get("awayTeam", {})
        if not h.get("id") or not a.get("id"): continue   # babak gugur yang lawannya belum pasti
        register_team(teams, h); register_team(teams, a)
        stt = m.get("status", "")
        ft = (m.get("score") or {}).get("fullTime") or {}
        hs, as_ = ft.get("home"), ft.get("away")
        if stt in ("IN_PLAY", "PAUSED", "LIVE"):
            state, det = 1, ("Turun minum" if stt == "PAUSED" else "LIVE")
        elif stt in ("FINISHED", "AWARDED"):
            state, det = 2, "FT"
        elif stt in ("POSTPONED", "SUSPENDED", "CANCELLED"):
            state, det = 3, {"POSTPONED": "Ditunda", "SUSPENDED": "Dihentikan", "CANCELLED": "Dibatalkan"}[stt]
        else:
            state, det = 0, ""
        out.append({"id": str(m.get("id")), "h": nama(h), "a": nama(a), "t": parse_time(m.get("utcDate", "")),
                    "s": state, "hs": "" if hs is None else str(hs), "as": "" if as_ is None else str(as_),
                    "dt": det, "hid": h.get("id", 0), "aid": a.get("id", 0)})
    out.sort(key=lambda x: x["t"])
    return out

def register_team(teams, t):
    tid = str(t.get("id", ""))
    if not tid: return
    e = teams.setdefault(tid, {})
    e["name"] = nama(t)
    if t.get("tla"): e["tla"] = t["tla"]
    if t.get("crest"): e["crest"] = t["crest"]

def load(path, default):
    try:
        with open(path, encoding="utf-8") as f: return json.load(f)
    except Exception:
        return default

def save(path, obj):
    tmp = path + ".tmp"
    with open(tmp, "w", encoding="utf-8") as f:
        json.dump(obj, f, ensure_ascii=False, separators=(",", ":"), sort_keys=False)
    os.replace(tmp, path)

def find_badge(name_full):
    """Logo PNG dari TheSportsDB (kunci publik 123). Mengembalikan URL atau ''."""
    js = http_json(f"{TSDB}/searchteams.php?t={urllib.parse.quote(name_full)}", tries=1)
    for t in (js or {}).get("teams") or []:
        if (t.get("strSport") or "") != "Soccer": continue
        url = t.get("strBadge") or t.get("strTeamBadge") or ""
        if url: return url
    return ""

def fill_badges(teams, overrides, limit=40):
    """Cari logo untuk klub yang belum punya; maksimal `limit` per putaran (batas 30 req/menit)."""
    done = 0
    for tid, e in teams.items():
        if tid in overrides: e.update(overrides[tid])
        if e.get("badge") is not None: continue          # sudah dicari (boleh kosong)
        if done >= limit: break
        e["badge"] = find_badge(e.get("full") or e.get("name", ""))
        done += 1
        time.sleep(2.2)
    return done

def main():
    token = os.environ.get("FOOTBALL_DATA_TOKEN", "").strip()
    if not token:
        print("FOOTBALL_DATA_TOKEN belum diisi di GitHub Secrets", file=sys.stderr); return 1
    os.makedirs(OUT, exist_ok=True)
    teams_path = os.path.join(OUT, "teams.json")
    teams = load(teams_path, {})
    overrides = load(os.path.join(OUT, "team_overrides.json"), {})
    hdr = {"X-Auth-Token": token}
    ok = 0
    for code, fd in COMPETITIONS.items():
        print(f"{code} ({fd})")
        st = http_json(f"{FD}/competitions/{fd}/standings", hdr); time.sleep(7)
        ms = http_json(f"{FD}/competitions/{fd}/matches", hdr);  time.sleep(7)
        if not st and not ms:
            continue
        path = os.path.join(OUT, f"{code}.json")
        old = load(path, {})
        table = convert_table(code, st, teams) if st else old.get("table", [])
        matches = convert_matches(ms, teams) if ms else old.get("matches", [])
        season = ""
        sj = st or ms or {}
        sea = sj.get("season") or {}
        if sea.get("startDate"): season = f'{sea["startDate"][:4]}/{sea.get("endDate", "")[:4]}'
        baru = {"v": 2, "season": season or old.get("season", ""), "source": "football-data.org",
                "fetchedAt": int(time.time() * 1000), "table": table, "matches": matches}
        if old.get("table") == table and old.get("matches") == matches:
            baru["fetchedAt"] = old.get("fetchedAt", baru["fetchedAt"])   # tidak berubah: jangan bikin commit baru
        save(path, baru)
        ok += 1
    n = fill_badges(teams, overrides)
    print(f"logo baru dicari: {n}")
    save(teams_path, teams)
    print(f"selesai: {ok}/{len(COMPETITIONS)} liga")
    return 0 if ok else 1

if __name__ == "__main__":
    sys.exit(main())
