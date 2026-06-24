package com.xdsz.darkaltarui.emi;

import com.sighs.apricityui.instance.ApricityContainerMenu;
import com.xdsz.darkaltarui.AdvancedMod;
import com.xdsz.darkaltarui.network.BackpackExtractPacket;
import com.xdsz.darkaltarui.network.ModNetwork;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.EmiRecipeHandler;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * 客户端只负责发送 BackpackExtractPacket。
 * 服务端线程接收后执行实际提取（确保 BackpackStorage 持久化）。
 */
public class RitualEmiTransferHandler implements EmiRecipeHandler<ApricityContainerMenu> {

    @Override public EmiPlayerInventory getInventory(AbstractContainerScreen<ApricityContainerMenu> screen) {
        List<EmiStack> stacks = new ArrayList<>();
        var menu = screen.getMenu();
        int start = menu.slots.size() - 36;
        for (int i = 0; i < 36; i++) stacks.add(EmiStack.of(menu.slots.get(start + i).getItem()));
        return new EmiPlayerInventory(stacks);
    }
    @Override public boolean supportsRecipe(EmiRecipe recipe) {
        var id = recipe.getCategory().getId();
        return "goety".equals(id.getNamespace()) && id.getPath().startsWith("ritual");
    }
    @Override public boolean canCraft(EmiRecipe recipe, EmiCraftContext<ApricityContainerMenu> ctx) { return true; }

    /** 由 AltarEvents 在打开 UI 时设置，供 craft 使用 */
    public static BlockPos currentAltarPos = null;

    @Override
    public boolean craft(EmiRecipe recipe, EmiCraftContext<ApricityContainerMenu> ctx) {
        // 底座物品
        List<ItemStack> ingredients = new ArrayList<>();
        for (var input : recipe.getInputs()) {
            if (input.isEmpty()) ingredients.add(ItemStack.EMPTY);
            else ingredients.add(input.getEmiStacks().get(0).getItemStack().copy());
        }
        // 激活物品：从 EMI 催化剂中取（Goety 的 activation_item 在 EMI 中作为 catalyst 显示）
        ItemStack activation = ItemStack.EMPTY;
        var catalysts = recipe.getCatalysts();
        if (!catalysts.isEmpty()) activation = catalysts.get(0).getEmiStacks().get(0).getItemStack().copy();
        // 如果没有 catalyst，尝试从 backing recipe 获取
        if (activation.isEmpty() && recipe.getBackingRecipe() instanceof com.Polarice3.Goety.common.crafting.RitualRecipe rr) {
            var ing = rr.getActivationItem();
            if (ing != null && ing.getItems().length > 0) activation = ing.getItems()[0].copy();
        }
        int x = currentAltarPos != null ? currentAltarPos.getX() : 0;
        int y = currentAltarPos != null ? currentAltarPos.getY() : 0;
        int z = currentAltarPos != null ? currentAltarPos.getZ() : 0;
        // 把激活物品放在 ingredients 最前面
        List<ItemStack> all = new ArrayList<>();
        all.add(activation);
        all.addAll(ingredients);
        ModNetwork.CHANNEL.sendToServer(new BackpackExtractPacket(all, x, y, z));
        AdvancedMod.LOGGER.info("[DAU] packet sent, act={}, ped={}, altar=({},{},{})",
            activation.isEmpty() ? "none" : activation.getDisplayName().getString(), ingredients.size(), x, y, z);
        return true;
    }
}
