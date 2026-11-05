package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Doa harian untuk SwadayaTV: ambil dari GitHub quran-json,
 * simpan cache di SharedPreferences "doa_cache", rotasi harian lokal.
 */
final class DailyDoa {
    private static final String TAG = "DailyDoa";
    private static final String URL = "https://raw.githubusercontent.com/iherbs/quran-json/main/doa_harian.json";
    private static final String PREFS = "doa_cache";
    private static final String KEY_JSON = "raw_json";

    static final class Entry {
        final String title;
        final String arabic;
        final String translation;

        Entry(String title, String arabic, String translation) {
            this.title = title;
            this.arabic = arabic;
            this.translation = translation;
        }
    }

    // Lima belas doa cadangan bila offline / JSON kosong.
    private static final Entry[] FALLBACK = {
        new Entry("DOA HARIAN · QS. AL-BAQARAH: 45",
                "وَاسْتَعِينُوا بِالصَّبْرِ وَالصَّلَاةِ ۚ وَإِنَّهَا لَكَبِيرَةٌ إِلَّا عَلَى الْخَاشِعِينَ",
                "Dan mohonlah pertolongan (kepada Allah) dengan sabar dan salat. Dan (salat) itu sungguh berat, kecuali bagi orang-orang yang khusyuk."),
        new Entry("DOA HARIAN · QS. AL-BAQARAH: 201",
                "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ",
                "Ya Tuhan kami, berilah kami kebaikan di dunia dan kebaikan di akhirat, dan lindungilah kami dari azab neraka."),
        new Entry("DOA HARIAN · QS. ALI 'IMRAN: 8",
                "رَبَّنَا لَا تُزِغْ قُلُوبَنَا بَعْدَ إِذْ هَدَيْتَنَا وَهَبْ لَنَا مِنْ لَدُنْكَ رَحْمَةً ۚ إِنَّكَ أَنْتَ الْوَهَّابُ",
                "Ya Tuhan kami, janganlah Engkau condongkan hati kami kepada kesesatan setelah Engkau beri petunjuk, dan karuniakanlah rahmat dari sisi-Mu."),
        new Entry("DOA HARIAN · QS. AL-ISRA: 24",
                "رَبِّ ارْحَمْهُمَا كَمَا رَبَّيَانِي صَغِيرًا",
                "Wahai Tuhanku, sayangilah kedua orang tuaku sebagaimana mereka berdua telah mendidik aku pada waktu kecil."),
        new Entry("DOA HARIAN · QS. THAHA: 25-28",
                "رَبِّ اشْرَحْ لِي صَدْرِي وَيَسِّرْ لِي أَمْرِي وَاحْلُلْ عُقْدَةً مِنْ لِسَانِي يَفْقَهُوا قَوْلِي",
                "Ya Tuhanku, lapangkanlah dadaku, mudahkanlah urusanku, dan lepaskanlah kekakuan lidahku, agar mereka mengerti perkataanku."),
        new Entry("DOA HARIAN · QS. AL-ANBIYA: 87",
                "لَا إِلَٰهَ إِلَّا أَنْتَ سُبْحَانَكَ إِنِّي كُنْتُ مِنَ الظَّالِمِينَ",
                "Tidak ada tuhan selain Engkau, Mahasuci Engkau. Sungguh, aku termasuk orang-orang yang zalim."),
        new Entry("DOA HARIAN · QS. AL-FURQAN: 74",
                "رَبَّنَا هَبْ لَنَا مِنْ أَزْوَاجِنَا وَذُرِّيَّاتِنَا قُرَّةَ أَعْيُنٍ وَاجْعَلْنَا لِلْمُتَّقِينَ إِمَامًا",
                "Ya Tuhan kami, anugerahkanlah kami pasangan dan keturunan sebagai penyejuk hati, dan jadikanlah kami pemimpin orang bertakwa."),
        new Entry("DOA HARIAN · QS. AL-MU'MINUN: 118",
                "رَبِّ اغْفِرْ وَارْحَمْ وَأَنْتَ خَيْرُ الرَّاحِمِينَ",
                "Ya Tuhanku, berilah ampunan dan berilah rahmat, Engkaulah sebaik-baik pemberi rahmat."),
        new Entry("DOA HARIAN · QS. AL-KAHFI: 10",
                "رَبَّنَا آتِنَا مِنْ لَدُنْكَ رَحْمَةً وَهَيِّئْ لَنَا مِنْ أَمْرِنَا رَشَدًا",
                "Ya Tuhan kami, berikanlah rahmat kepada kami dari sisi-Mu dan sempurnakanlah petunjuk yang lurus bagi kami dalam urusan kami."),
        new Entry("DOA HARIAN · QS. IBRAHIM: 40",
                "رَبِّ اجْعَلْنِي مُقِيمَ الصَّلَاةِ وَمِنْ ذُرِّيَّتِي ۚ رَبَّنَا وَتَقَبَّلْ دُعَاءِ",
                "Ya Tuhanku, jadikanlah aku dan anak cucuku orang yang tetap melaksanakan salat, ya Tuhan kami, perkenankanlah doaku."),
        new Entry("DOA PAGI · HR. IBNU MAJAH",
                "اللَّهُمَّ إِنِّي أَسْأَلُكَ عِلْمًا نَافِعًا وَرِزْقًا طَيِّبًا وَعَمَلًا مُتَقَبَّلًا",
                "Ya Allah, sesungguhnya aku memohon kepada-Mu ilmu yang bermanfaat, rezeki yang baik, dan amal yang diterima."),
        new Entry("DOA HARIAN · HR. BUKHARI",
                "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْهَمِّ وَالْحَزَنِ وَالْعَجْزِ وَالْكَسَلِ وَالْجُبْنِ وَالْبُخْلِ",
                "Ya Allah, aku berlindung kepada-Mu dari rasa gundah dan sedih, dari kelemahan dan kemalasan, serta sifat pengecut dan kikir."),
        new Entry("DOA HARIAN · HR. MUSLIM",
                "اللَّهُمَّ إِنِّي أَسْأَلُكَ الْهُدَى وَالتُّقَى وَالْعَفَافَ وَالْغِنَى",
                "Ya Allah, sesungguhnya aku memohon kepada-Mu petunjuk, ketakwaan, kesucian diri, dan kecukupan."),
        new Entry("DOA HARIAN · HR. IBNU HIBBAN",
                "اللَّهُمَّ لَا سَهْلَ إِلَّا مَا جَعَلْتَهُ سَهْلًا وَأَنْتَ تَجْعَلُ الْحَزْنَ إِذَا شِئْتَ سَهْلًا",
                "Ya Allah, tidak ada kemudahan kecuali apa yang Engkau jadikan mudah, dan Engkau jadikan kesulitan mudah bila Engkau kehendaki."),
        new Entry("DOA HARIAN · QS. AN-NAML: 19",
                "رَبِّ أَوْزِعْنِي أَنْ أَشْكُرَ نِعْمَتَكَ الَّتِي أَنْعَمْتَ عَلَيَّ وَعَلَىٰ وَالِدَيَّ",
                "Ya Tuhanku, anugerahkanlah aku ilham untuk tetap mensyukuri nikmat-Mu yang telah Engkau limpahkan kepadaku dan kedua orang tuaku.")
    };

