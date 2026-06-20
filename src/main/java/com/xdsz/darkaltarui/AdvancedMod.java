package com.xdsz.darkaltarui;

import com.mojang.logging.LogUtils;
import com.xdsz.darkaltarui.event.AltarEvents;
import com.xdsz.darkaltarui.network.ModNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(AdvancedMod.MODID)
public class AdvancedMod {

    public static final String MODID = "darkaltarui";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AdvancedMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);

        DarkAltarConfig.register();

        MinecraftForge.EVENT_BUS.register(AltarEvents.class);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        ModNetwork.register();
    }
}
