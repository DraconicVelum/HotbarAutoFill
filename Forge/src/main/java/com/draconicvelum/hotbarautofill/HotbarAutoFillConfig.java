package com.draconicvelum.hotbarautofill;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import net.minecraft.client.Minecraft;

final class HotbarAutoFillConfig {
	private static final String FILE_NAME = "hotbarautofill.properties";
	private static final String REFILL_FROM_OTHER_HOTBAR_SLOTS = "refillFromOtherHotbarSlots";
	private static final String PREVENT_TOOL_BREAKING = "preventToolBreaking";
	private static final String SHOW_HELD_ITEM_TOTAL_COUNTER = "showHeldItemTotalCounter";
	private static final boolean DEFAULT_REFILL_FROM_OTHER_HOTBAR_SLOTS = true;
	private static final boolean DEFAULT_PREVENT_TOOL_BREAKING = false;
	private static final boolean DEFAULT_SHOW_HELD_ITEM_TOTAL_COUNTER = true;

	private final boolean refillFromOtherHotbarSlots;
	private final boolean preventToolBreaking;
	private final boolean showHeldItemTotalCounter;

	private HotbarAutoFillConfig(
			boolean refillFromOtherHotbarSlots,
			boolean preventToolBreaking,
			boolean showHeldItemTotalCounter
	) {
		this.refillFromOtherHotbarSlots = refillFromOtherHotbarSlots;
		this.preventToolBreaking = preventToolBreaking;
		this.showHeldItemTotalCounter = showHeldItemTotalCounter;
	}

	static HotbarAutoFillConfig load() {
		File configFile = configFile();
		Properties properties = new Properties();

		if (configFile.isFile()) {
			try (InputStream input = new FileInputStream(configFile)) {
				properties.load(input);
			} catch (IOException ignored) {
				return defaults();
			}
		}

		boolean refillFromOtherHotbarSlots = Boolean.parseBoolean(properties.getProperty(
				REFILL_FROM_OTHER_HOTBAR_SLOTS,
				Boolean.toString(DEFAULT_REFILL_FROM_OTHER_HOTBAR_SLOTS)
		));
		boolean preventToolBreaking = Boolean.parseBoolean(properties.getProperty(
				PREVENT_TOOL_BREAKING,
				Boolean.toString(DEFAULT_PREVENT_TOOL_BREAKING)
		));
		boolean showHeldItemTotalCounter = Boolean.parseBoolean(properties.getProperty(
				SHOW_HELD_ITEM_TOTAL_COUNTER,
				Boolean.toString(DEFAULT_SHOW_HELD_ITEM_TOTAL_COUNTER)
		));

		properties.setProperty(REFILL_FROM_OTHER_HOTBAR_SLOTS, Boolean.toString(refillFromOtherHotbarSlots));
		properties.setProperty(PREVENT_TOOL_BREAKING, Boolean.toString(preventToolBreaking));
		properties.setProperty(SHOW_HELD_ITEM_TOTAL_COUNTER, Boolean.toString(showHeldItemTotalCounter));
		saveDefaults(configFile, properties);
		return new HotbarAutoFillConfig(refillFromOtherHotbarSlots, preventToolBreaking, showHeldItemTotalCounter);
	}

	boolean refillFromOtherHotbarSlots() {
		return refillFromOtherHotbarSlots;
	}

	boolean preventToolBreaking() {
		return preventToolBreaking;
	}

	boolean showHeldItemTotalCounter() {
		return showHeldItemTotalCounter;
	}

	private static HotbarAutoFillConfig defaults() {
		return new HotbarAutoFillConfig(
				DEFAULT_REFILL_FROM_OTHER_HOTBAR_SLOTS,
				DEFAULT_PREVENT_TOOL_BREAKING,
				DEFAULT_SHOW_HELD_ITEM_TOTAL_COUNTER
		);
	}

	private static File configFile() {
		return new File(new File(Minecraft.getMinecraft().gameDir, "config"), FILE_NAME);
	}

	private static void saveDefaults(File configFile, Properties properties) {
		File parent = configFile.getParentFile();
		if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
			return;
		}

		try (OutputStream output = new FileOutputStream(configFile)) {
			properties.store(output, "Hotbar Auto Fill client config");
		} catch (IOException ignored) {
		}
	}
}
