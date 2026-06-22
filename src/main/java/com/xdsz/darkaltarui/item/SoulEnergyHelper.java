package com.xdsz.darkaltarui.item;

import com.Polarice3.Goety.utils.SEHelper;
import net.minecraft.world.entity.player.Player;

/**
 * Goety 玩家灵魂能量辅助。
 * 用 SEHelper（玩家个人灵魂能量，非诅咒之笼）。
 */
public final class SoulEnergyHelper {

    private SoulEnergyHelper() {}

    public static boolean hasEnough(Player player, int amount) {
        return SEHelper.getSoulsAmount(player, amount);
    }

    public static void consume(Player player, int amount) {
        SEHelper.decreaseSouls(player, amount);
    }

    public static int getSouls(Player player) {
        return SEHelper.getSESouls(player);
    }
}
