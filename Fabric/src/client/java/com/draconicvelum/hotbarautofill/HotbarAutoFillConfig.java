package com.draconicvelum.hotbarautofill;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.minecraft.client.Minecraft;

final class HotbarAutoFillConfig {
	private static final String FILE_NAME = "hotbarautofill.properties";
	private static final String REFILL_FROM_OTHER_HOTBAR_SLOTS = "refillFromOtherHotbarSlots";
	private static final String PREVENT_TOOL_BREAKING_WITHOUT_REPLACEMENT = "preventToolBreakingWithoutReplacement";
	private static final boolean DEFAULT_REFILL_FROM_OTHER_HOTBAR_SLOTS = true;
	private static final boolean DEFAULT_PREVENT_TOOL_BREAKING_WITHOUT_REPLACEMENT = false;

	private final boolean refillFromOtherHotbarSlots;
	private final boolean preventToolBreakingWithoutReplacement;

	private HotbarAutoFillConfig(boolean refillFromOtherHotbarSlots, boolean preventToolBreakingWithoutReplacement) {
		this.refillFromOtherHotbarSlots = refillFromOtherHotbarSlots;
		this.preventToolBreakingWithoutReplacement = preventToolBreakingWithoutReplacement;
	}

	static HotbarAutoFillConfig load() {
		Path configPath = Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve(FILE_NAME);
		Properties properties = new Properties();

		if (Files.exists(configPath)) {
			try (InputStream input = Files.newInputStream(configPath)) {
				properties.load(input);
			} catch (IOException ignored) {
				return new HotbarAutoFillConfig(
						DEFAULT_REFILL_FROM_OTHER_HOTBAR_SLOTS,
						DEFAULT_PREVENT_TOOL_BREAKING_WITHOUT_REPLACEMENT
				);
			}
		}

		boolean refillFromOtherHotbarSlots = Boolean.parseBoolean(
				properties.getProperty(
						REFILL_FROM_OTHER_HOTBAR_SLOTS,
						Boolean.toString(DEFAULT_REFILL_FROM_OTHER_HOTBAR_SLOTS)
				)
		);
		boolean preventToolBreakingWithoutReplacement = Boolean.parseBoolean(
				properties.getProperty(
						PREVENT_TOOL_BREAKING_WITHOUT_REPLACEMENT,
						Boolean.toString(DEFAULT_PREVENT_TOOL_BREAKING_WITHOUT_REPLACEMENT)
				)
		);

		properties.setProperty(REFILL_FROM_OTHER_HOTBAR_SLOTS, Boolean.toString(refillFromOtherHotbarSlots));
		properties.setProperty(
				PREVENT_TOOL_BREAKING_WITHOUT_REPLACEMENT,
				Boolean.toString(preventToolBreakingWithoutReplacement)
		);
		saveDefaults(configPath, properties);
		return new HotbarAutoFillConfig(refillFromOtherHotbarSlots, preventToolBreakingWithoutReplacement);
	}

	boolean refillFromOtherHotbarSlots() {
		return refillFromOtherHotbarSlots;
	}

	boolean preventToolBreakingWithoutReplacement() {
		return preventToolBreakingWithoutReplacement;
	}

	private static void saveDefaults(Path configPath, Properties properties) {
		try {
			Files.createDirectories(configPath.getParent());
			try (OutputStream output = Files.newOutputStream(configPath)) {
				properties.store(output, "Hotbar Auto Fill client config");
			}
		} catch (IOException ignored) {
		}
	}
}
