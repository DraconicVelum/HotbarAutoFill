package com.draconicvelum.hotbarautofill;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

final class HotbarItemCounter {
	private static final int RIGHT_HOTBAR_EDGE_OFFSET = 25;
	private static HotbarAutoFillConfig config;

	private HotbarItemCounter() {
	}

	static void configure(HotbarAutoFillConfig config) {
		HotbarItemCounter.config = config;
	}

	static void render() {
		if (config == null || !config.showHeldItemTotalCounter()) {
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || client.currentScreen != null) {
			return;
		}

		PlayerInventory inventory = client.player.inventory;
		ItemStack selectedStack = normalize(inventory.getMainHandStack());
		if (selectedStack == null) {
			return;
		}

		ItemStack counterStack = getCounterStack(selectedStack);
		if (counterStack == null) {
			return;
		}

		boolean matchData = counterStack != selectedStack;
		int total = countHeldItem(inventory, counterStack, matchData);
		if (total <= 0) {
			return;
		}

		String totalText = Integer.toString(total);
		int x = client.window.getScaledWidth() / 2 + 91 + RIGHT_HOTBAR_EDGE_OFFSET;
		int y = client.window.getScaledHeight() - 22;
		int textX = x + 20;
		int textY = y + 5;

		ItemStack iconStack = counterStack.copy();
		iconStack.setCount(1);

		DiffuseLighting.enableForItems();
		client.getItemRenderer().renderGuiItem(iconStack, x, y);
		DiffuseLighting.disable();
		client.textRenderer.drawWithShadow(totalText, textX, textY, 0xFFFFFFFF);
	}

	private static ItemStack getCounterStack(ItemStack selectedStack) {
		if (selectedStack.getItem() instanceof BowItem) {
			return new ItemStack(Items.ARROW);
		}

		return selectedStack;
	}

	private static int countHeldItem(PlayerInventory inventory, ItemStack countedStack, boolean matchData) {
		int total = 0;
		for (int slot = 0; slot < inventory.main.size(); slot++) {
			ItemStack stack = normalize(inventory.getInvStack(slot));
			if (isCountedStack(stack, countedStack, matchData)) {
				total += stack.getCount();
			}
		}
		return total;
	}

	private static boolean isCountedStack(ItemStack stack, ItemStack countedStack, boolean matchData) {
		if (stack == null) {
			return false;
		}
		if (matchData) {
			return stack.getItem() == countedStack.getItem()
					&& stack.getDamage() == countedStack.getDamage()
					&& ItemStack.areTagsEqual(stack, countedStack);
		}

		return stack.getItem() == countedStack.getItem();
	}

	private static ItemStack normalize(ItemStack stack) {
		return stack == null || stack.isEmpty() ? null : stack;
	}
}
