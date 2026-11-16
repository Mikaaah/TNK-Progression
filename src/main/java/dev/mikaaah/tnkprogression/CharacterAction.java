package dev.mikaaah.tnkprogression;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** No target-player field, XP values, grants or perk amounts can be submitted by the client. */
public record CharacterAction(String action,String value) implements CustomPacketPayload {
 private static final java.util.Map<ServerPlayer,CharacterActionGate> GATES=new java.util.WeakHashMap<>();
 private static final java.util.Set<String> ACTIONS=java.util.Set.of("refresh","origin_open","origin","skills","title","passive","track","hud");
 static void forget(ServerPlayer p){GATES.remove(p);}
 static void clear(){GATES.clear();}
 public static final Type<CharacterAction> TYPE=new Type<>(ResourceLocation.parse("tnkprogression:character_action"));
 public static final StreamCodec<RegistryFriendlyByteBuf,CharacterAction> CODEC=StreamCodec.of((b,p)->{b.writeUtf(p.action(),16);b.writeUtf(p.value(),128);},b->new CharacterAction(b.readUtf(16),b.readUtf(128)));
 @Override public Type<CharacterAction> type(){return TYPE;}
 public void handle(ServerPlayer p){
  if(action==null || value==null || !ACTIONS.contains(action))return;
  if((action.equals("refresh") || action.equals("origin_open") || action.equals("skills")) && !value.isEmpty())return;
  long now=p.server.overworld().getGameTime();
  // A busy server may process opening and refresh packets in the same tick. Record the visit even
  // when its response is throttled; this never grants an origin or bypasses the one-time choice.
  if(action.equals("origin_open") && value.isEmpty())Origins.open(p);
  if(!GATES.computeIfAbsent(p,ignored->new CharacterActionGate()).accept(now,action.equals("refresh")))return;
  if(!CharacterState.action(p,action,value))p.sendSystemMessage(net.minecraft.network.chat.Component.literal(action.startsWith("origin")?"Origin unavailable. Use a Sigil of Ascension to unlock the advanced origins and choose again.":action.equals("skills")?"Skill tree unavailable. Check that a compatible Pufferfish's Skills build is loaded and the configured character category is enabled.":"Title unavailable or swap cooldown active."));
  if(action.equals("refresh"))CharacterState.sendFull(p);else CharacterState.send(p);
 }
}
