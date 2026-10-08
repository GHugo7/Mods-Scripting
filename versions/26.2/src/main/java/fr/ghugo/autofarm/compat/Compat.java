package fr.ghugo.autofarm.compat;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.function.Consumer;

/** Appels Minecraft / Fabric qui diffèrent selon la version : implémentation pour Minecraft 26.2. */
public final class Compat {
	private Compat() {
	}

	public static Screen screen(Minecraft mc) {
		return mc.gui.screen();
	}

	public static void setScreen(Minecraft mc, Screen screen) {
		mc.gui.setScreen(screen);
	}

	public static void title(Minecraft mc, Component title, Component subtitle) {
		mc.gui.hud.setTitle(title);
		mc.gui.hud.setSubtitle(subtitle);
	}

	public static void overlay(Minecraft mc, Component message) {
		mc.gui.hud.setOverlayMessage(message, false);
	}

	public static void chat(Minecraft mc, Component message) {
		mc.gui.hud.getChat().addClientSystemMessage(message);
	}

	public static boolean hudHidden(Minecraft mc) {
		return mc.gui.hud.isHidden();
	}

	public static KeyMapping registerKey(KeyMapping key) {
		return KeyMappingHelper.registerKeyMapping(key);
	}

	/**
	 * N'accepte que les saisies correspondant à l'expression régulière (EditBox n'a plus de filtre en 26.2 :
	 * une saisie invalide est annulée) ; {@code onChange} peut être null.
	 */
	public static void restrictInput(EditBox box, String regex, Consumer<String> onChange) {
		String[] lastValid = {box.getValue().matches(regex) ? box.getValue() : ""};
		box.setResponder(s -> {
			if (!s.matches(regex)) {
				box.setValue(lastValid[0]);
				return;
			}
			lastValid[0] = s;
			if (onChange != null) {
				onChange.accept(s);
			}
		});
	}

	public static void registerHud(String path, Consumer<Draw> renderer) {
		Minecraft mc = Minecraft.getInstance();
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("autofarm", path),
				(graphics, delta) -> renderer.accept(draw(graphics, mc.font)));
	}

	static Draw draw(GuiGraphicsExtractor graphics, Font font) {
		return new Draw() {
			@Override
			public void text(String text, int x, int y, int color) {
				graphics.text(font, text, x, y, color);
			}

			@Override
			public void centered(String text, int x, int y, int color) {
				graphics.centeredText(font, text, x, y, color);
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
