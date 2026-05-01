package com.draconicvelum.hotbarautofill;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = HotbarAutoFillMod.MOD_ID, dist = Dist.CLIENT)
public final class HotbarAutoFillMod {
	static final String MOD_ID = "hotbarautofill";

	public HotbarAutoFillMod() {
		HotbarRefill.configure(HotbarAutoFillConfig.load());
		NeoForge.EVENT_BUS.addListener(this::onClientTick);
	}

	private void onClientTick(ClientTickEvent.Pre event) {
		HotbarRefill.tick(net.minecraft.client.Minecraft.getInstance());
	}
}
