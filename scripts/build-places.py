#!/usr/bin/env python3
"""Builds the launcher's Indonesian places list (BMKG adm4 codes + coordinates).

Reads BMKG's open region list (base.csv, Kemendagri codes), picks one kelurahan per
city, asks the BMKG forecast API for that kelurahan's coordinates, and writes
launcher/app/src/main/assets/places.json. Run once; the result is committed.
"""
import csv
import json
import time
import urllib.request

CSV = "/tmp/base.csv"
OUT = "launcher/app/src/main/assets/places.json"

# city name | province | city (adm2) code | centre kecamatan name (optional)
CITIES = [
    ("Banda Aceh", "Aceh", "11.71", None),
    ("Medan", "Sumatera Utara", "12.71", None),
    ("Padang", "Sumatera Barat", "13.71", None),
    ("Pekanbaru", "Riau", "14.71", None),
    ("Jambi", "Jambi", "15.71", None),
    ("Palembang", "Sumatera Selatan", "16.71", None),
    ("Bengkulu", "Bengkulu", "17.71", None),
    ("Bandar Lampung", "Lampung", "18.71", None),
    ("Pangkal Pinang", "Kepulauan Bangka Belitung", "19.71", None),
    ("Batam", "Kepulauan Riau", "21.71", "Lubuk Baja"),
    ("Tanjung Pinang", "Kepulauan Riau", "21.72", None),
    ("Jakarta", "DKI Jakarta", "31.71", None),
    ("Bogor", "Jawa Barat", "32.71", None),
    ("Sukabumi", "Jawa Barat", "32.72", None),
    ("Bandung", "Jawa Barat", "32.73", None),
    ("Cirebon", "Jawa Barat", "32.74", None),
    ("Bekasi", "Jawa Barat", "32.75", None),
    ("Depok", "Jawa Barat", "32.76", None),
    ("Magelang", "Jawa Tengah", "33.71", None),
    ("Surakarta", "Jawa Tengah", "33.72", None),
    ("Semarang", "Jawa Tengah", "33.74", None),
    ("Yogyakarta", "DI Yogyakarta", "34.71", None),
    ("Kediri", "Jawa Timur", "35.71", None),
    ("Malang", "Jawa Timur", "35.73", None),
    ("Surabaya", "Jawa Timur", "35.78", None),
    ("Tangerang", "Banten", "36.71", None),
    ("Cilegon", "Banten", "36.72", None),
    ("Serang", "Banten", "36.73", None),
    ("Tangerang Selatan", "Banten", "36.74", None),
    ("Denpasar", "Bali", "51.71", None),
    ("Mataram", "Nusa Tenggara Barat", "52.71", None),
    ("Kupang", "Nusa Tenggara Timur", "53.71", None),
    ("Pontianak", "Kalimantan Barat", "61.71", None),
    ("Singkawang", "Kalimantan Barat", "61.72", None),
    ("Palangka Raya", "Kalimantan Tengah", "62.71", None),
    ("Banjarmasin", "Kalimantan Selatan", "63.71", None),
    ("Banjarbaru", "Kalimantan Selatan", "63.72", None),
    ("Balikpapan", "Kalimantan Timur", "64.71", None),
    ("Samarinda", "Kalimantan Timur", "64.72", None),
    ("Bontang", "Kalimantan Timur", "64.74", None),
    ("Tarakan", "Kalimantan Utara", "65.71", None),
    ("Tanjung Selor", "Kalimantan Utara", "65.01", "Tanjung Selor"),
    ("Manado", "Sulawesi Utara", "71.71", None),
    ("Bitung", "Sulawesi Utara", "71.72", None),
    ("Tomohon", "Sulawesi Utara", "71.73", None),
    ("Palu", "Sulawesi Tengah", "72.71", None),
    ("Makassar", "Sulawesi Selatan", "73.71", None),
    ("Palopo", "Sulawesi Selatan", "73.73", None),
    ("Kendari", "Sulawesi Tenggara", "74.71", None),
    ("Baubau", "Sulawesi Tenggara", "74.72", None),
    ("Gorontalo", "Gorontalo", "75.71", None),
    ("Mamuju", "Sulawesi Barat", "76.02", "Mamuju"),
    ("Ambon", "Maluku", "81.71", None),
    ("Tual", "Maluku", "81.72", None),
    ("Ternate", "Maluku Utara", "82.71", None),
        ("Jayapura", "Papua", "91.71", None),
    ("Manokwari", "Papua Barat", "92.02", "Manokwari Barat"),
    ("Sorong", "Papua Barat Daya", "92.71", None),
    ("Merauke", "Papua Selatan", "91.01", "Merauke"),
    ("Nabire", "Papua Tengah", "91.04", "Nabire"),
    ("Timika", "Papua Tengah", "91.09", "Mimika Baru"),
    ("Wamena", "Papua Pegunungan", "91.02", "Wamena"),
]


def read_regions():
    import os
    if not os.path.exists(CSV):
        # BMKG's open list of Kemendagri region codes (the same codes its forecast API takes).
        urllib.request.urlretrieve(
            "https://raw.githubusercontent.com/dwirx/bmkg-api/main/base.csv", CSV)
    rows = []
    with open(CSV, newline="", encoding="utf-8") as f:
        for code, name in csv.reader(f):
            code, name = code.strip(), name.strip()
            if code:
                rows.append((code, name))
    return rows


def pick_village(rows, adm2, centre):
    kecs = [(c, n) for c, n in rows if c.count(".") == 2 and c.startswith(adm2 + ".")]
    if not kecs:
        raise SystemExit("no kecamatan for " + adm2)
    chosen = None
    if centre:
        for c, n in kecs:
            if n.lower() == centre.lower():
                chosen = c
                break
    if chosen is None:
        chosen = kecs[0][0]
    villages = [c for c, n in rows if c.count(".") == 3 and c.startswith(chosen + ".")]
    if not villages:
        raise SystemExit("no kelurahan for " + chosen)
    return villages[0]


def bmkg(adm4):
    url = "https://api.bmkg.go.id/publik/prakiraan-cuaca?adm4=" + adm4
    req = urllib.request.Request(url, headers={"User-Agent": "SwadayaTV/1.0 (+https://github.com/frhnkrnwn/SwadayaTV)"})
    with urllib.request.urlopen(req, timeout=30) as r:
        return json.load(r)


def main():
    rows = read_regions()
    out = []
    for city, province, adm2, centre in CITIES:
        adm4 = pick_village(rows, adm2, centre)
        try:
            data = bmkg(adm4)
            loc = data["data"][0]["lokasi"]
            if loc.get("adm4") != adm4:
                raise ValueError("adm4 mismatch: " + str(loc.get("adm4")))
        except Exception as e:
            print("FAILED", city, adm4, e)
            continue
        out.append({
            "id": adm4,
            "name": city,
            "province": province,
            "lat": round(float(loc["lat"]), 5),
            "lon": round(float(loc["lon"]), 5),
        })
        print("ok", city, adm4, loc.get("kecamatan"))
        time.sleep(1.1)
    with open(OUT, "w", encoding="utf-8") as f:
        json.dump(out, f, ensure_ascii=False, indent=1)
        f.write("\n")
    print("wrote", len(out), "places")


if __name__ == "__main__":
    main()
