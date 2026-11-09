package dev.mikaaah.tnkprogression;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** No target-player field, XP values, grants or perk amounts can be submitted by the client. */
public record CharacterAction(String action,String value) implements CustomPacketPayload {
 public static final Type<CharacterAction> TYPE=new Type<>(ResourceLocation.parse("tnkprogression:character_action"));
 public static final StreamCodec<RegistryFriendlyByteBuf,CharacterAction> CODEC=StreamCodec.of((b,p)->{b.writeUtf(p.action(),16);b.writeUtf(p.value(),128);},b->new CharacterAction(b.readUtf(16),b.readUtf(128)));
 @Override public Type<CharacterAction> type(){return TYPE;}
 public void handle(ServerPlayer p){
  var n=ProgressionAPI.profile(p);long now=p.server.overworld().getGameTime();
  // A busy server may process opening and refresh packets in the same tick. Record the visit even
  // when its response is throttled; this never grants an origin or bypasses the one-time choice.
  if(action.equals("origin_open") && value.isEmpty())Origins.open(p);
  if(n.contains("character_packet_tick") && now>=n.getLong("character_packet_tick") && now-n.getLong("character_packet_tick")<2)return;
  n.putLong("character_packet_tick",now);
  if(!CharacterState.action(p,action,value))p.sendSystemMessage(net.minecraft.network.chat.Component.literal(action.startsWith("origin")?"Origin unavailable. Use a Sigil of Ascension to unlock the advanced origins and choose again.":action.equals("skills")?"Skill tree unavailable. Check that a compatible Pufferfish's Skills build is loaded and the configured character category is enabled.":"Title unavailable or swap cooldown active."));
  CharacterState.sendFull(p);
 }
}
