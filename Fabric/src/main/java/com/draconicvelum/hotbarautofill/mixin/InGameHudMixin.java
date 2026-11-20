package com.draconicvelum.hotbarautofill.mixin;

import com.draconicvelum.hotbarautofill.HotbarAutoFillMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;

@Mixin(InGameHud.class)
abstract class InGameHudMixin {
	@Inject(method = "renderHotbar", at = @At("TAIL"))
	private void hotbarAutoFill$renderHotbar(float tickDelta, MatrixStack matrixStack, CallbackInfo callbackInfo) {
		HotbarAutoFillMod.onRenderHotbar(matrixStack);
	}
}
