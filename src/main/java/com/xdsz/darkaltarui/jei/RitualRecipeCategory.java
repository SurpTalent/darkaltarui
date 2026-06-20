package com.xdsz.darkaltarui.jei;

import com.Polarice3.Goety.common.crafting.RitualRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class RitualRecipeCategory implements IRecipeCategory<RitualRecipe> {

    private final IDrawable bg;
    private final IDrawable icon;

    public RitualRecipeCategory(IGuiHelper h) {
        this.bg = h.createBlankDrawable(160, 80);
        this.icon = h.createDrawableItemStack(new ItemStack(Items.ENCHANTING_TABLE));
    }

    @Override public RecipeType<RitualRecipe> getRecipeType() { return DarkAltarJEIPlugin.RITUAL; }
    @Override public Component getTitle() { return Component.literal("仪式合成"); }
    @Override public IDrawable getBackground() { return bg; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder b, RitualRecipe r, IFocusGroup f) {
        var act = r.getActivationItem();
        // 激活物品 → CATALYST 角色（点击+不会自动填充）
        if (act != null && !act.isEmpty())
            b.addSlot(RecipeIngredientRole.CATALYST, 72, 10).addIngredients(act);
        int x = 10, y = 30;
        for (var ing : r.getIngredients()) {
            b.addSlot(RecipeIngredientRole.INPUT, x, y).addIngredients(ing);
            x += 18; if (x > 140) { x = 10; y += 18; }
        }
        ItemStack out = r.getResultItem(Minecraft.getInstance().level.registryAccess());
        if (!out.isEmpty())
            b.addSlot(RecipeIngredientRole.OUTPUT, 130, 30).addItemStack(out);
    }

    @Override public void draw(RitualRecipe r, IRecipeSlotsView s, GuiGraphics g, double mx, double my) {
        var font = Minecraft.getInstance().font;
        g.drawString(font, "魂力:" + r.getSoulCost(), 2, 62, 0xFFaaaaaa, false);
        g.drawString(font, "时长:" + r.getDuration() + "s", 2, 72, 0xFFaaaaaa, false);
    }
}
