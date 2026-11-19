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
	public void onMouseInput(InputEvent.MouseInputEvent event) {
		net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
		if (event.getAction() == 1 && (client.gameSettings.keyBindAttack.matchesMouseKey(event.getButton())
				|| client.gameSettings.keyBindUseItem.matchesMouseKey(event.getButton()))) {
			HotbarRefill.protectToolBeforeUse();
		}
	}
	@SubscribeEvent
	public void onKeyInput(InputEvent.KeyInputEvent event) {
		net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
		if (event.getAction() == 1 && (client.gameSettings.keyBindAttack.matchesKey(event.getKey(), event.getScanCode())
				|| client.gameSettings.keyBindUseItem.matchesKey(event.getKey(), event.getScanCode()))) {
			HotbarRefill.protectToolBeforeUse();
		}
	}
	@SubscribeEvent
	public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
		if (event.getType() == RenderGameOverlayEvent.ElementType.HOTBAR) {
			HotbarItemCounter.render();
		}
	}

	static void configure(HotbarAutoFillConfig config) {
		HotbarRefill.configure(config);
		HotbarItemCounter.configure(config);
	}
}
