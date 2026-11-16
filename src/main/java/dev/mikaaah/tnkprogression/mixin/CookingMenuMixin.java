package dev.mikaaah.tnkprogression.mixin;
import dev.mikaaah.tnkprogression.CookingXp;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AbstractContainerMenu.class)
public abstract class CookingMenuMixin {
 @WrapMethod(method="clicked") private void tnk$manual(int slot,int button,ClickType type,Player player,Operation<Void> original){
  CookingXp.begin(player,(AbstractContainerMenu)(Object)this);
  try{original.call(slot,button,type,player);}finally{CookingXp.end();}
 }
}
