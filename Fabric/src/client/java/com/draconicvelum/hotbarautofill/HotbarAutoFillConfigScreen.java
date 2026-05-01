package com.draconicvelum.hotbarautofill;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

final class HotbarAutoFillConfigScreen extends OptionsSubScreen {
	private boolean refillFromOtherHotbarSlots;
	private boolean preventToolBreaking;
	private boolean showHeldItemTotalCounter;

	HotbarAutoFillConfigScreen(Screen previous) {
		super(previous, Minecraft.getInstance().options, Component.translatable("hotbarautofill.configuration.title"));
		HotbarAutoFillConfig config = HotbarAutoFillConfig.load();
		refillFromOtherHotbarSlots = config.refillFromOtherHotbarSlots();
		preventToolBreaking = config.preventToolBreaking();
		showHeldItemTotalCounter = config.showHeldItemTotalCounter();
	}

	@Override
	protected void addOptions() {
		if (list != null) {
			list.addSmall(
					label("hotbarautofill.configuration.refill_from_other_hotbar_slots"),
					Button.builder(toggleText(refillFromOtherHotbarSlots), button -> {
						refillFromOtherHotbarSlots = !refillFromOtherHotbarSlots;
						button.setMessage(toggleText(refillFromOtherHotbarSlots));
					}).build()
			);
			list.addSmall(
					label("hotbarautofill.configuration.prevent_tool_breaking"),
					Button.builder(toggleText(preventToolBreaking), button -> {
						preventToolBreaking = !preventToolBreaking;
						button.setMessage(toggleText(preventToolBreaking));
					}).build()
			);
			list.addSmall(
					label("hotbarautofill.configuration.show_held_item_total_counter"),
					Button.builder(toggleText(showHeldItemTotalCounter), button -> {
						showHeldItemTotalCounter = !showHeldItemTotalCounter;
						button.setMessage(toggleText(showHeldItemTotalCounter));
					}).build()
			);
		}
	}

	@Override
	public void removed() {
		HotbarAutoFillConfig config = HotbarAutoFillConfig.save(
				refillFromOtherHotbarSlots,
				preventToolBreaking,
				showHeldItemTotalCounter
		);
		HotbarAutoFillMod.configure(config);
	}

	private StringWidget label(String translationKey) {
		return new StringWidget(0, 0, Button.DEFAULT_WIDTH, Button.DEFAULT_HEIGHT, Component.translatable(translationKey), font);
	}

	private static Component toggleText(boolean enabled) {
		return Component.translatable(enabled ? "options.on" : "options.off");
	}
}
