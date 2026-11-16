package dev.mikaaah.tnkprogression;

import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;

/** Registry IDs keep every supported boss mod optional. */
public final class BossLevels {
    public static final int MAX_LEVEL = 110;
    private BossLevels() {}

    public static Map<String, Integer> defaults() {
        return BossLevelDefaults.levels();
    }

    public static boolean apotheosisEncounter(Mob mob) {
        // Apotheosis 1.21 marks elites with a registry-id string before world insertion,
        // then a boolean after its LOWEST-priority join listener initializes them.
        var data = mob.getPersistentData();
        return isInvader(mob) || data.getBoolean("apoth.miniboss")
                || (data.contains("apoth.miniboss", 8)
                    && net.minecraft.resources.ResourceLocation.tryParse(data.getString("apoth.miniboss")) != null);
    }

    public static boolean isInvader(Mob mob) {
        return mob.getPersistentData().getBoolean("apoth.boss");
    }

    public static boolean isMiniboss(Mob mob) {
        if (mob instanceof OwnableEntity owned && owned.getOwnerUUID() != null) return false;
        return apotheosisEncounter(mob) || ProgressionConfig.current.minibosses.contains(
                BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString());
    }

    public static int baseLevel(Mob mob) {
        // A wild boss becoming a player's companion must not receive new boss scaling.
        if (mob instanceof OwnableEntity owned && owned.getOwnerUUID() != null) return 0;
        return ProgressionConfig.current.bossBaseLevels.getOrDefault(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString(), 0);
    }
}
