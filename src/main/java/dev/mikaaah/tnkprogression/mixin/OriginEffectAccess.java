package dev.mikaaah.tnkprogression.mixin;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(MobEffectInstance.class)
public interface OriginEffectAccess {
    @Accessor("duration") void tnk$duration(int value);
    @Accessor("hiddenEffect") MobEffectInstance tnk$hidden();
    @Accessor("hiddenEffect") void tnk$hidden(MobEffectInstance value);
}
