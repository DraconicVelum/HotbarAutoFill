package com.draconicvelum.hotbarautofill;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;

public final class HotbarAutoFillMod implements ClientModInitializer {
	private static final Identifier ITEM_COUNTER_ID = Identifier.parse("hotbarautofill:item_counter");

	@Override
	public void onInitializeClient() {
		configure(HotbarAutoFillConfig.load());
		ClientTickEvents.START_CLIENT_TICK.register(HotbarAutoFillMod::onClientTick);
		HudElementRegistry.attachElementAfter(
				VanillaHudElements.HOTBAR,
				ITEM_COUNTER_ID,
				(graphics, tickCounter) -> HotbarItemCounter.render(graphics)
		);
	}

	private static void onClientTick(net.minecraft.client.Minecraft client) {
		HotbarRefill.tick(client);
	}

	static void configure(HotbarAutoFillConfig config) {
		HotbarRefill.configure(config);
		HotbarItemCounter.configure(config);
	}
}
