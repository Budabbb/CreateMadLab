package net.buda1bb.createmadlab.network.packet;

import net.buda1bb.createmadlab.util.ShaderUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class HeroinEffectS2CPacket {
    private final boolean active;
    private final int durationTicks;
    private final int totalTicks;
    private final float visualStrength;
    private final boolean cinematicCamera;
    private final boolean fadeOut;

    public HeroinEffectS2CPacket(boolean active, int durationTicks) {
        this(active, durationTicks, 0, 1.0F, true, false);
    }

    public HeroinEffectS2CPacket(boolean active, int durationTicks, int totalTicks, float visualStrength, boolean cinematicCamera) {
        this(active, durationTicks, totalTicks, visualStrength, cinematicCamera, false);
    }

    public HeroinEffectS2CPacket(boolean active, int durationTicks, int totalTicks, float visualStrength, boolean cinematicCamera, boolean fadeOut) {
        this.active = active;
        this.durationTicks = durationTicks;
        this.totalTicks = totalTicks;
        this.visualStrength = visualStrength;
        this.cinematicCamera = cinematicCamera;
        this.fadeOut = fadeOut;
    }

    public static void encode(HeroinEffectS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.active);
        buffer.writeVarInt(packet.durationTicks);
        buffer.writeVarInt(packet.totalTicks);
        buffer.writeFloat(packet.visualStrength);
        buffer.writeBoolean(packet.cinematicCamera);
        buffer.writeBoolean(packet.fadeOut);
    }

    public static HeroinEffectS2CPacket decode(FriendlyByteBuf buffer) {
        return new HeroinEffectS2CPacket(
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readFloat(),
                buffer.readBoolean(),
                buffer.readBoolean()
        );
    }

    public static void handle(HeroinEffectS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (packet.active) {
                ShaderUtils.activateHeroinShaders(packet.durationTicks, packet.totalTicks,
                        packet.visualStrength, packet.cinematicCamera, packet.fadeOut);
            } else {
                ShaderUtils.deactivateHeroinShaders();
            }
        });
        context.setPacketHandled(true);
    }
}
