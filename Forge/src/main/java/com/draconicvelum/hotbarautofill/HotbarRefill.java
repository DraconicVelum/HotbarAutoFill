package com.draconicvelum.hotbarautofill;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;

final class HotbarRefill {
	private static final int HOTBAR_SIZE = 9;
	private static final int INVENTORY_SIZE = 36;
	private static final int NOT_FOUND = -1;
	private static final int MAX_REFILL_RETRY_ATTEMPTS = 5;
	private static final int REFILL_INPUT_BLOCK_TICKS = 3;
	private static final int DELAYED_REFILL_TICKS = 1;
	private static final int RECENT_ACTION_WINDOW_TICKS = 4;
	private static final ItemStack[] LAST_SELECTED_STACKS = new ItemStack[HOTBAR_SIZE];
	private static HotbarAutoFillConfig config;
	private static int cooldownTicks;
	private static int recentUseOrAttackTicks;
	private static int recentDropTicks;
	private static int warningCooldownTicks;
	private static int pendingRefillSlot = NOT_FOUND;
	private static ItemStack pendingRefillStack;
	private static int pendingRefillAttemptsRemaining;
	private static int pendingRefillDelayTicks;
	private static int refillUseInputBlockTicks;

	private HotbarRefill() {
	}

	static void configure(HotbarAutoFillConfig config) {
		HotbarRefill.config = config;
	}

	static boolean protectToolBeforeUse() {
		Minecraft client = Minecraft.getMinecraft();
		if (client.player == null || client.playerController == null || client.currentScreen != null) {
			return false;
		}

		InventoryPlayer inventory = client.player.inventory;
		int selectedSlot = inventory.currentItem;
		ItemStack selectedStack = normalize(inventory.getStackInSlot(selectedSlot));
		return protectToolBeforeUse(client, inventory, selectedSlot, selectedStack);
	}

	static void tick() {
		Minecraft client = Minecraft.getMinecraft();
		if (cooldownTicks > 0) {
			cooldownTicks--;
		}
		if (warningCooldownTicks > 0) {
			warningCooldownTicks--;
		}
		if (refillUseInputBlockTicks > 0) {
			refillUseInputBlockTicks--;
			if (!isConsumableRefillPending()) {
				blockUseInput(client);
			}
		}

		if (client.player == null || client.playerController == null) {
			clearTrackedStacks();
			return;
		}

		InventoryPlayer inventory = client.player.inventory;
		int selectedSlot = inventory.currentItem;
		ItemStack selectedStack = normalize(inventory.getStackInSlot(selectedSlot));

		if (client.currentScreen != null) {
			updateTrackedStack(selectedSlot, selectedStack);
			cooldownTicks = 0;
			recentUseOrAttackTicks = 0;
			recentDropTicks = 0;
			refillUseInputBlockTicks = 0;
			clearPendingRefill();
			return;
		}

		if (handlePendingRefill(client, inventory, selectedSlot, selectedStack)) {
			return;
		}

		if (handleToolProtection(client, inventory, selectedSlot, selectedStack)) {
			return;
		}

		updateRecentActionTicks(client);

		if (selectedStack != null) {
			ItemStack wantedStack = LAST_SELECTED_STACKS[selectedSlot];
			if (shouldReplaceChangedStack(selectedStack, wantedStack)) {
				if (needsContainerSafeRefill(wantedStack)) {
					if (refillSelectedSlotWithoutSwappingRemainder(client, inventory, selectedSlot, selectedStack, wantedStack)) {
						updateTrackedStack(selectedSlot, wantedStack);
						schedulePendingRefill(selectedSlot, wantedStack);
						return;
					}
				} else {
					mergeSelectedRemainderIntoExistingStack(client, inventory, selectedSlot, selectedStack);
					blockUseInputForRefill(client, wantedStack);
					if (refillSelectedSlot(client, inventory, selectedSlot, wantedStack)) {
						client.player.resetActiveHand();
						updateTrackedStack(selectedSlot, wantedStack);
						schedulePendingRefill(selectedSlot, wantedStack);
						return;
					}
				}
			}

			updateTrackedStack(selectedSlot, selectedStack);
			return;
		}

		ItemStack wantedStack = LAST_SELECTED_STACKS[selectedSlot];
		if (cooldownTicks > 0 || wantedStack == null) {
			return;
		}

		if (!wasEmptiedByUseDropOrBreak(wantedStack)) {
			LAST_SELECTED_STACKS[selectedSlot] = null;
			return;
		}

		scheduleDelayedPendingRefill(client, selectedSlot, wantedStack);
	}

