package com.xdsz.darkaltarui.item;

import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.api.network.grid.IGridManager;
import com.refinedmods.refinedstorage.api.network.item.INetworkItem;
import com.refinedmods.refinedstorage.api.network.item.INetworkItemManager;
import com.refinedmods.refinedstorage.api.network.security.Permission;
import com.refinedmods.refinedstorage.apiimpl.API;
import com.refinedmods.refinedstorage.apiimpl.network.grid.factory.WirelessGridGridFactory;
import com.refinedmods.refinedstorage.inventory.player.PlayerSlot;
import com.refinedmods.refinedstorage.item.NetworkItem;
import com.refinedmods.refinedstorage.util.LevelUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * 灵魂便捷合成终端。
 * 继承 RS NetworkItem：右键 RS 设备绑定，右键空气消耗灵魂打开网格。
 */
public class SoulRemoteTerminal extends NetworkItem {

    public SoulRemoteTerminal() {
        super(new Item.Properties().stacksTo(1), false, () -> 0);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide && isValid(stack)) {
            int cost = com.xdsz.darkaltarui.DarkAltarConfig.TERMINAL_SOUL_COST.get();
            if (!SoulEnergyHelper.hasEnough(player, cost)) {
                player.sendSystemMessage(Component.translatable("chat.darkaltarui.soul_not_enough", cost));
                return InteractionResultHolder.fail(stack);
            }
            SoulEnergyHelper.consume(player, cost);

            applyNetwork(level.getServer(), stack,
                n -> n.getNetworkItemManager().open(player, stack,
                    com.refinedmods.refinedstorage.inventory.player.PlayerSlot.getSlotForHand(player, hand)),
                player::sendSystemMessage);
        }

        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.darkaltarui.soul_remote_terminal.1"));
        tooltip.add(Component.translatable("tooltip.darkaltarui.soul_remote_terminal.2"));
        if (isValid(stack)) {
            tooltip.add(Component.translatable("tooltip.darkaltarui.soul_remote_terminal.bound",
                getX(stack), getY(stack), getZ(stack)));
        }
    }

    @Nonnull
    @Override
    public INetworkItem provide(INetworkItemManager manager, Player player, ItemStack stack, PlayerSlot slot) {
        return new SoulNetworkItem(manager, player, stack, slot);
    }

    /** 自定义 INetworkItem：跳过 FE 检查，直接打开无线网格 */
    private static class SoulNetworkItem implements INetworkItem {
        private final INetworkItemManager manager;
        private final Player player;
        private final ItemStack stack;

        SoulNetworkItem(INetworkItemManager m, Player p, ItemStack s, PlayerSlot sl) {
            this.manager = m;
            this.player = p;
            this.stack = s;
        }

        @Override public Player getPlayer() { return player; }

        @Override public void drainEnergy(int amount) { /* 使用灵魂能量，不走 FE */ }

        @Override
        public boolean onOpen(INetwork network) {
            if (!network.getSecurityManager().hasPermission(Permission.MODIFY, player)) {
                LevelUtils.sendNoPermissionMessage(player);
                return false;
            }
            API.instance().getGridManager().openGrid(
                WirelessGridGridFactory.ID,
                (ServerPlayer) player,
                stack,
                PlayerSlot.getSlotForHand(player, InteractionHand.MAIN_HAND)
            );
            return true;
        }
    }
}
