package com.random30sek;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record TogglePayload() implements CustomPayload {
    public static final Id<TogglePayload> ID = new Id<>(Identifier.of("random30sek", "toggle"));
    public static final PacketCodec<RegistryByteBuf, TogglePayload> CODEC = PacketCodec.unit(new TogglePayload());

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
