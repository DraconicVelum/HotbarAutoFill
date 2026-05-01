package com.draconicvelum.hotbarautofill;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = HotbarAutoFillMod.MOD_ID, dist = Dist.CLIENT)
public final class HotbarAutoFillMod {
	static final String MOD_ID = "hotbarautofill";

	public HotbarAutoFillMod(ModContainer container) {
		container.registerConfig(ModConfig.Type.CLIENT, HotbarAutoFillConfig.CLIENT_SPEC);
		container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
		NeoForge.EVENT_BUS.addListener(this::onClientTick);
		NeoForge.EVENT_BUS.addListener(this::onRenderGui);
	}

	private void onClientTick(ClientTickEvent.Pre event) {
		configure();
		HotbarRefill.tick(net.minecraft.client.Minecraft.getInstance());
	}

	private void onRenderGui(RenderGuiEvent.Post event) {
		configure();
		HotbarItemCounter.render(event.getGuiGraphics());
	}

	private static void configure() {
		HotbarAutoFillConfig config = HotbarAutoFillConfig.load();
		HotbarRefill.configure(config);
		HotbarItemCounter.configure(config);
	}
}
