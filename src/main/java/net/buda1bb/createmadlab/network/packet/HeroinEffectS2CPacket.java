package net.buda1bb.createmadlab.network.packet;

import net.buda1bb.createmadlab.util.ShaderUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class HeroinEffectS2CPacket {
    private final boolean active;
    private final int durationTicks;

    public HeroinEffectS2CPacket(boolean active, int durationTicks) {
        this.active = active;
        this.durationTicks = durationTicks;
    }

    public static void encode(HeroinEffectS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.active);
        buffer.writeVarInt(packet.durationTicks);
    }

    public static HeroinEffectS2CPacket decode(FriendlyByteBuf buffer) {
        return new HeroinEffectS2CPacket(buffer.readBoolean(), buffer.readVarInt());
    }

    public static void handle(HeroinEffectS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (packet.active) {
                ShaderUtils.activateHeroinShaders(packet.durationTicks);
            } else {
                ShaderUtils.deactivateHeroinShaders();
            }
        });
        context.setPacketHandled(true);
    }
}
