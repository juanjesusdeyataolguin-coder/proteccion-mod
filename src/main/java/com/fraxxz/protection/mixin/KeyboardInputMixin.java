package com.fraxxz.protection.mixin;

import com.fraxxz.protection.FreecamClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {
    @Inject(method = "tick", at = @At("TAIL"))
    private void protection$freeze(boolean slowDown, float f, CallbackInfo ci) {
        if (FreecamClient.active) {
            this.pressingForward = false; this.pressingBack = false;
            this.pressingLeft = false; this.pressingRight = false;
            this.jumping = false; this.sneaking = false;
            this.movementForward = 0; this.movementSideways = 0;
        }
    }
}
