package net.buda1bb.createmadlab.network.packet;

import net.buda1bb.createmadlab.CreateMadLab;
import net.buda1bb.createmadlab.util.ShaderUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record MorphineEffectS2CPacket(int remainingTicks, int totalTicks, int unstableHp, float health,
                                      boolean silentDecay, boolean convertedDamage, boolean fadeOut,
                                      float startIntensity, float startDebtIntensity,
                                      float visualStrength) implements CustomPacketPayload {
    public static final Type<MorphineEffectS2CPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateMadLab.MOD_ID, "morphine_effect"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MorphineEffectS2CPacket> STREAM_CODEC =
            StreamCodec.of(MorphineEffectS2CPacket::encode, MorphineEffectS2CPacket::decode);

    public MorphineEffectS2CPacket(int remainingTicks, int totalTicks, int unstableHp, float health,
                                   boolean silentDecay, boolean convertedDamage) {
        this(remainingTicks, totalTicks, unstableHp, health, silentDecay, convertedDamage,
                false, 0.0F, 0.0F, 1.0F);
    }

    public MorphineEffectS2CPacket(int remainingTicks, int totalTicks, int unstableHp, float health,
                                   boolean silentDecay, boolean convertedDamage, boolean fadeOut,
                                   float startIntensity, float startDebtIntensity) {
        this(remainingTicks, totalTicks, unstableHp, health, silentDecay, convertedDamage,
                fadeOut, startIntensity, startDebtIntensity, 1.0F);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(RegistryFriendlyByteBuf buffer, MorphineEffectS2CPacket packet) {
        buffer.writeVarInt(packet.remainingTicks);
        buffer.writeVarInt(packet.totalTicks);
        buffer.writeVarInt(packet.unstableHp);
        buffer.writeFloat(packet.health);
        buffer.writeBoolean(packet.silentDecay);
        buffer.writeBoolean(packet.convertedDamage);
        buffer.writeBoolean(packet.fadeOut);
        buffer.writeFloat(packet.startIntensity);
        buffer.writeFloat(packet.startDebtIntensity);
        buffer.writeFloat(packet.visualStrength);
    }

    private static MorphineEffectS2CPacket decode(RegistryFriendlyByteBuf buffer) {
        return new MorphineEffectS2CPacket(
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readFloat(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat()
        );
    }

    public static void handle(MorphineEffectS2CPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (packet.fadeOut && (packet.remainingTicks > 0 || packet.unstableHp > 0 || packet.silentDecay)) {
                ShaderUtils.activateMorphineShaders(
                        packet.remainingTicks,
                        packet.totalTicks,
                        packet.unstableHp,
                        packet.health,
                        packet.silentDecay,
                        packet.convertedDamage,
                        true,
                        packet.startIntensity,
                        packet.startDebtIntensity,
                        packet.visualStrength
                );
                return;
            }

            if (packet.remainingTicks <= 0 && packet.unstableHp <= 0) {
                ShaderUtils.deactivateMorphineShaders();
                return;
            }

            ShaderUtils.activateMorphineShaders(
                    packet.remainingTicks,
                    packet.totalTicks,
                    packet.unstableHp,
                    packet.health,
                    packet.silentDecay,
                    packet.convertedDamage,
                    packet.visualStrength
            );
        });
    }
}
