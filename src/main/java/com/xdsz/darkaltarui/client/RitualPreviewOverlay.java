package com.xdsz.darkaltarui.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xdsz.darkaltarui.network.ModNetwork;
import com.xdsz.darkaltarui.network.Previews;
import com.xdsz.darkaltarui.network.RitualPreviewPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        tick++;
        if (tick % 10 != 0) return; // 每10tick一次

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        HitResult hit = mc.hitResult;
        if (!(hit instanceof BlockHitResult bhr)) return;
        BlockPos pos = bhr.getBlockPos();
        BlockEntity be = mc.level.getBlockEntity(pos);
        if (be == null) return;
        String cn = be.getClass().getName();
        if (!cn.contains("DarkAltarBlockEntity")) return;

        // 发送预览请求
        RitualPreviewPacket req = new RitualPreviewPacket();
        req.type = RitualPreviewPacket.TYPE_REQUEST;
        req.altarPos = pos;
        ModNetwork.CHANNEL.sendToServer(req);
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        RitualPreviewPacket pkt = Previews.getLatest();
        if (pkt == null || pkt.ritualName == null) return;

        GuiGraphics g = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;
        int x = 10;
        int y = 10;

        // 半透明背景
        int maxW = 0;
        for (var m : pkt.materials) {
            int w = font.width("  " + m.name + ": " + m.found + "/" + m.needed + "  ");
            if (w > maxW) maxW = w;
        }
        maxW = Math.max(maxW, font.width("  🕯️ " + pkt.ritualName + "  "));
        int bgH = 12 + (pkt.materials.size() + 1) * 11;
        g.fill(x, y, x + maxW, y + bgH, 0x80000000);

        // 仪式名
        g.drawString(font, "🕯️ " + pkt.ritualName, x + 4, y + 4, 0xFFD4A017);
        y += 15;

        // 材料列表
        for (var m : pkt.materials) {
            String line = (m.present ? "§a✓" : "§c✗") + " §f" + m.name + " §7" + m.found + "/" + m.needed;
            g.drawString(font, line, x + 4, y, 0xFFFFFF);
            y += 11;
        }
    }
}
