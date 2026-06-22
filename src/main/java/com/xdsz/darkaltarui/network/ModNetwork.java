package com.xdsz.darkaltarui.network;

import com.xdsz.darkaltarui.AdvancedMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(AdvancedMod.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    private ModNetwork() {}

    public static void register() {
        CHANNEL.registerMessage(
                packetId++,
                AltarInfoPacket.class,
                AltarInfoPacket::encode,
                AltarInfoPacket::decode,
                AltarInfoPacket::handle
        );
        CHANNEL.registerMessage(
                packetId++,
                BackpackExtractPacket.class,
                BackpackExtractPacket::encode,
                BackpackExtractPacket::decode,
                BackpackExtractPacket::handle
        );
    }
}
