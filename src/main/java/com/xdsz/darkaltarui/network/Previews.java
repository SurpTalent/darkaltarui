package com.xdsz.darkaltarui.network;

import com.Polarice3.Goety.common.blocks.entities.DarkAltarBlockEntity;
import com.Polarice3.Goety.common.crafting.ModRecipeSerializer;
import com.Polarice3.Goety.common.crafting.RitualRecipe;
import com.xdsz.darkaltarui.AdvancedMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务端：扫描激活物品→找配方→对比底座→返回结果。
 * 客户端：缓存最新结果供 Overlay 渲染。
 */
public class Previews {

    private static RitualPreviewPacket latest = null;

    public static RitualPreviewPacket getLatest() { return latest; }

    /** 服务端处理请求 */
    public static void handleRequest(ServerPlayer sp, BlockPos altarPos) {
        Level level = sp.level();
        BlockEntity be = level.getBlockEntity(altarPos);
        if (!(be instanceof DarkAltarBlockEntity altar)) return;

        // 取祭坛上的激活物品
        var cap = altar.getCapability(ForgeCapabilities.ITEM_HANDLER, null);
        if (!cap.isPresent()) return;
        IItemHandler h = cap.resolve().get();
        ItemStack activation = h.getSlots() > 0 ? h.getStackInSlot(0) : ItemStack.EMPTY;
        if (activation.isEmpty()) { latest = null; return; }

        // 找匹配的仪式配方
        var recipes = level.getRecipeManager().getAllRecipesFor(ModRecipeSerializer.RITUAL_TYPE.get());
        RitualRecipe match = null;
        for (var r : recipes) {
            if (r.getActivationItem().test(activation)) {
                match = r; break;
            }
        }
        if (match == null) { latest = null; return; }

        // 扫描底座物品
        List<ItemStack> pedestals = new ArrayList<>();
        for (int dx = -3; dx <= 3; dx += 2) {
            for (int dz = -3; dz <= 3; dz += 2) {
                if (dx == 0 || dz == 0 || Math.abs(dx) + Math.abs(dz) != 4) continue;
                BlockPos pPos = altarPos.offset(dx, 0, dz);
                BlockEntity pBe = level.getBlockEntity(pPos);
                if (pBe != null) {
                    var pCap = pBe.getCapability(ForgeCapabilities.ITEM_HANDLER, null);
                    pCap.ifPresent(ph -> {
                        ItemStack ps = ph.getSlots() > 0 ? ph.getStackInSlot(0) : ItemStack.EMPTY;
                        pedestals.add(ps);
                    });
                }
            }
        }

        // 对比配方
        RitualPreviewPacket pkt = new RitualPreviewPacket();
        pkt.type = RitualPreviewPacket.TYPE_RESPONSE;
        pkt.ritualName = match.getRitualType().toString();
        for (net.minecraft.world.item.crafting.Ingredient ing : match.getIngredients()) {
            ItemStack need = ing.getItems().length > 0 ? ing.getItems()[0] : ItemStack.EMPTY;
            if (need.isEmpty()) continue;
            int needed = need.getCount();
            int found = 0;
            for (ItemStack have : pedestals) {
                if (ing.test(have)) found += have.getCount();
            }
            pkt.materials.add(new RitualPreviewPacket.MaterialInfo(
                need.getDisplayName().getString(), found >= needed, needed, found));
        }
        ModNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp), pkt);
    }

    /** 客户端接收响应 */
    public static void handleResponse(RitualPreviewPacket pkt) {
        latest = pkt;
    }
}
