package com.xdsz.darkaltarui.network;

import net.minecraft.core.BlockPos;

/** 不含 EMI 导入的祭坛坐标持有者，避免 NoClassDefFoundError */
public class AltarPosHolder {
    private static BlockPos pos = null;
    public static void set(BlockPos p) { pos = p; }
    public static BlockPos get() { return pos; }
}
