package dev.mikaaah.tnkprogression;

import java.util.List;
import static dev.mikaaah.tnkprogression.OriginRules.Perk.*;
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
        Attributes.MAX_HEALTH, Attributes.ATTACK_DAMAGE, Attributes.ARMOR, Attributes.MOVEMENT_SPEED, Attributes.LUCK, Attributes.KNOCKBACK_RESISTANCE, Attributes.SNEAKING_SPEED,
        net.neoforged.neoforge.common.NeoForgeMod.SWIM_SPEED);

    private Origins() {}

    public static String selected(ServerPlayer player) { return ProgressionAPI.profile(player).getString(KEY); }

    public static int revision(ServerPlayer player) { return Math.max(1, ProgressionAPI.profile(player).getInt("origin_revision")); }

    /** Server profiles are authoritative; remote client players never borrow the local player's bonuses. */
    public static double bonus(net.minecraft.world.entity.player.Player player, OriginRules.Perk perk) {
        if (player instanceof ServerPlayer serverPlayer) {
            if (serverPlayer.server == null || serverPlayer.server.overworld() == null) return 0;
            return OriginRules.bonus(selected(serverPlayer), revision(serverPlayer), perk);
        }
        var state = CharacterClientState.current;
        return player.isLocalPlayer() && state != null ? OriginRules.bonus(state.origin(), state.originRevision(), perk) : 0;
    }

    public static boolean manaAvailable(ServerPlayer player) {
        return manaAttribute(player, "max_mana") != null && manaAttribute(player, "mana_regen") != null;
    }

    private static net.minecraft.world.entity.ai.attributes.AttributeInstance manaAttribute(ServerPlayer player, String path) {
        return net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse("irons_spellbooks:" + path))
            .map(player::getAttribute).orElse(null);
    }

    public static boolean ascendedUnlocked(ServerPlayer player){return ProgressionAPI.profile(player).getBoolean("ascended_origins_unlocked");}
    public static void ascend(ServerPlayer player){var n=ProgressionAPI.profile(player);n.putBoolean("ascended_origins_unlocked",true);n.putLong("origin_selection_request",n.getLong("origin_selection_request")+1);adminReset(player);open(player);}
    public static boolean available(ServerPlayer player,String id){var origin=OriginRules.find(id);if(origin.isEmpty() || OriginRules.isAscended(id)&&!ascendedUnlocked(player))return false;var perks=origin.get().bonuses();return (!perks.containsKey(OriginRules.Perk.MANA)&&!perks.containsKey(OriginRules.Perk.MANA_REGEN))||manaAvailable(player);}


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
        if (!OriginRules.mayChoose(profile.getBoolean("origin_prompted"), profile.contains(KEY), id, ascendedUnlocked(player)) || !available(player, id)) return false;
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
        profile.remove("origin_revision");
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
        profile.putInt("origin_revision", 2);
        profile.putBoolean("origin_prompted", true);
        profile.putLong("origin_chosen_at", player.server.overworld().getGameTime());
        clearRefunds(profile);
        ProgressionData.get(player.server).setDirty();
        apply(player);
        sync(player);
    }

    private static void clearRefunds(CompoundTag profile) {
        for (String key : java.util.Set.copyOf(profile.getAllKeys()))
            if (key.startsWith("origin_credit/") || key.equals("origin_enchant_credit") || key.equals("origin_anvil_credit")) profile.remove(key);
    }

    public static int extra(ServerPlayer player, String source, int amount, double fraction) {
        if (fraction <= 0 || amount <= 0) return 0;
        var profile = ProgressionAPI.profile(player);
        String key = "origin_credit/" + source;
        var result = OriginRules.extra(amount, fraction, profile.getDouble(key));
        profile.putDouble(key, result.remainder());
        ProgressionData.get(player.server).setDirty();
        return result.points();
    }

    private static void sync(ServerPlayer player) {
        if (player.connection != null) CharacterState.sendFull(player);
    }

    public static void apply(ServerPlayer player) {
        for (Holder<Attribute> type : ATTRIBUTES) {
            double amount = 0;
            var operation = AttributeModifier.Operation.ADD_VALUE;
            if (type.equals(Attributes.MAX_HEALTH)) amount = bonus(player, HEALTH);
            if (type.equals(Attributes.ATTACK_DAMAGE)) amount = bonus(player, ATTACK);
            if (type.equals(Attributes.ARMOR)) amount = bonus(player, ARMOUR);
            if (type.equals(Attributes.LUCK)) amount = bonus(player, LUCK);
            if (type.equals(Attributes.KNOCKBACK_RESISTANCE)) amount = bonus(player, KNOCKBACK_RESISTANCE);
            if (type.equals(Attributes.MOVEMENT_SPEED)) {
                amount = bonus(player, FLAT_MOVE);
                if (bonus(player, MOVE_SPEED) > 0) { amount = bonus(player, MOVE_SPEED); operation = AttributeModifier.Operation.ADD_MULTIPLIED_BASE; }
            }
            if (type.equals(Attributes.SNEAKING_SPEED)) { amount = bonus(player, SNEAK_SPEED); operation = AttributeModifier.Operation.ADD_MULTIPLIED_BASE; }
            if (type.equals(net.neoforged.neoforge.common.NeoForgeMod.SWIM_SPEED)) { amount = bonus(player, SWIM_SPEED); operation = AttributeModifier.Operation.ADD_MULTIPLIED_BASE; }
            modifier(player.getAttribute(type), "origin/" + type.unwrapKey().orElseThrow().location().getPath(), amount, operation);
        }
        modifier(manaAttribute(player, "max_mana"), "origin/max_mana", bonus(player, MANA), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        modifier(manaAttribute(player, "mana_regen"), "origin/mana_regen", bonus(player, MANA_REGEN), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    static void modifier(net.minecraft.world.entity.ai.attributes.AttributeInstance attribute, String path, double amount, AttributeModifier.Operation operation) {
        if (attribute == null) return;
        var id = ResourceLocation.fromNamespaceAndPath("tnkprogression", path);
        var previous = attribute.getModifier(id);
        if (previous != null && previous.amount() == amount && previous.operation() == operation) return;
        if (previous != null) attribute.removeModifier(id);
        if (amount != 0) attribute.addTransientModifier(new AttributeModifier(id, amount, operation));
    }

    public static long experiencePoints(ServerPlayer player) {
        return Math.min(Integer.MAX_VALUE, OriginRules.pointsAtLevel(player.experienceLevel)
            + Math.round((double) player.experienceProgress * player.getXpNeededForNextLevel()));
    }

    public static long captureXp(net.minecraft.world.entity.player.Player player, boolean anvil) {
        if (!(player instanceof ServerPlayer serverPlayer) || !ActivityGuard.player(serverPlayer)) return -1;
        double fraction = anvil ? bonus(serverPlayer, ANVIL_REFUND) : bonus(serverPlayer, ENCHANT_REFUND);
        return fraction > 0 ? experiencePoints(serverPlayer) : -1;
    }

    public static long captureAnvilXp(net.minecraft.world.entity.player.Player player, net.minecraft.world.item.ItemStack input, net.minecraft.world.item.ItemStack output) {
        long legacy = captureXp(player, true);
        if (legacy >= 0) return legacy;
        if (!(player instanceof ServerPlayer serverPlayer) || !ActivityGuard.player(serverPlayer)) return -1;
        return bonus(player, ANVIL_REPAIR_REFUND) > 0 && input.isDamageableItem() && input.is(output.getItem())
            && output.getDamageValue() < input.getDamageValue() ? experiencePoints(serverPlayer) : -1;
    }

    /** Refunds only the measured XP actually spent, leaving offer strength and required levels intact. */
    public static void refundXp(net.minecraft.world.entity.player.Player player, boolean anvil, long before) {
        if (before < 0 || !(player instanceof ServerPlayer serverPlayer) || !ActivityGuard.player(serverPlayer)) return;
        long spent = before - experiencePoints(serverPlayer);
        if (spent <= 0) return;
        String origin = selected(serverPlayer);
        double fraction = anvil ? bonus(serverPlayer, ANVIL_REFUND) + bonus(serverPlayer, ANVIL_REPAIR_REFUND) : bonus(serverPlayer, ENCHANT_REFUND);
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
