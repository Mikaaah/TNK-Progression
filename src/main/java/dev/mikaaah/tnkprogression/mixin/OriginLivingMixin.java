package dev.mikaaah.tnkprogression.mixin;
import dev.mikaaah.tnkprogression.OriginHooks;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(LivingEntity.class)
public abstract class OriginLivingMixin {
    @ModifyVariable(method="addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at=@At("HEAD"), argsOnly=true)
    private MobEffectInstance tnk$duration(MobEffectInstance effect) {
        return (Object)this instanceof Player player && !player.level().isClientSide ? OriginHooks.effect(player, effect) : effect;
    }
}
