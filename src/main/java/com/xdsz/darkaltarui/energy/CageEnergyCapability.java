package com.xdsz.darkaltarui.energy;

import com.Polarice3.Goety.common.blocks.entities.CursedCageBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class CageEnergyCapability {

    private static final ResourceLocation KEY = new ResourceLocation("darkaltarui", "cage_energy");

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<BlockEntity> event) {
        BlockEntity be = event.getObject();
        if (be instanceof CursedCageBlockEntity cage) {
            CageEnergyStorage storage = new CageEnergyStorage(cage);
            event.addCapability(KEY, new ICapabilityProvider() {
                @Nonnull
                @Override
                public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
                    if (cap == ForgeCapabilities.ENERGY) {
                        return LazyOptional.of(() -> storage).cast();
                    }
                    return LazyOptional.empty();
                }
            });
        }
    }
}
