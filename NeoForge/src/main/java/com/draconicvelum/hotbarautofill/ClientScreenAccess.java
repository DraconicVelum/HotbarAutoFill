package com.draconicvelum.hotbarautofill;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.minecraft.client.Minecraft;

final class ClientScreenAccess {
	private static final Method GUI_SCREEN_METHOD = findGuiScreenMethod();
	private static final Field MINECRAFT_SCREEN_FIELD = findMinecraftScreenField();

	private ClientScreenAccess() {
	}

	static boolean hasScreen(Minecraft client) {
		if (GUI_SCREEN_METHOD != null) {
			try {
				return GUI_SCREEN_METHOD.invoke(client.gui) != null;
			} catch (ReflectiveOperationException ignored) {
			}
		}

		if (MINECRAFT_SCREEN_FIELD != null) {
			try {
				return MINECRAFT_SCREEN_FIELD.get(client) != null;
			} catch (ReflectiveOperationException ignored) {
			}
		}

		return false;
	}

	private static Method findGuiScreenMethod() {
		try {
			return clientGuiClass().getMethod("screen");
		} catch (ReflectiveOperationException ignored) {
			return null;
		}
	}

	private static Field findMinecraftScreenField() {
		try {
			return Minecraft.class.getField("screen");
		} catch (ReflectiveOperationException ignored) {
			return null;
		}
	}

	private static Class<?> clientGuiClass() throws ClassNotFoundException {
		return Class.forName("net.minecraft.client.gui.Gui");
	}
}
