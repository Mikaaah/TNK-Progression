package dev.mikaaah.tnkprogression.mixin;
import dev.mikaaah.tnkprogression.OriginHooks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import java.util.function.Consumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(ItemStack.class)
public abstract class OriginDurabilityMixin {
    @ModifyVariable(method="hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V", at=@At("HEAD"), argsOnly=true, ordinal=0)
    private int tnk$weaponWear(int amount, int original, ServerLevel world, LivingEntity entity, Consumer<Item> broken) {
        return OriginHooks.weaponDamage((ItemStack)(Object)this, entity, amount);
    }
}
