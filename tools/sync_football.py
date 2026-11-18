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

BASE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "data")
OUT = os.path.join(BASE, "football")
FD = "https://api.football-data.org/v4"
TSDB = "https://www.thesportsdb.com/api/v1/json/123"

# kode aplikasi -> kode football-data.org (id liga FotMob dipakai app sebagai kunci pilihan)
COMPETITIONS = {"EPL": "PL", "LL": "PD", "SA": "SA", "BL": "BL1", "L1": "FL1", "UCL": "CL"}
# Liga 1 Indonesia tidak ada di football-data.org. FotMob (id 8983) publik, tanpa kunci.
FOTMOB_IDN = "https://www.fotmob.com/api/data/leagues?id=8983"
FOTMOB_UA = {"User-Agent": "Mozilla/5.0 (Linux; Android TV) SwadayaTV-sync/1.0", "Accept": "application/json"}

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
        # football-data.org mengisi "Matchday"/"League Phase" untuk liga dengan satu tabel.
        # Itu bukan grup sungguhan: kalau ditampilkan, aplikasi memberi judul grup palsu
        # dan warna zona (q) tidak pernah dihitung.
        if grp.upper() in ("", "REGULAR_SEASON", "MATCHDAY", "LEAGUE PHASE"):
            grp = ""
        else:
            grp = grp.replace("_", " ").title()
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

def hex_color(h):
    if not h or not isinstance(h, str) or not h.startswith("#") or len(h) < 7:
        return 0
    try:
        return s32(0xFF000000 | int(h[1:7], 16))
    except Exception:
        return 0

def pecah_skor(s):
    if not s or not isinstance(s, str) or "-" not in s:
        return "", ""
    a, b = s.split("-", 1)
    return a.strip(), b.strip()

def fotmob_laga(status):
    """state, hs, as, detail dari objek status FotMob."""
    if not isinstance(status, dict):
        return 0, "", "", ""
    reason = status.get("reason") or {}
    short = (reason.get("short") or "").strip()
    kunci = (reason.get("shortKey") or "").lower()
    if status.get("cancelled") or kunci in ("cancelled", "canceled", "abandoned"):
        return 3, "", "", "Dibatalkan"
    if kunci in ("postponed", "suspended") or short.upper() in ("PP", "POSTPONED"):
        return 3, "", "", "Dihentikan" if "susp" in kunci else "Ditunda"
    selesai = bool(status.get("finished")) or short.upper() == "FT" or kunci in ("fulltime_short", "finished")
    hs, aws = pecah_skor(status.get("scoreStr") or "")
    if selesai:
        return 2, hs, aws, short or "FT"
    if status.get("started"):
        live = status.get("liveTime") or {}
        det = (live.get("short") if isinstance(live, dict) else None) or short or "LIVE"
        return 1, hs, aws, det
    return 0, "", "", ""

def sync_indonesia(teams):
    """Klasemen + jadwal Liga 1 Indonesia (kode berkas IDN) dari FotMob."""
    print("IDN (FotMob 8983)")
    js = http_json(FOTMOB_IDN, FOTMOB_UA)
    if not js:
        print("  Liga 1 Indonesia gagal diunduh, file lama dibiarkan", file=sys.stderr)
        return False
    details = js.get("details") or {}
    table_src = (((js.get("table") or [{}])[0].get("data") or {}).get("table") or {}).get("all") or []
    if not table_src:
        print("  klasemen FotMob kosong", file=sys.stderr)
        return False
    total = len(table_src)
    table = []
    for r in table_src:
        tid = int(r.get("id") or 0)
        nama_tim = (r.get("shortName") or r.get("name") or "").strip()
        rank = int(r.get("idx") or 0)
        q = hex_color(r.get("qualColor"))
        if not q:
            if rank <= 2: q = HIJAU
            elif rank == 3: q = BIRU
            elif rank > total - 3: q = MERAH
        table.append({"n": nama_tim, "g": "", "r": rank, "p": int(r.get("played") or 0),
                      "w": int(r.get("wins") or 0), "d": int(r.get("draws") or 0), "l": int(r.get("losses") or 0),
                      "gd": int(r.get("goalConDiff") or 0), "pt": int(r.get("pts") or 0), "q": q, "tid": tid})
        if tid:
            e = teams.setdefault(str(tid), {})
            e["name"] = nama_tim
            e.setdefault("badge", f"https://images.fotmob.com/image_resources/logo/teamlogo/{tid}.png")
    matches = []
    for m in (js.get("fixtures") or {}).get("allMatches") or []:
        home, away = m.get("home") or {}, m.get("away") or {}
        try:
            hid, aid = int(home.get("id") or 0), int(away.get("id") or 0)
        except Exception:
            continue
        if not hid or not aid:
            continue
        state, hs, aws, det = fotmob_laga(m.get("status") or {})
        utc = ((m.get("status") or {}).get("utcTime")) or ""
        matches.append({"id": str(m.get("id") or ""), "h": (home.get("shortName") or home.get("name") or "").strip(),
                        "a": (away.get("shortName") or away.get("name") or "").strip(),
                        "t": parse_time(utc), "s": state, "hs": hs, "as": aws, "dt": det, "hid": hid, "aid": aid})
        for tid, tm in ((hid, home), (aid, away)):
            e = teams.setdefault(str(tid), {})
            e["name"] = (tm.get("shortName") or tm.get("name") or e.get("name") or "").strip()
            e.setdefault("badge", f"https://images.fotmob.com/image_resources/logo/teamlogo/{tid}.png")
    matches.sort(key=lambda x: x["t"])
    path = os.path.join(OUT, "IDN.json")
    old = load(path, {})
    season = details.get("selectedSeason") or old.get("season") or ""
    baru = {"v": 2, "season": season, "source": "fotmob", "fetchedAt": int(time.time() * 1000),
            "table": table, "matches": matches}
    if old.get("table") == table and old.get("matches") == matches:
        baru["fetchedAt"] = old.get("fetchedAt", baru["fetchedAt"])
    save(path, baru)
    print(f"  klasemen {len(table)} tim, laga {len(matches)}, musim {season}")
    return True

def main():
    token = os.environ.get("FOOTBALL_DATA_TOKEN", "").strip()
    # Kalau dulu pernah ada berkas bernama "data" (bukan folder), hapus supaya
    # data/football bisa dibuat. Ini penyebab GitHub Action selalu merah.
    if os.path.isfile(BASE):
        print(f"menghapus berkas '{BASE}' karena seharusnya folder", file=sys.stderr)
        os.remove(BASE)
    os.makedirs(OUT, exist_ok=True)
    teams_path = os.path.join(OUT, "teams.json")
    teams = load(teams_path, {})
    overrides = load(os.path.join(OUT, "team_overrides.json"), {})
    ok = 0
    if not token:
        print("FOOTBALL_DATA_TOKEN belum diisi — liga Eropa dilewati, Liga 1 Indonesia tetap diambil", file=sys.stderr)
    else:
        hdr = {"X-Auth-Token": token}
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
                baru["fetchedAt"] = old.get("fetchedAt", baru["fetchedAt"])
            save(path, baru)
            ok += 1
    if sync_indonesia(teams):
        ok += 1
    n = fill_badges(teams, overrides)
    print(f"logo baru dicari: {n}")
    save(teams_path, teams)
    print(f"selesai: {ok}/{len(COMPETITIONS)} liga")
    return 0 if ok else 1

if __name__ == "__main__":
    sys.exit(main())
