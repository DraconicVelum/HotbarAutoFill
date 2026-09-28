package com.draconicvelum.hotbarautofill.mixin;

import com.draconicvelum.hotbarautofill.HotbarAutoFillMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;

@Mixin(Minecraft.class)
abstract class MinecraftMixin {
	@Inject(method = "handleKeybinds", at = @At("HEAD"))
	private void hotbarAutoFill$protectToolBeforeInput(CallbackInfo callbackInfo) {
		HotbarAutoFillMod.protectToolBeforeUse(Minecraft.getInstance());
	}
}
