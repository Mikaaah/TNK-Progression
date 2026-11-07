package dev.mikaaah.tnkprogression.mixin;
import dev.mikaaah.tnkprogression.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(FoodData.class)
public abstract class OriginFoodMixin implements OriginFoodOwner {
    @Unique private Player tnk$owner;
    public void tnk$owner(Player player) { tnk$owner = player; }
    @Inject(method="tick", at=@At("HEAD"))
    private void tnk$bind(Player player, CallbackInfo callback) { tnk$owner = player; }
    @ModifyVariable(method="addExhaustion", at=@At("HEAD"), argsOnly=true)
    private float tnk$exhaustion(float amount) {
        return tnk$owner == null ? amount : amount * (float)(1 - Origins.bonus(tnk$owner, OriginRules.Perk.EXHAUSTION_REDUCTION));
    }
    @ModifyArg(method="tick", at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/player/Player;heal(F)V"), index=0)
    private float tnk$naturalHealing(float amount) {
        return tnk$owner == null ? amount : amount * (float)(1 + Origins.bonus(tnk$owner, OriginRules.Perk.NATURAL_HEAL));
    }
}
