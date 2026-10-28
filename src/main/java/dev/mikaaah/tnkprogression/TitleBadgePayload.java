package dev.mikaaah.tnkprogression;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Only the server supplies another player's currently equipped title. */
public record TitleBadgePayload(UUID playerId,Component display) implements CustomPacketPayload {
    public static final Type<TitleBadgePayload> TYPE=new Type<>(ResourceLocation.parse("tnkprogression:title_badge"));
    public static final StreamCodec<RegistryFriendlyByteBuf,TitleBadgePayload> CODEC=StreamCodec.of(
        (buffer,payload)->{buffer.writeUUID(payload.playerId());ComponentSerialization.STREAM_CODEC.encode(buffer,payload.display());},
        buffer->new TitleBadgePayload(buffer.readUUID(),ComponentSerialization.STREAM_CODEC.decode(buffer)));
    @Override public Type<TitleBadgePayload> type(){return TYPE;}
}
