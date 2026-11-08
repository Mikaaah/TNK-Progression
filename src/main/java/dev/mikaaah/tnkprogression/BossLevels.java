package dev.mikaaah.tnkprogression;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.LinkedHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;

/** Registry IDs keep every supported boss mod optional. */
public final class BossLevels {
    public static final int MAX_LEVEL = 110;
    private BossLevels() {}

    public static Map<String, Integer> defaults() {
        var stream = BossLevels.class.getResourceAsStream("/data/tnkprogression/boss_levels.json");
        if (stream == null) throw new IllegalStateException("Missing boss level defaults");
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            Map<String, Integer> levels = new Gson().fromJson(reader, new TypeToken<LinkedHashMap<String, Integer>>() {}.getType());
            return levels;
        } catch (java.io.IOException ex) { throw new IllegalStateException("Cannot read boss level defaults", ex); }
    }

    public static int baseLevel(Mob mob) {
        // A wild boss becoming a player's companion must not receive new boss scaling.
        if (mob instanceof OwnableEntity owned && owned.getOwnerUUID() != null) return 0;
        return ProgressionConfig.current.bossBaseLevels.getOrDefault(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString(), 0);
    }
}