	private static boolean handlePendingRefill(
			Minecraft client,
			InventoryPlayer inventory,
			int selectedSlot,
			ItemStack selectedStack
	) {
		if (pendingRefillStack == null) {
			return false;
		}
		if (selectedSlot != pendingRefillSlot) {
			clearPendingRefill();
			return false;
		}
		if (resolveCarriedStackDuringPendingRefill(client, inventory, selectedSlot, selectedStack, pendingRefillStack)) {
			cooldownTicks = 1;
			return true;
		}
		if (isMatchingRefillStack(selectedStack, pendingRefillStack)) {
			updateTrackedStack(selectedSlot, selectedStack);
			clearPendingRefill();
			return true;
		}
		if (pendingRefillDelayTicks > 0) {
			pendingRefillDelayTicks--;
			blockUseInputForRefill(client, pendingRefillStack);
			cooldownTicks = 1;
			return true;
		}
		if (pendingRefillAttemptsRemaining <= 0) {
			LAST_SELECTED_STACKS[selectedSlot] = null;
			clearPendingRefill();
			return false;
		}

		pendingRefillAttemptsRemaining--;
		if (needsContainerSafeRefill(pendingRefillStack)) {
			if (selectedStack == null && completeCarriedRefill(client, inventory, selectedSlot, pendingRefillStack)) {
				cooldownTicks = 1;
				return true;
			}
			if (refillSelectedSlotWithoutSwappingRemainder(client, inventory, selectedSlot, selectedStack, pendingRefillStack)) {
				cooldownTicks = 1;
				return true;
			}

			LAST_SELECTED_STACKS[selectedSlot] = null;
			clearPendingRefill();
			return false;
		}

		blockUseInputForRefill(client, pendingRefillStack);
		if (selectedStack == null && completeCarriedRefill(client, inventory, selectedSlot, pendingRefillStack)) {
			keepUseInputBlocked();
			cooldownTicks = 1;
			return true;
		}
		if (refillSelectedSlot(client, inventory, selectedSlot, pendingRefillStack)) {
			keepUseInputBlocked();
			cooldownTicks = 1;
			return true;
		}

		LAST_SELECTED_STACKS[selectedSlot] = null;
		clearPendingRefill();
		return false;
	}

	private static boolean handleToolProtection(
			Minecraft client,
			InventoryPlayer inventory,
			int selectedSlot,
			ItemStack selectedStack
	) {
		if (config == null || !config.preventToolBreaking()) {
			return false;
		}
		if (selectedStack == null || !selectedStack.isItemStackDamageable() || !isToolAboutToBreak(selectedStack)) {
			return false;
		}
		if (!client.gameSettings.keyBindUseItem.isKeyDown()
				&& !client.gameSettings.keyBindAttack.isKeyDown()) {
			return false;
		}

		int sourceInventorySlot = findRefillSourceSlot(inventory, selectedSlot, selectedStack);
		if (sourceInventorySlot != NOT_FOUND) {
			if (swapIntoSelectedSlot(client, selectedSlot, sourceInventorySlot)) {
				blockCurrentUseInput(client);
				updateTrackedStack(selectedSlot, selectedStack);
				schedulePendingRefill(selectedSlot, selectedStack);
				recentUseOrAttackTicks = 0;
				return true;
			}
		}

		blockCurrentUseInput(client);

		if (warningCooldownTicks == 0) {
			client.player.sendMessage(new TextComponentString("Hotbar Auto Fill: no replacement tool found."));
			warningCooldownTicks = 40;
		}

		updateTrackedStack(selectedSlot, selectedStack);
		recentUseOrAttackTicks = 0;
		return true;
	}

