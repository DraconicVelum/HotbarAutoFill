package com.draconicvelum.hotbarautofill;

import net.neoforged.neoforge.common.ModConfigSpec;

final class HotbarAutoFillConfig {
	private static final boolean DEFAULT_REFILL_FROM_OTHER_HOTBAR_SLOTS = true;
	private static final boolean DEFAULT_PREVENT_TOOL_BREAKING = false;
	private static final boolean DEFAULT_SHOW_HELD_ITEM_TOTAL_COUNTER = true;

	static final ModConfigSpec CLIENT_SPEC;

	private static final ModConfigSpec.BooleanValue REFILL_FROM_OTHER_HOTBAR_SLOTS;
	private static final ModConfigSpec.BooleanValue PREVENT_TOOL_BREAKING;
	private static final ModConfigSpec.BooleanValue SHOW_HELD_ITEM_TOTAL_COUNTER;

	static {
		ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
		REFILL_FROM_OTHER_HOTBAR_SLOTS = builder
				.comment("Refill matching hotbar stacks from other hotbar slots before using the main inventory.")
				.translation("hotbarautofill.configuration.refill_from_other_hotbar_slots")
				.define("refillFromOtherHotbarSlots", DEFAULT_REFILL_FROM_OTHER_HOTBAR_SLOTS);
		PREVENT_TOOL_BREAKING = builder
				.comment("Move tools out of the selected slot before they break.")
				.translation("hotbarautofill.configuration.prevent_tool_breaking")
				.define("preventToolBreaking", DEFAULT_PREVENT_TOOL_BREAKING);
		SHOW_HELD_ITEM_TOTAL_COUNTER = builder
				.comment("Show the total count for the held item above the hotbar.")
				.translation("hotbarautofill.configuration.show_held_item_total_counter")
				.define("showHeldItemTotalCounter", DEFAULT_SHOW_HELD_ITEM_TOTAL_COUNTER);
		CLIENT_SPEC = builder.build();
	}

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
		return new HotbarAutoFillConfig(
				REFILL_FROM_OTHER_HOTBAR_SLOTS.get(),
				PREVENT_TOOL_BREAKING.get(),
				SHOW_HELD_ITEM_TOTAL_COUNTER.get()
		);
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
}
