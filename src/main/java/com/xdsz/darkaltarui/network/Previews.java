package com.xdsz.darkaltarui.network;

import com.Polarice3.Goety.common.blocks.entities.DarkAltarBlockEntity;
import com.Polarice3.Goety.common.crafting.ModRecipeSerializer;
import com.Polarice3.Goety.common.crafting.RitualRecipe;
import com.xdsz.darkaltarui.AdvancedMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务端仪式预览：查祭坛物品→找配方→扫 12 底座→对比→回传。
 */
public class Previews {

    // Goety 底座偏移 (12 个菱形)
    private static final int[][] PEDESTAL_OFFSETS = {
        {-3,0,-1},{-1,0,-3},{1,0,-3},{3,0,-1},
        {3,0,1},{1,0,3},{-1,0,3},{-3,0,1},
        {-2,0,-2},{2,0,-2},{2,0,2},{-2,0,2}
    };

    private static RitualPreviewPacket latest = null;
    public static RitualPreviewPacket getLatest() { return latest; }

    public static void handleRequest(ServerPlayer sp, BlockPos altarPos) {
        Level level = sp.level();
        BlockEntity be = level.getBlockEntity(altarPos);
        if (!(be instanceof DarkAltarBlockEntity altar)) { latest = null; return; }

        // 祭坛上的物品
        var cap = altar.getCapability(ForgeCapabilities.ITEM_HANDLER, null);
        if (!cap.isPresent()) { latest = null; return; }
        IItemHandler h = cap.resolve().get();
        ItemStack activation = h.getSlots() > 0 ? h.getStackInSlot(0) : ItemStack.EMPTY;
        if (activation.isEmpty()) { latest = null; return; }

        // 找匹配配方
        var recipes = level.getRecipeManager().getAllRecipesFor(ModRecipeSerializer.RITUAL_TYPE.get());
        RitualRecipe match = null;
        for (var r : recipes) {
            if (r.getActivationItem() != null && r.getActivationItem().test(activation)) {
                match = r; break;
            }
        }
        if (match == null) { latest = null; return; }

        // 扫描底座
        List<ItemStack> pedestals = new ArrayList<>();
        for (int[] off : PEDESTAL_OFFSETS) {
            BlockPos pPos = altarPos.offset(off[0], off[1], off[2]);
            BlockEntity pBe = level.getBlockEntity(pPos);
            if (pBe != null) {
                var pCap = pBe.getCapability(ForgeCapabilities.ITEM_HANDLER, null);
                pCap.ifPresent(ph -> {
                    ItemStack ps = ph.getSlots() > 0 ? ph.getStackInSlot(0) : ItemStack.EMPTY;
                    if (!ps.isEmpty()) pedestals.add(ps.copy());
                });
            }
        }

        // 对比
        RitualPreviewPacket pkt = new RitualPreviewPacket();
        pkt.type = RitualPreviewPacket.TYPE_RESPONSE;
        String id = match.getId().toString();
        int slash = id.lastIndexOf('/');
        pkt.ritualName = slash >= 0 ? id.substring(slash + 1) : id; // 提取配方名
        pkt.ritualType = match.getRitualType().toString();
        if (pkt.ritualType.contains(":")) pkt.ritualType = pkt.ritualType.split(":")[1]; // 取类型短名

        List<String> msgs = new ArrayList<>();
        for (Ingredient ing : match.getIngredients()) {
            ItemStack need = ing.getItems().length > 0 ? ing.getItems()[0] : ItemStack.EMPTY;
            if (need.isEmpty()) continue;
            int required = need.getCount();
            int found = 0;
            for (ItemStack have : pedestals) {
                if (ing.test(have)) found += have.getCount();
            }
            boolean ok = found >= required;
            String name = need.getHoverName().getString();
            pkt.materials.add(new RitualPreviewPacket.MaterialInfo(name, ok, required, found));
            if (!ok) msgs.add(name + "×" + (required - found));
        }

        if (!msgs.isEmpty()) {
            pkt.missing = String.join(", ", msgs);
        }

        ModNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp), pkt);
        latest = pkt;
    }

    public static void handleResponse(RitualPreviewPacket pkt) {
        latest = pkt;
    }
}
