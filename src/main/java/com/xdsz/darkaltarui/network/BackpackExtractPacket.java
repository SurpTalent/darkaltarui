package com.xdsz.darkaltarui.network;

import com.sighs.apricityui.instance.ApricityContainerMenu;
import com.xdsz.darkaltarui.AdvancedMod;
import com.xdsz.darkaltarui.rs.RSUtils;
import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.api.storage.cache.InvalidateCause;
import com.refinedmods.refinedstorage.api.util.Action;
import com.refinedmods.refinedstorage.api.util.IComparer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticatedbackpacks.api.CapabilityBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class BackpackExtractPacket {

    private final List<ItemStack> ingredients;
    private final int altarX, altarY, altarZ;

    public BackpackExtractPacket(List<ItemStack> ingredients, int ax, int ay, int az) {
        this.ingredients = ingredients;
        this.altarX = ax; this.altarY = ay; this.altarZ = az;
    }

    public static void encode(BackpackExtractPacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.ingredients.size());
        for (ItemStack s : pkt.ingredients) buf.writeItem(s);
        buf.writeInt(pkt.altarX);
        buf.writeInt(pkt.altarY);
        buf.writeInt(pkt.altarZ);
    }

    public static BackpackExtractPacket decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<ItemStack> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) list.add(buf.readItem());
        return new BackpackExtractPacket(list, buf.readInt(), buf.readInt(), buf.readInt());
    }

    public static void handle(BackpackExtractPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sp = ctx.get().getSender();
            if (sp == null || !(sp.containerMenu instanceof ApricityContainerMenu menu)) return;

            int savedStart = menu.slots.size() - 36 - 12;
            int invStart = menu.slots.size() - 36;
            int slotIdx = 0;

            // 收集背包
            List<InventoryHandler> handlers = new ArrayList<>();
            try {
                PlayerInventoryProvider.get().runOnBackpacks(sp,
                    (bp, hn, id, s) -> {
                        LazyOptional<IBackpackWrapper> cap = bp.getCapability(CapabilityBackpackWrapper.getCapabilityInstance());
                        cap.resolve().ifPresent(w -> handlers.add(w.getInventoryHandler()));
                        return false;
                    });
            } catch (Exception ignored) {}

            // RS 网络
            INetwork rsNet = RSUtils.findNetworkOnPlayer(sp);
            AdvancedMod.LOGGER.info("[DAU-PKT] RS: {}", rsNet != null ? "FOUND" : "none");

            // 初始底座快照
            ItemStack[] init = new ItemStack[12];
            for (int s = 0; s < 12; s++) init[s] = menu.slots.get(savedStart + s).getItem().copy();

            // 填充底座（索引 1+）
            for (int idx = 1; idx < pkt.ingredients.size(); idx++) {
                ItemStack needed = pkt.ingredients.get(idx);
                if (needed.isEmpty()) continue;

                boolean already = false;
                for (int s = 0; s < 12 && !already; s++)
                    if (matches(needed, init[s])) { already = true; break; }
                if (already) continue;

                while (slotIdx < 12 && !menu.slots.get(savedStart + slotIdx).getItem().isEmpty()) slotIdx++;
                if (slotIdx >= 12) break;

                boolean found = extract(sp, menu, savedStart, invStart, slotIdx, needed, handlers, rsNet);
                if (found) slotIdx++;
                else AdvancedMod.LOGGER.info("[DAU-PKT] ing[{}] NOT FOUND", idx);
            }

            // 激活物品（索引 0）
            ItemStack activation = pkt.ingredients.isEmpty() ? ItemStack.EMPTY : pkt.ingredients.get(0);
            if (!activation.isEmpty()) {
                boolean ex = extract(sp, menu, savedStart, invStart, -1, activation, handlers, rsNet);
                if (ex) {
                    BlockPos ap = new BlockPos(pkt.altarX, pkt.altarY, pkt.altarZ);
                    var be = sp.level().getBlockEntity(ap);
                    if (be != null) {
                        var cap = be.getCapability(ForgeCapabilities.ITEM_HANDLER, null);
                        cap.resolve().ifPresent(h -> {
                            if (h.getSlots() > 0 && h.getStackInSlot(0).isEmpty()) {
                                h.insertItem(0, activation.copy(), false);
                                be.setChanged();
                                sp.level().sendBlockUpdated(ap, be.getBlockState(), be.getBlockState(), 3);
                                AdvancedMod.LOGGER.info("[DAU-PKT] activation on altar");
                            }
                        });
                    }
                } else AdvancedMod.LOGGER.info("[DAU-PKT] activation NOT FOUND");
            }

            if (rsNet != null && slotIdx > 0)
                rsNet.getItemStorageCache().invalidate(InvalidateCause.DISK_INVENTORY_CHANGED);

            // 关键：强制同步容器到客户端（修复幽灵物品）
            if (slotIdx > 0) {
                menu.broadcastChanges();
            }

            AdvancedMod.LOGGER.info("[DAU-PKT] done, filled {}", slotIdx);
        });
        ctx.get().setPacketHandled(true);
    }

    /** 提取 1 个物品到指定槽位（slotIdx 为 -1 表示只提取不放置） */
    private static boolean extract(ServerPlayer sp, ApricityContainerMenu menu,
            int savedStart, int invStart, int slotIdx,
            ItemStack needed, List<InventoryHandler> handlers, INetwork rsNet) {
        // 玩家背包
        for (int i = 0; i < 36; i++) {
            ItemStack st = menu.slots.get(invStart + i).getItem();
            if (st.isEmpty()) continue;
            if (matches(needed, st)) {
                if (slotIdx >= 0) {
                    menu.clicked(invStart + i, 0, ClickType.PICKUP, sp);
                    menu.clicked(savedStart + slotIdx, 1, ClickType.PICKUP, sp);
                    menu.clicked(invStart + i, 0, ClickType.PICKUP, sp);
                } else {
                    // 激活物品消耗（shrink + 标记脏）
                    st.shrink(1);
                    menu.slots.get(invStart + i).setChanged();
                }
                return true;
            }
        }
        // 背包
        for (InventoryHandler h : handlers) {
            for (int i = 0; i < h.getSlots(); i++) {
                ItemStack st = h.getSlotStack(i);
                if (st.isEmpty()) continue;
                if (matches(needed, st)) {
                    ItemStack reduced = st.copy(); reduced.shrink(1);
                    h.setStackInSlot(i, reduced);
                    if (slotIdx >= 0) {
                        ItemStack placed = st.copy(); placed.setCount(1);
                        menu.slots.get(savedStart + slotIdx).set(placed);
                        menu.slots.get(savedStart + slotIdx).setChanged();
                    }
                    return true;
                }
            }
        }
        // RS
        if (rsNet != null) {
            var storages = rsNet.getItemStorageCache().getStorages();
            for (var storage : storages) {
                for (ItemStack stored : storage.getStacks()) {
                    if (!matches(needed, stored)) continue;
                    ItemStack toExtract = stored.copy(); toExtract.setCount(1);
                    storage.extract(toExtract, 1, IComparer.COMPARE_NBT, Action.PERFORM);
                    rsNet.getItemStorageTracker().changed(sp, toExtract);
                    if (slotIdx >= 0) {
                        ItemStack placed = stored.copy(); placed.setCount(1);
                        menu.slots.get(savedStart + slotIdx).set(placed);
                        menu.slots.get(savedStart + slotIdx).setChanged();
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean matches(ItemStack recipe, ItemStack backpack) {
        if (!ItemStack.isSameItem(recipe, backpack)) return false;
        var rTag = recipe.getTag();
        if (rTag == null || rTag.isEmpty()) return true;
        var bTag = backpack.getTag();
        if (bTag == null) return false;
        for (String key : rTag.getAllKeys()) {
            if (!bTag.contains(key)) return false;
            if (!bTag.get(key).equals(rTag.get(key))) return false;
        }
        return true;
    }
}
