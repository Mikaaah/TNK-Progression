package dev.mikaaah.tnkprogression;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/** One small update when tracking starts; the level lives on that client entity, not in a global cache. */
public record MobLevelPayload(int entityId,UUID uuid,int level) implements CustomPacketPayload {
 public static final Type<MobLevelPayload> TYPE=new Type<>(ResourceLocation.parse("tnkprogression:mob_level"));
 public static final StreamCodec<RegistryFriendlyByteBuf,MobLevelPayload> CODEC=StreamCodec.of(
  (b,p)->{b.writeVarInt(p.entityId);b.writeUUID(p.uuid);b.writeVarInt(p.level);},
  b->new MobLevelPayload(b.readVarInt(),b.readUUID(),b.readVarInt()));
 @Override public Type<MobLevelPayload> type(){return TYPE;}
 public void accept(Player viewer){
  var entity=viewer.level().getEntity(entityId);
  if(entity instanceof Mob && entity.getUUID().equals(uuid))entity.getPersistentData().putInt("tnk_level",Math.clamp(level,1,100));
 }
}
