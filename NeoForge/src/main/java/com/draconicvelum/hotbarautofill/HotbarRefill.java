package com.draconicvelum.hotbarautofill;

import java.util.OptionalInt;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

final class HotbarRefill {
	private static final int RECENT_ACTION_WINDOW_TICKS = 4;
	private static final ItemStack[] LAST_SELECTED_STACKS = new ItemStack[Inventory.getSelectionSize()];
	private static HotbarAutoFillConfig config;
	private static int cooldownTicks;
	private static int recentUseOrAttackTicks;
	private static int warningCooldownTicks;

	static {
		for (int i = 0; i < LAST_SELECTED_STACKS.length; i++) {
			LAST_SELECTED_STACKS[i] = ItemStack.EMPTY;
		}
	}

	private HotbarRefill() {
	}

	static void configure(HotbarAutoFillConfig config) {
		HotbarRefill.config = config;
	}

	static void tick(Minecraft client) {
		if (cooldownTicks > 0) {
			cooldownTicks--;
		}
		if (warningCooldownTicks > 0) {
			warningCooldownTicks--;
		}

		LocalPlayer player = client.player;
		MultiPlayerGameMode gameMode = client.gameMode;
		if (player == null || gameMode == null) {
			clearTrackedStacks();
			return;
		}

		Inventory inventory = player.getInventory();
		int selectedSlot = inventory.getSelectedSlot();
		ItemStack selectedStack = inventory.getSelectedItem();

		if (client.screen != null) {
			updateTrackedStack(selectedSlot, selectedStack);
			cooldownTicks = 0;
			recentUseOrAttackTicks = 0;
			return;
		}

		if (handleToolProtection(client, player, gameMode, inventory, selectedSlot, selectedStack)) {
			return;
		}

		updateRecentUseOrAttackTicks(client, player);

		if (!selectedStack.isEmpty()) {
			updateTrackedStack(selectedSlot, selectedStack);
			return;
		}

		ItemStack wantedStack = LAST_SELECTED_STACKS[selectedSlot];
		if (cooldownTicks > 0 || wantedStack.isEmpty()) {
			return;
		}

		if (!wasEmptiedByUseOrBreak(wantedStack)) {
			LAST_SELECTED_STACKS[selectedSlot] = ItemStack.EMPTY;
			return;
		}

		int sourceInventorySlot = findRefillSourceSlot(inventory, selectedSlot, wantedStack);
		if (sourceInventorySlot == Inventory.NOT_FOUND_INDEX) {
			LAST_SELECTED_STACKS[selectedSlot] = ItemStack.EMPTY;
			return;
		}

		AbstractContainerMenu menu = player.containerMenu;
		OptionalInt sourceMenuSlot = menu.findSlot(inventory, sourceInventorySlot);
		if (sourceMenuSlot.isEmpty()) {
			return;
		}

		gameMode.handleContainerInput(
				menu.containerId,
				sourceMenuSlot.getAsInt(),
				selectedSlot,
				ContainerInput.SWAP,
				player
		);
		cooldownTicks = 2;
	}

	private static int findRefillSourceSlot(Inventory inventory, int selectedSlot, ItemStack wantedStack) {
		for (int slot = Inventory.getSelectionSize(); slot < inventory.getContainerSize(); slot++) {
			ItemStack candidate = inventory.getItem(slot);
			if (isMatchingRefillStack(candidate, wantedStack)) {
				return slot;
			}
		}

		if (config != null && config.refillFromOtherHotbarSlots()) {
			for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
				if (slot == selectedSlot) {
					continue;
				}

				ItemStack candidate = inventory.getItem(slot);
				if (isMatchingRefillStack(candidate, wantedStack)) {
					return slot;
				}
			}
		}

