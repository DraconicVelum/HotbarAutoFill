package com.draconicvelum.hotbarautofill;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.util.math.MatrixStack;

public final class HotbarAutoFillMod implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		configure(HotbarAutoFillConfig.load());
	}

	public static void onClientTick() {
		HotbarRefill.tick();
	}

	public static void onRenderHotbar(MatrixStack matrixStack) {
		HotbarItemCounter.render(matrixStack);
	}

	public static boolean protectToolBeforeUse() {
		return HotbarRefill.protectToolBeforeUse();
	}
	static void configure(HotbarAutoFillConfig config) {
		HotbarRefill.configure(config);
		HotbarItemCounter.configure(config);
	}
}
