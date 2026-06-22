package com.xdsz.darkaltarui.rs;

import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.api.network.node.INetworkNodeProxy;
import com.refinedmods.refinedstorage.api.util.Action;
import com.refinedmods.refinedstorage.api.util.IComparer;
import com.refinedmods.refinedstorage.util.NetworkUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Refined Storage 网络工具。
 * 从终端 NBT 获取网络、列出物品、提取物品。
 */
public final class RSUtils {

    /** NetworkItem 同款 NBT 键 */
    public static final String NBT_NODE_X = "NodeX";
    public static final String NBT_NODE_Y = "NodeY";
    public static final String NBT_NODE_Z = "NodeZ";
    public static final String NBT_DIMENSION = "Dimension";

    /**
     * 扫描服务端玩家所有装备栏找到带 RS 绑定的物品并返回 INetwork。
     */
    @Nullable
    public static INetwork findNetworkOnPlayer(ServerPlayer sp) {
        for (ItemStack stack : sp.getInventory().items) {
            INetwork net = getNetworkFromStack(stack, sp.serverLevel());
            if (net != null) return net;
        }
        for (ItemStack stack : sp.getInventory().armor) {
            INetwork net = getNetworkFromStack(stack, sp.serverLevel());
            if (net != null) return net;
        }
        if (!sp.getOffhandItem().isEmpty()) {
            INetwork net = getNetworkFromStack(sp.getOffhandItem(), sp.serverLevel());
            if (net != null) return net;
        }
        return null;
    }

    private RSUtils() {}

    /**
     * 从 ItemStack 的 NBT 解析绑定的 RS 网络。
     * NBT 格式对齐 {@code NetworkItem}。
     */
    @Nullable
    public static INetwork getNetworkFromStack(ItemStack stack, ServerLevel fallbackLevel) {
        var tag = stack.getTag();
        if (tag == null) return null;
        if (!tag.contains(NBT_NODE_X) || !tag.contains(NBT_DIMENSION)) return null;

        int x = tag.getInt(NBT_NODE_X);
        int y = tag.getInt(NBT_NODE_Y);
        int z = tag.getInt(NBT_NODE_Z);
        String dimStr = tag.getString(NBT_DIMENSION);
        ResourceLocation dimRL = ResourceLocation.tryParse(dimStr);
        if (dimRL == null) return null;

        ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, dimRL);
        ServerLevel level = fallbackLevel.getServer().getLevel(dimKey);
        if (level == null) return null;

        var be = level.getBlockEntity(new BlockPos(x, y, z));
        if (be instanceof INetworkNodeProxy<?> proxy) {
            return NetworkUtils.getNetworkFromNode(proxy.getNode());
        }
        return null;
    }

    /**
     * 从 RS 网络提取指定物品（模拟 put → 返回剩余量）。
     * 返回 null 表示网络中没有足够物品。
     */
    @Nullable
    public static ItemStack extractFromNetwork(INetwork network, ItemStack wanted, int amount) {
        // network.extractItem(stack, count, flags, action) → returns REMAINING
        ItemStack remaining = network.extractItem(wanted, amount, IComparer.COMPARE_NBT, Action.PERFORM);
        int extracted = amount - remaining.getCount();
        if (extracted <= 0) return null;
        ItemStack result = wanted.copy();
        result.setCount(extracted);
        return result;
    }

    /**
     * 检查 RS 网络中是否有匹配物品。
     */
    public static int countInNetwork(INetwork network, ItemStack wanted) {
        var list = network.getItemStorageCache().getList().getStacks();
        int total = 0;
        for (var entry : list) {
            ItemStack s = entry.getStack();
            if (ItemStack.isSameItemSameTags(wanted, s)) {
                total += entry.getStack().getCount();
            }
        }
        return total;
    }
}
