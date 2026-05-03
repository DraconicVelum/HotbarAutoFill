package com.draconicvelum.hotbarautofill;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ChargedProjectiles;

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

		ItemStack counterStack = getCounterStack(player, selectedStack);
		if (counterStack.isEmpty()) {
			return;
		}

		boolean matchComponents = counterStack != selectedStack;
		int total = countHeldItem(inventory, counterStack, matchComponents);
		if (total <= 0) {
			return;
		}

		String totalText = Integer.toString(total);
		int x = graphics.guiWidth() / 2 + 91 + RIGHT_HOTBAR_EDGE_OFFSET;
		int y = graphics.guiHeight() - 22;
		int textX = x + 20;
		int textY = y + 5;

		graphics.item(counterStack.copyWithCount(1), x, y);
		graphics.text(client.font, totalText, textX, textY, 0xFFFFFFFF, true);
	}

	private static ItemStack getCounterStack(LocalPlayer player, ItemStack selectedStack) {
		if (selectedStack.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(selectedStack)) {
			ChargedProjectiles chargedProjectiles = selectedStack.getOrDefault(
					DataComponents.CHARGED_PROJECTILES,
					ChargedProjectiles.EMPTY
			);
			if (!chargedProjectiles.isEmpty()) {
				return chargedProjectiles.itemCopies().getFirst();
			}
		}

		if (selectedStack.getItem() instanceof BowItem || selectedStack.getItem() instanceof CrossbowItem) {
			return player.getProjectile(selectedStack);
		}

		return selectedStack;
	}

	private static int countHeldItem(Inventory inventory, ItemStack countedStack, boolean matchComponents) {
		int total = 0;
		int inventorySlots = Math.min(Inventory.INVENTORY_SIZE, inventory.getContainerSize());
		for (int slot = 0; slot < inventorySlots; slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (isCountedStack(stack, countedStack, matchComponents)) {
				total += stack.getCount();
			}
		}
		return total;
	}

	private static boolean isCountedStack(ItemStack stack, ItemStack countedStack, boolean matchComponents) {
		if (stack.isEmpty()) {
			return false;
		}

		if (matchComponents) {
			return ItemStack.isSameItemSameComponents(stack, countedStack);
		}

		return stack.getItem() == countedStack.getItem();
	}
}
