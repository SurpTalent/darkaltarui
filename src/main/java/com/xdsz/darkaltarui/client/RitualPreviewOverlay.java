package com.xdsz.darkaltarui.client;

import com.xdsz.darkaltarui.network.ModNetwork;
import com.xdsz.darkaltarui.network.RitualPreviewPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;

public class RitualPreviewOverlay {

    private static int tick = 0;
    private static RitualPreviewPacket cached = null;

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(
            EventPriority.NORMAL, false, RenderGuiEvent.Post.class, event -> {
                if (Minecraft.getInstance().screen == null) {
                    onRenderTick(event.getGuiGraphics());
                }
            });
    }

    private static void onRenderTick(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        tick++;
        if (tick % 10 == 0) {
            boolean hit = false;
            if (mc.hitResult instanceof BlockHitResult bhr) {
                BlockPos pos = bhr.getBlockPos();
                BlockEntity be = mc.level.getBlockEntity(pos);
                if (be != null && be.getClass().getName().contains("DarkAltarBlockEntity")) {
                    RitualPreviewPacket req = new RitualPreviewPacket();
                    req.type = RitualPreviewPacket.TYPE_REQUEST;
                    req.altarPos = pos;
                    ModNetwork.CHANNEL.sendToServer(req);
                    hit = true;
                }
            }
            if (!hit) cached = null;
            else {
                RitualPreviewPacket fresh = RitualPreviewPacket.latestClient;
                if (fresh != null) cached = fresh;
            }
        }
        if (cached == null) return;

        Font font = mc.font;
        int x = 8, y = 8, lh = 12;

        if (cached.supportedRituals != null && !cached.supportedRituals.isEmpty()) {
            int w = 180;
            for (String s : cached.supportedRituals) w = Math.max(w, font.width("  " + s) + 20);
            int lines = 1 + cached.supportedRituals.size();
            g.fill(x, y, x + w, y + lines * lh + 8, 0xDD222222);
            g.fill(x, y, x + 3, y + lines * lh + 8, 0xFFFFAA00);
            x += 8; y += 4;
            g.drawString(font, "§e支持的仪式:", x, y, 0xFFFFFF); y += lh;
            for (String s : cached.supportedRituals) {
                g.drawString(font, "  §a✓ §f" + s, x, y, 0xFFFFFF);
                y += lh;
            }
            return;
        }

        if (cached.ritualName.isEmpty()) return;

        int w = 180;
        for (var m : cached.materials)
            w = Math.max(w, font.width("  " + (m.present ? "✓" : "✗") + " " + m.name + " " + (m.present ? ""+m.needed : m.found+"/"+m.needed) + "个") + 20);
        w = Math.max(w, font.width(" 缺少: " + cached.missing) + 20);

        int lines = 2 + (cached.missing.isEmpty() ? 0 : 1) + cached.materials.size();
        if (!cached.ritualSupported && !cached.ritualDesc.isEmpty()) lines++;
        g.fill(x, y, x + w, y + lines * lh + 8, 0xDD222222);
        g.fill(x, y, x + 3, y + lines * lh + 8, 0xFFFFAA00);
        x += 8; y += 4;

        String color = cached.ritualSupported ? "§a" : "§c";
        String icon = cached.ritualSupported ? "✅" : "❌";
        g.drawString(font, "预合成: §e" + cached.ritualName, x, y, 0xFFFFFF); y += lh;
        g.drawString(font, "仪式: " + color + cached.ritualType + " " + icon, x, y, 0xFFFFFF); y += lh;
        if (!cached.ritualSupported && !cached.ritualDesc.isEmpty()) {
            g.drawString(font, "  §7需求: " + cached.ritualDesc, x, y, 0xFFAAAAAA); y += lh;
        }
        if (!cached.missing.isEmpty()) {
            g.drawString(font, "  缺少: §c" + cached.missing, x, y, 0xFF6666); y += lh;
        }
        for (var m : cached.materials) {
            String count = m.present ? (m.needed + "个") : (m.found + "/" + m.needed + "个");
            g.drawString(font, "  " + (m.present ? "§a✓" : "§c✗") + " §f" + m.name + " §7" + count, x, y, 0xFFFFFF);
            y += lh;
        }
    }
}
