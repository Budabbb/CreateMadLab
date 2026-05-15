package net.buda1bb.createmadlab.network;

import net.buda1bb.createmadlab.CreateMadLab;
import net.buda1bb.createmadlab.network.packet.DrugVisualStateS2CPacket;
import net.buda1bb.createmadlab.network.packet.FentanylEffectS2CPacket;
import net.buda1bb.createmadlab.network.packet.HeroinEffectS2CPacket;
import net.buda1bb.createmadlab.network.packet.LSDEffectS2CPacket;
import net.buda1bb.createmadlab.network.packet.MorphineEffectS2CPacket;
import net.buda1bb.createmadlab.network.packet.OpiateWithdrawalEffectS2CPacket;
import net.buda1bb.createmadlab.network.packet.OpioidOverdoseEffectS2CPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModMessages {
    private static final String PROTOCOL_VERSION = "2";
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
        INSTANCE.messageBuilder(FentanylEffectS2CPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(FentanylEffectS2CPacket::encode)
                .decoder(FentanylEffectS2CPacket::decode)
                .consumerMainThread(FentanylEffectS2CPacket::handle)
                .add();
        INSTANCE.messageBuilder(OpioidOverdoseEffectS2CPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpioidOverdoseEffectS2CPacket::encode)
                .decoder(OpioidOverdoseEffectS2CPacket::decode)
                .consumerMainThread(OpioidOverdoseEffectS2CPacket::handle)
                .add();
        INSTANCE.messageBuilder(OpiateWithdrawalEffectS2CPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpiateWithdrawalEffectS2CPacket::encode)
                .decoder(OpiateWithdrawalEffectS2CPacket::decode)
                .consumerMainThread(OpiateWithdrawalEffectS2CPacket::handle)
                .add();
        INSTANCE.messageBuilder(DrugVisualStateS2CPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DrugVisualStateS2CPacket::encode)
                .decoder(DrugVisualStateS2CPacket::decode)
                .consumerMainThread(DrugVisualStateS2CPacket::handle)
                .add();
    }

    public static void sendToPlayer(Object message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    private static int nextId() {
        return packetId++;
    }
}
