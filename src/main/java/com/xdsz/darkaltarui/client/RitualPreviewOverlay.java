package com.xdsz.darkaltarui.client;

import com.xdsz.darkaltarui.network.ModNetwork;
import com.xdsz.darkaltarui.network.RitualPreviewPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;

public class RitualPreviewOverlay {

    private static int tick = 0;
    private static RitualPreviewPacket cached = null;
    private static int posX = 8, posY = 8;
    private static boolean dragging = false;
    private static int dragStartX, dragStartY, panelStartX, panelStartY;

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(
            EventPriority.NORMAL, false, RenderGuiEvent.Post.class, RitualPreviewOverlay::onRenderTick);
    }

    private static void onRenderTick(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        tick++;
        if (tick % 10 == 0) {
            if (mc.hitResult instanceof BlockHitResult bhr) {
                BlockPos pos = bhr.getBlockPos();
                BlockEntity be = mc.level.getBlockEntity(pos);
                if (be != null && be.getClass().getName().contains("DarkAltarBlockEntity")) {
                    if (mc.screen == null) {
                        RitualPreviewPacket req = new RitualPreviewPacket();
                        req.type = RitualPreviewPacket.TYPE_REQUEST;
                        req.altarPos = pos;
                        ModNetwork.CHANNEL.sendToServer(req);
                    }
                    RitualPreviewPacket fresh = RitualPreviewPacket.latestClient;
                    if (fresh != null) cached = fresh;
                } else {
                    cached = null;
                }
            } else {
                cached = null;
            }
        }
        if (cached == null) return;

        Font font = mc.font;
        int lh = 12;

        // 计算面板宽高
        int w = 180;
        if (cached.supportedRituals != null && !cached.supportedRituals.isEmpty()) {
            for (String s : cached.supportedRituals) w = Math.max(w, font.width("  " + s) + 20);
        } else if (!cached.ritualName.isEmpty()) {
            for (var m : cached.materials) {
                String dn = I18n.get(m.name);
                if (dn.isEmpty()) dn = m.name;
                w = Math.max(w, font.width("  " + (m.present ? "✓" : "✗") + " " + dn + " "
                    + (m.present ? ""+m.needed : m.found+"/"+m.needed) + "个") + 20);
            }
            w = Math.max(w, font.width(" 缺少: " + cached.missing) + 20);
        }
        int lines;
        if (cached.supportedRituals != null && !cached.supportedRituals.isEmpty())
            lines = 1 + cached.supportedRituals.size();
        else if (cached.ritualName.isEmpty()) return;
        else {
            lines = 2 + cached.materials.size() + (cached.missing.isEmpty() ? 0 : 1);
            if (!cached.ritualSupported && !cached.ritualDesc.isEmpty()) lines++;
        }
        int panelW = w + 12;
        int panelH = lines * lh + 8;

        // 鼠标拖动
        var win = mc.getWindow();
        int mx = (int) (mc.mouseHandler.xpos() * win.getGuiScaledWidth() / win.getScreenWidth());
        int my = (int) (mc.mouseHandler.ypos() * win.getGuiScaledHeight() / win.getScreenHeight());
        boolean left = org.lwjgl.glfw.GLFW.glfwGetMouseButton(win.getWindow(), org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        int maxW = win.getGuiScaledWidth();
        int maxH = win.getGuiScaledHeight();

        int dragZoneH = 16;
        if (left && mx >= posX && mx <= posX + panelW && my >= posY && my <= posY + dragZoneH) {
            if (!dragging) {
                dragging = true;
                dragStartX = mx;
                dragStartY = my;
                panelStartX = posX;
                panelStartY = posY;
            }
        }
        if (!left) dragging = false;

        if (dragging) {
            posX = panelStartX + (mx - dragStartX);
            posY = panelStartY + (my - dragStartY);
            posX = Math.max(0, Math.min(posX, maxW - panelW));
            posY = Math.max(0, Math.min(posY, maxH - panelH));
        }

        int x = posX, y = posY;

        GuiGraphics g = event.getGuiGraphics();

        if (cached.supportedRituals != null && !cached.supportedRituals.isEmpty()) {
            g.fill(x, y, x + panelW, y + panelH, 0xDD222222);
            g.fill(x, y, x + 3, y + panelH, 0xFFFFAA00);
            x += 8; y += 4;
            g.drawString(font, "§e支持的仪式:", x, y, 0xFFFFFF); y += lh;
            for (String s : cached.supportedRituals) {
                g.drawString(font, "  §a✓ §f" + s, x, y, 0xFFFFFF); y += lh;
            }
            return;
        }

        g.fill(x, y, x + panelW, y + panelH, 0xDD222222);
        g.fill(x, y, x + panelW, y + 16, 0xFF333333); // 拖动条
        g.fill(x, y, x + 3, y + panelH, 0xFFFFAA00);
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
            String displayName = I18n.get(m.name);
            if (displayName.isEmpty()) displayName = m.name;
            String count = m.present ? (m.needed + "个") : (m.found + "/" + m.needed + "个");
            g.drawString(font, "  " + (m.present ? "§a✓" : "§c✗") + " §f" + displayName + " §7" + count, x, y, 0xFFFFFF);
            y += lh;
        }
    }
}
