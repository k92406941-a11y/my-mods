package com.random30sek;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record StatePayload(boolean running, String eventName, int ticksLeft) implements CustomPayload {
    public static final Id<StatePayload> ID = new Id<>(Identifier.of("random30sek", "state"));
    public static final PacketCodec<RegistryByteBuf, StatePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOLEAN, StatePayload::running,
            PacketCodecs.STRING, StatePayload::eventName,
            PacketCodecs.VAR_INT, StatePayload::ticksLeft,
            StatePayload::new);

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
