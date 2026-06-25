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
        // 激活物品（index 0）— 从 backing recipe 或 catalyst 获取
        ItemStack activation = ItemStack.EMPTY;
        if (recipe.getBackingRecipe() instanceof com.Polarice3.Goety.common.crafting.RitualRecipe rr) {
            var ing = rr.getActivationItem();
            if (ing != null && ing.getItems().length > 0) activation = ing.getItems()[0].copy();
        }
        if (activation.isEmpty()) {
            var cats = recipe.getCatalysts();
            if (!cats.isEmpty()) activation = cats.get(0).getEmiStacks().get(0).getItemStack().copy();
        }
        // 底座物品（跳过 index 0，那是 EMI 显示的催化剂/激活物品）
        List<ItemStack> ingredients = new ArrayList<>();
        ingredients.add(activation); // index 0 = 放黑暗祭坛
        var inputs = recipe.getInputs();
        for (int i = 1; i < inputs.size(); i++) {
            var input = inputs.get(i);
            if (input.isEmpty()) ingredients.add(ItemStack.EMPTY);
            else ingredients.add(input.getEmiStacks().get(0).getItemStack().copy());
        }
        int x = currentAltarPos != null ? currentAltarPos.getX() : 0;
        int y = currentAltarPos != null ? currentAltarPos.getY() : 0;
        int z = currentAltarPos != null ? currentAltarPos.getZ() : 0;
        ModNetwork.CHANNEL.sendToServer(new BackpackExtractPacket(ingredients, x, y, z));
        AdvancedMod.LOGGER.info("[DAU] act={} ped={} altar=({},{},{})",
            activation.isEmpty() ? "none" : activation.getDisplayName().getString(), ingredients.size()-1, x, y, z);
        return true;
    }
}
