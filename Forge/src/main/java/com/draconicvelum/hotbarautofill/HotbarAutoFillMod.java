package com.draconicvelum.hotbarautofill;

import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;

@Mod(HotbarAutoFillMod.MOD_ID)
public final class HotbarAutoFillMod {
	static final String MOD_ID = "hotbarautofill";

	public HotbarAutoFillMod() {
		configure(HotbarAutoFillConfig.load());
		MinecraftForge.EVENT_BUS.register(this);
	}

	@SubscribeEvent
	public void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase == TickEvent.Phase.START) {
			HotbarRefill.tick();
		}
	}

	@SubscribeEvent
	public void onClickInput(InputEvent.InteractionKeyMappingTriggered event) {
		if ((event.isUseItem() || event.isAttack()) && HotbarRefill.protectToolBeforeUse()) {
			event.setSwingHand(false);
			event.setCanceled(true);
		}
	}
	@SubscribeEvent
	public void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
		if (event.getOverlay().equals(VanillaGuiOverlay.HOTBAR.type())) {
			HotbarItemCounter.render(event.getPoseStack());
		}
	}

	static void configure(HotbarAutoFillConfig config) {
		HotbarRefill.configure(config);
		HotbarItemCounter.configure(config);
	}
}
