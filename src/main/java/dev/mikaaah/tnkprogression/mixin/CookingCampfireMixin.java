package dev.mikaaah.tnkprogression.mixin;
import dev.mikaaah.tnkprogression.CookingXp;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(CampfireBlockEntity.class)
public abstract class CookingCampfireMixin {
 @Unique private ItemStack[] tnk$before,tnk$results,tnk$placed;
 @Inject(method="placeFood",at=@At("HEAD")) private void tnk$placeBefore(LivingEntity entity,ItemStack stack,int time,CallbackInfoReturnable<Boolean> ci){tnk$placed=CookingXp.campfireItems((CampfireBlockEntity)(Object)this);}
 @Inject(method="placeFood",at=@At("RETURN")) private void tnk$placeAfter(LivingEntity entity,ItemStack stack,int time,CallbackInfoReturnable<Boolean> ci){if(ci.getReturnValue()&&entity instanceof Player p)CookingXp.campfirePlaced((CampfireBlockEntity)(Object)this,p,tnk$placed);}
 @Inject(method="cookTick",at=@At("HEAD")) private static void tnk$before(Level level,BlockPos pos,BlockState state,CampfireBlockEntity be,CallbackInfo ci){var mixin=(CookingCampfireMixin)(Object)be;mixin.tnk$before=CookingXp.campfireItems(be);mixin.tnk$results=CookingXp.campfireResults(be);}
 @Inject(method="cookTick",at=@At("RETURN")) private static void tnk$after(Level level,BlockPos pos,BlockState state,CampfireBlockEntity be,CallbackInfo ci){var mixin=(CookingCampfireMixin)(Object)be;CookingXp.campfireDone(be,mixin.tnk$before,mixin.tnk$results);}
}
