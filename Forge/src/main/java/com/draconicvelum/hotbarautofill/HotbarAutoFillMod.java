package com.draconicvelum.hotbarautofill;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.event.TickEvent;

@Mod(HotbarAutoFillMod.MOD_ID)
public final class HotbarAutoFillMod {
	static final String MOD_ID = "hotbarautofill";
	private static final ResourceLocation COUNTER_LAYER = new ResourceLocation(MOD_ID, "held_item_counter");

	public HotbarAutoFillMod() {
		configure(HotbarAutoFillConfig.load());
		MinecraftForge.EVENT_BUS.register(this);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::addGuiLayers);
	}

	@SubscribeEvent
	public void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase == TickEvent.Phase.START) {
			HotbarRefill.tick();
		}
	}

	private void addGuiLayers(AddGuiOverlayLayersEvent event) {
		event.getLayeredDraw().add(
				COUNTER_LAYER,
				(guiGraphics, partialTick) -> HotbarItemCounter.render(guiGraphics)
		);
	}

	static void configure(HotbarAutoFillConfig config) {
		HotbarRefill.configure(config);
		HotbarItemCounter.configure(config);
	}
}
