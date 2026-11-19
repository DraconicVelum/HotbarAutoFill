package com.draconicvelum.hotbarautofill;

import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
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
	public void onClickInput(InputEvent.ClickInputEvent event) {
		if ((event.isUseItem() || event.isAttack()) && HotbarRefill.protectToolBeforeUse()) {
			event.setSwingHand(false);
			event.setCanceled(true);
		}
	}
	@SubscribeEvent
	public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
		if (event.getType() == RenderGameOverlayEvent.ElementType.HOTBAR) {
			HotbarItemCounter.render(event.getMatrixStack());
		}
	}

	static void configure(HotbarAutoFillConfig config) {
		HotbarRefill.configure(config);
		HotbarItemCounter.configure(config);
	}
}
