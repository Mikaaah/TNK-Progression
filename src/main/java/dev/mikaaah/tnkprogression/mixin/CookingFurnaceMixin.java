package dev.mikaaah.tnkprogression.mixin;
import dev.mikaaah.tnkprogression.CookingXp;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class CookingFurnaceMixin {
 @Unique private CookingXp.Cook tnk$cook;
 @Inject(method="serverTick",at=@At("HEAD")) private static void tnk$before(Level level,BlockPos pos,BlockState state,AbstractFurnaceBlockEntity be,CallbackInfo ci){((CookingFurnaceMixin)(Object)be).tnk$cook=CookingXp.beforeCook(be);}
 @Inject(method="serverTick",at=@At("RETURN")) private static void tnk$after(Level level,BlockPos pos,BlockState state,AbstractFurnaceBlockEntity be,CallbackInfo ci){CookingXp.afterCook(be,((CookingFurnaceMixin)(Object)be).tnk$cook);}
 @Inject(method="setItem",at=@At("HEAD")) private void tnk$input(int slot,ItemStack stack,CallbackInfo ci){CookingXp.externalChange((AbstractFurnaceBlockEntity)(Object)this,slot);}
}
