package com.xdsz.darkaltarui.energy;

import com.Polarice3.Goety.common.blocks.entities.CursedCageBlockEntity;
import com.xdsz.darkaltarui.DarkAltarConfig;
import net.minecraftforge.energy.IEnergyStorage;

/**
 * 将诅咒之笼的灵魂能量暴露为 FE 输出。
 * RS 控制器、磁盘驱动器等可以从中抽取能量。
 */
public class CageEnergyStorage implements IEnergyStorage {

    private final CursedCageBlockEntity cage;

    public CageEnergyStorage(CursedCageBlockEntity cage) {
        this.cage = cage;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return 0; // 只输出
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        if (maxExtract <= 0) return 0;
        int fePerSoul = DarkAltarConfig.FE_PER_SOUL.get();
        if (fePerSoul <= 0) return 0;

        int soulsAvailable = cage.getSouls();
        int soulsNeeded = (maxExtract + fePerSoul - 1) / fePerSoul; // ceil
        int soulsToUse = Math.min(soulsNeeded, soulsAvailable);
        if (soulsToUse <= 0) return 0;

        int feToExtract = Math.min(soulsToUse * fePerSoul, maxExtract);

        if (!simulate) {
            cage.decreaseSouls(soulsToUse);
        }
        return feToExtract;
    }

    @Override
    public int getEnergyStored() {
        return cage.getSouls() * DarkAltarConfig.FE_PER_SOUL.get();
    }

    @Override
    public int getMaxEnergyStored() {
        return Integer.MAX_VALUE / 2; // 够大但不溢出的安全值
    }

    @Override
    public boolean canExtract() {
        return DarkAltarConfig.FE_PER_SOUL.get() > 0 && cage.getSouls() > 0;
    }

    @Override
    public boolean canReceive() {
        return false;
    }
}
