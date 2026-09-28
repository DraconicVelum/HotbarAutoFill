package com.draconicvelum.hotbarautofill;

import java.util.OptionalInt;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

final class HotbarRefill {
	private static final int MAX_REFILL_RETRY_ATTEMPTS = 5;
	private static final int REFILL_INPUT_BLOCK_TICKS = 3;
	private static final int DELAYED_REFILL_TICKS = 1;
	private static final int RECENT_ACTION_WINDOW_TICKS = 4;
	private static final ItemStack[] LAST_SELECTED_STACKS = new ItemStack[Inventory.getSelectionSize()];
	private static HotbarAutoFillConfig config;
	private static int cooldownTicks;
	private static int recentUseOrAttackTicks;
	private static int recentDropTicks;
	private static int warningCooldownTicks;
	private static int pendingRefillSlot = Inventory.NOT_FOUND_INDEX;
	private static ItemStack pendingRefillStack = ItemStack.EMPTY;
	private static int pendingRefillAttemptsRemaining;
	private static int pendingRefillDelayTicks;
	private static int refillUseInputBlockTicks;

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

	static boolean protectToolBeforeUse(Minecraft client) {
		if (client == null || client.player == null || client.gameMode == null || ClientScreenAccess.hasScreen(client)) {
			return false;
		}

		Inventory inventory = client.player.getInventory();
		int selectedSlot = inventory.getSelectedSlot();
		ItemStack selectedStack = inventory.getSelectedItem();
		return handleToolProtection(client, client.player, client.gameMode, inventory, selectedSlot, selectedStack);
	}

	static void tick(Minecraft client) {
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

		LocalPlayer player = client.player;
		MultiPlayerGameMode gameMode = client.gameMode;
		if (player == null || gameMode == null) {
			clearTrackedStacks();
			return;
		}

		Inventory inventory = player.getInventory();
		int selectedSlot = inventory.getSelectedSlot();
		ItemStack selectedStack = inventory.getSelectedItem();

		if (ClientScreenAccess.hasScreen(client)) {
			updateTrackedStack(selectedSlot, selectedStack);
			cooldownTicks = 0;
			recentUseOrAttackTicks = 0;
			recentDropTicks = 0;
			refillUseInputBlockTicks = 0;
			clearPendingRefill();
			return;
		}

		if (handlePendingRefill(client, player, gameMode, inventory, selectedSlot, selectedStack)) {
			return;
		}

		if (handleToolProtection(client, player, gameMode, inventory, selectedSlot, selectedStack)) {
			return;
		}

		updateRecentActionTicks(client, player);

		if (!selectedStack.isEmpty()) {
			ItemStack wantedStack = LAST_SELECTED_STACKS[selectedSlot];
			if (shouldReplaceChangedStack(selectedStack, wantedStack)) {
				mergeSelectedRemainderIntoExistingStack(player, gameMode, inventory, selectedSlot, selectedStack);
				blockUseInputForRefill(client, wantedStack);
				if (refillSelectedSlot(player, gameMode, inventory, selectedSlot, wantedStack)) {
					player.stopUsingItem();
					updateTrackedStack(selectedSlot, wantedStack);
					schedulePendingRefill(selectedSlot, wantedStack);
					return;
				}
			}

			updateTrackedStack(selectedSlot, selectedStack);
			return;
		}

		ItemStack wantedStack = LAST_SELECTED_STACKS[selectedSlot];
		if (cooldownTicks > 0 || wantedStack.isEmpty()) {
			return;
		}

		if (!wasEmptiedByUseDropOrBreak(wantedStack)) {
			LAST_SELECTED_STACKS[selectedSlot] = ItemStack.EMPTY;
			return;
		}

		scheduleDelayedPendingRefill(client, selectedSlot, wantedStack);
	}