	private static boolean protectToolBeforeUse(
			Minecraft client,
			InventoryPlayer inventory,
			int selectedSlot,
			ItemStack selectedStack
	) {
		if (config == null || !config.preventToolBreaking()) {
			return false;
		}
		if (selectedStack == null || !selectedStack.isItemStackDamageable() || !isToolAboutToBreak(selectedStack)) {
			return false;
		}

		int sourceInventorySlot = findRefillSourceSlot(inventory, selectedSlot, selectedStack);
		if (sourceInventorySlot != NOT_FOUND && swapIntoSelectedSlot(client, selectedSlot, sourceInventorySlot)) {
			blockCurrentUseInput(client);
			updateTrackedStack(selectedSlot, selectedStack);
			schedulePendingRefill(selectedSlot, selectedStack);
			recentUseOrAttackTicks = 0;
			return true;
		}

		blockCurrentUseInput(client);

		if (warningCooldownTicks == 0) {
			client.player.sendMessage(new TextComponentString("Hotbar Auto Fill: no replacement tool found."));
			warningCooldownTicks = 40;
		}

		updateTrackedStack(selectedSlot, selectedStack);
		recentUseOrAttackTicks = 0;
		return true;
	}

	private static int findRefillSourceSlot(InventoryPlayer inventory, int selectedSlot, ItemStack wantedStack) {
		for (int slot = HOTBAR_SIZE; slot < INVENTORY_SIZE; slot++) {
			ItemStack candidate = normalize(inventory.getStackInSlot(slot));
			if (isUsableRefillSource(candidate, wantedStack)) {
				return slot;
			}
		}

		if (config != null && config.refillFromOtherHotbarSlots()) {
			for (int slot = 0; slot < HOTBAR_SIZE; slot++) {
				if (slot == selectedSlot) {
					continue;
				}

				ItemStack candidate = normalize(inventory.getStackInSlot(slot));
				if (isUsableRefillSource(candidate, wantedStack)) {
					return slot;
				}
			}
		}

		return NOT_FOUND;
	}

	private static boolean isUsableRefillSource(ItemStack candidate, ItemStack wantedStack) {
		return isMatchingRefillStack(candidate, wantedStack)
				&& (!wantedStack.isItemStackDamageable() || !isToolAboutToBreak(candidate));
	}

	private static boolean isMatchingRefillStack(ItemStack candidate, ItemStack wantedStack) {
		if (candidate == null || wantedStack == null) {
			return false;
		}

		if (wantedStack.isItemStackDamageable()) {
			return candidate.getItem() == wantedStack.getItem();
		}

		return areSameItemAndData(candidate, wantedStack);
	}

	private static boolean wasEmptiedByUseDropOrBreak(ItemStack wantedStack) {
		if (wantedStack.isItemStackDamageable()) {
			return recentDropTicks > 0
					|| recentUseOrAttackTicks > 0 && wantedStack.getItemDamage() >= wantedStack.getMaxDamage() - 1;
		}

		return recentDropTicks > 0 || wantedStack.getCount() <= 1 && recentUseOrAttackTicks > 0;
	}

	private static boolean shouldReplaceChangedStack(ItemStack selectedStack, ItemStack wantedStack) {
		if (wantedStack == null || cooldownTicks > 0 || recentUseOrAttackTicks <= 0) {
			return false;
		}
		if (wantedStack.isItemStackDamageable() || wantedStack.getCount() > 1) {
			return false;
		}

		return !areSameItemAndData(selectedStack, wantedStack);
	}

	private static boolean refillSelectedSlot(
			Minecraft client,
			InventoryPlayer inventory,
			int selectedSlot,
			ItemStack wantedStack
	) {
		int sourceInventorySlot = findRefillSourceSlot(inventory, selectedSlot, wantedStack);
		if (sourceInventorySlot == NOT_FOUND) {
			return false;
		}

		return swapIntoSelectedSlot(client, selectedSlot, sourceInventorySlot);
	}

