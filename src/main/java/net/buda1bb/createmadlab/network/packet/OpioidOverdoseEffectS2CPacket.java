package net.buda1bb.createmadlab.network.packet;

import net.buda1bb.createmadlab.util.ShaderUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpioidOverdoseEffectS2CPacket {
    private final boolean active;
    private final int remainingTicks;
    private final int totalTicks;
    private final boolean fadeOut;
    private final float startIntensity;
    private final float startBlackoutAlpha;

    public OpioidOverdoseEffectS2CPacket(boolean active, int remainingTicks, int totalTicks) {
        this(active, remainingTicks, totalTicks, false, 0.0F, 0.0F);
    }

    public OpioidOverdoseEffectS2CPacket(boolean active, int remainingTicks, int totalTicks,
                                         boolean fadeOut, float startIntensity, float startBlackoutAlpha) {
        this.active = active;
        this.remainingTicks = remainingTicks;
        this.totalTicks = totalTicks;
        this.fadeOut = fadeOut;
        this.startIntensity = startIntensity;
        this.startBlackoutAlpha = startBlackoutAlpha;
    }

    public static void encode(OpioidOverdoseEffectS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.active);
        buffer.writeVarInt(packet.remainingTicks);
        buffer.writeVarInt(packet.totalTicks);
        buffer.writeBoolean(packet.fadeOut);
        buffer.writeFloat(packet.startIntensity);
        buffer.writeFloat(packet.startBlackoutAlpha);
    }

    public static OpioidOverdoseEffectS2CPacket decode(FriendlyByteBuf buffer) {
        return new OpioidOverdoseEffectS2CPacket(
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readFloat(),
                buffer.readFloat()
        );
    }

    public static void handle(OpioidOverdoseEffectS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (packet.active) {
                ShaderUtils.activateOpioidOverdoseShaders(packet.remainingTicks, packet.totalTicks,
                        packet.fadeOut, packet.startIntensity, packet.startBlackoutAlpha);
            } else {
                ShaderUtils.deactivateOpioidOverdoseShaders();
            }
        });
        context.setPacketHandled(true);
    }
}
