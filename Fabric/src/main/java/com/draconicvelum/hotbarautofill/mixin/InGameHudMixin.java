package com.draconicvelum.hotbarautofill.mixin;

import com.draconicvelum.hotbarautofill.HotbarAutoFillMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;

@Mixin(InGameHud.class)
abstract class InGameHudMixin {
	@Inject(method = "renderHotbar", at = @At("TAIL"))
	private void hotbarAutoFill$renderHotbar(DrawContext context, float tickDelta, CallbackInfo callbackInfo) {
		HotbarAutoFillMod.onRenderHotbar(context);
	}
}