		return Inventory.NOT_FOUND_INDEX;
	}

	private static boolean isMatchingRefillStack(ItemStack candidate, ItemStack wantedStack) {
		if (candidate.isEmpty()) {
			return false;
		}

		if (wantedStack.isDamageableItem()) {
			return candidate.getItem() == wantedStack.getItem();
		}

		return ItemStack.isSameItemSameComponents(candidate, wantedStack);
	}

	private static boolean wasEmptiedByUseOrBreak(ItemStack wantedStack) {
		if (recentUseOrAttackTicks <= 0) {
			return false;
		}

		if (wantedStack.isDamageableItem()) {
			return wantedStack.getDamageValue() >= wantedStack.getMaxDamage() - 1;
		}

		return wantedStack.getCount() <= 1;
	}

	private static boolean handleToolProtection(
			Minecraft client,
			LocalPlayer player,
			MultiPlayerGameMode gameMode,
			Inventory inventory,
			int selectedSlot,
			ItemStack selectedStack
	) {
		if (config == null || !config.preventToolBreakingWithoutReplacement()) {
			return false;
		}
		if (!selectedStack.isDamageableItem() || !isToolAboutToBreak(selectedStack)) {
			return false;
		}
		if (!client.options.keyUse.isDown() && !client.options.keyAttack.isDown()) {
			return false;
		}

		int sourceInventorySlot = findRefillSourceSlot(inventory, selectedSlot, selectedStack);
		if (sourceInventorySlot != Inventory.NOT_FOUND_INDEX) {
			if (swapIntoSelectedSlot(player, gameMode, inventory, selectedSlot, sourceInventorySlot)) {
				blockCurrentUseInput(client);
				updateTrackedStack(selectedSlot, selectedStack);
				cooldownTicks = 2;
				recentUseOrAttackTicks = 0;
				return true;
			}
		}

		blockCurrentUseInput(client);

		if (warningCooldownTicks == 0) {
			player.sendSystemMessage(Component.literal("Hotbar Auto Fill: no replacement tool found."));
			warningCooldownTicks = 40;
		}

		updateTrackedStack(selectedSlot, selectedStack);
		recentUseOrAttackTicks = 0;
		return true;
	}

	private static boolean swapIntoSelectedSlot(
			LocalPlayer player,
			MultiPlayerGameMode gameMode,
			Inventory inventory,
			int selectedSlot,
			int sourceInventorySlot
	) {
		AbstractContainerMenu menu = player.containerMenu;
		OptionalInt sourceMenuSlot = menu.findSlot(inventory, sourceInventorySlot);
		if (sourceMenuSlot.isEmpty()) {
			return false;
		}

		gameMode.handleContainerInput(
				menu.containerId,
				sourceMenuSlot.getAsInt(),
				selectedSlot,
				ContainerInput.SWAP,
				player
		);
		return true;
	}

	private static void blockCurrentUseInput(Minecraft client) {
		client.options.keyUse.setDown(false);
		client.options.keyAttack.setDown(false);
		while (client.options.keyUse.consumeClick()) {
		}
		while (client.options.keyAttack.consumeClick()) {
		}
	}

	private static boolean isToolAboutToBreak(ItemStack stack) {
		return stack.getDamageValue() >= stack.getMaxDamage() - 1;
	}

	private static void updateRecentUseOrAttackTicks(Minecraft client, LocalPlayer player) {
		if (client.options.keyUse.isDown() || client.options.keyAttack.isDown() || player.isUsingItem()) {
			recentUseOrAttackTicks = RECENT_ACTION_WINDOW_TICKS;
		} else if (recentUseOrAttackTicks > 0) {
			recentUseOrAttackTicks--;
		}
	}

	private static void updateTrackedStack(int selectedSlot, ItemStack selectedStack) {
		LAST_SELECTED_STACKS[selectedSlot] = selectedStack.isEmpty() ? ItemStack.EMPTY : selectedStack.copy();
	}

	private static void clearTrackedStacks() {
		for (int i = 0; i < LAST_SELECTED_STACKS.length; i++) {
			LAST_SELECTED_STACKS[i] = ItemStack.EMPTY;
		}
		cooldownTicks = 0;
		recentUseOrAttackTicks = 0;
		warningCooldownTicks = 0;
	}
}
