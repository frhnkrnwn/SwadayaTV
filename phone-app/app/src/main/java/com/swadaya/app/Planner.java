package com.swadaya.app;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The plan: what is installed, known and safe to switch off, grouped by category. Everything the
 * app refuses to touch (Home's owner, the keyboard, TV inputs, Android's own parts) is listed as
 * kept, with the reason, so nothing disappears silently.
 */
final class Planner {

    static final class Item {
        final String pkg;
        final String name;
        final String group;

        Item(String pkg, String name, String group) {
            this.pkg = pkg;
            this.name = name;
            this.group = group;
        }
    }

    static final class Kept {
        final String pkg;
        final String name;
        final String why;

        Kept(String pkg, String name, String why) {
            this.pkg = pkg;
            this.name = name;
            this.why = why;
        }
    }

    final List<Item> items = new ArrayList<>();
    final List<Kept> kept = new ArrayList<>();
    final Map<String, List<Item>> byGroup = new LinkedHashMap<>();
    final Set<String> groups = new LinkedHashSet<>();
    int installedKnown;

    List<Item> itemsIn(Set<String> selected) {
        List<Item> out = new ArrayList<>();
        for (Item i : items) {
            if (selected.contains(i.group)) {
                out.add(i);
            }
        }
        return out;
    }

    /** Packages never switched off, whatever a list says. */
    private static final String[] PROTECTED = {
            "android",
            "com.android.systemui",
            "com.android.shell",
            "com.android.settings",
            "com.android.tv.settings",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
            "com.android.providers.settings",
            "com.android.providers.media",
            "com.android.providers.downloads",
            "com.android.providers.contacts",
            "com.android.location.fused",
            "com.android.sdksandbox",
            "com.google.android.gms",
            "com.google.android.gsf",
            "com.android.vending",
            "com.google.android.webview",
            "com.google.android.tts",
            "com.android.inputmethod.latin",
            "com.google.android.inputmethod.latin",
            "com.google.android.tungsten.setupwraith",
            "com.android.tv.frameworkpackagestubs",
    };

    static Planner build(Catalog cat, List<String> enabled, List<String> disabled,
                          String brand, String homeOwner, List<String> homeClaims, String keyboard) {
        Planner p = new Planner();
        Set<String> on = new LinkedHashSet<>(enabled);
        Set<String> off = new LinkedHashSet<>(disabled);
        String brandText = brand == null ? "" : brand.toLowerCase();
        String home = homeOwner == null ? "" : homeOwner;
        String kb = keyboard == null ? "" : keyboard;

        // Which Home claimants stay enabled: everything installed and enabled except the owner,
        // which only goes when another claimant is installed and staying.
        List<String> enabledHomes = new ArrayList<>();
        for (String h : homeClaims) {
            if (on.contains(h)) {
                enabledHomes.add(h);
            }
        }
        boolean canSwitchHome = false;
        for (String h : enabledHomes) {
            if (!h.equals(home)) {
                canSwitchHome = true;
                break;
            }
        }

        for (Catalog.App a : cat.apps) {
            boolean installed = on.contains(a.pkg) || off.contains(a.pkg);
            if (!installed) {
                continue;
            }
            p.installedKnown++;
            boolean brandOk = a.brands == null;
            if (!brandOk) {
                for (String b : a.brands) {
                    if (brandText.contains(b)) {
                        brandOk = true;
                        break;
                    }
                }
            }
            if (!brandOk) {
                continue;
            }
            if (off.contains(a.pkg)) {
                p.kept.add(new Kept(a.pkg, a.name, "sudah nonaktif"));
                continue;
            }
            if (a.keep || isProtected(a.pkg)) {
                p.kept.add(new Kept(a.pkg, a.name, "dilindungi"));
                continue;
            }
            if (a.pkg.equals(kb)) {
                p.kept.add(new Kept(a.pkg, a.name, "keyboard aktif"));
                continue;
            }
            if (a.pkg.equals(home)) {
                if (!canSwitchHome) {
                    p.kept.add(new Kept(a.pkg, a.name, "satu-satunya Home; pasang SwadayaTV dulu"));
                } else {
                    p.items.add(new Item(a.pkg, a.name, a.group));
                }
                continue;
            }
            p.items.add(new Item(a.pkg, a.name, a.group));
        }

        for (Catalog.Group g : cat.groups) {
            List<Item> list = new ArrayList<>();
            for (Item i : p.items) {
                if (i.group.equals(g.id)) {
                    list.add(i);
                }
            }
            if (!list.isEmpty()) {
                p.byGroup.put(g.id, list);
                p.groups.add(g.id);
            }
        }
        return p;
    }

    static boolean isProtected(String pkg) {
        for (String s : PROTECTED) {
            if (s.equals(pkg)) {
                return true;
            }
        }
        return false;
    }
}
