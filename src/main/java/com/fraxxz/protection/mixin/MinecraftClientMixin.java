package com.fraxxz.protection.mixin;

import com.fraxxz.protection.FreecamClient;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void protection$attack(CallbackInfoReturnable<Boolean> cir) {
        if (FreecamClient.active) { FreecamClient.select(true); cir.setReturnValue(false); }
    }
    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void protection$use(CallbackInfo ci) {
        if (FreecamClient.active) { FreecamClient.select(false); ci.cancel(); }
    }
    @Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true)
    private void protection$break(boolean breaking, CallbackInfo ci) {
        if (FreecamClient.active) ci.cancel();
    }
}
