package dev.mikaaah.tnkprogression.mixin;

import dev.mikaaah.tnkprogression.AbyssIntegration;
import java.util.function.Consumer;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="com.pla.spiral_abyss.encounter.EncounterReinforcements",remap=false)
public abstract class AbyssReinforcementsMixin {
    @Inject(method="inheritScaling",at=@At("RETURN"),remap=false)
    private static void tnk$phase(Mob root,Mob phase,CallbackInfo ci) { AbyssIntegration.phase(root,phase); }
    @Redirect(method="discover",at=@At(value="INVOKE",target="Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"),remap=false)
    private void tnk$adopted(Consumer<Object> consumer,Object child) {
        if (child instanceof Mob mob) AbyssIntegration.adopted(mob);
        consumer.accept(child);
    }
}
