package net.buda1bb.createmadlab.network.packet;

import net.buda1bb.createmadlab.CreateMadLab;
import net.buda1bb.createmadlab.util.ShaderUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record FentanylEffectS2CPacket(boolean active, int remainingTicks, int totalTicks, boolean fadeOut,
                                      float startIntensity, float startBlackoutAlpha,
                                      float visualStrength) implements CustomPacketPayload {
    public static final Type<FentanylEffectS2CPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateMadLab.MOD_ID, "fentanyl_effect"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FentanylEffectS2CPacket> STREAM_CODEC =
            StreamCodec.of(FentanylEffectS2CPacket::encode, FentanylEffectS2CPacket::decode);

    public FentanylEffectS2CPacket(boolean active, int remainingTicks, int totalTicks) {
        this(active, remainingTicks, totalTicks, false, 0.0F, 0.0F, 1.0F);
    }

    public FentanylEffectS2CPacket(boolean active, int remainingTicks, int totalTicks, boolean fadeOut,
                                   float startIntensity, float startBlackoutAlpha) {
        this(active, remainingTicks, totalTicks, fadeOut, startIntensity, startBlackoutAlpha, 1.0F);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(RegistryFriendlyByteBuf buffer, FentanylEffectS2CPacket packet) {
        buffer.writeBoolean(packet.active);
        buffer.writeVarInt(packet.remainingTicks);
        buffer.writeVarInt(packet.totalTicks);
        buffer.writeBoolean(packet.fadeOut);
        buffer.writeFloat(packet.startIntensity);
        buffer.writeFloat(packet.startBlackoutAlpha);
        buffer.writeFloat(packet.visualStrength);
    }

    private static FentanylEffectS2CPacket decode(RegistryFriendlyByteBuf buffer) {
        return new FentanylEffectS2CPacket(
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat()
        );
    }

    public static void handle(FentanylEffectS2CPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (packet.active) {
                ShaderUtils.activateFentanylShaders(packet.remainingTicks, packet.totalTicks,
                        packet.fadeOut, packet.startIntensity, packet.startBlackoutAlpha, packet.visualStrength);
            } else {
                ShaderUtils.deactivateFentanylShaders();
            }
        });
    }
}