    private DailyDoa() {}

    /** Rotasi harian lokal berdasarkan hari ke-berapa dalam setahun. */
    static Entry getTodayEntry(Context ctx) {
        List<Entry> daftar = ambilDaftar(ctx);
        int hari = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        return daftar.get(Math.abs(hari) % daftar.size());
    }

    /** Daftar doa: dari cache, kalau kosong pakai cadangan internal. */
    static List<Entry> ambilDaftar(Context ctx) {
        if (ctx != null) {
            try {
                SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
                String mentah = sp.getString(KEY_JSON, null);
                if (mentah != null && !mentah.isEmpty()) {
                    List<Entry> hasil = uraikan(mentah);
                    if (!hasil.isEmpty()) return hasil;
                }
            } catch (Exception e) {
                Log.w(TAG, "cache doa rusak: " + e.getMessage());
            }
        }
        List<Entry> cadangan = new ArrayList<>();
        for (Entry e : FALLBACK) cadangan.add(e);
        return cadangan;
    }

    /** Segarkan cache dari GitHub (jalankan di background). */
    static void Segarkan(Context ctx) {
        final Context app = ctx.getApplicationContext();
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(URL).openConnection();
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.setRequestProperty("User-Agent", "SwadayaTV/1.0");
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String baris;
                while ((baris = br.readLine()) != null) sb.append(baris);
                br.close();
                String json = sb.toString().trim();
                if (!json.isEmpty()) {
                    app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                            .edit().putString(KEY_JSON, json).apply();
                }
            } catch (Exception e) {
                Log.w(TAG, "gagal segarkan doa: " + e.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    /** Uraikan JSON array (atau objek dengan data) jadi daftar Entry. */
    static List<Entry> uraikan(String json) {
        List<Entry> keluar = new ArrayList<>();
        try {
            JSONArray arr = null;
            String t = json.trim();
            if (t.startsWith("[")) {
                arr = new JSONArray(t);
            } else if (t.startsWith("{")) {
                JSONObject obj = new JSONObject(t);
                for (String kunci : new String[]{"data", "doa", "result"}) {
                    if (obj.has(kunci) && obj.optJSONArray(kunci) != null) {
                        arr = obj.optJSONArray(kunci);
                        break;
                    }
                }
            }
            if (arr == null) return keluar;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                // Format quran-json: {no, name, text_ayah, transliteration, text_id}
                // Format lain: {title/judul, arabic/arab, translation/terjemahan}
                String judul = o.optString("title", o.optString("judul", o.optString("name", "")));
                String arab = o.optString("arabic", o.optString("arab", o.optString("ar", o.optString("text_ayah", ""))));
                String arti = o.optString("translation", o.optString("terjemahan",
                        o.optString("text_id", o.optString("id", o.optString("latin", "")))));
                String latin = o.optString("transliteration", "");
                if (judul.isEmpty()) judul = "DOA HARIAN";
                if (arab.isEmpty()) continue;
                if (arti.isEmpty() && latin.isEmpty()) continue;
                if (!latin.isEmpty()) arti = arti + "\n(" + latin + ")";
                keluar.add(new Entry(judul, arab, arti));
            }
        } catch (Exception e) {
            Log.w(TAG, "uraikan doa gagal: " + e.getMessage());
        }
        return keluar;
    }
}
