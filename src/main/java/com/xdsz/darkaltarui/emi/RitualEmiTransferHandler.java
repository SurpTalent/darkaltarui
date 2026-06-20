package com.xdsz.darkaltarui.emi;

import com.sighs.apricityui.instance.ApricityContainerMenu;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.EmiRecipeHandler;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public class RitualEmiTransferHandler implements EmiRecipeHandler<ApricityContainerMenu> {

    @Override
    public EmiPlayerInventory getInventory(AbstractContainerScreen<ApricityContainerMenu> screen) {
        List<EmiStack> stacks = new ArrayList<>();
        var menu = screen.getMenu();
        int start = menu.slots.size() - 36;
        for (int i = 0; i < 36; i++) {
            ItemStack s = menu.slots.get(start + i).getItem();
            stacks.add(s.isEmpty() ? EmiStack.EMPTY : EmiStack.of(s));
        }
        return new EmiPlayerInventory(stacks);
    }

    @Override
    public boolean supportsRecipe(EmiRecipe recipe) {
        var id = recipe.getCategory().getId();
        return "goety".equals(id.getNamespace()) && id.getPath().startsWith("ritual");
    }

    @Override
    public boolean canCraft(EmiRecipe recipe, EmiCraftContext<ApricityContainerMenu> ctx) {
        return true;
    }

    @Override
    public boolean craft(EmiRecipe recipe, EmiCraftContext<ApricityContainerMenu> ctx) {
        try {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player == null || mc.gameMode == null) return false;

            var menu = ctx.getScreenHandler();
            int total = menu.slots.size();
            int savedStart = total - 36 - 12;
            int invStart = total - 36;

            var inputs = recipe.getInputs();
            int slotIdx = 0;

            for (int idx = 1; idx < inputs.size() && slotIdx < 12; idx++) {
                var input = inputs.get(idx);
                if (input.isEmpty()) continue;

                boolean found = false;
                for (int i = 0; i < 36 && !found; i++) {
                    Slot invSlot = menu.slots.get(invStart + i);
                    ItemStack stack = invSlot.getItem();
                    if (stack.isEmpty()) continue;

                    for (var es : input.getEmiStacks()) {
                        if (ItemStack.isSameItemSameTags(es.getItemStack(), stack)) {
                            // 找下一个空槽位
                            while (slotIdx < 12 && !menu.slots.get(savedStart + slotIdx).getItem().isEmpty()) {
                                slotIdx++;
                            }
                            if (slotIdx >= 12) break;

                            // 模拟玩家操作：背包→光标→目标槽位(1个)→光标回背包
                            mc.gameMode.handleInventoryMouseClick(
                                menu.containerId, invStart + i, 0,
                                net.minecraft.world.inventory.ClickType.PICKUP, player);
                            mc.gameMode.handleInventoryMouseClick(
                                menu.containerId, savedStart + slotIdx, 1,
                                net.minecraft.world.inventory.ClickType.PICKUP, player);
                            mc.gameMode.handleInventoryMouseClick(
                                menu.containerId, invStart + i, 0,
                                net.minecraft.world.inventory.ClickType.PICKUP, player);
                            slotIdx++;
                            found = true;
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            com.xdsz.darkaltarui.AdvancedMod.LOGGER.error("[DarkAltarUI] EMI craft failed", e);
        }
        return true;
    }
}
