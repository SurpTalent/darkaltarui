package com.xdsz.darkaltarui.jei;

import com.Polarice3.Goety.common.crafting.RitualRecipe;
import com.sighs.apricityui.instance.ApricityContainerMenu;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import java.util.*;

public class RitualRecipeTransferInfo implements IRecipeTransferInfo<ApricityContainerMenu, RitualRecipe> {
    @Override public Class<? extends ApricityContainerMenu> getContainerClass() { return ApricityContainerMenu.class; }
    @Override public Optional<MenuType<ApricityContainerMenu>> getMenuType() { return Optional.empty(); }
    @Override public RecipeType<RitualRecipe> getRecipeType() { return DarkAltarJEIPlugin.RITUAL; }

    @Override public List<Slot> getRecipeSlots(ApricityContainerMenu m, RitualRecipe r) {
        List<Slot> s = new ArrayList<>();
        int start = m.slots.size() - 36 - 12;
        for (int i = 0; i < 12; i++) s.add(m.slots.get(start + i));
        return s;
    }

    @Override public List<Slot> getInventorySlots(ApricityContainerMenu m, RitualRecipe r) {
        List<Slot> s = new ArrayList<>();
        int start = m.slots.size() - 36;
        for (int i = 0; i < 36; i++) s.add(m.slots.get(start + i));
        return s;
    }

    @Override public boolean canHandle(ApricityContainerMenu m, RitualRecipe r) { return m.slots.size() >= 48; }
}
