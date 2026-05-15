package net.buda1bb.createmadlab.network.packet;

import net.buda1bb.createmadlab.CreateMadLab;
import net.buda1bb.createmadlab.util.ShaderUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record HeroinEffectS2CPacket(boolean active, int durationTicks, int totalTicks, float visualStrength,
                                    boolean cinematicCamera, boolean fadeOut) implements CustomPacketPayload {
    public static final Type<HeroinEffectS2CPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateMadLab.MOD_ID, "heroin_effect"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HeroinEffectS2CPacket> STREAM_CODEC =
            StreamCodec.of(HeroinEffectS2CPacket::encode, HeroinEffectS2CPacket::decode);

    public HeroinEffectS2CPacket(boolean active, int durationTicks) {
        this(active, durationTicks, 0, 1.0F, true, false);
    }

    public HeroinEffectS2CPacket(boolean active, int durationTicks, int totalTicks,
                                 float visualStrength, boolean cinematicCamera) {
        this(active, durationTicks, totalTicks, visualStrength, cinematicCamera, false);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(RegistryFriendlyByteBuf buffer, HeroinEffectS2CPacket packet) {
        buffer.writeBoolean(packet.active);
        buffer.writeVarInt(packet.durationTicks);
        buffer.writeVarInt(packet.totalTicks);
        buffer.writeFloat(packet.visualStrength);
        buffer.writeBoolean(packet.cinematicCamera);
        buffer.writeBoolean(packet.fadeOut);
    }

    private static HeroinEffectS2CPacket decode(RegistryFriendlyByteBuf buffer) {
        return new HeroinEffectS2CPacket(
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readFloat(),
                buffer.readBoolean(),
                buffer.readBoolean()
        );
    }

    public static void handle(HeroinEffectS2CPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (packet.active) {
                ShaderUtils.activateHeroinShaders(packet.durationTicks, packet.totalTicks,
                        packet.visualStrength, packet.cinematicCamera, packet.fadeOut);
            } else {
                ShaderUtils.deactivateHeroinShaders();
            }
        });
    }
}
