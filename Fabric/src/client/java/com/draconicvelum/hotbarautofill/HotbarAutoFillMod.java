package com.draconicvelum.hotbarautofill;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class HotbarAutoFillMod implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		HotbarRefill.configure(HotbarAutoFillConfig.load());
		ClientTickEvents.START_CLIENT_TICK.register(HotbarAutoFillMod::onClientTick);
	}

	private static void onClientTick(net.minecraft.client.Minecraft client) {
		HotbarRefill.tick(client);
	}
}
