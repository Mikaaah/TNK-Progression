package dev.mikaaah.tnkprogression.mixin;

import dev.mikaaah.tnkprogression.Origins;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilMenu.class)
public abstract class OriginAnvilMixin {
    @Unique private long tnk$originXpBefore = -1;

    @Inject(method = "onTake", at = @At("HEAD"))
    private void tnk$captureAnvilCost(Player player, ItemStack stack, CallbackInfo callback) {
        tnk$originXpBefore = Origins.captureXp(player, true);
    }

    @Inject(method = "onTake", at = @At("RETURN"))
    private void tnk$refundAnvilCost(Player player, ItemStack stack, CallbackInfo callback) {
        long before = tnk$originXpBefore;
        tnk$originXpBefore = -1;
        Origins.refundXp(player, true, before);
    }
}