	private static boolean resolveCarriedStackDuringPendingRefill(
			Minecraft client,
			InventoryPlayer inventory,
			int selectedSlot,
			ItemStack selectedStack,
			ItemStack wantedStack
	) {
		ItemStack carriedStack = normalize(client.player.inventory.getItemStack());
		if (carriedStack == null) {
			return false;
		}

		if (selectedStack == null && isMatchingRefillStack(carriedStack, wantedStack)) {
			completeCarriedRefill(client, inventory, selectedSlot, wantedStack);
			return true;
		}

		moveCarriedStackAway(client, inventory, selectedSlot, carriedStack);
		return true;
	}

	private static boolean refillSelectedSlotByPickup(
			Minecraft client,
			InventoryPlayer inventory,
			int selectedSlot,
			ItemStack wantedStack
	) {
		int sourceInventorySlot = findRefillSourceSlot(inventory, selectedSlot, wantedStack);
		if (sourceInventorySlot == NOT_FOUND || client.player.inventory.getItemStack() != null) {
			return false;
		}

		client.playerController.windowClick(
				client.player.inventoryContainer.windowId,
				inventorySlotToContainerSlot(sourceInventorySlot),
				0,
				ClickType.PICKUP,
				client.player
		);
		client.playerController.windowClick(
				client.player.inventoryContainer.windowId,
				inventorySlotToContainerSlot(selectedSlot),
				0,
				ClickType.PICKUP,
				client.player
		);
		return true;
	}

	private static boolean refillSelectedSlotWithoutSwappingRemainder(
			Minecraft client,
			InventoryPlayer inventory,
			int selectedSlot,
			ItemStack selectedStack,
			ItemStack wantedStack
	) {
		ItemStack carriedStack = normalize(client.player.inventory.getItemStack());
		if (carriedStack != null) {
			if (!isMatchingRefillStack(carriedStack, wantedStack)) {
				return false;
			}
			if (selectedStack != null && !moveSelectedRemainderAway(client, inventory, selectedSlot, selectedStack)) {
				return false;
			}
			return completeCarriedRefill(client, inventory, selectedSlot, wantedStack);
		}

		if (selectedStack != null && !moveSelectedRemainderAway(client, inventory, selectedSlot, selectedStack)) {
			return false;
		}

		return refillSelectedSlot(client, inventory, selectedSlot, wantedStack);
	}

	private static boolean moveSelectedRemainderAway(
			Minecraft client,
			InventoryPlayer inventory,
			int selectedSlot,
			ItemStack selectedStack
	) {
		if (mergeSelectedRemainderIntoExistingStack(client, inventory, selectedSlot, selectedStack)) {
			return true;
		}

		int emptySlot = findEmptyInventorySlot(inventory, selectedSlot);
		if (emptySlot == NOT_FOUND) {
			return false;
		}

		client.playerController.windowClick(
				client.player.inventoryContainer.windowId,
				inventorySlotToContainerSlot(selectedSlot),
				0,
				ClickType.PICKUP,
				client.player
		);
		client.playerController.windowClick(
				client.player.inventoryContainer.windowId,
				inventorySlotToContainerSlot(emptySlot),
				0,
				ClickType.PICKUP,
				client.player
		);
		return true;
	}

	private static boolean completeCarriedRefill(
			Minecraft client,
			InventoryPlayer inventory,
			int selectedSlot,
			ItemStack wantedStack
	) {
		ItemStack carriedStack = normalize(client.player.inventory.getItemStack());
		if (!isMatchingRefillStack(carriedStack, wantedStack)) {
			return false;
		}

		client.playerController.windowClick(
				client.player.inventoryContainer.windowId,
				inventorySlotToContainerSlot(selectedSlot),
				0,
				ClickType.PICKUP,
				client.player
		);
		return true;
	}

	private static boolean swapIntoSelectedSlot(Minecraft client, int selectedSlot, int sourceInventorySlot) {
		int sourceContainerSlot = inventorySlotToContainerSlot(sourceInventorySlot);
		client.playerController.windowClick(
				client.player.inventoryContainer.windowId,
				sourceContainerSlot,
				selectedSlot,
				ClickType.SWAP,
				client.player
		);
		return true;
	}

