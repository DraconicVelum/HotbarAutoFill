package com.draconicvelum.hotbarautofill;

import net.fabricmc.api.ClientModInitializer;

public final class HotbarAutoFillMod implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		configure(HotbarAutoFillConfig.load());
	}

	public static void onClientTick() {
		HotbarRefill.tick();
	}

	public static void onRenderHotbar() {
		HotbarItemCounter.render();
	}

	public static boolean protectToolBeforeUse() {
		return HotbarRefill.protectToolBeforeUse();
	}
	static void configure(HotbarAutoFillConfig config) {
		HotbarRefill.configure(config);
		HotbarItemCounter.configure(config);
	}
}
