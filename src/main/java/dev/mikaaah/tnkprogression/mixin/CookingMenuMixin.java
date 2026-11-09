package dev.mikaaah.tnkprogression.mixin;
import dev.mikaaah.tnkprogression.CookingXp;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AbstractContainerMenu.class)
public abstract class CookingMenuMixin {
 @Inject(method="clicked",at=@At("HEAD")) private void tnk$before(int slot,int button,ClickType type,Player player,CallbackInfo ci){CookingXp.begin(player,(AbstractContainerMenu)(Object)this);}
 @Inject(method="clicked",at=@At("RETURN")) private void tnk$after(int slot,int button,ClickType type,Player player,CallbackInfo ci){CookingXp.end();}
}
