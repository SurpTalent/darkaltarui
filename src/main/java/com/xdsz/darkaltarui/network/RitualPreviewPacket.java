package com.xdsz.darkaltarui.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class RitualPreviewPacket {

    public static final int TYPE_REQUEST = 0;
    public static final int TYPE_RESPONSE = 1;
    public static volatile RitualPreviewPacket latestClient = null;

    public int type;
    public BlockPos altarPos;
    public String ritualName = "";
    public String ritualType = "";
    public String missing = "";
    public boolean ritualSupported = false;
    public String ritualDesc = "";
    public List<MaterialInfo> materials = new ArrayList<>();
    public List<String> supportedRituals = null;

    public static class MaterialInfo {
        public String name;
        public boolean present;
        public int needed, found;
        public MaterialInfo() {}
        public MaterialInfo(String n, boolean p, int nd, int f) { name=n; present=p; needed=nd; found=f; }
    }

    public static void encode(RitualPreviewPacket pkt, FriendlyByteBuf buf) {
        buf.writeByte(pkt.type);
        buf.writeBlockPos(pkt.altarPos != null ? pkt.altarPos : BlockPos.ZERO);
        if (pkt.type == TYPE_RESPONSE) {
            buf.writeUtf(pkt.ritualName);
            buf.writeUtf(pkt.ritualType);
            buf.writeBoolean(pkt.ritualSupported);
            buf.writeUtf(pkt.ritualDesc);
            buf.writeUtf(pkt.missing);
            buf.writeVarInt(pkt.materials.size());
            for (var m : pkt.materials) {
                buf.writeUtf(m.name);
                buf.writeBoolean(m.present);
                buf.writeVarInt(m.needed);
                buf.writeVarInt(m.found);
            }
            buf.writeBoolean(pkt.supportedRituals != null);
            if (pkt.supportedRituals != null) {
                buf.writeVarInt(pkt.supportedRituals.size());
                for (String s : pkt.supportedRituals) buf.writeUtf(s);
            }
        }
    }

    public static RitualPreviewPacket decode(FriendlyByteBuf buf) {
        RitualPreviewPacket pkt = new RitualPreviewPacket();
        pkt.type = buf.readByte();
        pkt.altarPos = buf.readBlockPos();
        if (pkt.type == TYPE_RESPONSE) {
            pkt.ritualName = buf.readUtf();
            pkt.ritualType = buf.readUtf();
            pkt.ritualSupported = buf.readBoolean();
            pkt.ritualDesc = buf.readUtf();
            pkt.missing = buf.readUtf();
            int size = buf.readVarInt();
            for (int i = 0; i < size; i++)
                pkt.materials.add(new MaterialInfo(buf.readUtf(), buf.readBoolean(), buf.readVarInt(), buf.readVarInt()));
            if (buf.readBoolean()) {
                int rsize = buf.readVarInt();
                pkt.supportedRituals = new ArrayList<>();
                for (int i = 0; i < rsize; i++) pkt.supportedRituals.add(buf.readUtf());
            }
        }
        return pkt;
    }

    public static void handle(RitualPreviewPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (pkt.type == TYPE_REQUEST) Previews.handleRequest(ctx.get().getSender(), pkt.altarPos);
            else { latestClient = pkt; }
        });
        ctx.get().setPacketHandled(true);
    }
}
