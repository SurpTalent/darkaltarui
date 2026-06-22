package com.xdsz.darkaltarui;

import com.mojang.logging.LogUtils;
import com.xdsz.darkaltarui.event.AltarEvents;
import com.xdsz.darkaltarui.item.SoulRemoteTerminal;
import com.xdsz.darkaltarui.network.ModNetwork;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(AdvancedMod.MODID)
public class AdvancedMod {

    public static final String MODID = "darkaltarui";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final RegistryObject<Item> SOUL_REMOTE_TERMINAL = ITEMS.register("soul_remote_terminal", SoulRemoteTerminal::new);

    public AdvancedMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);

        ITEMS.register(modEventBus);
        DarkAltarConfig.register();

        MinecraftForge.EVENT_BUS.register(AltarEvents.class);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        ModNetwork.register();
    }
}