	private static boolean mergeSelectedRemainderIntoExistingStack(
			Minecraft client,
			InventoryPlayer inventory,
			int selectedSlot,
			ItemStack selectedStack
	) {
		int targetInventorySlot = findMergeTargetSlot(inventory, selectedSlot, selectedStack);
		if (targetInventorySlot == NOT_FOUND) {
			return false;
		}

		client.playerController.windowClick(
				client.player.inventoryContainer.windowId,
				inventorySlotToContainerSlot(selectedSlot),
				0,
				ClickType.PICKUP,
				client.player
		);
		client.playerController.windowClick(
				client.player.inventoryContainer.windowId,
				inventorySlotToContainerSlot(targetInventorySlot),
				0,
				ClickType.PICKUP,
				client.player
		);
		return true;
	}

	private static boolean moveCarriedStackAway(
			Minecraft client,
			InventoryPlayer inventory,
			int selectedSlot,
			ItemStack carriedStack
	) {
		int targetInventorySlot = findCarriedMergeTargetSlot(inventory, selectedSlot, carriedStack);
		if (targetInventorySlot == NOT_FOUND) {
			targetInventorySlot = findEmptyInventorySlot(inventory, selectedSlot);
		}
		if (targetInventorySlot == NOT_FOUND) {
			return false;
		}

		client.playerController.windowClick(
				client.player.inventoryContainer.windowId,
				inventorySlotToContainerSlot(targetInventorySlot),
				0,
				ClickType.PICKUP,
				client.player
		);
		return true;
	}

	private static int findMergeTargetSlot(InventoryPlayer inventory, int selectedSlot, ItemStack selectedStack) {
		for (int slot = 0; slot < INVENTORY_SIZE; slot++) {
			if (slot == selectedSlot) {
				continue;
			}

			ItemStack candidate = normalize(inventory.getStackInSlot(slot));
			if (canFullyMerge(candidate, selectedStack)) {
				return slot;
			}
		}
		return NOT_FOUND;
	}

	private static int findCarriedMergeTargetSlot(InventoryPlayer inventory, int selectedSlot, ItemStack carriedStack) {
		for (int slot = 0; slot < INVENTORY_SIZE; slot++) {
			if (slot == selectedSlot) {
				continue;
			}

			ItemStack candidate = normalize(inventory.getStackInSlot(slot));
			if (canFullyMerge(candidate, carriedStack)) {
				return slot;
			}
		}
		return NOT_FOUND;
	}

	private static int findEmptyInventorySlot(InventoryPlayer inventory, int selectedSlot) {
		for (int slot = HOTBAR_SIZE; slot < INVENTORY_SIZE; slot++) {
			if (normalize(inventory.getStackInSlot(slot)) == null) {
				return slot;
			}
		}

		if (config != null && config.refillFromOtherHotbarSlots()) {
			for (int slot = 0; slot < HOTBAR_SIZE; slot++) {
				if (slot != selectedSlot && normalize(inventory.getStackInSlot(slot)) == null) {
					return slot;
				}
			}
		}

		return NOT_FOUND;
	}

	private static boolean canFullyMerge(ItemStack candidate, ItemStack selectedStack) {
		if (candidate == null || selectedStack == null || !candidate.isStackable() || !selectedStack.isStackable()) {
			return false;
		}
		if (!areSameItemAndData(candidate, selectedStack)) {
			return false;
		}

		int maxStackSize = Math.min(candidate.getMaxStackSize(), selectedStack.getMaxStackSize());
		return candidate.getCount() + selectedStack.getCount() <= maxStackSize;
	}

	private static boolean areSameItemAndData(ItemStack first, ItemStack second) {
		return first.getItem() == second.getItem()
				&& first.getItemDamage() == second.getItemDamage()
				&& ItemStack.areItemStackTagsEqual(first, second);
	}

	private static int inventorySlotToContainerSlot(int inventorySlot) {
		return inventorySlot < HOTBAR_SIZE ? inventorySlot + 36 : inventorySlot;
	}

	private static void blockCurrentUseInput(Minecraft client) {
		blockUseInput(client);
		blockInput(client.gameSettings.keyBindAttack);
	}

	private static void blockUseInput(Minecraft client) {
		blockInput(client.gameSettings.keyBindUseItem);
	}

