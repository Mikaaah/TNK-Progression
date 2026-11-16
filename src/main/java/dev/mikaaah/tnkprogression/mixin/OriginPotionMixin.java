package dev.mikaaah.tnkprogression.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.mikaaah.tnkprogression.OriginHooks;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(PotionItem.class)
public abstract class OriginPotionMixin {
    @WrapMethod(method="finishUsingItem")
    private ItemStack tnk$drink(ItemStack stack, Level world, LivingEntity entity, Operation<ItemStack> original) {
        var previous=OriginHooks.drinkContext();OriginHooks.beginDrink(entity);
        try{return original.call(stack,world,entity);}
        finally{OriginHooks.restoreDrink(previous);}
    }
}
