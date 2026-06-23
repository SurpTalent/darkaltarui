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

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        tick++;
        if (tick % 20 != 0) return; // 每秒一次

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null) return;

        HitResult hit = mc.hitResult;
        if (!(hit instanceof BlockHitResult bhr)) return;
        BlockPos pos = bhr.getBlockPos();
        if (pos.equals(lastPos)) return; // 没变，不重复请求
        lastPos = pos;

        BlockEntity be = mc.level.getBlockEntity(pos);
        if (be == null || !be.getClass().getName().contains("DarkAltarBlockEntity")) {
            Previews.handleResponse(new RitualPreviewPacket()); // 清空
            return;
        }

        RitualPreviewPacket req = new RitualPreviewPacket();
        req.type = RitualPreviewPacket.TYPE_REQUEST;
        req.altarPos = pos;
        ModNetwork.CHANNEL.sendToServer(req);
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        RitualPreviewPacket pkt = Previews.getLatest();
        if (pkt == null || pkt.ritualName.isEmpty()) return;

        GuiGraphics g = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;
        int x = 12, y = 12;

        // 半透明背景
        int lineH = 11;
        int lines = 3 + pkt.materials.size();
        int maxW = font.width(" ⚡ " + pkt.ritualName + "  ");
        for (var m : pkt.materials)
            maxW = Math.max(maxW, font.width("  " + m.name + " : " + m.found + "/" + m.needed + "  "));
        maxW = Math.max(maxW, font.width("  仪式: " + pkt.ritualType + (pkt.missing.isEmpty() ? " ✅" : " ❌ " + pkt.missing) + "  "));

        g.fill(x, y, x + maxW + 8, y + lines * lineH + 6, 0xCC000000);

        y += 4;
        g.drawString(font, "⚡ " + pkt.ritualName, x + 4, y, 0xFFD4A017);
        y += lineH;

        String statusLine = pkt.missing.isEmpty()
            ? "仪式 " + pkt.ritualType + " ✅ 材料齐全"
            : "仪式 " + pkt.ritualType + " ❌ 缺少 " + pkt.missing;
        g.drawString(font, statusLine, x + 4, y, 0xFFAAAAAA);
        y += lineH;

        g.drawString(font, "材料:", x + 4, y, 0xFFAAAAAA);
        y += lineH;

        for (var m : pkt.materials) {
            String line = (m.present ? "§a✓" : "§c✗") + " §f" + m.name + " §7" + m.found + "/" + m.needed;
            g.drawString(font, line, x + 8, y, 0xFFFFFF);
            y += lineH;
        }
    }
}