	private static boolean handlePendingRefill(
			Minecraft client,
			LocalPlayer player,
			MultiPlayerGameMode gameMode,
			Inventory inventory,
			int selectedSlot,
			ItemStack selectedStack
	) {
		if (pendingRefillStack.isEmpty()) {
			return false;
		}
		if (selectedSlot != pendingRefillSlot) {
			clearPendingRefill();
			return false;
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
			LAST_SELECTED_STACKS[selectedSlot] = ItemStack.EMPTY;
			clearPendingRefill();
			return false;
		}

		pendingRefillAttemptsRemaining--;
		blockUseInputForRefill(client, pendingRefillStack);
		if (selectedStack.isEmpty()
				&& completeCarriedRefill(player, gameMode, inventory, selectedSlot, pendingRefillStack)) {
			keepUseInputBlocked();
			cooldownTicks = 1;
			return true;
		}
		if (selectedStack.isEmpty()
				&& refillSelectedSlotByPickup(player, gameMode, inventory, selectedSlot, pendingRefillStack)) {
			keepUseInputBlocked();
			cooldownTicks = 1;
			return true;
		}
		if (refillSelectedSlot(player, gameMode, inventory, selectedSlot, pendingRefillStack)) {
			keepUseInputBlocked();
			cooldownTicks = 1;
			return true;
		}

		LAST_SELECTED_STACKS[selectedSlot] = ItemStack.EMPTY;
		clearPendingRefill();
		return false;
	}

	private static int findRefillSourceSlot(Inventory inventory, int selectedSlot, ItemStack wantedStack) {
		for (int slot = Inventory.getSelectionSize(); slot < inventory.getContainerSize(); slot++) {
			ItemStack candidate = inventory.getItem(slot);
			if (isUsableRefillSource(candidate, wantedStack)) {
				return slot;
			}
		}

		if (config != null && config.refillFromOtherHotbarSlots()) {
			for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
				if (slot == selectedSlot) {
					continue;
				}

				ItemStack candidate = inventory.getItem(slot);
				if (isUsableRefillSource(candidate, wantedStack)) {
					return slot;
				}
			}
		}

