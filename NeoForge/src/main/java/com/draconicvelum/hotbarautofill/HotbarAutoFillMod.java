package com.draconicvelum.hotbarautofill;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(HotbarAutoFillMod.MOD_ID)
public final class HotbarAutoFillMod {
	static final String MOD_ID = "hotbarautofill";

	public HotbarAutoFillMod() {
		if (FMLEnvironment.dist != Dist.CLIENT) {
			return;
		}
		ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, HotbarAutoFillConfig.CLIENT_SPEC);
		NeoForge.EVENT_BUS.addListener(this::onClientTick);
		NeoForge.EVENT_BUS.addListener(this::onRenderGui);
		NeoForge.EVENT_BUS.addListener(this::onClickInput);
	}

	private void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.START) {
			return;
		}
		configure();
		HotbarRefill.tick();
	}

	private void onRenderGui(RenderGuiEvent.Post event) {
		configure();
		HotbarItemCounter.render(event.getGuiGraphics());
	}

	private void onClickInput(InputEvent.InteractionKeyMappingTriggered event) {
		configure();
		if ((event.isUseItem() || event.isAttack()) && HotbarRefill.protectToolBeforeUse()) {
			event.setSwingHand(false);
			event.setCanceled(true);
		}
	}
	private static void configure() {
		HotbarAutoFillConfig config = HotbarAutoFillConfig.load();
		HotbarRefill.configure(config);
		HotbarItemCounter.configure(config);
	}
}
