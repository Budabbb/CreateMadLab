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

    public MorphineEffectS2CPacket(int remainingTicks, int totalTicks, int unstableHp, float health, boolean silentDecay, boolean convertedDamage) {
        this.remainingTicks = remainingTicks;
        this.totalTicks = totalTicks;
        this.unstableHp = unstableHp;
        this.health = health;
        this.silentDecay = silentDecay;
        this.convertedDamage = convertedDamage;
    }

    public static void encode(MorphineEffectS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.remainingTicks);
        buffer.writeVarInt(packet.totalTicks);
        buffer.writeVarInt(packet.unstableHp);
        buffer.writeFloat(packet.health);
        buffer.writeBoolean(packet.silentDecay);
        buffer.writeBoolean(packet.convertedDamage);
    }

    public static MorphineEffectS2CPacket decode(FriendlyByteBuf buffer) {
        return new MorphineEffectS2CPacket(
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readFloat(),
                buffer.readBoolean(),
                buffer.readBoolean()
        );
    }

    public static void handle(MorphineEffectS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
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
        context.setPacketHandled(true);
    }
}
