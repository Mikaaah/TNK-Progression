package dev.mikaaah.tnkprogression.mixin;

import dev.mikaaah.tnkprogression.AbyssIntegration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="com.pla.spiral_abyss.encounter.WaveScaling",remap=false)
public abstract class AbyssWaveScalingMixin {
    @Inject(method="apply",at=@At("RETURN"),remap=false)
    private static void tnk$completed(Mob mob,int floor,@Coerce Object category,ResourceLocation id,
                                      double party,@Coerce Object plans,CallbackInfo ci) {
        AbyssIntegration.scaled(mob,floor,category,party);
    }
}
