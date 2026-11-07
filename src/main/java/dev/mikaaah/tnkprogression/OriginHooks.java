package dev.mikaaah.tnkprogression;

import static dev.mikaaah.tnkprogression.OriginRules.Perk.*;
import dev.mikaaah.tnkprogression.mixin.OriginEffectAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.Tags;

/** Small, side-safe operations called only at verified vanilla activity boundaries. */
public final class OriginHooks {
    private static final ThreadLocal<LivingEntity> DRINKING = new ThreadLocal<>();
    private static final TagKey<Item> WRENCHES = TagKey.create(Registries.ITEM, ResourceLocation.parse("tnkprogression:wrenches"));
    private OriginHooks() {}

    public static void beginDrink(LivingEntity entity) { if (!entity.level().isClientSide) DRINKING.set(entity); }
    public static void endDrink() { DRINKING.remove(); }
    public static boolean drinking(LivingEntity entity) { return DRINKING.get() == entity; }

    public static MobEffectInstance effect(Player player, MobEffectInstance original) {
        double scale = 1;
        if (original.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) scale -= Origins.bonus(player, HARMFUL_DURATION_REDUCTION);
        else if (drinking(player) && original.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL) scale += Origins.bonus(player, POTION_DURATION);
        if (scale == 1 || original.isInfiniteDuration()) return original;
        return copyDuration(original, scale);
    }

    private static MobEffectInstance copyDuration(MobEffectInstance original, double scale) {
        var copy = new MobEffectInstance(original);
        var access = (OriginEffectAccess)(Object)copy;
        access.tnk$duration(original.mapDuration(ticks -> Math.max(1, (int)Math.min(Integer.MAX_VALUE, Math.round(ticks * scale)))));
        var hidden = ((OriginEffectAccess)(Object)original).tnk$hidden();
        if (hidden != null) access.tnk$hidden(copyDuration(hidden, scale));
        return copy;
    }

    public static int weaponDamage(ItemStack stack, LivingEntity entity, int damage) {
        if (!(entity instanceof ServerPlayer player) || !ActivityGuard.player(player) || damage <= 0) return damage;
        if (!stack.is(Tags.Items.MELEE_WEAPON_TOOLS) && !stack.is(Tags.Items.RANGED_WEAPON_TOOLS)) return damage;
        return damage - Origins.extra(player, "weapon_durability", damage, Origins.bonus(player, WEAPON_DURABILITY_REDUCTION));
    }

    public static double reach(Player player) {
        double amount = 0;
        if (player.getMainHandItem().getItem() instanceof BlockItem || player.getOffhandItem().getItem() instanceof BlockItem) amount = Origins.bonus(player, PLACEMENT_REACH);
        if (player.getMainHandItem().is(WRENCHES) || player.getOffhandItem().is(WRENCHES)) amount += Origins.bonus(player, WRENCH_REACH);
        return amount;
    }
}
