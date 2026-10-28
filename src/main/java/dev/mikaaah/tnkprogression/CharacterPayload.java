package dev.mikaaah.tnkprogression;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CharacterPayload(String json) implements CustomPacketPayload {
 public static final Type<CharacterPayload> TYPE=new Type<>(ResourceLocation.parse("tnkprogression:character_state"));
 public static final StreamCodec<RegistryFriendlyByteBuf,CharacterPayload> CODEC=StreamCodec.of((b,p)->b.writeUtf(p.json(),262144),b->new CharacterPayload(b.readUtf(262144)));
 @Override public Type<CharacterPayload> type(){return TYPE;}
}
