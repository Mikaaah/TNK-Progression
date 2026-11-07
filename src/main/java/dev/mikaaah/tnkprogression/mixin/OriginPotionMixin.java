package dev.mikaaah.tnkprogression.mixin;
import dev.mikaaah.tnkprogression.OriginHooks;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(PotionItem.class)
public abstract class OriginPotionMixin {
    @Inject(method="finishUsingItem", at=@At("HEAD"))
    private void tnk$begin(ItemStack stack, Level world, LivingEntity entity, CallbackInfoReturnable<ItemStack> callback) { OriginHooks.beginDrink(entity); }
    @Inject(method="finishUsingItem", at=@At("RETURN"))
    private void tnk$end(ItemStack stack, Level world, LivingEntity entity, CallbackInfoReturnable<ItemStack> callback) { OriginHooks.endDrink(); }
}
