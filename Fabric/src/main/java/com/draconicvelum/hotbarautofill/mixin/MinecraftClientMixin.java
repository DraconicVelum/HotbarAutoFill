package com.draconicvelum.hotbarautofill.mixin;

import com.draconicvelum.hotbarautofill.HotbarAutoFillMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;

@Mixin(MinecraftClient.class)
abstract class MinecraftClientMixin {
	@Inject(method = "tick", at = @At("HEAD"))
	private void hotbarAutoFill$tick(CallbackInfo callbackInfo) {
		HotbarAutoFillMod.onClientTick();
	}
	@Inject(method = "handleInputEvents", at = @At("HEAD"))
	private void hotbarAutoFill$protectToolBeforeInput(CallbackInfo callbackInfo) {
		HotbarAutoFillMod.protectToolBeforeUse();
	}
}
