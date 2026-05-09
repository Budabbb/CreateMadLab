package net.buda1bb.createmadlab.network.packet;

import net.buda1bb.createmadlab.CreateMadLab;
import net.buda1bb.createmadlab.util.ShaderUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record MorphineEffectS2CPacket(
        int remainingTicks,
        int totalTicks,
        int unstableHp,
        float health,
        boolean silentDecay,
        boolean convertedDamage
) implements CustomPacketPayload {
    public static final Type<MorphineEffectS2CPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateMadLab.MOD_ID, "morphine_effect"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MorphineEffectS2CPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MorphineEffectS2CPacket::remainingTicks,
            ByteBufCodecs.VAR_INT, MorphineEffectS2CPacket::totalTicks,
            ByteBufCodecs.VAR_INT, MorphineEffectS2CPacket::unstableHp,
            ByteBufCodecs.FLOAT, MorphineEffectS2CPacket::health,
            ByteBufCodecs.BOOL, MorphineEffectS2CPacket::silentDecay,
            ByteBufCodecs.BOOL, MorphineEffectS2CPacket::convertedDamage,
            MorphineEffectS2CPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MorphineEffectS2CPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
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
                    packet.convertedDamage
            );
        });
    }
}
