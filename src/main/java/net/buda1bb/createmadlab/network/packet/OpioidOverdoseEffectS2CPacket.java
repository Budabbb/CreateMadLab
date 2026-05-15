package net.buda1bb.createmadlab.network.packet;

import net.buda1bb.createmadlab.CreateMadLab;
import net.buda1bb.createmadlab.util.ShaderUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpioidOverdoseEffectS2CPacket(boolean active, int remainingTicks, int totalTicks, boolean fadeOut,
                                            float startIntensity,
                                            float startBlackoutAlpha) implements CustomPacketPayload {
    public static final Type<OpioidOverdoseEffectS2CPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateMadLab.MOD_ID, "opioid_overdose_effect"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpioidOverdoseEffectS2CPacket> STREAM_CODEC =
            StreamCodec.of(OpioidOverdoseEffectS2CPacket::encode, OpioidOverdoseEffectS2CPacket::decode);

    public OpioidOverdoseEffectS2CPacket(boolean active, int remainingTicks, int totalTicks) {
        this(active, remainingTicks, totalTicks, false, 0.0F, 0.0F);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(RegistryFriendlyByteBuf buffer, OpioidOverdoseEffectS2CPacket packet) {
        buffer.writeBoolean(packet.active);
        buffer.writeVarInt(packet.remainingTicks);
        buffer.writeVarInt(packet.totalTicks);
        buffer.writeBoolean(packet.fadeOut);
        buffer.writeFloat(packet.startIntensity);
        buffer.writeFloat(packet.startBlackoutAlpha);
    }

    private static OpioidOverdoseEffectS2CPacket decode(RegistryFriendlyByteBuf buffer) {
        return new OpioidOverdoseEffectS2CPacket(
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readFloat(),
                buffer.readFloat()
        );
    }

    public static void handle(OpioidOverdoseEffectS2CPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (packet.active) {
                ShaderUtils.activateOpioidOverdoseShaders(packet.remainingTicks, packet.totalTicks,
                        packet.fadeOut, packet.startIntensity, packet.startBlackoutAlpha);
            } else {
                ShaderUtils.deactivateOpioidOverdoseShaders();
            }
        });
    }
}
