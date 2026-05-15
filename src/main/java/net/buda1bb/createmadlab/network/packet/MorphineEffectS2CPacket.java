package net.buda1bb.createmadlab.network.packet;

import net.buda1bb.createmadlab.util.ShaderUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MorphineEffectS2CPacket {
    private final int remainingTicks;
    private final int totalTicks;
    private final int unstableHp;
    private final float health;
    private final boolean silentDecay;
    private final boolean convertedDamage;
    private final boolean fadeOut;
    private final float startIntensity;
    private final float startDebtIntensity;
    private final float visualStrength;

    public MorphineEffectS2CPacket(int remainingTicks, int totalTicks, int unstableHp, float health, boolean silentDecay, boolean convertedDamage) {
        this(remainingTicks, totalTicks, unstableHp, health, silentDecay, convertedDamage, false, 0.0F, 0.0F, 1.0F);
    }

    public MorphineEffectS2CPacket(int remainingTicks, int totalTicks, int unstableHp, float health, boolean silentDecay,
                                  boolean convertedDamage, boolean fadeOut, float startIntensity, float startDebtIntensity) {
        this(remainingTicks, totalTicks, unstableHp, health, silentDecay, convertedDamage, fadeOut,
                startIntensity, startDebtIntensity, 1.0F);
    }

    public MorphineEffectS2CPacket(int remainingTicks, int totalTicks, int unstableHp, float health, boolean silentDecay,
                                  boolean convertedDamage, boolean fadeOut, float startIntensity, float startDebtIntensity,
                                  float visualStrength) {
        this.remainingTicks = remainingTicks;
        this.totalTicks = totalTicks;
        this.unstableHp = unstableHp;
        this.health = health;
        this.silentDecay = silentDecay;
        this.convertedDamage = convertedDamage;
        this.fadeOut = fadeOut;
        this.startIntensity = startIntensity;
        this.startDebtIntensity = startDebtIntensity;
        this.visualStrength = visualStrength;
    }

    public static void encode(MorphineEffectS2CPacket packet, FriendlyByteBuf buffer) {
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

    public static MorphineEffectS2CPacket decode(FriendlyByteBuf buffer) {
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

    public static void handle(MorphineEffectS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
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
        context.setPacketHandled(true);
    }
}
