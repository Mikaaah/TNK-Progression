package dev.mikaaah.tnkprogression.mixin;

import dev.mikaaah.tnkprogression.Origins;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class OriginEnchantingMixin {
    @Unique private long tnk$originXpBefore = -1;

    @Inject(method = "onEnchantmentPerformed", at = @At("HEAD"))
    private void tnk$captureEnchantingCost(ItemStack stack, int levels, CallbackInfo callback) {
        tnk$originXpBefore = levels > 0 ? Origins.captureXp((Player) (Object) this, false) : -1;
    }

    @Inject(method = "onEnchantmentPerformed", at = @At("RETURN"))
    private void tnk$refundEnchantingCost(ItemStack stack, int levels, CallbackInfo callback) {
        long before = tnk$originXpBefore;
        tnk$originXpBefore = -1;
        Origins.refundXp((Player) (Object) this, false, before);
    }
}
