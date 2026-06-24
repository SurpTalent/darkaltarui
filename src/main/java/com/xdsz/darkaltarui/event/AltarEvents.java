package com.xdsz.darkaltarui.event;

import com.mojang.logging.LogUtils;
import com.xdsz.darkaltarui.DarkAltarConfig;
import com.xdsz.darkaltarui.emi.RitualEmiTransferHandler;
import com.xdsz.darkaltarui.network.AltarInfoPacket;
import com.xdsz.darkaltarui.network.ModNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.util.*;

public final class AltarEvents {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SCAN_RANGE = 8;
    private static final int MAX_PEDESTALS = 12;
    public static final String SAVEDDATA_NAME = "altar_pedestal_data";
    public static final String TEMPLATE_PATH = "altar/index.html";
    private static final String INVENTORY_KEY = "saved_data";

    private static final Map<String, String> RITUAL_TYPE_NAMES = Map.ofEntries(
            Map.entry("animation", "§a活化仪式"),     Map.entry("necroturgy", "§c死灵仪式"),
            Map.entry("forge", "§6锻造仪式"),          Map.entry("geoturgy", "§d地卜仪式"),
            Map.entry("magic", "§b魔法仪式"),          Map.entry("sabbath", "§4安息仪式"),
            Map.entry("adept_nether", "§4下界仪式"),   Map.entry("expert_nether", "§4下界进阶仪式"),
            Map.entry("end", "§5末地仪式"),            Map.entry("frost", "§3冰霜仪式"),
            Map.entry("sky", "§9天空仪式"),            Map.entry("storm", "§e风暴仪式"),
            Map.entry("deep", "§1深渊仪式"),           Map.entry("overgrown", "§2丛林仪式"),
            Map.entry("divination", "§d占卜仪式")
    );

    private AltarEvents() {}

