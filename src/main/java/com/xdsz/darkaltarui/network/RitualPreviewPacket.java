package com.xdsz.darkaltarui.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 双向包：客户端请求 → 服务端扫描 → 返回仪式预览。
 */
public class RitualPreviewPacket {

    public static final int TYPE_REQUEST = 0;
    public static final int TYPE_RESPONSE = 1;

    public int type;
    public BlockPos altarPos;
    // response fields
    public String ritualName;
    public List<MaterialInfo> materials = new ArrayList<>();

    public static class MaterialInfo {
        public String name;
        public boolean present;
        public int needed;
        public int found;
        public MaterialInfo() {}
        public MaterialInfo(String n, boolean p, int need, int f) { name=n; present=p; needed=need; found=f; }
    }

    public RitualPreviewPacket() {}

    public static void encode(RitualPreviewPacket pkt, FriendlyByteBuf buf) {
        buf.writeByte(pkt.type);
        buf.writeBlockPos(pkt.altarPos != null ? pkt.altarPos : BlockPos.ZERO);
        if (pkt.type == TYPE_RESPONSE) {
            buf.writeUtf(pkt.ritualName != null ? pkt.ritualName : "");
            buf.writeVarInt(pkt.materials.size());
            for (var m : pkt.materials) {
                buf.writeUtf(m.name);
                buf.writeBoolean(m.present);
                buf.writeVarInt(m.needed);
                buf.writeVarInt(m.found);
            }
        }
    }

    public static RitualPreviewPacket decode(FriendlyByteBuf buf) {
        RitualPreviewPacket pkt = new RitualPreviewPacket();
        pkt.type = buf.readByte();
        pkt.altarPos = buf.readBlockPos();
        if (pkt.type == TYPE_RESPONSE) {
            pkt.ritualName = buf.readUtf();
            int size = buf.readVarInt();
            for (int i = 0; i < size; i++) {
                pkt.materials.add(new MaterialInfo(
                    buf.readUtf(), buf.readBoolean(), buf.readVarInt(), buf.readVarInt()));
            }
        }
        return pkt;
    }

    public static void handle(RitualPreviewPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (pkt.type == TYPE_REQUEST) {
                Previews.handleRequest(ctx.get().getSender(), pkt.altarPos);
            } else {
                Previews.handleResponse(pkt);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
