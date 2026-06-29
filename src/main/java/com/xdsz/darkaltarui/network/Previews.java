package com.xdsz.darkaltarui.network;

import com.Polarice3.Goety.common.blocks.entities.RitualBlockEntity;
import com.Polarice3.Goety.common.crafting.ModRecipeSerializer;
import com.Polarice3.Goety.common.crafting.RitualRecipe;
import com.Polarice3.Goety.common.ritual.RitualRequirements;
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

    private static final int SCAN = 8;
    private static RitualPreviewPacket latest = null;

    private static final Map<String, String> RITUAL_NAMES = Map.ofEntries(
        Map.entry("animation", "活力仪式"), Map.entry("necroturgy", "死灵仪式"),
        Map.entry("forge", "锻造仪式"), Map.entry("geoturgy", "大地仪式"),
        Map.entry("magic", "魔法仪式"), Map.entry("sabbath", "安息仪式"),
        Map.entry("adept_nether", "下界仪式"), Map.entry("expert_nether", "下界进阶仪式"),
        Map.entry("end", "末地仪式"), Map.entry("frost", "冰霜仪式"),
        Map.entry("sky", "天空仪式"), Map.entry("storm", "风暴仪式"),
        Map.entry("deep", "深渊仪式"), Map.entry("overgrown", "增生仪式"),
        Map.entry("divination", "占卜仪式")
    );
    private static final Map<String, String> RITUAL_DESC = Map.ofEntries(
        Map.entry("animation", "1高级附魔台 16智慧书架 1带书讲台"),
        Map.entry("master_magic", "1高级附魔台 16智慧书架 1带书讲台"),
        Map.entry("forge", "1充满熔岩的大锅 1锻造台 1铁砧 1符文镶嵌台"),
        Map.entry("master_forge", "1充满熔岩的大锅 1锻造台 1铁砧 1符文镶嵌台"),
        Map.entry("necroturgy", "16非木质台阶 16幽匿块 8非空花盆 须夜晚"),
        Map.entry("overgrown", "丛林群系 或 32叶子 16增生之根 16覆苔方块"),
        Map.entry("frost", "降雪群系 或 16冰块 8雪块 4极寒冰灯"),
        Map.entry("geoturgy", "高度≤32不露天 或 1锻造台 8紫水晶块 16深板岩"),
        Map.entry("magic", "16书架 1附魔台 1有书讲台"),
        Map.entry("storm", "暴露于风暴 4避雷针 20锁链 12铜块"),
        Map.entry("deep", "深海群系 或 4海晶灯 16海晶石 16花岗岩 须水下"),
        Map.entry("end", "须在末地 16虚空块 64末地石砖 32紫珀方块"),
        Map.entry("sabbath", "8哭泣黑曜石 16黑曜石 4灵魂火")
    );

    private static List<ItemStack> collectPedestals(Level level, BlockPos pos) {
        List<ItemStack> list = new ArrayList<>();
        for (int dx = -SCAN; dx <= SCAN; dx++)
            for (int dy = -SCAN; dy <= SCAN; dy++)
                for (int dz = -SCAN; dz <= SCAN; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos pPos = pos.offset(dx, dy, dz);
                    BlockEntity be = level.getBlockEntity(pPos);
                    if (be == null) continue;
                    if (!be.getClass().getName().contains("PedestalBlockEntity")) continue;
                    var cap = be.getCapability(ForgeCapabilities.ITEM_HANDLER, null);
                    cap.ifPresent(ph -> {
                        ItemStack ps = ph.getSlots() > 0 ? ph.getStackInSlot(0) : ItemStack.EMPTY;
                        if (!ps.isEmpty()) list.add(ps.copy());
                    });
                }
        return list;
    }

    private static RitualRecipe findBestMatch(List<ItemStack> pedestals, List<RitualRecipe> recipes) {
        RitualRecipe best = null;
        int bestScore = -1;
        for (var r : recipes) {
            int score = 0, total = 0;
            List<ItemStack> rem = new ArrayList<>(pedestals.size());
            for (ItemStack ps : pedestals) rem.add(ps.copy());
            for (Ingredient ing : r.getIngredients()) {
                int need = ing.getItems().length > 0 ? ing.getItems()[0].getCount() : 0;
                int found = 0;
                int toFind = need;
                for (Iterator<ItemStack> it = rem.iterator(); it.hasNext() && toFind > 0;) {
                    ItemStack ps = it.next();
                    if (ing.test(ps)) {
                        int take = Math.min(ps.getCount(), toFind);
                        found += take;
                        toFind -= take;
                        ps.shrink(take);
                        if (ps.isEmpty()) it.remove();
                    }
                }
                score += Math.min(found, need);
                total += need;
            }
            if (score > bestScore) { bestScore = score; best = r; }
        }
        return best;
    }

    public static void handleRequest(ServerPlayer sp, BlockPos pos) {
        Level level = sp.level();
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof RitualBlockEntity ritualBE)) { latest = null; return; }

        List<ItemStack> pedestals = collectPedestals(level, pos);
        var all = level.getRecipeManager().getAllRecipesFor(ModRecipeSerializer.RITUAL_TYPE.get());

        Set<String> supported = new LinkedHashSet<>();
        for (String type : RITUAL_NAMES.keySet()) {
            try { if (RitualRequirements.getProperStructure(type, ritualBE, pos, level)) supported.add(type); }
            catch (Exception ignored) {}
        }

        if (pedestals.isEmpty()) {
            if (supported.isEmpty()) { latest = null; return; }
            RitualPreviewPacket pkt = new RitualPreviewPacket();
            pkt.type = RitualPreviewPacket.TYPE_RESPONSE;
            pkt.ritualName = "";
            pkt.ritualType = "支持的仪式:";
            pkt.supportedRituals = new ArrayList<>();
            for (String t : supported) pkt.supportedRituals.add(RITUAL_NAMES.get(t));
            ModNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp), pkt);
            latest = pkt;
            return;
        }

        RitualRecipe match = findBestMatch(pedestals, all);
        if (match == null) { latest = null; return; }

        String craftType = match.getCraftType();
        if (craftType.isEmpty()) craftType = match.getRitualType().getPath();
        String ritualName = RITUAL_NAMES.getOrDefault(craftType, craftType);
        String recipeId = match.getResultItem(sp.serverLevel().registryAccess()).getDescriptionId();
        boolean ritualOk = supported.contains(craftType);

        // 消耗追踪
        List<ItemStack> rem = new ArrayList<>(pedestals.size());
        for (ItemStack ps : pedestals) rem.add(ps.copy());
        Map<String, int[]> agg = new LinkedHashMap<>();
        for (Ingredient ing : match.getIngredients()) {
            ItemStack[] items = ing.getItems();
            if (items.length == 0) continue;
            String name = items[0].getDescriptionId();
            int need = items[0].getCount();
            int found = 0, toFind = need;
            for (Iterator<ItemStack> it = rem.iterator(); it.hasNext() && toFind > 0;) {
                ItemStack ps = it.next();
                if (ing.test(ps)) {
                    int take = Math.min(ps.getCount(), toFind);
                    found += take;
                    toFind -= take;
                    ps.shrink(take);
                    if (ps.isEmpty()) it.remove();
                }
            }
            String key = name + "\0" + need;
            int[] cur = agg.get(key);
            if (cur == null) agg.put(key, new int[]{need, found});
            else { cur[0] += need; cur[1] += found; }
        }
        List<RitualPreviewPacket.MaterialInfo> mat = new ArrayList<>();
        List<String> miss = new ArrayList<>();
        for (var e : agg.entrySet()) {
            String name = e.getKey().split("\0")[0];
            int need = e.getValue()[0], found = e.getValue()[1];
            boolean ok = found >= need;
            if (!ok) miss.add(name + "×" + (need - found));
            mat.add(new RitualPreviewPacket.MaterialInfo(name, ok, need, found));
        }

        RitualPreviewPacket pkt = new RitualPreviewPacket();
        pkt.type = RitualPreviewPacket.TYPE_RESPONSE;
        pkt.ritualName = recipeId;
        pkt.ritualType = ritualName;
        pkt.missing = String.join(", ", miss);
        pkt.materials = mat;
        pkt.ritualSupported = ritualOk;
        pkt.supportedRituals = null;
        pkt.ritualDesc = ritualOk ? "" : RITUAL_DESC.getOrDefault(craftType, "");

        ModNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp), pkt);
        latest = pkt;
    }

    public static RitualPreviewPacket getLatest() { return latest; }
    public static void handleResponse(RitualPreviewPacket pkt) { latest = pkt; }
}
