package com.draconicvelum.hotbarautofill;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;

@Mod(
		modid = HotbarAutoFillMod.MOD_ID,
		name = HotbarAutoFillMod.MOD_NAME,
		version = HotbarAutoFillMod.VERSION,
		acceptedMinecraftVersions = "[1.7.10]"
)
public final class HotbarAutoFillMod {
	static final String MOD_ID = "hotbarautofill";
	static final String MOD_NAME = "Hotbar Auto Fill";
	static final String VERSION = "@VERSION@";

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		configure(HotbarAutoFillConfig.load());
		FMLCommonHandler.instance().bus().register(this);
		MinecraftForge.EVENT_BUS.register(this);
	}

	@SubscribeEvent
	public void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase == TickEvent.Phase.START) {
			HotbarRefill.tick();
		}
	}

	@SubscribeEvent
	public void onMouseInput(MouseEvent event) {
		if (event.buttonstate && (event.button == 0 || event.button == 1) && HotbarRefill.protectToolBeforeUse()) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
		if (event.type == RenderGameOverlayEvent.ElementType.HOTBAR) {
			HotbarItemCounter.render(event.resolution);
		}
	}

	static void configure(HotbarAutoFillConfig config) {
		HotbarRefill.configure(config);
		HotbarItemCounter.configure(config);
	}
}
