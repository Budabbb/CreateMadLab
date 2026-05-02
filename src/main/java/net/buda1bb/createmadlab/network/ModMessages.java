package net.buda1bb.createmadlab.network;

import net.buda1bb.createmadlab.CreateMadLab;
import net.buda1bb.createmadlab.network.packet.HeroinEffectS2CPacket;
import net.buda1bb.createmadlab.network.packet.LSDEffectS2CPacket;
import net.buda1bb.createmadlab.network.packet.MorphineEffectS2CPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModMessages {
    private static final String PROTOCOL_VERSION = "1";
    private static int packetId;

    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CreateMadLab.MOD_ID, "messages"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private ModMessages() {
    }

    public static void register() {
        packetId = 0;
        INSTANCE.messageBuilder(HeroinEffectS2CPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(HeroinEffectS2CPacket::encode)
                .decoder(HeroinEffectS2CPacket::decode)
                .consumerMainThread(HeroinEffectS2CPacket::handle)
                .add();
        INSTANCE.messageBuilder(LSDEffectS2CPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(LSDEffectS2CPacket::encode)
                .decoder(LSDEffectS2CPacket::decode)
                .consumerMainThread(LSDEffectS2CPacket::handle)
                .add();
        INSTANCE.messageBuilder(MorphineEffectS2CPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(MorphineEffectS2CPacket::encode)
                .decoder(MorphineEffectS2CPacket::decode)
                .consumerMainThread(MorphineEffectS2CPacket::handle)
                .add();
    }

    public static void sendToPlayer(Object message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    private static int nextId() {
        return packetId++;
    }
}
