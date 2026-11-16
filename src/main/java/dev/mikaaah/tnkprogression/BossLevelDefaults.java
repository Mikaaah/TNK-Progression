package dev.mikaaah.tnkprogression;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Resource-backed defaults and one-time migration, without optional mod class loading. */
public final class BossLevelDefaults {
    public static final int REVISION = 2;
    private BossLevelDefaults() {}

    public static Map<String, Integer> levels() {
        return read("boss_levels.json", new TypeToken<LinkedHashMap<String, Integer>>() {});
    }

    public static Set<String> minibosses() {
        return read("minibosses.json", new TypeToken<LinkedHashSet<String>>() {});
    }

    private static <T> T read(String file, TypeToken<T> type) {
        var stream = BossLevelDefaults.class.getResourceAsStream("/data/tnkprogression/" + file);
        if (stream == null) throw new IllegalStateException("Missing boss defaults: " + file);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return new Gson().fromJson(reader, type.getType());
        } catch (java.io.IOException ex) { throw new IllegalStateException("Cannot read boss defaults", ex); }
    }

    /** Change only previous shipped values; custom values and intentionally removed entries survive. */
    public static void migrate(Map<String, Integer> configured) {
        Map<String, Integer> previous = read("boss_levels_v1.json", new TypeToken<LinkedHashMap<String, Integer>>() {});
        var current = levels();
        previous.forEach((id, old) -> {
            Integer replacement = current.get(id);
            if (replacement != null && !replacement.equals(old)) configured.replace(id, old, replacement);
        });
        // A newly supported boss is added only when it never existed in the old roster.
        current.forEach((id, value) -> { if (!previous.containsKey(id)) configured.putIfAbsent(id, value); });
    }
}
