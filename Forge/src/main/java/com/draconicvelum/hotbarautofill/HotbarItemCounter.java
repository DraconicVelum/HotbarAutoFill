package com.draconicvelum.hotbarautofill;

import cpw.mods.fml.client.FMLClientHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

final class HotbarItemCounter {
	private static final int RIGHT_HOTBAR_EDGE_OFFSET = 25;
	private static final RenderItem RENDER_ITEM = new RenderItem();
	private static HotbarAutoFillConfig config;

	private HotbarItemCounter() {
	}

	static void configure(HotbarAutoFillConfig config) {
		HotbarItemCounter.config = config;
	}

	static void render(ScaledResolution resolution) {
		if (config == null || !config.showHeldItemTotalCounter()) {
			return;
		}

		Minecraft client = Minecraft.getMinecraft();
		if (client.thePlayer == null || client.currentScreen != null) {
			return;
		}

		InventoryPlayer inventory = client.thePlayer.inventory;
		ItemStack selectedStack = normalize(inventory.getCurrentItem());
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
		int x = resolution.getScaledWidth() / 2 + 91 + RIGHT_HOTBAR_EDGE_OFFSET;
		int y = resolution.getScaledHeight() - 22;
		int textX = x + 20;
		int textY = y + 5;

		ItemStack iconStack = counterStack.copy();
		iconStack.stackSize = 1;

		GL11.glPushMatrix();
		RenderHelper.enableGUIStandardItemLighting();
		RENDER_ITEM.renderItemIntoGUI(
				client.fontRenderer,
				client.getTextureManager(),
				iconStack,
				x,
				y
		);
		RenderHelper.disableStandardItemLighting();
		GL11.glPopMatrix();

		client.fontRenderer.drawStringWithShadow(totalText, textX, textY, 0xFFFFFFFF);
		FMLClientHandler.instance().getClient().renderEngine.bindTexture(net.minecraft.client.gui.Gui.icons);
	}

	private static ItemStack getCounterStack(ItemStack selectedStack) {
		if (selectedStack.getItem() instanceof ItemBow) {
			return new ItemStack(Items.arrow);
		}

		return selectedStack;
	}

	private static int countHeldItem(InventoryPlayer inventory, ItemStack countedStack, boolean matchData) {
		int total = 0;
		for (int slot = 0; slot < inventory.mainInventory.length; slot++) {
			ItemStack stack = normalize(inventory.getStackInSlot(slot));
			if (isCountedStack(stack, countedStack, matchData)) {
				total += stack.stackSize;
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
					&& stack.getItemDamage() == countedStack.getItemDamage()
					&& ItemStack.areItemStackTagsEqual(stack, countedStack);
		}

		return stack.getItem() == countedStack.getItem();
	}

	private static ItemStack normalize(ItemStack stack) {
		return stack == null || stack.stackSize <= 0 ? null : stack;
	}
}
