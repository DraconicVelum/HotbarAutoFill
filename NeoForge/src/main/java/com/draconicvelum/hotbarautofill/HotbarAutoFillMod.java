package com.draconicvelum.hotbarautofill;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = HotbarAutoFillMod.MOD_ID, dist = Dist.CLIENT)
public final class HotbarAutoFillMod {
	static final String MOD_ID = "hotbarautofill";
	private static boolean configured;

	public HotbarAutoFillMod() {
		NeoForge.EVENT_BUS.addListener(this::onClientTick);
		NeoForge.EVENT_BUS.addListener(this::onRenderGui);
	}

	private void onClientTick(ClientTickEvent.Pre event) {
		if (!ensureConfigured()) {
			return;
		}
		HotbarRefill.tick(net.minecraft.client.Minecraft.getInstance());
	}

	private void onRenderGui(RenderGuiEvent.Post event) {
		if (!ensureConfigured()) {
			return;
		}
		HotbarItemCounter.render(event.getGuiGraphics());
	}

	private static boolean ensureConfigured() {
		if (configured || net.minecraft.client.Minecraft.getInstance() == null) {
			return configured;
		}

		HotbarAutoFillConfig config = HotbarAutoFillConfig.load();
		HotbarRefill.configure(config);
		HotbarItemCounter.configure(config);
		configured = true;
		return true;
	}
}
