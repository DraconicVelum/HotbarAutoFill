package com.draconicvelum.hotbarautofill;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class HotbarItemCounter {
	private static final int RIGHT_HOTBAR_EDGE_OFFSET = 25;
	private static HotbarAutoFillConfig config;

	private HotbarItemCounter() {
	}

	static void configure(HotbarAutoFillConfig config) {
		HotbarItemCounter.config = config;
	}

	static void render(PoseStack matrixStack) {
		if (config == null || !config.showHeldItemTotalCounter()) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.screen != null) {
			return;
		}

		Inventory inventory = client.player.getInventory();
		ItemStack selectedStack = normalize(inventory.getSelected());
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
		int x = client.getWindow().getGuiScaledWidth() / 2 + 91 + RIGHT_HOTBAR_EDGE_OFFSET;
		int y = client.getWindow().getGuiScaledHeight() - 22;
		int textX = x + 20;
		int textY = y + 5;

		ItemStack iconStack = counterStack.copy();
		iconStack.setCount(1);

		Lighting.setupForFlatItems();
		client.getItemRenderer().renderAndDecorateItem(iconStack, x, y);
		Lighting.setupFor3DItems();
		client.font.drawShadow(matrixStack, totalText, textX, textY, 0xFFFFFFFF);
	}

	private static ItemStack getCounterStack(ItemStack selectedStack) {
		if (selectedStack.getItem() instanceof BowItem) {
			return new ItemStack(Items.ARROW);
		}

		return selectedStack;
	}

	private static int countHeldItem(Inventory inventory, ItemStack countedStack, boolean matchData) {
		int total = 0;
		for (int slot = 0; slot < inventory.items.size(); slot++) {
			ItemStack stack = normalize(inventory.getItem(slot));
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
					&& stack.getDamageValue() == countedStack.getDamageValue()
					&& ItemStack.tagMatches(stack, countedStack);
		}

		return stack.getItem() == countedStack.getItem();
	}

	private static ItemStack normalize(ItemStack stack) {
		return stack == null || stack.isEmpty() ? null : stack;
	}
}
