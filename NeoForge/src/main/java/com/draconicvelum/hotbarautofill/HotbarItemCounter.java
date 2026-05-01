package com.draconicvelum.hotbarautofill;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

final class HotbarItemCounter {
	private static final int RIGHT_HOTBAR_EDGE_OFFSET = 25;
	private static HotbarAutoFillConfig config;

	private HotbarItemCounter() {
	}

	static void configure(HotbarAutoFillConfig config) {
		HotbarItemCounter.config = config;
	}

	static void render(GuiGraphicsExtractor graphics) {
		if (config == null || !config.showHeldItemTotalCounter()) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (player == null || client.screen != null) {
			return;
		}

		Inventory inventory = player.getInventory();
		ItemStack selectedStack = inventory.getSelectedItem();
		if (selectedStack.isEmpty()) {
			return;
		}

		int total = countHeldItem(inventory, selectedStack);
		if (total <= 0) {
			return;
		}

		String totalText = Integer.toString(total);
		int x = graphics.guiWidth() / 2 + 91 + RIGHT_HOTBAR_EDGE_OFFSET;
		int y = graphics.guiHeight() - 22;
		int textX = x + 20;
		int textY = y + 5;

		graphics.item(selectedStack.copyWithCount(1), x, y);
		graphics.text(client.font, totalText, textX, textY, 0xFFFFFFFF, true);
	}

	private static int countHeldItem(Inventory inventory, ItemStack selectedStack) {
		int total = 0;
		int inventorySlots = Math.min(Inventory.INVENTORY_SIZE, inventory.getContainerSize());
		for (int slot = 0; slot < inventorySlots; slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (!stack.isEmpty() && stack.getItem() == selectedStack.getItem()) {
				total += stack.getCount();
			}
		}
		return total;
	}
}
