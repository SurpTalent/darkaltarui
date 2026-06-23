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

import java.util.*;

public class Previews {

    private static final int SCAN_RANGE = 8;
    private static final int MAX_PEDESTALS = 12;

    // Goety 仪式类型 → 中文名
    private static final Map<String,String> RITUAL_NAMES = new HashMap<>();
    static {
        RITUAL_NAMES.put("craft", "制作仪式"); RITUAL_NAMES.put("enchant", "附魔仪式");
        RITUAL_NAMES.put("forge", "锻造仪式"); RITUAL_NAMES.put("convert", "转化仪式");
        RITUAL_NAMES.put("convert_tamed", "驯服转化"); RITUAL_NAMES.put("convert_complete_tamed", "完美驯服转化");
        RITUAL_NAMES.put("summon", "召唤仪式"); RITUAL_NAMES.put("necromancy", "死灵仪式");
    }

    private static RitualPreviewPacket latest = null;
    public static RitualPreviewPacket getLatest() { return latest; }

    public static void handleRequest(ServerPlayer sp, BlockPos altarPos) {
        Level level = sp.level();
        BlockEntity be = level.getBlockEntity(altarPos);
        if (!(be instanceof DarkAltarBlockEntity altar)) { latest = null; return; }

        var cap = altar.getCapability(ForgeCapabilities.ITEM_HANDLER, null);
        if (!cap.isPresent()) { latest = null; return; }
        IItemHandler h = cap.resolve().get();
        ItemStack activation = h.getSlots() > 0 ? h.getStackInSlot(0) : ItemStack.EMPTY;
        if (activation.isEmpty()) { latest = null; return; }

        var recipes = level.getRecipeManager().getAllRecipesFor(ModRecipeSerializer.RITUAL_TYPE.get());
        RitualRecipe match = null;
        for (var r : recipes) {
            if (r.getActivationItem() != null && r.getActivationItem().test(activation)) {
                match = r; break;
            }
        }
        if (match == null) { latest = null; return; }

        // 扫底座（同 AltarEvents 逻辑）
        Map<ItemStack, Integer> pedCounts = new HashMap<>();
        for (int dx = -SCAN_RANGE; dx <= SCAN_RANGE; dx++)
            for (int dy = -SCAN_RANGE; dy <= SCAN_RANGE; dy++)
                for (int dz = -SCAN_RANGE; dz <= SCAN_RANGE; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    if (pedCounts.size() >= MAX_PEDESTALS * 2) break;
                    BlockPos pPos = altarPos.offset(dx, dy, dz);
                    BlockEntity pBe = level.getBlockEntity(pPos);
                    if (pBe == null) continue;
                    String cn = pBe.getClass().getName();
                    if (!cn.contains("PedestalBlockEntity") || cn.contains("DarkAltarBlockEntity")) continue;
                    var pCap = pBe.getCapability(ForgeCapabilities.ITEM_HANDLER, null);
                    pCap.ifPresent(ph -> {
                        ItemStack ps = ph.getSlots() > 0 ? ph.getStackInSlot(0) : ItemStack.EMPTY;
                        if (!ps.isEmpty()) pedCounts.merge(ps.copy(), ps.getCount(), Integer::sum);
                    });
                }

        // 对比
        String typeId = match.getRitualType().getPath();
        String ritualName = RITUAL_NAMES.getOrDefault(typeId, typeId);
        String recipeId = match.getId().getPath();

        List<RitualPreviewPacket.MaterialInfo> materials = new ArrayList<>();
        List<String> missingNames = new ArrayList<>();
        boolean allOk = true;

        for (Ingredient ing : match.getIngredients()) {
            ItemStack[] items = ing.getItems();
            if (items.length == 0) continue;
            ItemStack sample = items[0].copy();
            int needed = sample.getCount();

            int found = 0;
            for (var e : pedCounts.entrySet()) {
                if (ing.test(e.getKey())) found += e.getValue();
            }

            boolean ok = found >= needed;
            if (!ok) { allOk = false; missingNames.add(sample.getHoverName().getString() + "×" + (needed - found)); }
            materials.add(new RitualPreviewPacket.MaterialInfo(sample.getHoverName().getString(), ok, needed, found));
        }

        RitualPreviewPacket pkt = new RitualPreviewPacket();
        pkt.type = RitualPreviewPacket.TYPE_RESPONSE;
        pkt.ritualName = recipeId;
        pkt.ritualType = ritualName;
        pkt.missing = allOk ? "" : String.join(", ", missingNames);
        pkt.materials = (List) materials;

        AdvancedMod.LOGGER.info("[DAU-PREVIEW] {} ped items, recipe={}, type={}, mats={}",
            pedCounts.size(), recipeId, ritualName,
            materials.stream().map(m -> m.name + ":" + m.found + "/" + m.needed + (m.present ? "✓" : "✗")).toList());

        ModNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp), pkt);
        latest = pkt;
    }

    public static void handleResponse(RitualPreviewPacket pkt) {
        latest = pkt;
    }
}
