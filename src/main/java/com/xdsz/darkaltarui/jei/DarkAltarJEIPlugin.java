package com.xdsz.darkaltarui.jei;

import com.Polarice3.Goety.common.blocks.ModBlocks;
import com.Polarice3.Goety.common.crafting.ModRecipeSerializer;
import com.Polarice3.Goety.common.crafting.RitualRecipe;
import com.sighs.apricityui.instance.ApricityContainerScreen;
import com.xdsz.darkaltarui.AdvancedMod;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiProperties;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import java.util.List;

@JeiPlugin
public class DarkAltarJEIPlugin implements IModPlugin {

    public static final RecipeType<RitualRecipe> RITUAL =
            RecipeType.create(AdvancedMod.MODID, "ritual", RitualRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(AdvancedMod.MODID, "jei");
    }

    private boolean ok() {
        return ModList.get().isLoaded("goety") && ModList.get().isLoaded("apricityui");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration r) {
        if (!ok()) return;
        r.addRecipeCategories(new RitualRecipeCategory(r.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration r) {
        if (!ok()) return;
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        List<RitualRecipe> recipes = level.getRecipeManager()
                .getAllRecipesFor(ModRecipeSerializer.RITUAL_TYPE.get());
        r.addRecipes(RITUAL, recipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration r) {
        if (!ok()) return;
        r.addRecipeCatalyst(new ItemStack(ModBlocks.DARK_ALTAR.get()), RITUAL);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration r) {
        if (!ok()) return;
        r.addRecipeTransferHandler(new RitualRecipeTransferInfo());
    }

    /** ★ 告诉 JEI：AUI 屏幕的 GUI 区域是中央 200px 面板 */
    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration r) {
        if (!ok()) return;
        r.addGuiScreenHandler(ApricityContainerScreen.class,
            screen -> {
                int guiW = 200;
                int guiLeft = (screen.width - guiW) / 2;
                return new IGuiProperties() {
                    public Class<? extends net.minecraft.client.gui.screens.Screen> getScreenClass() { return ApricityContainerScreen.class; }
                    public int getGuiLeft() { return guiLeft; }
                    public int getGuiTop() { return 0; }
                    public int getGuiXSize() { return guiW; }
                    public int getGuiYSize() { return screen.height; }
                    public int getScreenWidth() { return screen.width; }
                    public int getScreenHeight() { return screen.height; }
                };
            });
    }
}
