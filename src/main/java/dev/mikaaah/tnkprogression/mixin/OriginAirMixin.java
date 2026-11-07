package dev.mikaaah.tnkprogression.mixin;
import dev.mikaaah.tnkprogression.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Entity.class)
public abstract class OriginAirMixin {
    @Inject(method="getMaxAirSupply", at=@At("RETURN"), cancellable=true)
    private void tnk$breath(CallbackInfoReturnable<Integer> callback) {
        if ((Object)this instanceof Player player) callback.setReturnValue(callback.getReturnValue() + (int)Origins.bonus(player, OriginRules.Perk.AIR_TICKS));
    }
}
