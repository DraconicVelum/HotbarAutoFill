package com.draconicvelum.hotbarautofill;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public final class HotbarAutoFillModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return HotbarAutoFillConfigScreen::new;
	}
}
