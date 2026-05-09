package net.buda1bb.createmadlab.network;

import net.buda1bb.createmadlab.network.packet.FentanylEffectS2CPacket;
import net.buda1bb.createmadlab.network.packet.HeroinEffectS2CPacket;
import net.buda1bb.createmadlab.network.packet.LSDEffectS2CPacket;
import net.buda1bb.createmadlab.network.packet.MorphineEffectS2CPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModMessages {
    private static final String PROTOCOL_VERSION = "1";

    private ModMessages() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION).optional();
        registrar.playToClient(HeroinEffectS2CPacket.TYPE, HeroinEffectS2CPacket.STREAM_CODEC, HeroinEffectS2CPacket::handle);
        registrar.playToClient(LSDEffectS2CPacket.TYPE, LSDEffectS2CPacket.STREAM_CODEC, LSDEffectS2CPacket::handle);
        registrar.playToClient(MorphineEffectS2CPacket.TYPE, MorphineEffectS2CPacket.STREAM_CODEC, MorphineEffectS2CPacket::handle);
        registrar.playToClient(FentanylEffectS2CPacket.TYPE, FentanylEffectS2CPacket.STREAM_CODEC, FentanylEffectS2CPacket::handle);
    }

    public static void sendToPlayer(CustomPacketPayload message, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, message);
    }
}
