package com.xdsz.darkaltarui.client;

import com.xdsz.darkaltarui.network.ModNetwork;
import com.xdsz.darkaltarui.network.Previews;
import com.xdsz.darkaltarui.network.RitualPreviewPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber({Dist.CLIENT})
public class RitualPreviewOverlay {

    private static int tick = 0;
    private static BlockPos lastPos = null;
    private static RitualPreviewPacket cached = null;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (++tick % 30 != 0) return; // 降低频率，减少卡顿

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null) {
            cached = null;
            return;
        }

        HitResult hit = mc.hitResult;
        if (!(hit instanceof BlockHitResult bhr)) { cached = null; return; }
        BlockPos pos = bhr.getBlockPos();
        if (pos.equals(lastPos) && cached != null) return;
        lastPos = pos;

        BlockEntity be = mc.level.getBlockEntity(pos);
        if (be == null || !be.getClass().getName().contains("DarkAltarBlockEntity")) {
            cached = null;
            return;
        }

        RitualPreviewPacket req = new RitualPreviewPacket();
        req.type = RitualPreviewPacket.TYPE_REQUEST;
        req.altarPos = pos;
        ModNetwork.CHANNEL.sendToServer(req);
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (cached == null) return;
        RitualPreviewPacket pkt = cached;

        GuiGraphics g = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;
        int x = 8, y = 8;
        int lineH = 12;

        // 计算宽度
        int maxW = 160;
        for (var m : pkt.materials)
            maxW = Math.max(maxW, font.width("  " + m.name + " ✅ " + m.found + "/" + m.needed));
        maxW = Math.max(maxW, font.width(" 当前仪式需求: " + pkt.ritualType + (pkt.missing.isEmpty() ? " ✅" : " ❌")));

        // 背景
        int lines = 3 + pkt.materials.size();
        g.fill(x, y, x + maxW + 10, y + lines * lineH + 6, 0xDD222222);
        g.fill(x, y, x + 3, y + lines * lineH + 6, 0xFFFFAA00); // 左边金色条

        x += 6; y += 4;
        g.drawString(font, "当前预合成: §e" + pkt.ritualName, x, y, 0xFFFFFF); y += lineH;
        String status = pkt.missing.isEmpty()
            ? "当前仪式需求: §a" + pkt.ritualType + " ✅"
            : "当前仪式需求: §c" + pkt.ritualType + " ❌ 缺少 " + pkt.missing;
        g.drawString(font, status, x, y, 0xFFFFFF); y += lineH;

        StringBuilder items = new StringBuilder("需要物品: ");
        for (int i = 0; i < pkt.materials.size(); i++) {
            var m = pkt.materials.get(i);
            items.append(m.present ? "§a" : "§c").append(m.name).append(" ")
                .append(m.found).append("/").append(m.needed).append("§r");
            if (i < pkt.materials.size() - 1) items.append(", ");
        }
        // 折行处理
        String itemStr = items.toString();
        int iw = font.width(itemStr);
        if (iw > maxW) {
            g.drawString(font, "需要物品:", x, y, 0xFFFFFF); y += lineH;
            for (var m : pkt.materials) {
                String line = "  " + (m.present ? "§a" : "§c") + m.name + " §7" + m.needed + "个";
                if (m.found < m.needed) line += " §c(缺" + (m.needed - m.found) + ")";
                g.drawString(font, line, x, y, 0xFFFFFF); y += lineH;
            }
        } else {
            g.drawString(font, itemStr, x, y, 0xFFFFFF);
        }
    }
}
