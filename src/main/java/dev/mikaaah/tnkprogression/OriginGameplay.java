package dev.mikaaah.tnkprogression;

import static dev.mikaaah.tnkprogression.OriginRules.Perk.*;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.GrindstoneEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** Hooks scoped to their source activity, rather than multiplying every XP or healing award. */
public final class OriginGameplay {
    private record Breeding(BabyEntitySpawnEvent event, double reduction) {}
    private static final List<Breeding> BREEDING = new ArrayList<>();
    private static final Map<ServerPlayer, LivingEntity> MOUNTS = new WeakHashMap<>();
    private OriginGameplay() {}

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void damage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;
        float amount = event.getAmount();
        if (event.getEntity() instanceof ServerPlayer player && event.getSource().getEntity() != null
            && event.getSource().getEntity().getType().is(EntityTypeTags.UNDEAD)) amount *= 1 - Origins.bonus(player, UNDEAD_REDUCTION);
        if (event.getSource().getEntity() instanceof ServerPlayer player && ActivityGuard.player(player)) {
            var direct = event.getSource().getDirectEntity();
            boolean ranged = direct instanceof AbstractArrow arrow && arrow.getWeaponItem() != null
                && (arrow.getWeaponItem().getItem() instanceof BowItem || arrow.getWeaponItem().getItem() instanceof CrossbowItem)
                || direct instanceof FireworkRocketEntity rocket && rocket.isShotAtAngle();
            if (ranged) amount *= 1 + Origins.bonus(player, RANGED_DAMAGE);
        }
        event.setAmount(amount);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void killXp(LivingExperienceDropEvent event) {
        if (event.getAttackingPlayer() instanceof ServerPlayer player && ActivityGuard.player(player)
            && event.getEntity().getType().is(EntityTypeTags.UNDEAD))
            event.setDroppedExperience(event.getDroppedExperience() + Origins.extra(player, "undead_xp", event.getDroppedExperience(), Origins.bonus(player, UNDEAD_XP)));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void hunt(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || !ActivityGuard.player(player)) return;
        double fraction = Origins.bonus(player, HUNT_YIELD);
        if (fraction <= 0) return;
        for (ItemEntity drop : List.copyOf(event.getDrops())) {
            ItemStack stack = drop.getItem();
            if (!stack.is(Tags.Items.FOODS_RAW_MEAT) && !stack.is(Tags.Items.FOODS_COOKED_MEAT) && !stack.is(Tags.Items.LEATHERS)) continue;
            int extra = Origins.extra(player, "hunt_yield", stack.getCount(), fraction);
            if (extra > 0) event.getDrops().add(new ItemEntity(player.serverLevel(), drop.getX(), drop.getY(), drop.getZ(), stack.copyWithCount(extra)));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void grindstone(GrindstoneEvent.OnTakeItem event) {
        if (event.getPlayer() instanceof ServerPlayer player && ActivityGuard.player(player))
            event.setXp(event.getXp() + Origins.extra(player, "grindstone_xp", event.getXp(), Origins.bonus(player, GRINDSTONE_XP)));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void breed(BabyEntitySpawnEvent event) {
        if (!(event.getCausedByPlayer() instanceof ServerPlayer player) || !ActivityGuard.player(player) || event.getChild() == null) return;
        double fraction = Origins.bonus(player, BREEDING_REDUCTION);
        if (fraction > 0) BREEDING.add(new Breeding(event, fraction));
    }

    @SubscribeEvent public static void afterBreeding(ServerTickEvent.Post event) {
        for (var receipt : List.copyOf(BREEDING)) {
            if (receipt.event().getParentA().getServer() != event.getServer()) continue;
            BREEDING.remove(receipt);
            if (receipt.event().isCanceled() || receipt.event().getChild() == null) continue;
            for (var parent : List.of(receipt.event().getParentA(), receipt.event().getParentB()))
                if (parent instanceof AgeableMob animal && animal.getAge() > 0) animal.setAge((int)Math.round(animal.getAge() * (1 - receipt.reduction())));
        }
        for (var entry : List.copyOf(MOUNTS.entrySet())) {
            var player = entry.getKey(); var mount = entry.getValue();
            if (mount.getControllingPassenger() != player || !player.isAlive() || player.isRemoved() || Origins.bonus(player, MOUNT_SPEED) <= 0) {
                Origins.modifier(mount.getAttribute(Attributes.MOVEMENT_SPEED), "origin/mount_speed", 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                MOUNTS.remove(player);
            }
        }
        OriginHooks.endDrink();
    }

    @SubscribeEvent public static void visibility(LivingEvent.LivingVisibilityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.isShiftKeyDown() && event.getLookingEntity() instanceof Enemy)
            event.modifyVisibility(1 - Origins.bonus(player, SNEAK_VISIBILITY_REDUCTION));
    }

    @SubscribeEvent public static void potionHeal(LivingHealEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && OriginHooks.drinking(player))
            event.setAmount(event.getAmount() * (float)(1 + Origins.bonus(player, POTION_HEAL)));
    }

    @SubscribeEvent public static void mount(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        LivingEntity current = player.getVehicle() instanceof LivingEntity entity && entity.getControllingPassenger() == player
            && Origins.bonus(player, MOUNT_SPEED) > 0 ? entity : null;
        LivingEntity previous = MOUNTS.get(player);
        if (previous != current && previous != null) Origins.modifier(previous.getAttribute(Attributes.MOVEMENT_SPEED), "origin/mount_speed", 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        if (current == null) MOUNTS.remove(player);
        else {
            Origins.modifier(current.getAttribute(Attributes.MOVEMENT_SPEED), "origin/mount_speed", Origins.bonus(player, MOUNT_SPEED), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            MOUNTS.put(player, current);
        }
    }

    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var mount = MOUNTS.remove(player);
        if (mount != null) Origins.modifier(mount.getAttribute(Attributes.MOVEMENT_SPEED), "origin/mount_speed", 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }

    @SubscribeEvent public static void stopped(ServerStoppedEvent event) {
        BREEDING.clear(); MOUNTS.clear(); OriginHooks.endDrink();
    }
}
