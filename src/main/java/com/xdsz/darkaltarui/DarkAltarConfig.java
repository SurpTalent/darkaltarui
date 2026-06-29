package com.xdsz.darkaltarui;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class DarkAltarConfig {

    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final ForgeConfigSpec COMMON_SPEC;

    public static final ForgeConfigSpec.BooleanValue SOUL_COST_ENABLED;
    public static final ForgeConfigSpec.IntValue SOUL_COST_PER_ITEM;
    public static final ForgeConfigSpec.IntValue FE_PER_SOUL;
    public static final ForgeConfigSpec.IntValue TERMINAL_SOUL_COST;
    public static final ForgeConfigSpec.BooleanValue EMI_ON_ALL_AUI;

    static {
        ForgeConfigSpec.Builder common = new ForgeConfigSpec.Builder();

        common.push("soul_cost");
        SOUL_COST_ENABLED = common
                .comment("是否启用灵魂能量消耗。开启后每操控一个物品到祭坛底座消耗诅咒之笼的灵魂")
                .define("enabled", false);
        SOUL_COST_PER_ITEM = common
                .comment("操控每个物品消耗的灵魂能量点数")
                .defineInRange("per_item", 1, 0, Integer.MAX_VALUE);
        common.pop();

        common.push("terminal");
        TERMINAL_SOUL_COST = common
                .comment("每次打开灵魂便捷终端消耗的灵魂能量")
                .defineInRange("soul_cost_per_open", 2, 0, Integer.MAX_VALUE);
        common.pop();

        common.push("rs_power");
        FE_PER_SOUL = common
                .comment("诅咒之笼供电：每灵魂能量转换的 FE。0=禁用")
                .defineInRange("fe_per_soul", 1000, 0, Integer.MAX_VALUE);
        common.pop();

        common.push("emi");
        EMI_ON_ALL_AUI = common
                .comment("是否让 JEI/EMI 在所有 ApricityUI 界面显示（默认仅黑暗祭坛界面显示）。开启后银行/市场/拍卖等界面也会显示 JEI/EMI")
                .define("show_on_all_aui", false);
        common.pop();

        COMMON_SPEC = common.build();
        CLIENT_SPEC = new ForgeConfigSpec.Builder().build();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, COMMON_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
    }
}
