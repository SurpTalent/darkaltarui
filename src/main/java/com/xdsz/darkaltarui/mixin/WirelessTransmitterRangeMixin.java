package com.xdsz.darkaltarui.mixin;

import com.refinedmods.refinedstorage.apiimpl.network.node.WirelessTransmitterNetworkNode;
import com.refinedmods.refinedstorage.inventory.item.UpgradeItemHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 无线发射器范围 Mixin。
 * 当升级槽有链接宝石时，提供无限范围。
 */
@Mixin(value = WirelessTransmitterNetworkNode.class, remap = false)
public class WirelessTransmitterRangeMixin {

    @Shadow @Final private UpgradeItemHandler upgrades;

    @Inject(method = "getRange", at = @At("HEAD"), cancellable = true)
    private void getRange(CallbackInfoReturnable<Integer> cir) {
        if (hasLinkGem()) {
            cir.setReturnValue(Integer.MAX_VALUE);
        }
    }

    private boolean hasLinkGem() {
        for (int i = 0; i < upgrades.getSlots(); i++) {
            var stack = upgrades.getStackInSlot(i);
            if (!stack.isEmpty() && isLinkGem(stack)) {
                // 检查链接宝石是否绑定了拥有者（有 owner NBT 才算有效）
                var tag = stack.getTag();
                if (tag != null && tag.contains("owner")) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isLinkGem(net.minecraft.world.item.ItemStack stack) {
        String name = stack.getItem().getClass().getName();
        return name.contains("SoulTransfer") || name.contains("soul_transfer");
    }
}
