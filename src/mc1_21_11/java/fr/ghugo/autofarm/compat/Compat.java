package fr.ghugo.autofarm.compat;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.function.Consumer;

/** Appels Minecraft / Fabric qui diffèrent selon la version : implémentation pour Minecraft 1.21.11. */
public final class Compat {
	private Compat() {
	}

	public static Screen screen(Minecraft mc) {
		return mc.screen;
	}

	public static void setScreen(Minecraft mc, Screen screen) {
		mc.setScreen(screen);
	}

	public static void title(Minecraft mc, Component title, Component subtitle) {
		mc.gui.setTitle(title);
		mc.gui.setSubtitle(subtitle);
	}

	public static void overlay(Minecraft mc, Component message) {
		mc.gui.setOverlayMessage(message, false);
	}

	public static void chat(Minecraft mc, Component message) {
		mc.gui.getChat().addMessage(message);
	}

	public static boolean hudHidden(Minecraft mc) {
		return mc.options.hideGui;
	}

	public static KeyMapping registerKey(KeyMapping key) {
		return KeyBindingHelper.registerKeyBinding(key);
	}

	/** N'accepte que les saisies correspondant à l'expression régulière ; {@code onChange} peut être null. */
	public static void restrictInput(EditBox box, String regex, Consumer<String> onChange) {
		box.setFilter(s -> s.matches(regex));
		if (onChange != null) {
			box.setResponder(onChange);
		}
	}

	public static void registerHud(String path, Consumer<Draw> renderer) {
		Minecraft mc = Minecraft.getInstance();
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("autofarm", path),
				(graphics, delta) -> renderer.accept(draw(graphics, mc.font)));
	}

	static Draw draw(GuiGraphics graphics, Font font) {
		return new Draw() {
			@Override
			public void text(String text, int x, int y, int color) {
				graphics.drawString(font, text, x, y, color);
			}

			@Override
			public void centered(String text, int x, int y, int color) {
				graphics.drawCenteredString(font, text, x, y, color);
			}

			@Override
			public void fill(int x1, int y1, int x2, int y2, int color) {
				graphics.fill(x1, y1, x2, y2, color);
			}

			@Override
			public int width(String text) {
				return font.width(text);
			}
		};
	}
}
