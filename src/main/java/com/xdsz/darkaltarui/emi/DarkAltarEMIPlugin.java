package com.xdsz.darkaltarui.emi;

import com.sighs.apricityui.instance.ApricityContainerScreen;
import com.sighs.apricityui.registry.ApricityMenus;
import com.xdsz.darkaltarui.DarkAltarConfig;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

/**
 * EMI 插件：只做两件事
 * 1. 告诉 EMI AUI 屏幕的 GUI 边界
 * 2. 给 Goety 已有的 ritual 分类添加 + 传输处理器
 */
@EmiEntrypoint // 仅在 EMI 存在时加载
public class DarkAltarEMIPlugin implements EmiPlugin {

    /** Goety 的仪式配方分类 ID */
    public static final EmiRecipeCategory RITUAL = new EmiRecipeCategory(
            new ResourceLocation("goety", "ritual"), EmiStack.of(Items.ENCHANTING_TABLE));

    @Override
    public void register(EmiRegistry registry) {
        // ★ EMI 边界：配置控制是否影响其他 AUI 屏幕
        boolean allAui = DarkAltarConfig.EMI_ON_ALL_AUI.get();
        registry.addScreenBoundsProvider(ApricityContainerScreen.class, screen -> {
            if (!allAui) {
                String path = screen.getMenu().getTemplatePath();
                if (path == null || !path.contains("altar")) return null; // 仅祭坛
            }
            return new Bounds((screen.width - 200) / 2, 0, 200, screen.height);
        });

        // 注册 + 按钮传输处理器
        registry.addRecipeHandler(ApricityMenus.APRICITY_CONTAINER.get(),
                new RitualEmiTransferHandler());
    }
}
