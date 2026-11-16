package dev.mikaaah.tnkprogression.mixin;
import dev.mikaaah.tnkprogression.StarcatcherFishing;
import net.minecraft.server.level.ServerPlayer;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Only loaded when Starcatcher supplies the target. No required Starcatcher dependency. */
@Pseudo
@Mixin(targets="com.wdiscute.starcatcher.fish.FishApi",remap=false)
public abstract class StarcatcherCatchMixin {
 @WrapMethod(method="spawnFishFromPlayerFishing",require=0,remap=false)
 private static void tnk$catch(ServerPlayer player,boolean completed,int time,boolean treasure,boolean perfect,int hits,Operation<Void> original){
  StarcatcherFishing.begin(player,completed);
  try{original.call(player,completed,time,treasure,perfect,hits);StarcatcherFishing.finish(player);}finally{StarcatcherFishing.clear();}
 }
}