		return Inventory.NOT_FOUND_INDEX;
	}

	private static boolean isUsableRefillSource(ItemStack candidate, ItemStack wantedStack) {
		return isMatchingRefillStack(candidate, wantedStack)
				&& (!wantedStack.isDamageableItem() || !isToolAboutToBreak(candidate));
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

	private static boolean wasEmptiedByUseDropOrBreak(ItemStack wantedStack) {
		if (wantedStack.isDamageableItem()) {
			return recentDropTicks > 0
					|| recentUseOrAttackTicks > 0 && wantedStack.getDamageValue() >= wantedStack.getMaxDamage() - 1;
		}

		return recentDropTicks > 0 || wantedStack.getCount() <= 1 && recentUseOrAttackTicks > 0;
	}

	private static boolean shouldReplaceChangedStack(ItemStack selectedStack, ItemStack wantedStack) {
		if (wantedStack.isEmpty() || cooldownTicks > 0 || recentUseOrAttackTicks <= 0) {
			return false;
		}
		if (wantedStack.isDamageableItem()) {
			return false;
		}
		if (wantedStack.getCount() > 1) {
			return false;
		}

		return !ItemStack.isSameItemSameComponents(selectedStack, wantedStack);
	}

	private static boolean handleToolProtection(
			Minecraft client,
			LocalPlayer player,
			MultiPlayerGameMode gameMode,
			Inventory inventory,
			int selectedSlot,
			ItemStack selectedStack
	) {
		if (config == null || !config.preventToolBreaking()) {
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
				schedulePendingRefill(selectedSlot, selectedStack);
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

	private static boolean mergeSelectedRemainderIntoExistingStack(
			LocalPlayer player,
			MultiPlayerGameMode gameMode,
			Inventory inventory,
			int selectedSlot,
			ItemStack selectedStack
	) {
		int targetInventorySlot = findMergeTargetSlot(inventory, selectedSlot, selectedStack);
		if (targetInventorySlot == Inventory.NOT_FOUND_INDEX) {
			return false;
		}

		AbstractContainerMenu menu = player.containerMenu;
		OptionalInt selectedMenuSlot = menu.findSlot(inventory, selectedSlot);
		OptionalInt targetMenuSlot = menu.findSlot(inventory, targetInventorySlot);
		if (selectedMenuSlot.isEmpty() || targetMenuSlot.isEmpty()) {
			return false;
		}

		gameMode.handleContainerInput(menu.containerId, selectedMenuSlot.getAsInt(), 0, ContainerInput.PICKUP, player);
		gameMode.handleContainerInput(menu.containerId, targetMenuSlot.getAsInt(), 0, ContainerInput.PICKUP, player);
		return true;
	}

	private static int findMergeTargetSlot(Inventory inventory, int selectedSlot, ItemStack selectedStack) {
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			if (slot == selectedSlot) {
				continue;
			}

			ItemStack candidate = inventory.getItem(slot);
			if (canFullyMerge(candidate, selectedStack)) {
				return slot;
			}
		}
		return Inventory.NOT_FOUND_INDEX;
	}

	private static boolean canFullyMerge(ItemStack candidate, ItemStack selectedStack) {
		if (candidate.isEmpty() || !candidate.isStackable() || !selectedStack.isStackable()) {
			return false;
		}
		if (!ItemStack.isSameItemSameComponents(candidate, selectedStack)) {
			return false;
		}

		int maxStackSize = Math.min(candidate.getMaxStackSize(), selectedStack.getMaxStackSize());
		return candidate.getCount() + selectedStack.getCount() <= maxStackSize;
	}

	private static boolean refillSelectedSlot(
			LocalPlayer player,
			MultiPlayerGameMode gameMode,
			Inventory inventory,
			int selectedSlot,
			ItemStack wantedStack
	) {
		int sourceInventorySlot = findRefillSourceSlot(inventory, selectedSlot, wantedStack);
		if (sourceInventorySlot == Inventory.NOT_FOUND_INDEX) {
			return false;
		}

		return swapIntoSelectedSlot(player, gameMode, inventory, selectedSlot, sourceInventorySlot);
	}

	private static boolean refillSelectedSlotByPickup(
			LocalPlayer player,
			MultiPlayerGameMode gameMode,
			Inventory inventory,
			int selectedSlot,
			ItemStack wantedStack
	) {
		int sourceInventorySlot = findRefillSourceSlot(inventory, selectedSlot, wantedStack);
		if (sourceInventorySlot == Inventory.NOT_FOUND_INDEX) {
			return false;
		}

		AbstractContainerMenu menu = player.containerMenu;
		if (!menu.getCarried().isEmpty()) {
			return false;
		}

		OptionalInt sourceMenuSlot = menu.findSlot(inventory, sourceInventorySlot);
		OptionalInt selectedMenuSlot = menu.findSlot(inventory, selectedSlot);
		if (sourceMenuSlot.isEmpty() || selectedMenuSlot.isEmpty()) {
			return false;
		}

		gameMode.handleContainerInput(menu.containerId, sourceMenuSlot.getAsInt(), 0, ContainerInput.PICKUP, player);
		gameMode.handleContainerInput(menu.containerId, selectedMenuSlot.getAsInt(), 0, ContainerInput.PICKUP, player);
		return true;
	}

	private static boolean completeCarriedRefill(
			LocalPlayer player,
			MultiPlayerGameMode gameMode,
			Inventory inventory,
			int selectedSlot,
			ItemStack wantedStack
	) {
		AbstractContainerMenu menu = player.containerMenu;
		if (!isMatchingRefillStack(menu.getCarried(), wantedStack)) {
			return false;
		}

		OptionalInt selectedMenuSlot = menu.findSlot(inventory, selectedSlot);
		if (selectedMenuSlot.isEmpty()) {
			return false;
		}

		gameMode.handleContainerInput(menu.containerId, selectedMenuSlot.getAsInt(), 0, ContainerInput.PICKUP, player);
		return true;
	}

	private static void blockCurrentUseInput(Minecraft client) {
		blockUseInput(client);
		client.options.keyAttack.setDown(false);
		while (client.options.keyAttack.consumeClick()) {
		}
	}

	private static void blockUseInput(Minecraft client) {
		client.options.keyUse.setDown(false);
		while (client.options.keyUse.consumeClick()) {
		}
	}

	private static boolean isToolAboutToBreak(ItemStack stack) {
		return stack.getDamageValue() >= stack.getMaxDamage() - 1;
	}

	private static void updateRecentActionTicks(Minecraft client, LocalPlayer player) {
		if (client.options.keyUse.isDown() || client.options.keyAttack.isDown() || player.isUsingItem()) {
			recentUseOrAttackTicks = RECENT_ACTION_WINDOW_TICKS;
		} else if (recentUseOrAttackTicks > 0) {
			recentUseOrAttackTicks--;
		}

		if (client.options.keyDrop.isDown()) {
			recentDropTicks = RECENT_ACTION_WINDOW_TICKS;
		} else if (recentDropTicks > 0) {
			recentDropTicks--;
		}
	}

	private static void updateTrackedStack(int selectedSlot, ItemStack selectedStack) {
		LAST_SELECTED_STACKS[selectedSlot] = selectedStack.isEmpty() ? ItemStack.EMPTY : selectedStack.copy();
	}

	private static void schedulePendingRefill(int selectedSlot, ItemStack wantedStack) {
		pendingRefillSlot = selectedSlot;
		pendingRefillStack = wantedStack.copy();
		pendingRefillAttemptsRemaining = MAX_REFILL_RETRY_ATTEMPTS;
		pendingRefillDelayTicks = 0;
		keepUseInputBlocked();
		cooldownTicks = 1;
	}

	private static void scheduleDelayedPendingRefill(Minecraft client, int selectedSlot, ItemStack wantedStack) {
		pendingRefillSlot = selectedSlot;
		pendingRefillStack = wantedStack.copy();
		pendingRefillAttemptsRemaining = MAX_REFILL_RETRY_ATTEMPTS;
		pendingRefillDelayTicks = DELAYED_REFILL_TICKS;
		blockUseInputForRefill(client, wantedStack);
		cooldownTicks = 1;
	}

	private static void keepUseInputBlocked() {
		if (!isConsumableRefillPending()) {
			refillUseInputBlockTicks = REFILL_INPUT_BLOCK_TICKS;
		}
	}

	private static void blockUseInputForRefill(Minecraft client, ItemStack wantedStack) {
		if (!wantedStack.isEmpty() && !wantedStack.has(DataComponents.CONSUMABLE)) {
			blockUseInput(client);
			refillUseInputBlockTicks = 2;
		}
	}

	private static boolean isConsumableRefillPending() {
		return !pendingRefillStack.isEmpty() && pendingRefillStack.has(DataComponents.CONSUMABLE);
	}

	private static void clearPendingRefill() {
		pendingRefillSlot = Inventory.NOT_FOUND_INDEX;
		pendingRefillStack = ItemStack.EMPTY;
		pendingRefillAttemptsRemaining = 0;
		pendingRefillDelayTicks = 0;
		refillUseInputBlockTicks = 0;
	}

	private static void clearTrackedStacks() {
		for (int i = 0; i < LAST_SELECTED_STACKS.length; i++) {
			LAST_SELECTED_STACKS[i] = ItemStack.EMPTY;
		}
		cooldownTicks = 0;
		recentUseOrAttackTicks = 0;
		recentDropTicks = 0;
		warningCooldownTicks = 0;
		refillUseInputBlockTicks = 0;
		clearPendingRefill();
	}
}
