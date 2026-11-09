package dev.mikaaah.tnkprogression.mixin;
import dev.mikaaah.tnkprogression.CookingXp;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(BaseContainerBlockEntity.class)
public abstract class CookingContainerMixin {
 @Inject(method="removeItem",at=@At("HEAD")) private void tnk$remove(int slot,int count,CallbackInfoReturnable<ItemStack> ci){CookingXp.externalChange((BaseContainerBlockEntity)(Object)this,slot);}
 @Inject(method="removeItemNoUpdate",at=@At("HEAD")) private void tnk$removeAll(int slot,CallbackInfoReturnable<ItemStack> ci){CookingXp.externalChange((BaseContainerBlockEntity)(Object)this,slot);}
}
