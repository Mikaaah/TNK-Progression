package dev.mikaaah.tnkprogression.mixin;
import dev.mikaaah.tnkprogression.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(FishingHook.class)
public abstract class OriginFishingMixin {
    @Shadow private int timeUntilLured;
    @Unique private int tnk$waitBefore;
    @Inject(method="catchingFish", at=@At("HEAD"))
    private void tnk$captureWait(BlockPos pos, CallbackInfo callback) { tnk$waitBefore = timeUntilLured; }
    @Inject(method="catchingFish", at=@At("RETURN"))
    private void tnk$shortenWait(BlockPos pos, CallbackInfo callback) {
        if (tnk$waitBefore <= 0 && timeUntilLured > 0 && ((FishingHook)(Object)this).getPlayerOwner() instanceof ServerPlayer player)
            timeUntilLured = Math.max(1, (int)Math.round(timeUntilLured * (1 - Origins.bonus(player, OriginRules.Perk.FISH_WAIT_REDUCTION))));
    }
    @ModifyArg(method="retrieve", at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/ExperienceOrb;<init>(Lnet/minecraft/world/level/Level;DDDI)V"), index=4)
    private int tnk$fishingXp(int amount) {
        if (!(((FishingHook)(Object)this).getPlayerOwner() instanceof ServerPlayer player) || !ActivityGuard.player(player)) return amount;
        return amount + Origins.extra(player, "fishing_xp", amount, Origins.bonus(player, OriginRules.Perk.FISH_XP));
    }
}
