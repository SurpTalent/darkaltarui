package com.xdsz.darkaltarui.mixin;

import com.Polarice3.Goety.common.blocks.entities.CursedCageBlockEntity;
import com.refinedmods.refinedstorage.apiimpl.network.Network;
import com.xdsz.darkaltarui.DarkAltarConfig;
import com.xdsz.darkaltarui.energy.CageEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.energy.IEnergyStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 每 tick RS 网络更新时，从相邻诅咒之笼吸入 FE。
 */
@Mixin(value = Network.class, remap = false)
public class RSNetworkEnergyMixin {

    @Inject(method = "update", at = @At("HEAD"))
    private void onUpdate(CallbackInfo ci) {
        Network network = (Network) (Object) this;
        int fePerSoul = DarkAltarConfig.FE_PER_SOUL.get();
        if (fePerSoul <= 0) return;

        Level level = network.getLevel();
        BlockPos pos = network.getPosition();
        if (level == null || level.isClientSide) return;

        IEnergyStorage netEnergy = network.getEnergyStorage();
        int remaining = netEnergy.getMaxEnergyStored() - netEnergy.getEnergyStored();
        // 至少需要一个完整的 fePerSoul 才消耗灵魂（避免浪费）
        if (remaining < fePerSoul) return;

        // 扫描控制器周围 2 格范围内的诅咒之笼
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos cagePos = pos.offset(dx, dy, dz);
                    BlockEntity be = level.getBlockEntity(cagePos);
                    if (be instanceof CursedCageBlockEntity cage && cage.getSouls() > 0) {
                        CageEnergyStorage cageStorage = new CageEnergyStorage(cage);
                        int given = cageStorage.extractEnergy(remaining, false);
                        if (given > 0) {
                            netEnergy.receiveEnergy(given, false);
                            return; // 每次只从第一个笼子取，避免多笼子重复消耗
                        }
                    }
                }
            }
        }
    }
}
