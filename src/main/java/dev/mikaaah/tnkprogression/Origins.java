package dev.mikaaah.tnkprogression;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** One UUID-owned origin per saved character. Ordinary selection can only fill an empty slot. */
public final class Origins {
    public static final String KEY = "origin_id";
    private static final List<Holder<Attribute>> ATTRIBUTES = List.of(
        Attributes.MAX_HEALTH, Attributes.ATTACK_DAMAGE, Attributes.ARMOR, Attributes.MOVEMENT_SPEED, Attributes.LUCK);

    private Origins() {}

    public static String selected(ServerPlayer player) { return ProgressionAPI.profile(player).getString(KEY); }

    public static boolean open(ServerPlayer player) {
        CompoundTag profile = ProgressionAPI.profile(player);
        if (!profile.getBoolean("origin_prompted")) {
            profile.putBoolean("origin_prompted", true);
            ProgressionData.get(player.server).setDirty();
        }
        return true;
    }

    public static boolean choose(ServerPlayer player, String id) {
        CompoundTag profile = ProgressionAPI.profile(player);
        if (!OriginRules.mayChoose(profile.getBoolean("origin_prompted"), profile.contains(KEY), id)) return false;
        store(player, id);
        return true;
    }

    /** Called only by permission-level-2 commands and opt-in development fixtures, never by a packet. */
    static boolean adminSet(ServerPlayer player, String id) {
        if (OriginRules.find(id).isEmpty()) return false;
        store(player, id);
        return true;
    }

    static void adminReset(ServerPlayer player) {
        CompoundTag profile = ProgressionAPI.profile(player);
        profile.remove(KEY);
        profile.remove("origin_prompted");
        profile.remove("origin_chosen_at");
        clearRefunds(profile);
        ProgressionData.get(player.server).setDirty();
        apply(player);
        sync(player);
    }

    private static void store(ServerPlayer player, String id) {
        CompoundTag profile = ProgressionAPI.profile(player);
        profile.putString(KEY, id);
        profile.putBoolean("origin_prompted", true);
        profile.putLong("origin_chosen_at", player.server.overworld().getGameTime());
        clearRefunds(profile);
        ProgressionData.get(player.server).setDirty();
        apply(player);
        sync(player);
    }

    private static void clearRefunds(CompoundTag profile) {
        profile.remove("origin_enchant_credit");
        profile.remove("origin_anvil_credit");
    }

    private static void sync(ServerPlayer player) {
        if (player.connection != null) CharacterState.sendFull(player);
    }

    public static void apply(ServerPlayer player) {
        String origin = selected(player);
        for (Holder<Attribute> type : ATTRIBUTES) {
            var attribute = player.getAttribute(type);
            if (attribute == null) continue;
            double amount = 0;
            if (type.equals(Attributes.MAX_HEALTH) && origin.equals("tnk:farmer")) amount = 1;
            if (type.equals(Attributes.ATTACK_DAMAGE) && origin.equals("tnk:soldier")) amount = 1;
            if (type.equals(Attributes.ARMOR) && (origin.equals("tnk:soldier") || origin.equals("tnk:blacksmith"))) amount = 1;
            if (type.equals(Attributes.MOVEMENT_SPEED) && origin.equals("tnk:wanderer")) amount = 0.005;
            if (type.equals(Attributes.LUCK) && origin.equals("tnk:fisher")) amount = 1;
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("tnkprogression", "origin/" + type.unwrapKey().orElseThrow().location().getPath());
            var previous = attribute.getModifier(id);
            if (previous != null && previous.amount() == amount && previous.operation() == AttributeModifier.Operation.ADD_VALUE) continue;
            if (previous != null) attribute.removeModifier(id);
            if (amount != 0) attribute.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
        }
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    public static long experiencePoints(ServerPlayer player) {
        return Math.min(Integer.MAX_VALUE, OriginRules.pointsAtLevel(player.experienceLevel)
            + Math.round((double) player.experienceProgress * player.getXpNeededForNextLevel()));
    }

    public static long captureXp(net.minecraft.world.entity.player.Player player, boolean anvil) {
        if (!(player instanceof ServerPlayer serverPlayer) || !ActivityGuard.player(serverPlayer)) return -1;
        double fraction = anvil ? OriginRules.anvilRefund(selected(serverPlayer)) : OriginRules.enchantingRefund(selected(serverPlayer));
        return fraction > 0 ? experiencePoints(serverPlayer) : -1;
    }

    /** Refunds only the measured XP actually spent, leaving offer strength and required levels intact. */
    public static void refundXp(net.minecraft.world.entity.player.Player player, boolean anvil, long before) {
        if (before < 0 || !(player instanceof ServerPlayer serverPlayer) || !ActivityGuard.player(serverPlayer)) return;
        long spent = before - experiencePoints(serverPlayer);
        if (spent <= 0) return;
        String origin = selected(serverPlayer);
        double fraction = anvil ? OriginRules.anvilRefund(origin) : OriginRules.enchantingRefund(origin);
        if (fraction <= 0) return;
        String key = anvil ? "origin_anvil_credit" : "origin_enchant_credit";
        CompoundTag profile = ProgressionAPI.profile(serverPlayer);
        OriginRules.Refund refund = OriginRules.refund(spent, fraction, profile.getDouble(key));
        profile.putDouble(key, refund.remainder());
        ProgressionData.get(serverPlayer.server).setDirty();
        if (refund.points() > 0) {
            // A refund is not a new XP award: bypass positive-XP multipliers and their reward callbacks.
            long target = Math.min(Integer.MAX_VALUE, experiencePoints(serverPlayer) + refund.points());
            int level = OriginRules.levelForPoints(target);
            serverPlayer.setExperienceLevels(level);
            serverPlayer.setExperiencePoints((int) (target - OriginRules.pointsAtLevel(level)));
            serverPlayer.totalExperience = (int) Math.clamp((long) serverPlayer.totalExperience + refund.points(), 0, Integer.MAX_VALUE);
        }
    }

    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) apply(player);
    }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) apply(player);
    }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) apply(player);
    }
}
