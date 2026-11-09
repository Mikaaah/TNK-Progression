package dev.mikaaah.tnkprogression.mixin;
import dev.mikaaah.tnkprogression.StarcatcherFishing;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Only loaded when Starcatcher supplies the target. No required Starcatcher dependency. */
@Pseudo
@Mixin(targets="com.wdiscute.starcatcher.fish.FishApi",remap=false)
public abstract class StarcatcherCatchMixin {
 @Inject(method="spawnFishFromPlayerFishing",at=@At("HEAD"),require=0,remap=false)
 private static void tnk$before(ServerPlayer player,boolean completed,int time,boolean treasure,boolean perfect,int hits,CallbackInfo ci){StarcatcherFishing.begin(player,completed);}
 @Inject(method="spawnFishFromPlayerFishing",at=@At("RETURN"),require=0,remap=false)
 private static void tnk$after(ServerPlayer player,boolean completed,int time,boolean treasure,boolean perfect,int hits,CallbackInfo ci){StarcatcherFishing.finish(player);}
}
