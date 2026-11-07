package dev.mikaaah.tnkprogression.mixin;
import dev.mikaaah.tnkprogression.*;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(Player.class)
public abstract class OriginPlayerMixin {
    @Inject(method="<init>", at=@At("RETURN"))
    private void tnk$foodOwner(CallbackInfo callback) {
        var player = (Player)(Object)this;
        ((OriginFoodOwner)player.getFoodData()).tnk$owner(player);
    }
    @Inject(method="blockInteractionRange", at=@At("RETURN"), cancellable=true)
    private void tnk$reach(CallbackInfoReturnable<Double> callback) {
        callback.setReturnValue(callback.getReturnValue() + OriginHooks.reach((Player)(Object)this));
    }
}
