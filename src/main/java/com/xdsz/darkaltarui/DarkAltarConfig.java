package com.xdsz.darkaltarui;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class DarkAltarConfig {

    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final ForgeConfigSpec COMMON_SPEC;

    public static final ForgeConfigSpec.BooleanValue SOUL_COST_ENABLED;
    public static final ForgeConfigSpec.IntValue SOUL_COST_PER_ITEM;

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

        COMMON_SPEC = common.build();
        CLIENT_SPEC = new ForgeConfigSpec.Builder().build();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, COMMON_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
    }
}