    // ═══════════════ 打开 UI ═══════════════

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!event.getItemStack().isEmpty()) return;

        var player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        var level = event.getLevel();
        if (level.isClientSide()) return;

        BlockPos pos = event.getPos();
        var blockEntity = level.getBlockEntity(pos);
        var blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(
                level.getBlockState(pos).getBlock());
        if (blockId == null || !blockId.getPath().startsWith("dark_altar")) return;

        // 仪式进行中 → 交给 Goety 处理
        if (isRitualActive(blockEntity)) return;

        // 祭坛上有合成产物 → 让玩家拿走，不打开 UI
        if (hasAltarItem(blockEntity)) return;

        event.setCanceled(true);

        int souls = getCageSouls(blockEntity, level, pos);
        if (souls == 0) {
            serverPlayer.sendSystemMessage(Component.literal(
                    "§6[祭坛系统] 你的祭坛缺少能源！请在下方放置诅咒之笼并充能"));
            return;
        }

        List<PedestalInfo> pedestals = scanPedestals(level, pos);
        if (pedestals.isEmpty()) {
            serverPlayer.sendSystemMessage(Component.literal("§e[祭坛系统] 周围未检测到祭坛底座"));
            return;
        }

        persistPedestalPositions(serverPlayer, pedestals, pos);
        clearPedestalItems(level, pedestals);

        List<String> ritualTypes = getSupportedRitualTypes(blockEntity, level, pos);
        List<AltarInfoPacket.RecipeInfo> recipes = collectRecipes(level, ritualTypes);

        prefillSavedData(serverPlayer, pedestals);

        // 记录祭坛坐标供 EMI 填充使用（仅当 EMI 已加载）
        try { RitualEmiTransferHandler.currentAltarPos = pos.immutable(); } catch (NoClassDefFoundError ignored) {}

        com.sighs.apricityui.ApricityUI.menu(serverPlayer, TEMPLATE_PATH)
                .bind(b -> b.saveddata(SAVEDDATA_NAME, MAX_PEDESTALS).player());

        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                new AltarInfoPacket(pedestals.size(), souls, ritualTypes, recipes));
    }

    // ═══════════════ 关闭 UI → 提交 ═══════════════

    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        var menu = event.getContainer();
        if (!(menu instanceof com.sighs.apricityui.instance.ApricityContainerMenu auiMenu)) return;
        if (!TEMPLATE_PATH.equals(auiMenu.getTemplatePath())) return;

        var level = player.serverLevel();
        List<PedestalInfo> pedestals = loadPedestalPositions(player);
        BlockPos altarPos = loadAltarPos(player);
        clearPersistentData(player);

        if (pedestals.isEmpty()) return;

        List<ItemStack> savedItems = readFromSavedData(player);
        int placed = 0;
        int cageItemsPlaced = 0;

        for (int i = 0; i < pedestals.size(); i++) {
            PedestalInfo info = pedestals.get(i);
            ItemStack stack = i < savedItems.size() ? savedItems.get(i) : ItemStack.EMPTY;
            BlockPos pos = new BlockPos(info.x, info.y, info.z);
            if (!level.isLoaded(pos)) continue;

            BlockEntity be = level.getBlockEntity(pos);
            if (be == null) continue;

            var cap = be.getCapability(ForgeCapabilities.ITEM_HANDLER, null);
            if (!cap.isPresent()) continue;
            IItemHandler handler = cap.resolve().get();
            if (handler == null || handler.getSlots() == 0) continue;

            ItemStack oneItem = ItemStack.EMPTY;
            if (!stack.isEmpty()) {
                oneItem = stack.copy();
                oneItem.setCount(1);
                stack.shrink(1);
                if (!stack.isEmpty())
                    player.getInventory().placeItemBackInInventory(stack);
                cageItemsPlaced++;
            }

            handler.extractItem(0, 64, false);
            if (!oneItem.isEmpty()) handler.insertItem(0, oneItem, false);
            be.setChanged();
            placed++;
        }

        // 灵魂消耗
        if (DarkAltarConfig.SOUL_COST_ENABLED.get() && cageItemsPlaced > 0 && altarPos != null) {
            int cost = cageItemsPlaced * DarkAltarConfig.SOUL_COST_PER_ITEM.get();
            BlockPos cagePos = altarPos.below();
            if (level.isLoaded(cagePos)) {
                BlockEntity cageBE = level.getBlockEntity(cagePos);
                if (cageBE instanceof com.Polarice3.Goety.common.blocks.entities.CursedCageBlockEntity cage)
                    cage.decreaseSouls(cost);
            }
        }

        if (placed > 0)
            player.sendSystemMessage(Component.literal(
                    "§5✨ §d魔力操控完成！已同步 §e" + placed + " §d个祭坛底座"));
    }

    // ═══════════════ SavedData ═══════════════

    private static void prefillSavedData(ServerPlayer player, List<PedestalInfo> pedestals) {
        var server = player.getServer();
        if (server == null) return;
        var sd = com.sighs.apricityui.instance.ApricitySavedData.get(server, SAVEDDATA_NAME);
        var handler = sd.getOrCreate(INVENTORY_KEY, MAX_PEDESTALS);

        for (int i = 0; i < Math.min(MAX_PEDESTALS, pedestals.size()); i++) {
            var info = pedestals.get(i);
            if (info.itemId.isEmpty()) { handler.setStackInSlot(i, ItemStack.EMPTY); continue; }
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                    new net.minecraft.resources.ResourceLocation(info.itemId));
            if (item != null) {
                var stack = new ItemStack(item, Math.min(info.count, 64));
                if (info.tag != null && !info.tag.isEmpty()) stack.setTag(info.tag.copy());
                handler.setStackInSlot(i, stack);
            }
        }
        sd.setDirty();
    }

    private static List<ItemStack> readFromSavedData(ServerPlayer player) {
        var server = player.getServer();
        if (server == null) return List.of();
        var sd = com.sighs.apricityui.instance.ApricitySavedData.get(server, SAVEDDATA_NAME);
        var handler = sd.getOrCreate(INVENTORY_KEY, MAX_PEDESTALS);
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < MAX_PEDESTALS; i++)
            items.add(handler.getStackInSlot(i).copy());
        return items;
    }

    // ═══════════════ 扫描 ═══════════════

    private static List<PedestalInfo> scanPedestals(Level level, BlockPos center) {
        List<PedestalInfo> result = new ArrayList<>();
        for (int dx = -SCAN_RANGE; dx <= SCAN_RANGE; dx++)
            for (int dy = -SCAN_RANGE; dy <= SCAN_RANGE; dy++)
                for (int dz = -SCAN_RANGE; dz <= SCAN_RANGE; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    if (result.size() >= MAX_PEDESTALS) return result;

                    BlockPos pos = center.offset(dx, dy, dz);
                    if (!level.isLoaded(pos)) continue;
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be == null) continue;

                    String cn = be.getClass().getName();
                    if (!cn.contains("PedestalBlockEntity") || cn.contains("DarkAltarBlockEntity"))
                        continue;

                    var cap = be.getCapability(ForgeCapabilities.ITEM_HANDLER, null);
                    if (!cap.isPresent()) continue;
                    IItemHandler h = cap.resolve().get();
                    if (h == null || h.getSlots() == 0) continue;

                    ItemStack stack = h.getStackInSlot(0);
                    result.add(new PedestalInfo(pos.getX(), pos.getY(), pos.getZ(), result.size(),
                            stack.isEmpty() ? "" : net.minecraft.core.registries.BuiltInRegistries.ITEM
                                    .getKey(stack.getItem()).toString(),
                            stack.getCount(),
                            stack.hasTag() ? stack.getTag().copy() : null));
                }
        return result;
    }

    // ═══════════════ 持久化 ═══════════════

    private static void persistPedestalPositions(ServerPlayer player, List<PedestalInfo> pedestals, BlockPos altarPos) {
        CompoundTag root = player.getPersistentData();
        ListTag list = new ListTag();
        for (var info : pedestals) {
            CompoundTag t = new CompoundTag();
            t.putInt("x", info.x); t.putInt("y", info.y); t.putInt("z", info.z);
            t.putInt("slot", info.slotIndex);
            list.add(t);
        }
        root.put("altar_pedestals", list);
        root.putInt("altar_x", altarPos.getX());
        root.putInt("altar_y", altarPos.getY());
        root.putInt("altar_z", altarPos.getZ());
    }

    private static List<PedestalInfo> loadPedestalPositions(ServerPlayer player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains("altar_pedestals")) return List.of();
        ListTag list = root.getList("altar_pedestals", 10);
        List<PedestalInfo> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            result.add(new PedestalInfo(t.getInt("x"), t.getInt("y"), t.getInt("z"),
                    t.getInt("slot"), "", 0, null));
        }
        return result;
    }

    private static BlockPos loadAltarPos(ServerPlayer player) {
        CompoundTag root = player.getPersistentData();
        return root.contains("altar_x")
                ? new BlockPos(root.getInt("altar_x"), root.getInt("altar_y"), root.getInt("altar_z"))
                : null;
    }

    private static void clearPersistentData(ServerPlayer player) {
        CompoundTag root = player.getPersistentData();
        root.remove("altar_pedestals"); root.remove("altar_x"); root.remove("altar_y"); root.remove("altar_z");
    }

    private record PedestalInfo(int x, int y, int z, int slotIndex, String itemId, int count, CompoundTag tag) {}

    private static void clearPedestalItems(Level level, List<PedestalInfo> pedestals) {
        for (var info : pedestals) {
            BlockPos pos = new BlockPos(info.x, info.y, info.z);
            if (!level.isLoaded(pos)) continue;
            BlockEntity be = level.getBlockEntity(pos);
            if (be == null) continue;
            var cap = be.getCapability(ForgeCapabilities.ITEM_HANDLER, null);
            if (!cap.isPresent()) continue;
            IItemHandler h = cap.resolve().get();
            if (h == null || h.getSlots() == 0) continue;
            h.extractItem(0, 64, false);
            be.setChanged();
        }
    }

    // ═══════════════ 配方收集 ═══════════════

    private static List<AltarInfoPacket.RecipeInfo> collectRecipes(Level level, List<String> supportedTypes) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel sl)) return List.of();
        List<String> pureTypes = supportedTypes.stream()
                .map(s -> s.replaceAll("§[0-9a-fk-or]", "")).toList();
        var recipes = sl.getRecipeManager().getAllRecipesFor(
                com.Polarice3.Goety.common.crafting.ModRecipeSerializer.RITUAL_TYPE.get());
        List<AltarInfoPacket.RecipeInfo> result = new ArrayList<>();

        for (var r : recipes) {
            String ct = r.getCraftType();
            boolean match = pureTypes.isEmpty() || pureTypes.stream().anyMatch(pt -> ct != null && ct.contains(pt));
            if (!match) continue;

            List<List<ItemStack>> ingStacks = new ArrayList<>();
            for (var ing : r.getIngredients()) {
                List<ItemStack> stacks = Arrays.stream(ing.getItems())
                        .filter(s -> !s.isEmpty()).map(ItemStack::copy).toList();
                if (!stacks.isEmpty()) ingStacks.add(stacks);
            }

            result.add(new AltarInfoPacket.RecipeInfo(r.getId(),
                    r.getResultItem(sl.registryAccess()).getDisplayName().getString(),
                    ingStacks, r.getResultItem(sl.registryAccess()).copy(),
                    ct != null ? ct : "", r.getSoulCost()));
            if (result.size() >= 30) break;
        }
        return result;
    }

    // ═══════════════ 仪式类型 ═══════════════

    /** 检查祭坛本身是否有物品（合成产物） */
    private static boolean hasAltarItem(BlockEntity be) {
        if (be == null) return false;
        var cap = be.getCapability(ForgeCapabilities.ITEM_HANDLER, null);
        if (!cap.isPresent()) return false;
        IItemHandler h = cap.resolve().get();
        return h != null && h.getSlots() > 0 && !h.getStackInSlot(0).isEmpty();
    }

    private static boolean isRitualActive(BlockEntity be) {
        return be instanceof com.Polarice3.Goety.common.blocks.entities.DarkAltarBlockEntity da
                && da.getCurrentRitualRecipe() != null;
    }

    private static int getCageSouls(BlockEntity altarEntity, Level level, BlockPos altarPos) {
        if (!(altarEntity instanceof com.Polarice3.Goety.common.blocks.entities.DarkAltarBlockEntity))
            return 0;
        BlockPos cagePos = altarPos.below();
        if (!level.isLoaded(cagePos)) return 0;
        if (!level.getBlockState(cagePos).is(
                com.Polarice3.Goety.common.blocks.ModBlocks.CURSED_CAGE_BLOCK.get())) return 0;
        BlockEntity cageBE = level.getBlockEntity(cagePos);
        return cageBE instanceof com.Polarice3.Goety.common.blocks.entities.CursedCageBlockEntity cage
                ? cage.getSouls() : 0;
    }

    private static List<String> getSupportedRitualTypes(BlockEntity be, Level level, BlockPos pos) {
        if (!(be instanceof com.Polarice3.Goety.common.blocks.entities.RitualBlockEntity ritualBE))
            return List.of();
        List<String> results = new ArrayList<>();
        for (String type : RITUAL_TYPE_NAMES.keySet()) {
            try {
                if (com.Polarice3.Goety.common.ritual.RitualRequirements
                        .getProperStructure(type, ritualBE, pos, level))
                    results.add(RITUAL_TYPE_NAMES.get(type));
            } catch (Exception ignored) {}
        }
        return results;
    }
}