	private static void blockInput(KeyBinding keyBinding) {
		KeyBinding.setKeyBindState(keyBinding.getKeyCode(), false);
		while (keyBinding.isPressed()) {
		}
	}

	private static boolean isToolAboutToBreak(ItemStack stack) {
		return stack.getItemDamage() >= stack.getMaxDamage() - 1;
	}

	private static void updateRecentActionTicks(Minecraft client) {
		if (client.gameSettings.keyBindUseItem.isKeyDown()
				|| client.gameSettings.keyBindAttack.isKeyDown()
				|| client.player.isHandActive()) {
			recentUseOrAttackTicks = RECENT_ACTION_WINDOW_TICKS;
		} else if (recentUseOrAttackTicks > 0) {
			recentUseOrAttackTicks--;
		}

		if (client.gameSettings.keyBindDrop.isKeyDown()) {
			recentDropTicks = RECENT_ACTION_WINDOW_TICKS;
		} else if (recentDropTicks > 0) {
			recentDropTicks--;
		}
	}

	private static void updateTrackedStack(int selectedSlot, ItemStack selectedStack) {
		LAST_SELECTED_STACKS[selectedSlot] = selectedStack == null ? null : selectedStack.copy();
	}

	private static void schedulePendingRefill(int selectedSlot, ItemStack wantedStack) {
		pendingRefillSlot = selectedSlot;
		pendingRefillStack = wantedStack == null ? null : wantedStack.copy();
		pendingRefillAttemptsRemaining = MAX_REFILL_RETRY_ATTEMPTS;
		pendingRefillDelayTicks = 0;
		keepUseInputBlocked();
		cooldownTicks = 1;
	}

	private static void scheduleDelayedPendingRefill(Minecraft client, int selectedSlot, ItemStack wantedStack) {
		pendingRefillSlot = selectedSlot;
		pendingRefillStack = wantedStack == null ? null : wantedStack.copy();
		pendingRefillAttemptsRemaining = MAX_REFILL_RETRY_ATTEMPTS;
		pendingRefillDelayTicks = needsContainerSafeRefill(wantedStack) ? 0 : DELAYED_REFILL_TICKS;
		blockUseInputForRefill(client, wantedStack);
		cooldownTicks = 1;
	}

	private static void keepUseInputBlocked() {
		if (!isConsumableRefillPending()) {
			refillUseInputBlockTicks = REFILL_INPUT_BLOCK_TICKS;
		}
	}

	private static void blockUseInputForRefill(Minecraft client, ItemStack wantedStack) {
		if (wantedStack != null && !isConsumable(wantedStack)) {
			blockUseInput(client);
			refillUseInputBlockTicks = REFILL_INPUT_BLOCK_TICKS;
		}
	}

	private static boolean isConsumableRefillPending() {
		return pendingRefillStack != null && isConsumable(pendingRefillStack);
	}

	private static boolean needsContainerSafeRefill(ItemStack stack) {
		return stack != null && (isConsumable(stack) || hasContainerItem(stack));
	}

	private static boolean isConsumable(ItemStack stack) {
		EnumAction action = stack.getItemUseAction();
		return action == EnumAction.EAT || action == EnumAction.DRINK;
	}

	private static boolean hasContainerItem(ItemStack stack) {
		try {
			return stack.getItem().hasContainerItem(stack);
		} catch (RuntimeException ignored) {
			return false;
		}
	}

	private static void clearPendingRefill() {
		pendingRefillSlot = NOT_FOUND;
		pendingRefillStack = null;
		pendingRefillAttemptsRemaining = 0;
		pendingRefillDelayTicks = 0;
		refillUseInputBlockTicks = 0;
	}

	private static ItemStack normalize(ItemStack stack) {
		return stack == null || stack.getCount() <= 0 ? null : stack;
	}

	private static void clearTrackedStacks() {
		for (int i = 0; i < LAST_SELECTED_STACKS.length; i++) {
			LAST_SELECTED_STACKS[i] = null;
		}
		cooldownTicks = 0;
		recentUseOrAttackTicks = 0;
		recentDropTicks = 0;
		warningCooldownTicks = 0;
		refillUseInputBlockTicks = 0;
		clearPendingRefill();
	}
}
