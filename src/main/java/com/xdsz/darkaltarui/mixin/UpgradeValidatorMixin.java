package com.xdsz.darkaltarui.mixin;

import com.refinedmods.refinedstorage.inventory.item.validator.UpgradeItemValidator;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让 UpgradeItemValidator 也接受链接宝石（soul_transfer）。
 */
@Mixin(value = UpgradeItemValidator.class, remap = false)
public class UpgradeValidatorMixin {

    @Inject(method = "test", at = @At("HEAD"), cancellable = true)
    private void test(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        String name = stack.getItem().getClass().getName();
        if (name.contains("SoulTransfer") || name.contains("soul_transfer")) {
            cir.setReturnValue(true);
        }
    }
}
